/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.internal.audio

import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.annotation.RequiresApi
import com.amazonaws.services.chime.sdk.meetings.device.MediaDevice
import com.amazonaws.services.chime.sdk.meetings.device.MediaDeviceType
import com.amazonaws.services.chime.sdk.meetings.utils.logger.Logger

/**
 * [CommunicationDeviceBluetoothAudioRouter] handles Bluetooth audio routing for API 31+ using
 * the Communication Device API.
 *
 * This implementation uses [AudioManager.setCommunicationDevice] to route audio to Bluetooth
 * devices and monitors [AudioManager.OnCommunicationDeviceChangedListener] to track device changes.
 *
 * The routing operation is asynchronous:
 * - Success is reported when the listener confirms a Bluetooth SCO device
 * - Failure is reported when max retries are exceeded, device becomes unavailable,
 *   or when the operation times out after [BluetoothRoutingConfig.COMMUNICATION_DEVICE_TIMEOUT_MS]
 *
 * Implements the "retry-on-mismatch" pattern to handle race conditions during rapid
 * Bluetooth device transitions. When setCommunicationDevice() returns true but the
 * listener fires with a different device (e.g., Handset instead of Bluetooth), this
 * indicates the Bluetooth SCO channel from the previous device is still being torn down.
 *
 * @param context The application context for accessing system services
 * @param audioManager The AudioManager for controlling communication devices
 * @param logger The logger for debug and error messages
 * @param handler The handler for posting timeout and retry callbacks (defaults to main looper)
 */
