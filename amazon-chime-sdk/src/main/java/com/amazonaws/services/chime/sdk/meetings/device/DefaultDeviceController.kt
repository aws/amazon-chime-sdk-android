/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.device

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.annotation.RequiresApi
import androidx.annotation.VisibleForTesting
import com.amazonaws.services.chime.sdk.meetings.analytics.EventAnalyticsController
import com.amazonaws.services.chime.sdk.meetings.analytics.EventAttributeName
import com.amazonaws.services.chime.sdk.meetings.analytics.EventName
import com.amazonaws.services.chime.sdk.meetings.analytics.MeetingHistoryEventName
import com.amazonaws.services.chime.sdk.meetings.internal.audio.AudioClientController
import com.amazonaws.services.chime.sdk.meetings.internal.audio.AudioClientState
import com.amazonaws.services.chime.sdk.meetings.internal.audio.DefaultAudioClientController
import com.amazonaws.services.chime.sdk.meetings.internal.utils.ConcurrentSet
import com.amazonaws.services.chime.sdk.meetings.internal.utils.ObserverUtils
import com.amazonaws.services.chime.sdk.meetings.internal.video.VideoClientController
import com.amazonaws.services.chime.sdk.meetings.utils.MediaError
import com.amazonaws.services.chime.sdk.meetings.utils.logger.Logger
import com.xodee.client.audio.audioclient.AudioClient

