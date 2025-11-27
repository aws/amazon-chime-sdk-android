/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.internal.audio

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import com.amazonaws.services.chime.sdk.meetings.device.MediaDevice
import com.amazonaws.services.chime.sdk.meetings.device.MediaDeviceType
import com.amazonaws.services.chime.sdk.meetings.utils.logger.Logger

/**
 * [ScoBluetoothAudioRouter] handles Bluetooth audio routing for API 23-30 using SCO-based APIs.
 *
 * This implementation uses [AudioManager.startBluetoothSco] to initiate Bluetooth SCO connection
 * and monitors [AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED] broadcasts to track connection state.
 *
 * The routing operation is asynchronous:
 * - Success is reported when SCO state transitions to [AudioManager.SCO_AUDIO_STATE_CONNECTED]
 * - Failure is reported when SCO state transitions to ERROR, or DISCONNECTED from CONNECTING,
 *   or when the operation times out after [BluetoothRoutingConfig.SCO_CONNECTION_TIMEOUT_MS]
 *
 * @param context The application context for registering broadcast receivers
 * @param audioManager The AudioManager for controlling Bluetooth SCO
 * @param logger The logger for debug and error messages
 * @param handler The handler for posting timeout callbacks (defaults to main looper)
 */
internal class ScoBluetoothAudioRouter(
    private val context: Context,
    private val audioManager: AudioManager,
    private val logger: Logger,
    private val handler: Handler = Handler(Looper.getMainLooper())
) : BluetoothAudioRouter {

    private val TAG = "ScoBluetoothAudioRouter"

    // Pending Bluetooth operation state for deferred routing
    private var pendingOperation: PendingBluetoothOperation? = null
    private var timeoutRunnable: Runnable? = null

    // Track last SCO state to distinguish device switching from connection failure
    private var lastScoState: Int = AudioManager.SCO_AUDIO_STATE_DISCONNECTED

    // BroadcastReceiver for SCO state changes
    private var scoStateReceiver: BroadcastReceiver? = null

    init {
        setupScoStateReceiver()
    }

    /**
     * Sets up the BroadcastReceiver for ACTION_SCO_AUDIO_STATE_UPDATED.
     */
    private fun setupScoStateReceiver() {
        scoStateReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val state = intent?.getIntExtra(AudioManager.EXTRA_SCO_AUDIO_STATE, -1) ?: return
                val previousState = intent.getIntExtra(AudioManager.EXTRA_SCO_AUDIO_PREVIOUS_STATE, -1)
                onScoStateChanged(state, previousState)
            }
        }

        context.registerReceiver(
            scoStateReceiver,
            IntentFilter(AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED)
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

        // Start Bluetooth SCO connection
        audioManager.apply {
            mode = AudioManager.MODE_IN_COMMUNICATION
            isSpeakerphoneOn = false
            startBluetoothSco()
            isBluetoothScoOn = true
        }

        // Store pending operation with callbacks
        pendingOperation = PendingBluetoothOperation(
            route = route,
            deviceType = mediaDevice.type,
            deviceId = null, // Not used for SCO-based routing
            onSuccess = onSuccess,
            onFailure = onFailure
        )

        // Start timeout
        startTimeout()
        logger.info(TAG, "Bluetooth SCO started, waiting for connection confirmation")
    }

    override fun cancelPendingOperation() {
        if (pendingOperation != null) {
            logger.debug(TAG, "Cancelling pending Bluetooth operation")
            pendingOperation = null
            timeoutRunnable?.let { handler.removeCallbacks(it) }
            timeoutRunnable = null
        }
    }

    /**
     * Starts a timeout for the pending Bluetooth operation.
     * If the operation does not complete within [BluetoothRoutingConfig.SCO_CONNECTION_TIMEOUT_MS],
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
                logger.warn(TAG, "Bluetooth SCO connection timeout after ${BluetoothRoutingConfig.SCO_CONNECTION_TIMEOUT_MS}ms")
                val onFailure = pending.onFailure
                cancelPendingOperation()
                onFailure()
            }
        }
        handler.postDelayed(timeoutRunnable!!, BluetoothRoutingConfig.SCO_CONNECTION_TIMEOUT_MS)
    }

    /**
     * Handles SCO state transitions for pending Bluetooth operations.
     *
     * When SCO reaches CONNECTED state and there's a pending Bluetooth operation,
     * the success callback is invoked.
     *
     * When SCO transitions to DISCONNECTED from CONNECTING (connection attempt failed),
     * the failure callback is invoked. However, DISCONNECTED from CONNECTED is
     * expected during Bluetooth device switching and should NOT trigger failure.
     *
     * @param state The new SCO audio state
     * @param previousState The previous SCO audio state
     */
    private fun onScoStateChanged(state: Int, previousState: Int) {
        // Check if audio client is stopped - if so, just cancel silently
        if (DefaultAudioClientController.audioClientState == AudioClientState.STOPPED) {
            logger.debug(TAG, "AudioClient is stopped. Cancelling pending bluetooth operations")
            cancelPendingOperation()
            lastScoState = state
            return
        }

        val pending = pendingOperation ?: run {
            lastScoState = state
            return
        }

        when (state) {
            AudioManager.SCO_AUDIO_STATE_CONNECTED -> {
                val timeToConnect = System.currentTimeMillis() - pending.timestamp
                logger.info(TAG, "Bluetooth SCO connected in ${timeToConnect}ms, invoking success callback")
                val onSuccess = pending.onSuccess
                val route = pending.route
                val deviceType = pending.deviceType
                cancelPendingOperation()
                onSuccess(route, deviceType)
            }
            AudioManager.SCO_AUDIO_STATE_DISCONNECTED -> {
                // Only treat as failure if transitioning from CONNECTING (connection attempt failed)
                // CONNECTED → DISCONNECTED is expected during Bluetooth device switching
                if (previousState == AudioManager.SCO_AUDIO_STATE_CONNECTING) {
                    logger.warn(TAG, "Bluetooth SCO connection failed (DISCONNECTED from CONNECTING)")
                    val onFailure = pending.onFailure
                    cancelPendingOperation()
                    onFailure()
                }
            }
            AudioManager.SCO_AUDIO_STATE_ERROR -> {
                logger.warn(TAG, "Bluetooth SCO error")
                val onFailure = pending.onFailure
                cancelPendingOperation()
                onFailure()
            }
        }

        lastScoState = state
    }
}