@RequiresApi(Build.VERSION_CODES.S)
internal class CommunicationDeviceBluetoothAudioRouter(
    private val context: Context,
    private val audioManager: AudioManager,
    private val logger: Logger,
    private val handler: Handler = Handler(Looper.getMainLooper())
) : BluetoothAudioRouter {

    private val TAG = "CommunicationDeviceBluetoothAudioRouter"

    // Pending Bluetooth operation state for deferred routing
    private var pendingOperation: PendingBluetoothOperation? = null
    private var timeoutRunnable: Runnable? = null

    // Retry state for Bluetooth routing mismatch recovery
    private var retryCount: Int = 0
    private var retryRunnable: Runnable? = null

    // Communication device change listener
    private var communicationDeviceChangedListener: AudioManager.OnCommunicationDeviceChangedListener? = null

    init {
        setupCommunicationDeviceListener()
    }

    /**
     * Sets up the OnCommunicationDeviceChangedListener for monitoring device changes.
     */
    private fun setupCommunicationDeviceListener() {
        communicationDeviceChangedListener = AudioManager.OnCommunicationDeviceChangedListener { device ->
            logger.debug(TAG, "Communication device changed: ${device?.productName}, type=${device?.type}, id=${device?.id}")
            onCommunicationDeviceChanged(device)
        }
        audioManager.addOnCommunicationDeviceChangedListener(
            context.mainExecutor,
            communicationDeviceChangedListener!!
        )
    }

    override fun routeToBluetoothDevice(
        mediaDevice: MediaDevice,
        route: Int,
        onSuccess: (route: Int, deviceType: MediaDeviceType) -> Unit,
        onFailure: () -> Unit
    ) {
        logger.info(TAG, "routeToBluetoothDevice() called for device: ${mediaDevice.id}, type: ${mediaDevice.type}")

        // Cancel any existing pending operation
        cancelPendingOperation()

        // Find the AudioDeviceInfo by ID from available communication devices
        val audioDeviceInfo = findAudioDeviceById(mediaDevice.id)

        if (audioDeviceInfo == null) {
            logger.error(TAG, "Failed to route to Bluetooth device. AudioDeviceInfo not found for id: ${mediaDevice.id}")
            onFailure()
            return
        }

        // Call setCommunicationDevice
        val success = audioManager.setCommunicationDevice(audioDeviceInfo)
        logger.info(TAG, "setCommunicationDevice(${audioDeviceInfo.productName}, id=${audioDeviceInfo.id}) returned $success")

        if (!success) {
            logger.error(TAG, "setCommunicationDevice failed for device: ${mediaDevice.id}")
            onFailure()
            return
        }

        // Store pending operation with callbacks
        pendingOperation = PendingBluetoothOperation(
            route = route,
            deviceType = mediaDevice.type,
            deviceId = audioDeviceInfo.id,
            onSuccess = onSuccess,
            onFailure = onFailure
        )

        // Start timeout
        startTimeout()
        logger.info(TAG, "Bluetooth device selected - waiting for communication device change confirmation")
    }

    override fun cancelPendingOperation() {
        if (pendingOperation != null) {
            logger.debug(TAG, "Cancelling pending Bluetooth operation")
            pendingOperation = null
            retryCount = 0
            timeoutRunnable?.let { handler.removeCallbacks(it) }
            timeoutRunnable = null
            retryRunnable?.let { handler.removeCallbacks(it) }
            retryRunnable = null
        }
    }

    /**
     * Finds an AudioDeviceInfo by its ID from available communication devices.
     *
     * @param id The AudioDeviceInfo ID as a String (converted to Int for lookup)
     * @return The matching AudioDeviceInfo, or null if not found
     */
    private fun findAudioDeviceById(id: String?): AudioDeviceInfo? {
        val deviceId = id?.toIntOrNull() ?: return null
        return audioManager.availableCommunicationDevices.firstOrNull { it.id == deviceId }
    }

    /**
     * Starts a timeout for the pending Bluetooth operation.
     * If the operation does not complete within [BluetoothRoutingConfig.COMMUNICATION_DEVICE_TIMEOUT_MS],
     * the pending operation is cancelled and the failure callback is invoked.
     */
    private fun startTimeout() {
        timeoutRunnable?.let { handler.removeCallbacks(it) }
        timeoutRunnable = Runnable {
            // Check if audio client is stopped - if so, just cancel silently
            if (DefaultAudioClientController.audioClientState == AudioClientState.STOPPED) {
                logger.debug(TAG, "AudioClient is stopped. Cancelling pending bluetooth operations")
                cancelPendingOperation()
                return@Runnable
            }

            val pending = pendingOperation
            if (pending != null) {
                logger.warn(TAG, "Bluetooth communication device timeout after ${BluetoothRoutingConfig.COMMUNICATION_DEVICE_TIMEOUT_MS}ms")
                val onFailure = pending.onFailure
                cancelPendingOperation()
                onFailure()
            }
        }
        handler.postDelayed(timeoutRunnable!!, BluetoothRoutingConfig.COMMUNICATION_DEVICE_TIMEOUT_MS)
    }

    /**
     * Handles communication device change events for Bluetooth routing.
     *
     * Implements the "retry-on-mismatch" pattern to handle race conditions during rapid
     * Bluetooth device transitions. When setCommunicationDevice() returns true but the
     * listener fires with a different device (e.g., Handset instead of Bluetooth), this
     * indicates the Bluetooth SCO channel from the previous device is still being torn down.
     *
     * In this case, we retry the setCommunicationDevice() call after a short delay to allow
     * the Bluetooth stack to release the audio hardware lock.
     *
     * @param device The new communication device, or null if cleared
     */
    private fun onCommunicationDeviceChanged(device: AudioDeviceInfo?) {
        // Check if audio client is stopped - if so, just cancel silently
        if (DefaultAudioClientController.audioClientState == AudioClientState.STOPPED) {
            logger.debug(TAG, "AudioClient is stopped. Cancelling pending bluetooth operations")
            cancelPendingOperation()
            return
        }

        val pending = pendingOperation ?: return
        val pendingDeviceId = pending.deviceId ?: return

        if (device != null && device.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO) {
            // Success: Bluetooth SCO device confirmed
            val waitTime = System.currentTimeMillis() - pending.timestamp
            logger.info(TAG, "Bluetooth communication device confirmed with id: ${device.id} after ${waitTime}ms" +
                    " (retries: $retryCount) - invoking success callback")
            val onSuccess = pending.onSuccess
            val route = pending.route
            val deviceType = pending.deviceType
            cancelPendingOperation()
            onSuccess(route, deviceType)
        } else {
            // MISMATCH: We got Handset/Speaker instead of Bluetooth
            // This may happen during rapid Bluetooth device transitions when the SCO channel
            // from the previous device is still being torn down
            if (retryCount < BluetoothRoutingConfig.BLUETOOTH_ROUTING_MISMATCH_MAX_RETRIES) {
                retryCount++
                logger.info(TAG, "Bluetooth routing mismatch. Expected Bluetooth (id=$pendingDeviceId), " +
                        "got ${device?.id} (type=${device?.type}). Retrying ($retryCount/${BluetoothRoutingConfig.BLUETOOTH_ROUTING_MISMATCH_MAX_RETRIES}) " +
                        "after ${BluetoothRoutingConfig.BLUETOOTH_ROUTING_MISMATCH_RETRY_DELAY_MS}ms...")

                // Cancel any existing retry runnable
                retryRunnable?.let { handler.removeCallbacks(it) }

                // Schedule a retry after a short delay
                retryRunnable = Runnable {
                    retryBluetoothRouting(pendingDeviceId)
                }
                handler.postDelayed(retryRunnable!!, BluetoothRoutingConfig.BLUETOOTH_ROUTING_MISMATCH_RETRY_DELAY_MS)
            } else {
                // Max retries exceeded - invoke failure callback
                logger.error(TAG, "Failed to route to Bluetooth device (id=$pendingDeviceId) after ${BluetoothRoutingConfig.BLUETOOTH_ROUTING_MISMATCH_MAX_RETRIES} retries. Giving up.")
                val onFailure = pending.onFailure
                cancelPendingOperation()
                onFailure()
            }
        }
    }

    /**
     * Retries setting the communication device to Bluetooth after a mismatch.
     *
     * Re-verifies that the target device is still available before retrying to handle
     * cases where the device may have disconnected during the retry delay.
     *
     * @param targetDeviceId The AudioDeviceInfo ID of the target Bluetooth device
     */
    private fun retryBluetoothRouting(targetDeviceId: Int) {
        // Check if audio client is stopped - if so, just cancel silently
        if (DefaultAudioClientController.audioClientState == AudioClientState.STOPPED) {
            logger.debug(TAG, "AudioClient is stopped. Cancelling pending bluetooth operations")
            cancelPendingOperation()
            return
        }

        val pending = pendingOperation
        if (pending == null) {
            logger.debug(TAG, "No pending operation during retry. Aborting.")
            return
        }

        // Re-verify device is still available before retrying
        val audioDeviceInfo = audioManager.availableCommunicationDevices.firstOrNull { it.id == targetDeviceId }

        if (audioDeviceInfo == null) {
            logger.warn(TAG, "Target Bluetooth device (id=$targetDeviceId) no longer available. Aborting retry.")
            val onFailure = pending.onFailure
            cancelPendingOperation()
            onFailure()
            return
        }

        logger.info(TAG, "Retrying setCommunicationDevice(${audioDeviceInfo.productName}, id=${audioDeviceInfo.id})")
        val success = audioManager.setCommunicationDevice(audioDeviceInfo)
        logger.info(TAG, "Retry setCommunicationDevice returned $success")

        if (!success) {
            logger.error(TAG, "Retry setCommunicationDevice failed. Giving up.")
            val onFailure = pending.onFailure
            cancelPendingOperation()
            onFailure()
        }
        // If success, wait for the next OnCommunicationDeviceChangedListener callback
    }
}
