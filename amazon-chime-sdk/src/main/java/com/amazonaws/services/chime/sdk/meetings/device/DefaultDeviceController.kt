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
import com.amazonaws.services.chime.sdk.meetings.internal.audio.AudioClientController
import com.amazonaws.services.chime.sdk.meetings.internal.utils.ObserverUtils
import com.amazonaws.services.chime.sdk.meetings.internal.video.VideoClientController
import com.xodee.client.audio.audioclient.AudioClient

class DefaultDeviceController(
    private val context: Context,
    private val audioClientController: AudioClientController,
    private val videoClientController: VideoClientController,
    private val audioManager: AudioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager,
    private val buildVersion: Int = Build.VERSION.SDK_INT,
    private val bluetoothDeviceController: BluetoothDeviceController = BluetoothDeviceController(context)
) : DeviceController {
    private val deviceChangeObservers = mutableSetOf<DeviceChangeObserver>()
    // TODO: remove code blocks for lower API level after the minimum SDK version becomes 23
    private val AUDIO_MANAGER_API_LEVEL = 23
    private val AUDIO_RECORDING_CONFIG_API_LEVEL = 24

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
                    audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
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
        bluetoothDeviceController.startListening()
    }

    private fun listAudioDevicesWithType(excludeType: Int?): List<MediaDevice> {
        @SuppressLint("NewApi")
        if (buildVersion >= AUDIO_MANAGER_API_LEVEL) {
            var isWiredHeadsetOn = false
            var isHandsetAvailable = false
            val handsetDevicesInfo = setOf(
                AudioDeviceInfo.TYPE_BUILTIN_EARPIECE,
                AudioDeviceInfo.TYPE_TELEPHONY
            )

            val audioDevices = mutableListOf<MediaDevice>()
            for (device in audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)) {
                // System will select wired headset over receiver
                // so we want to filter receiver out when wired headset is connected
                if (device.type == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
                    device.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES
                ) {
                    isWiredHeadsetOn = true
                }

                // Return only one handset device to avoid confusion
                if (handsetDevicesInfo.contains(device.type)) {
                    if (isHandsetAvailable) continue
                    else {
                        isHandsetAvailable = true
                    }
                }

                if (device.type == excludeType) continue

                var name = device.productName
                // One plus device with Android 10 is not able to show the bluetooth name
                // We'll remap it so that it shows either bluetooth device name or "Bluetooth"
                if (device.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO && device.productName == Build.MODEL) {
                    name = bluetoothDeviceController.getBluetoothName()
                }

                audioDevices.add(
                    MediaDevice(
                        "$name (${getReadableType(device.type)})",
                        MediaDeviceType.fromAudioDeviceInfo(
                            device.type
                        )
                    )
                )
            }
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

    @SuppressLint("NewApi")
    override fun listAudioDevices(): List<MediaDevice> {
        if (buildVersion >= AUDIO_MANAGER_API_LEVEL) {
            // A2DP doesn't allow two way communication. We'll filter it out
            return listAudioDevicesWithType(AudioDeviceInfo.TYPE_BLUETOOTH_A2DP)
        }
        return listAudioDevicesWithType(null)
    }

    override fun chooseAudioDevice(mediaDevice: MediaDevice) {
        setupAudioDevice(mediaDevice.type)
        val route = when (mediaDevice.type) {
            MediaDeviceType.AUDIO_BUILTIN_SPEAKER -> AudioClient.SPK_STREAM_ROUTE_SPEAKER
            MediaDeviceType.AUDIO_BLUETOOTH -> AudioClient.SPK_STREAM_ROUTE_BT_AUDIO
            MediaDeviceType.AUDIO_WIRED_HEADSET -> AudioClient.SPK_STREAM_ROUTE_HEADSET
            else -> AudioClient.SPK_STREAM_ROUTE_RECEIVER
        }
        if (audioClientController.setRoute(route)) {
            ObserverUtils.notifyObserverOnMainThread(deviceChangeObservers) {
                it.onChooseAudioDeviceCalled(
                    mediaDevice
                )
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    override fun getActiveAudioDevice(): MediaDevice? {
        if (buildVersion >= AUDIO_RECORDING_CONFIG_API_LEVEL) {
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
                    return listAudioDevicesWithType(AudioDeviceInfo.TYPE_BLUETOOTH_A2DP).firstOrNull {
                        it.type == mediaDeviceType
                    }
                }

                // Some android devices doesn't have audio device for speaker
                if (audioManager.isSpeakerphoneOn) return listAudioDevicesWithType(AudioDeviceInfo.TYPE_BLUETOOTH_A2DP).firstOrNull {
                    it.type == MediaDeviceType.AUDIO_BUILTIN_SPEAKER
                }
            }
        }
        return null
    }

    private fun setupAudioDevice(type: MediaDeviceType) {
        when (type) {
            MediaDeviceType.AUDIO_BUILTIN_SPEAKER -> {
                audioManager.apply {
                    stopBluetoothSco()
                    mode = AudioManager.MODE_IN_COMMUNICATION
                    isBluetoothScoOn = false
                    isSpeakerphoneOn = true
                }
            }
            MediaDeviceType.AUDIO_BLUETOOTH ->
                audioManager.apply {
                    mode = AudioManager.MODE_IN_COMMUNICATION
                    isSpeakerphoneOn = false
                    isBluetoothScoOn = true
                    startBluetoothSco()
                }
            else ->
                audioManager.apply {
                    stopBluetoothSco()
                    mode = AudioManager.MODE_IN_COMMUNICATION
                    isBluetoothScoOn = false
                    isSpeakerphoneOn = false
                }
        }
    }

    private fun getReadableType(type: Int): String {
        return when (type) {
            AudioDeviceInfo.TYPE_WIRED_HEADSET -> "Wired Headset"
            AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> "Speaker"
            AudioDeviceInfo.TYPE_WIRED_HEADPHONES -> "Wired Headphone"
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

    @SuppressLint("NewApi")
    fun stopListening() {
        if (buildVersion >= AUDIO_MANAGER_API_LEVEL) {
            audioDeviceCallback?.let {
                audioManager.unregisterAudioDeviceCallback(it)
            }
        } else {
            receiver?.let {
                context.unregisterReceiver(it)
            }
        }

        bluetoothDeviceController.stopListening()
    }
}