class DefaultDeviceController(
    private val context: Context,
    private val audioClientController: AudioClientController,
    private val videoClientController: VideoClientController,
    private val eventAnalyticsController: EventAnalyticsController,
    private val logger: Logger,
    private val audioManager: AudioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager,
    private val buildVersion: Int = Build.VERSION.SDK_INT
) : DeviceController {
    private val deviceChangeObservers = ConcurrentSet.createConcurrentSet<DeviceChangeObserver>()

    // TODO: remove code blocks for lower API level after the minimum SDK version becomes 23
    private val AUDIO_MANAGER_API_LEVEL = 23

    // API level constant for setCommunicationDevice support (Android 12+)
    private val COMMUNICATION_DEVICE_API_LEVEL = Build.VERSION_CODES.S

    private var receiver: BroadcastReceiver? = null

    private var audioDeviceCallback: AudioDeviceCallback? = null

    private var scoStateReceiver: BroadcastReceiver? = null

    // Pending Bluetooth route state for deferred routing
    private var pendingBluetoothRoute: Int? = null
    private var pendingBluetoothTimestamp: Long = 0L

    // Communication device change listener for API 31+ Bluetooth routing
    private var communicationDeviceChangedListener: AudioManager.OnCommunicationDeviceChangedListener? = null

    // Pending Bluetooth device ID for API 31+ retry-on-mismatch pattern
    private var pendingBluetoothDeviceId: Int? = null

    // Retry counter for Bluetooth routing mismatch recovery (API 31+)
    private var bluetoothRoutingMismatchRetryCount: Int = 0

    // Track last SCO state to distinguish device switching from connection failure
    private var lastScoState: Int = AudioManager.SCO_AUDIO_STATE_DISCONNECTED

    // Timeout handler for pending Bluetooth operations
    private val handler = Handler(Looper.getMainLooper())
    private var timeoutRunnable: Runnable? = null
    private var retryRunnable: Runnable? = null

    // Timeout duration constant for SCO connection (API 30-)
    // This timeout is just a SWAG and can be tuned further if needed.
    private val SCO_CONNECTION_TIMEOUT_MS = 3000L

    // Timeout duration for API 31+ Bluetooth device switching
    // Android documentation recommends up to 30 seconds for Bluetooth device changes
    // See: https://developer.android.com/develop/connectivity/bluetooth/ble-audio/audio-manager#set_communication_device
    // Since 30 seconds is too long for a voice call, setting it to 5 seconds instead
    private val COMMUNICATION_DEVICE_TIMEOUT_MS = 5000L

    // Retry configuration for Bluetooth routing mismatch recovery (API 31+)
    // When setCommunicationDevice() returns true but the listener fires with a different device,
    // we retry after a delay to allow the Bluetooth SCO teardown to complete
    private val BLUETOOTH_ROUTING_MISMATCH_RETRY_DELAY_MS = 500L
    private val BLUETOOTH_ROUTING_MISMATCH_MAX_RETRIES = 3

    private val TAG = "DefaultDeviceController"

    init {
        if (buildVersion >= AUDIO_MANAGER_API_LEVEL) {
            audioDeviceCallback = object : AudioDeviceCallback() {
                override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>?) {
                    notifyAudioDeviceChange()
                }

                override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>?) {
                    notifyAudioDeviceChange()
                }
            }
            audioManager.registerAudioDeviceCallback(audioDeviceCallback, null)
        } else {
            receiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    // There is gap between notification and audioManager recognizing bluetooth devices
                    if (intent?.action == BluetoothDevice.ACTION_ACL_CONNECTED) {
                        Thread.sleep(1000)
                    }
                    notifyAudioDeviceChange()
                }
            }
            context.registerReceiver(receiver, IntentFilter(Intent.ACTION_HEADSET_PLUG))
            context.registerReceiver(receiver, IntentFilter(BluetoothDevice.ACTION_ACL_CONNECTED))
            context.registerReceiver(
                receiver, IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECTED)
            )
        }

        // Register communication device listener for API 31+ Bluetooth routing
        if (buildVersion >= COMMUNICATION_DEVICE_API_LEVEL) {
            communicationDeviceChangedListener = AudioManager.OnCommunicationDeviceChangedListener { device ->
                logger.info(TAG, "Communication device changed: ${device?.productName}, type=${device?.type}")
                onCommunicationDeviceChanged(device)
            }
            audioManager.addOnCommunicationDeviceChangedListener(
                context.mainExecutor,
                communicationDeviceChangedListener!!
            )
        } else {
            // Register Bluetooth SCO state receiver to handle pending Bluetooth route operations for API level < 31
            scoStateReceiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    val state =
                        intent?.getIntExtra(AudioManager.EXTRA_SCO_AUDIO_STATE, -1) ?: return
                    val previousState =
                        intent.getIntExtra(AudioManager.EXTRA_SCO_AUDIO_PREVIOUS_STATE, -1)
                    onScoStateChanged(state, previousState)
                }
            }

            context.registerReceiver(
                scoStateReceiver,
                IntentFilter(AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED)
            )
        }
    }

    override fun listAudioDevices(): List<MediaDevice> {
        @SuppressLint("NewApi")
        if (buildVersion >= AUDIO_MANAGER_API_LEVEL) {
            var isWiredHeadsetOn = false
            var isHandsetAvailable = false
            val handsetDevicesInfo = setOf(
                AudioDeviceInfo.TYPE_BUILTIN_EARPIECE,
                AudioDeviceInfo.TYPE_TELEPHONY
            )

            val audioDevices = mutableListOf<MediaDevice>()
            var wiredDeviceCount = 0
            for (device in audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)) {
                // System will select wired headset over receiver
                // so we want to filter receiver out when wired headset is connected
                if (device.type == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
                    device.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
                    device.type == AudioDeviceInfo.TYPE_USB_HEADSET
                ) {
                    isWiredHeadsetOn = true
                    wiredDeviceCount++
                }

                // Return only one handset device to avoid confusion
                if (handsetDevicesInfo.contains(device.type)) {
                    if (isHandsetAvailable) continue
                    else {
                        isHandsetAvailable = true
                    }
                }

                if (device.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP) continue

                audioDevices.add(
                    MediaDevice(
                        "${device.productName} (${getReadableType(device.type)})",
                        MediaDeviceType.fromAudioDeviceInfo(
                            device.type
                        ),
                        id = device.id.toString()
                    )
                )
            }
            if (audioDevices.isEmpty()) {
                val attributes = mutableMapOf<EventAttributeName, Any>(
                    EventAttributeName.audioInputErrorMessage to MediaError.NoAudioDevices
                )
                eventAnalyticsController.publishEvent(EventName.audioInputFailed, attributes)
                logger.error(TAG, "List audio devices failed: ${MediaError.NoAudioDevices}")
            }
            // It doesn't look like Android can switch between two wired connection, so we'll assume WIRED_HEADSET
            // is where audio is routed.
            if (wiredDeviceCount > 1) audioDevices.removeIf { it.type == MediaDeviceType.AUDIO_USB_HEADSET }
            val finalDevices = if (isWiredHeadsetOn) audioDevices.filter { it.type != MediaDeviceType.AUDIO_HANDSET } else audioDevices
            return finalDevices
        } else {
            val res = mutableListOf<MediaDevice>()
            res.add(
                MediaDevice(
                    getReadableType(AudioDeviceInfo.TYPE_BUILTIN_SPEAKER),
                    MediaDeviceType.AUDIO_BUILTIN_SPEAKER
                )
            )
            if (audioManager.isWiredHeadsetOn) {
                res.add(
                    MediaDevice(
                        getReadableType(AudioDeviceInfo.TYPE_WIRED_HEADSET),
                        MediaDeviceType.AUDIO_WIRED_HEADSET
                    )
                )
            } else {
                res.add(
                    MediaDevice(
                        getReadableType(AudioDeviceInfo.TYPE_BUILTIN_EARPIECE),
                        MediaDeviceType.AUDIO_HANDSET
                    )
                )
            }
            if (audioManager.isBluetoothScoOn) {
                res.add(
                    MediaDevice(
                        getReadableType(AudioDeviceInfo.TYPE_BLUETOOTH_SCO),
                        MediaDeviceType.AUDIO_BLUETOOTH
                    )
                )
            }
            return res
        }
    }

    @SuppressLint("NewApi")
    override fun chooseAudioDevice(mediaDevice: MediaDevice) {
        if (DefaultAudioClientController.audioClientState != AudioClientState.STARTED) {
            return
        }

        logger.info(TAG, "chooseAudioDevice() called for device: ${mediaDevice.label} with type: ${mediaDevice.type}")

        // Cancel any pending Bluetooth operation
        cancelPendingBluetoothOperation()

        val route = getRouteForDeviceType(mediaDevice.type)

        if (buildVersion >= COMMUNICATION_DEVICE_API_LEVEL) {
            // API 31+: Uses setCommunicationDevice
            val success = setupAudioDevice(mediaDevice)
            if (success) {
                if (mediaDevice.type == MediaDeviceType.AUDIO_BLUETOOTH) {
                    // Defer setRoute() until OnCommunicationDeviceChangedListener confirms Bluetooth device is ready
                    pendingBluetoothRoute = route
                    pendingBluetoothTimestamp = System.currentTimeMillis()
                    startBluetoothTimeout()
                    logger.info(TAG, "Bluetooth selected - deferring setRoute($route) until device change confirmed")
                } else {
                    // Non-Bluetooth: execute setRoute() immediately
                    executeSetRoute(route)
                }
            } else {
                notifyAudioDeviceChange()
            }
        } else {
            // API 23-30: Use legacy APIs with SCO-aware routing for Bluetooth
            setupAudioDevice(mediaDevice.type)

            if (mediaDevice.type == MediaDeviceType.AUDIO_BLUETOOTH) {
                // Defer setRoute() until SCO state is CONNECTED
                pendingBluetoothRoute = route
                pendingBluetoothTimestamp = System.currentTimeMillis()
                startBluetoothTimeout()
                logger.info(TAG, "Bluetooth device selected, deferring route change until SCO connected")
            } else {
                // Non-Bluetooth: call setRoute() immediately
                executeSetRoute(route)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    override fun getActiveAudioDevice(): MediaDevice? {
        if (buildVersion >= Build.VERSION_CODES.N) {
            if (audioManager.activeRecordingConfigurations.isNotEmpty()) {
                val device =
                    audioManager.activeRecordingConfigurations.firstOrNull { config -> config.audioDevice != null }?.audioDevice
                if (device != null) {
                    val type = device.type
                    var mediaDeviceType: MediaDeviceType = MediaDeviceType.fromAudioDeviceInfo(type)
                    if (type == AudioDeviceInfo.TYPE_BUILTIN_MIC) {
                        // Built-in mic has two case speaker, built-in receiver
                        mediaDeviceType = if (audioManager.isSpeakerphoneOn) {
                            MediaDeviceType.fromAudioDeviceInfo(AudioDeviceInfo.TYPE_BUILTIN_SPEAKER)
                        } else {
                            MediaDeviceType.fromAudioDeviceInfo(AudioDeviceInfo.TYPE_BUILTIN_EARPIECE)
                        }
                    }
                    return listAudioDevices().firstOrNull {
                        it.type == mediaDeviceType
                    }
                } else {
                    logger.info(TAG, "getActiveAudioDevice() audioDevice is null. isSpeakerPhoneOn is ${audioManager.isSpeakerphoneOn}")
                }

                // Some android devices doesn't have audio device for speaker
                if (audioManager.isSpeakerphoneOn) return listAudioDevices().firstOrNull {
                    it.type == MediaDeviceType.AUDIO_BUILTIN_SPEAKER
                }
            }
        }
        return null
    }

    private fun setupAudioDevice(type: MediaDeviceType) {
        when (type) {
            MediaDeviceType.AUDIO_BUILTIN_SPEAKER ->
                audioManager.apply {
                    // Sometimes stopBluetoothSco makes isSpeakerphoneOn to be false
                    // calling it before isSpeakerphoneOn
                    stopBluetoothSco()
                    mode = AudioManager.MODE_IN_COMMUNICATION
                    isBluetoothScoOn = false
                    isSpeakerphoneOn = true
                }
            MediaDeviceType.AUDIO_BLUETOOTH ->
                audioManager.apply {
                    mode = AudioManager.MODE_IN_COMMUNICATION
                    isSpeakerphoneOn = false
                    startBluetoothSco()
                    isBluetoothScoOn = true
                }
            else ->
                audioManager.apply {
                    stopBluetoothSco()
                    isBluetoothScoOn = false
                    mode = AudioManager.MODE_IN_COMMUNICATION
                    isSpeakerphoneOn = false
                }
        }
    }

    private fun getReadableType(type: Int): String {
        return when (type) {
            AudioDeviceInfo.TYPE_WIRED_HEADSET -> "Wired Headset"
            AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> "Speaker"
            AudioDeviceInfo.TYPE_WIRED_HEADPHONES -> "Wired Headphone"
            AudioDeviceInfo.TYPE_USB_HEADSET -> "USB Headset"
            AudioDeviceInfo.TYPE_BUILTIN_EARPIECE,
            AudioDeviceInfo.TYPE_TELEPHONY -> "Handset"
            AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
            AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> "Bluetooth"
            else -> "Unknown (AudioDeviceInfo: $type)"
        }
    }

    override fun getActiveCamera(): MediaDevice? {
        return videoClientController.getActiveCamera()
    }

    override fun switchCamera() {
        videoClientController.switchCamera()
    }

    override fun addDeviceChangeObserver(observer: DeviceChangeObserver) {
        deviceChangeObservers.add(observer)
    }

    override fun removeDeviceChangeObserver(observer: DeviceChangeObserver) {
        deviceChangeObservers.remove(observer)
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    fun notifyAudioDeviceChange() {
        ObserverUtils.notifyObserverOnMainThread(deviceChangeObservers) {
            it.onAudioDeviceChanged(
                listAudioDevices()
            )
        }
    }

    /**
     * Cancels any pending Bluetooth route operation and cleans up associated state.
     * Handles both API 23-30 (SCO-based) and API 31+ (setCommunicationDevice-based) pending operations.
     */
    private fun cancelPendingBluetoothOperation() {
        if (pendingBluetoothRoute != null || pendingBluetoothDeviceId != null) {
            pendingBluetoothRoute = null
            pendingBluetoothTimestamp = 0L
            pendingBluetoothDeviceId = null
            bluetoothRoutingMismatchRetryCount = 0
            timeoutRunnable?.let { handler.removeCallbacks(it) }
            timeoutRunnable = null
            retryRunnable?.let { handler.removeCallbacks(it) }
            retryRunnable = null
        }
    }

    /**
     * Starts a timeout for the pending Bluetooth operation.
     * Uses SCO_CONNECTION_TIMEOUT_MS for API 23-30, COMMUNICATION_DEVICE_TIMEOUT_MS for API 31+.
     * If the operation does not complete within the timeout period,
     * the pending operation is cancelled and observers are notified.
     */
    private fun startBluetoothTimeout() {
        val timeout = if (buildVersion >= COMMUNICATION_DEVICE_API_LEVEL) {
            COMMUNICATION_DEVICE_TIMEOUT_MS
        } else {
            SCO_CONNECTION_TIMEOUT_MS
        }

        timeoutRunnable?.let { handler.removeCallbacks(it) }
        timeoutRunnable = Runnable {
            if (DefaultAudioClientController.audioClientState == AudioClientState.STOPPED) {
                logger.debug(TAG, "AudioClient is stopped. Cancelling pending bluetooth operations")
                cancelPendingBluetoothOperation()
                return@Runnable
            }

            if (pendingBluetoothRoute != null) {
                logger.warn(TAG, "Bluetooth operation timeout after ${timeout}ms")
                pendingBluetoothRoute = null
                pendingBluetoothTimestamp = 0L
                notifyAudioDeviceChange()
            }
        }
        handler.postDelayed(timeoutRunnable!!, timeout)
    }

    /**
     * Executes the setRoute call and records the event.
     *
     * @param route The audio route value to set
     */
    private fun executeSetRoute(route: Int) {
        val selected = audioClientController.setRoute(route)
        if (selected) {
            eventAnalyticsController.pushHistory(MeetingHistoryEventName.audioInputSelected)
        }
    }

    /**
     * Maps a MediaDeviceType to the corresponding AudioClient route constant.
     *
     * @param type The media device type
     * @return The corresponding AudioClient route constant
     */
    private fun getRouteForDeviceType(type: MediaDeviceType): Int {
        return when (type) {
            MediaDeviceType.AUDIO_BUILTIN_SPEAKER -> AudioClient.SPK_STREAM_ROUTE_SPEAKER
            MediaDeviceType.AUDIO_BLUETOOTH -> AudioClient.SPK_STREAM_ROUTE_BT_AUDIO
            MediaDeviceType.AUDIO_WIRED_HEADSET -> AudioClient.SPK_STREAM_ROUTE_HEADSET
            MediaDeviceType.AUDIO_USB_HEADSET -> AudioClient.SPK_STREAM_ROUTE_HEADSET
            else -> AudioClient.SPK_STREAM_ROUTE_RECEIVER
        }
    }

    /**
     * Finds an AudioDeviceInfo by its ID from available communication devices.
     *
     * @param id The AudioDeviceInfo ID as a String (converted to Int for lookup)
     * @return The matching AudioDeviceInfo, or null if not found
     */
    @RequiresApi(Build.VERSION_CODES.S)
    private fun findAudioDeviceById(id: String?): AudioDeviceInfo? {
        val deviceId = id?.toIntOrNull() ?: return null
        return audioManager.availableCommunicationDevices.firstOrNull { it.id == deviceId }
    }

    /**
     * Handles communication device change events for API 31+ Bluetooth routing.
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
    @RequiresApi(Build.VERSION_CODES.S)
    private fun onCommunicationDeviceChanged(device: AudioDeviceInfo?) {
        if (DefaultAudioClientController.audioClientState == AudioClientState.STOPPED) {
            logger.debug(TAG, "AudioClient is stopped. Cancelling pending bluetooth operations")
            cancelPendingBluetoothOperation()
            return
        }

        val pendingRoute = pendingBluetoothRoute ?: return
        val pendingDeviceId = pendingBluetoothDeviceId ?: return

        if (device != null && device.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO) {
            // SUCCESS: We got a Bluetooth device (may or may not be the exact one we requested)
            val waitTime = System.currentTimeMillis() - pendingBluetoothTimestamp
            logger.info(TAG, "Bluetooth communication device confirmed after ${waitTime}ms" +
                    " (retries: $bluetoothRoutingMismatchRetryCount) - executing deferred setRoute($pendingRoute)")
            cancelPendingBluetoothOperation()
            executeSetRoute(pendingRoute)
        } else {
            // MISMATCH: We got Handset/Speaker instead of Bluetooth
            // This may happen during rapid Bluetooth device transitions when the SCO channel
            // from the previous device is still being torn down
            if (bluetoothRoutingMismatchRetryCount < BLUETOOTH_ROUTING_MISMATCH_MAX_RETRIES) {
                bluetoothRoutingMismatchRetryCount++
                logger.info(TAG, "Bluetooth routing mismatch. Expected Bluetooth (id=$pendingDeviceId), " +
                        "got ${device?.productName} (type=${device?.type}). Retrying ($bluetoothRoutingMismatchRetryCount/$BLUETOOTH_ROUTING_MISMATCH_MAX_RETRIES) " +
                        "after ${BLUETOOTH_ROUTING_MISMATCH_RETRY_DELAY_MS}ms...")

                // Cancel any existing retry runnable
                retryRunnable?.let { handler.removeCallbacks(it) }

                // Schedule a retry after a short delay
                retryRunnable = Runnable {
                    retryBluetoothRouting(pendingDeviceId, pendingRoute)
                }
                handler.postDelayed(retryRunnable!!, BLUETOOTH_ROUTING_MISMATCH_RETRY_DELAY_MS)
            } else {
                logger.error(TAG, "Failed to route to Bluetooth device (id=$pendingDeviceId) after $BLUETOOTH_ROUTING_MISMATCH_MAX_RETRIES retries. Giving up.")
                cancelPendingBluetoothOperation()
                notifyAudioDeviceChange()
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
     * @param pendingRoute The audio route to set once Bluetooth is confirmed
     */
    @RequiresApi(Build.VERSION_CODES.S)
    private fun retryBluetoothRouting(targetDeviceId: Int, pendingRoute: Int) {
        if (DefaultAudioClientController.audioClientState == AudioClientState.STOPPED) {
            logger.debug(TAG, "AudioClient is stopped. Cancelling pending bluetooth operations")
            cancelPendingBluetoothOperation()
            return
        }

        // Re-verify device is still available before retrying
        val audioDeviceInfo = audioManager.availableCommunicationDevices.firstOrNull { it.id == targetDeviceId }

        if (audioDeviceInfo == null) {
            logger.warn(TAG, "Target Bluetooth device (id=$targetDeviceId) no longer available. Aborting retry.")
            cancelPendingBluetoothOperation()
            notifyAudioDeviceChange()
            return
        }

        logger.info(TAG, "Retrying setCommunicationDevice(${audioDeviceInfo.productName}, id=${audioDeviceInfo.id})")
        val success = audioManager.setCommunicationDevice(audioDeviceInfo)
        logger.info(TAG, "Retry setCommunicationDevice returned $success")

        if (!success) {
            logger.error(TAG, "Retry setCommunicationDevice failed. Giving up.")
            cancelPendingBluetoothOperation()
            notifyAudioDeviceChange()
        }
        // If success, wait for the next OnCommunicationDeviceChangedListener callback
    }

    /**
     * Sets up audio device routing using the API 31+ setCommunicationDevice() API.
     *
     * NOTE: We intentionally do NOT call clearCommunicationDevice() before setting a new device.
     * Calling clear adds another async operation that can worsen race conditions during rapid
     * Bluetooth device transitions. Instead, we rely on the retry-on-mismatch pattern in
     * onCommunicationDeviceChanged() to handle cases where the routing fails.
     *
     * For Bluetooth devices, this method also stores the device ID for retry purposes.
     *
     * @param mediaDevice The media device to route audio to
     * @return true if setCommunicationDevice() succeeded, false otherwise
     */
    @RequiresApi(Build.VERSION_CODES.S)
    private fun setupAudioDevice(mediaDevice: MediaDevice): Boolean {
        val audioDeviceInfo = findAudioDeviceById(mediaDevice.id)

        if (audioDeviceInfo == null) {
            logger.error(TAG, "Failed to setup audio device. AudioDeviceInfo not found for id: ${mediaDevice.id}, type: ${mediaDevice.type}")
            return false
        }

        // Store the device ID to retry setting the communication device to Bluetooth upon mismatch from the onCommunicationDeviceChanged listener
        if (mediaDevice.type == MediaDeviceType.AUDIO_BLUETOOTH) {
            pendingBluetoothDeviceId = audioDeviceInfo.id
            bluetoothRoutingMismatchRetryCount = 0
        }

        val success = audioManager.setCommunicationDevice(audioDeviceInfo)
        logger.info(TAG, "setCommunicationDevice(${audioDeviceInfo.productName}, id=${audioDeviceInfo.id}) returned $success")
        return success
    }

    /**
     * Handles SCO state transitions for pending Bluetooth operations.
     *
     * When SCO reaches CONNECTED state and there's a pending Bluetooth operation,
     * the deferred setRoute() call is executed.
     *
     * When SCO transitions to DISCONNECTED from CONNECTING (connection attempt failed),
     * the pending operation is cancelled. However, DISCONNECTED from CONNECTED is
     * expected during Bluetooth device switching and should NOT cancel the pending
     * operation - we wait for the new device to connect.
     *
     * @param state The new SCO audio state
     * @param previousState The previous SCO audio state
     */
    private fun onScoStateChanged(state: Int, previousState: Int) {
        if (DefaultAudioClientController.audioClientState == AudioClientState.STOPPED) {
            logger.debug(TAG, "AudioClient is stopped. Cancelling pending bluetooth operations")
            cancelPendingBluetoothOperation()
            lastScoState = state
            return
        }

        val pendingRoute = pendingBluetoothRoute ?: run {
            lastScoState = state
            return
        }

        when (state) {
            AudioManager.SCO_AUDIO_STATE_CONNECTED -> {
                val timeToConnect = System.currentTimeMillis() - pendingBluetoothTimestamp
                logger.info(TAG, "Bluetooth SCO connected in $timeToConnect ms, applying route change")
                cancelPendingBluetoothOperation()
                executeSetRoute(pendingRoute)
            }
            AudioManager.SCO_AUDIO_STATE_DISCONNECTED -> {
                // Only treat as failure if transitioning from CONNECTING (connection attempt failed)
                // CONNECTED → DISCONNECTED is expected during Bluetooth device switching
                if (previousState == AudioManager.SCO_AUDIO_STATE_CONNECTING) {
                    logger.warn(TAG, "Bluetooth SCO connection failed")
                    cancelPendingBluetoothOperation()
                    notifyAudioDeviceChange()
                }
            }
            AudioManager.SCO_AUDIO_STATE_ERROR -> {
                logger.warn(TAG, "Bluetooth SCO error")
                cancelPendingBluetoothOperation()
                notifyAudioDeviceChange()
            }
        }

        lastScoState = state
    }
}
