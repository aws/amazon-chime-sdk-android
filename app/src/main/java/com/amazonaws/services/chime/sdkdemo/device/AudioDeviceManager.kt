package com.amazonaws.services.chime.sdkdemo.device

import com.amazonaws.services.chime.sdk.meetings.audiovideo.AudioVideoFacade
import com.amazonaws.services.chime.sdk.meetings.device.MediaDevice

/**
 * In order to support 23 or below we'll need to manually handle current device locally.
 */
class AudioDeviceManager(private val audioVideo: AudioVideoFacade) {
    // Handle current audio device for 21-23 since getCurrentAudioDevice is only supported 24+
    private var currentAudioDevice: MediaDevice? = null

    val activeAudioDevice: MediaDevice? get() = currentAudioDevice

    /**
     * Set current device whenever chooseAudioDevice is called.
     * This will be called by [DeviceChangeObserver.onChooseAudioDeviceCalled] in [MeetingFragment]
     */
    fun setCurrentAudioDevice(mediaDevice: MediaDevice) {
        currentAudioDevice = mediaDevice
    }

    /**
     * Reconfigure current device based on priority.
     * Current priority is bluetooth -> wired headset -> speakerphone -> earpiece
     */
    fun reconfigureCurrentAudioDevice() {
        val devices = audioVideo.listAudioDevices().sortedBy { it.order }
        if (devices.isNotEmpty()) {
            audioVideo.chooseAudioDevice(devices[0])
        }
    }
}
