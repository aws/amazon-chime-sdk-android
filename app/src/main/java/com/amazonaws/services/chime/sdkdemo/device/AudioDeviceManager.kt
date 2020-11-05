/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdkdemo.device

import com.amazonaws.services.chime.sdk.meetings.audiovideo.AudioVideoFacade
import com.amazonaws.services.chime.sdk.meetings.device.MediaDevice

/**
 * In order to support 23 or below we'll need to manually handle active audio device locally.
 */
class AudioDeviceManager(private val audioVideo: AudioVideoFacade) {
    // Handle active audio device for 21-23 since getActiveAudioDevice is only supported 24+
    private var currentActiveAudioDevice: MediaDevice? = null

    val activeAudioDevice: MediaDevice? get() = currentActiveAudioDevice

    /**
     * Set active device whenever chooseAudioDevice is called.
     */
    fun setActiveAudioDevice(mediaDevice: MediaDevice) {
        currentActiveAudioDevice = mediaDevice
    }

    /**
     * Reconfigure active device based on priority.
     * Current priority is bluetooth -> wired headset -> speakerphone -> earpiece
     */
    fun reconfigureActiveAudioDevice() {
        val devices = audioVideo.listAudioDevices().sortedBy { it.order }
        if (devices.isNotEmpty()) {
            if (devices[0] == currentActiveAudioDevice) return
            audioVideo.chooseAudioDevice(devices[0])
            setActiveAudioDevice(devices[0])
        }
    }
}
