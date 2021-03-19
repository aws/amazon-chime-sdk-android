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
import androidx.annotation.RequiresApi
import androidx.annotation.VisibleForTesting
import com.amazonaws.services.chime.sdk.meetings.analytics.EventAnalyticsController
import com.amazonaws.services.chime.sdk.meetings.analytics.MeetingHistoryEventName
import com.amazonaws.services.chime.sdk.meetings.internal.audio.AudioClientController
import com.amazonaws.services.chime.sdk.meetings.internal.audio.AudioClientState
import com.amazonaws.services.chime.sdk.meetings.internal.audio.DefaultAudioClientController
import com.amazonaws.services.chime.sdk.meetings.internal.utils.ObserverUtils
import com.amazonaws.services.chime.sdk.meetings.internal.video.VideoClientController
import com.xodee.client.audio.audioclient.AudioClient

class DefaultDeviceController(
    private val context: Context,
    private val audioClientController: AudioClientController,
    private val videoClientController: VideoClientController,
    private val eventAnalyticsController: EventAnalyticsController,
    private val audioManager: AudioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager,
    private val buildVersion: Int = Build.VERSION.SDK_INT
) : DeviceController {
    private val deviceChangeObservers = mutableSetOf<DeviceChangeObserver>()

    // TODO: remove code blocks for lower API level after the minimum SDK version becomes 23
    private val AUDIO_MANAGER_API_LEVEL = 23

    private var receiver: BroadcastReceiver? = null

    private var audioDeviceCallback: AudioDeviceCallback? = null

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

            // It doesn't look like Android can switch between two wired connection, so we'll assume WIRED_HEADSET
            // is where audio is routed.
            if (wiredDeviceCount > 1) audioDevices.removeIf { it.type == MediaDeviceType.AUDIO_USB_HEADSET }
            return if (isWiredHeadsetOn) audioDevices.filter { it.type != MediaDeviceType.AUDIO_HANDSET } else audioDevices
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
        setupAudioDevice(mediaDevice.type)
        val route = when (mediaDevice.type) {
            MediaDeviceType.AUDIO_BUILTIN_SPEAKER -> AudioClient.SPK_STREAM_ROUTE_SPEAKER
            MediaDeviceType.AUDIO_BLUETOOTH -> AudioClient.SPK_STREAM_ROUTE_BT_AUDIO
            MediaDeviceType.AUDIO_WIRED_HEADSET -> AudioClient.SPK_STREAM_ROUTE_HEADSET
            MediaDeviceType.AUDIO_USB_HEADSET -> AudioClient.SPK_STREAM_ROUTE_HEADSET
            else -> AudioClient.SPK_STREAM_ROUTE_RECEIVER
        }

        val selected = audioClientController.setRoute(route)
        if (selected) {
            eventAnalyticsController.pushHistory(MeetingHistoryEventName.audioInputSelected)
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
}
