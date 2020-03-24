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
import androidx.annotation.VisibleForTesting
import com.amazonaws.services.chime.sdk.meetings.internal.audio.AudioClientController
import com.amazonaws.services.chime.sdk.meetings.internal.video.VideoClientController
import com.xodee.client.audio.audioclient.AudioClient

class DefaultDeviceController(
    private val context: Context,
    private val audioClientController: AudioClientController,
    private val videoClientController: VideoClientController,
    private val audioManager: AudioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager,
    private val buildVersion: Int = Build.VERSION.SDK_INT
) : DeviceController {
    private val deviceChangeObservers = mutableSetOf<DeviceChangeObserver>()

    // TODO: remove code blocks for lower API level after the minimum SDK version becomes 23
    private val AUDIO_MANAGER_API_LEVEL = 23

    init {
        @SuppressLint("NewApi")
        if (buildVersion >= AUDIO_MANAGER_API_LEVEL) {
            audioManager.registerAudioDeviceCallback(object : AudioDeviceCallback() {
                override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>?) {
                    notifyAudioDeviceChange()
                }

                override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>?) {
                    notifyAudioDeviceChange()
                }
            }, null)
        } else {
            val receiver = object : BroadcastReceiver() {
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
            val audioDevices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS).map {
                // System will select wired headset over receiver
                // so we want to filter receiver out when wired headset is connected
                if (it.type == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
                    it.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES
                ) {
                    isWiredHeadsetOn = true
                }
                MediaDevice(
                    "${it.productName} (${getReadableType(it.type)})",
                    MediaDeviceType.fromAudioDeviceInfo(
                        it.type
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
                        getReadableType(AudioDeviceInfo.TYPE_TELEPHONY),
                        MediaDeviceType.AUDIO_HANDSET
                    )
                )
            }
            if (audioManager.isBluetoothScoOn || audioManager.isBluetoothA2dpOn) {
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
        setupAudioDevice(mediaDevice.type)

        val route = when (mediaDevice.type) {
            MediaDeviceType.AUDIO_BUILTIN_SPEAKER -> AudioClient.SPK_STREAM_ROUTE_SPEAKER
            MediaDeviceType.AUDIO_BLUETOOTH -> AudioClient.SPK_STREAM_ROUTE_BT_AUDIO
            MediaDeviceType.AUDIO_WIRED_HEADSET -> AudioClient.SPK_STREAM_ROUTE_HEADSET
            else -> AudioClient.SPK_STREAM_ROUTE_RECEIVER
        }
        audioClientController.setRoute(route)
    }

    private fun setupAudioDevice(type: MediaDeviceType) {
        when (type) {
            MediaDeviceType.AUDIO_BUILTIN_SPEAKER ->
                audioManager.apply {
                    mode = AudioManager.MODE_IN_COMMUNICATION
                    isSpeakerphoneOn = true
                }
            MediaDeviceType.AUDIO_BLUETOOTH ->
                audioManager.apply {
                    mode = AudioManager.MODE_IN_COMMUNICATION
                    isSpeakerphoneOn = false
                    startBluetoothSco()
                }
            else ->
                audioManager.apply {
                    mode = AudioManager.MODE_IN_COMMUNICATION
                    isSpeakerphoneOn = false
                    isBluetoothScoOn = false
                    stopBluetoothSco()
                }
        }
    }

    private fun getReadableType(type: Int): String {
        return when (type) {
            AudioDeviceInfo.TYPE_WIRED_HEADSET -> "Wired Headset"
            AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> "Speaker"
            AudioDeviceInfo.TYPE_WIRED_HEADPHONES -> "Wired Headphone"
            AudioDeviceInfo.TYPE_TELEPHONY -> "Handset"
            AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
            AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> "Bluetooth"
            else -> "Unknown"
        }
    }

    override fun getActiveCamera(): MediaDevice? {
        val activeCamera = videoClientController.getActiveCamera()
        return activeCamera?.let {
            MediaDevice(
                activeCamera.name,
                if (activeCamera.isFrontFacing) MediaDeviceType.VIDEO_FRONT_CAMERA else MediaDeviceType.VIDEO_BACK_CAMERA
            )
        }
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
        for (observer in deviceChangeObservers) {
            observer.onAudioDeviceChanged(listAudioDevices())
        }
    }
}
