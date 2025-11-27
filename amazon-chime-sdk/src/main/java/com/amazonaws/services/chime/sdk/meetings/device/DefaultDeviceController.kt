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

    private var receiver: BroadcastReceiver? = null

    private var audioDeviceCallback: AudioDeviceCallback? = null

    private var scoStateReceiver: BroadcastReceiver? = null

    // Pending Bluetooth route state for SCO-aware routing
    private var pendingBluetoothRoute: Int? = null
    private var pendingBluetoothTimestamp: Long = 0L

    // Track last SCO state to distinguish device switching from connection failure
    private var lastScoState: Int = AudioManager.SCO_AUDIO_STATE_DISCONNECTED

    // Timeout handler for pending Bluetooth operations
    private val handler = Handler(Looper.getMainLooper())
    private var timeoutRunnable: Runnable? = null

    // Timeout duration constant for SCO connection
    private val SCO_CONNECTION_TIMEOUT_MS = 3000L

    private val TAG = "DefaultDeviceController"

    init {
        @SuppressLint("NewApi")
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

        // Register Bluetooth SCO state receiver to handle pending Bluetooth route operations
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
                        )
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

    override fun chooseAudioDevice(mediaDevice: MediaDevice) {
        if (DefaultAudioClientController.audioClientState != AudioClientState.STARTED) {
            return
        }

        // Cancel any pending Bluetooth operation before processing new selection
        cancelPendingBluetoothOperation()

        logger.info(TAG, "chooseAudioDevice() called for device: ${mediaDevice.label} with type: ${mediaDevice.type}")
        setupAudioDevice(mediaDevice.type)

        val route = when (mediaDevice.type) {
            MediaDeviceType.AUDIO_BUILTIN_SPEAKER -> AudioClient.SPK_STREAM_ROUTE_SPEAKER
            MediaDeviceType.AUDIO_BLUETOOTH -> AudioClient.SPK_STREAM_ROUTE_BT_AUDIO
            MediaDeviceType.AUDIO_WIRED_HEADSET -> AudioClient.SPK_STREAM_ROUTE_HEADSET
            MediaDeviceType.AUDIO_USB_HEADSET -> AudioClient.SPK_STREAM_ROUTE_HEADSET
            else -> AudioClient.SPK_STREAM_ROUTE_RECEIVER
        }

        if (mediaDevice.type == MediaDeviceType.AUDIO_BLUETOOTH) {
            // Defer setRoute() until SCO connects to avoid race condition where audio bounce
            // occurs while SCO is still in CONNECTING state, causing audio to route to Handset
            pendingBluetoothRoute = route
            pendingBluetoothTimestamp = System.currentTimeMillis()
            startScoTimeout()
            logger.info(TAG, "Bluetooth device selected, deferring route change until SCO connected")
        } else {
            // Non-Bluetooth: call setRoute() immediately
            executeSetRoute(route)
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
     */
    private fun cancelPendingBluetoothOperation() {
        if (pendingBluetoothRoute != null) {
            pendingBluetoothRoute = null
            pendingBluetoothTimestamp = 0L
            timeoutRunnable?.let { handler.removeCallbacks(it) }
            timeoutRunnable = null
        }
    }

    /**
     * Starts a timeout for the pending Bluetooth SCO connection.
     * If SCO does not reach CONNECTED state within the timeout period,
     * the pending operation is cancelled and observers are notified.
     */
    private fun startScoTimeout() {
        timeoutRunnable?.let { handler.removeCallbacks(it) }
        timeoutRunnable = Runnable {
            if (pendingBluetoothRoute != null) {
                logger.warn(TAG, "Bluetooth SCO connection timeout")
                pendingBluetoothRoute = null
                pendingBluetoothTimestamp = 0L
                notifyAudioDeviceChange()
            }
        }
        handler.postDelayed(timeoutRunnable!!, SCO_CONNECTION_TIMEOUT_MS)
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
        val pendingRoute = pendingBluetoothRoute ?: run {
            lastScoState = state
            return
        }

        when (state) {
            AudioManager.SCO_AUDIO_STATE_CONNECTED -> {
                logger.info(TAG, "Bluetooth SCO connected, applying route change")
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
