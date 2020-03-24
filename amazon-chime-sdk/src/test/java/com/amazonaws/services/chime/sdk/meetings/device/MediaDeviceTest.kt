/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.device

import android.media.AudioDeviceInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaDeviceTest {
    @Test
    fun `MediaDevice should be able to sort by order`() {
        val speaker = MediaDevice(
            "speaker",
            MediaDeviceType.AUDIO_BUILTIN_SPEAKER
        )
        val receiver = MediaDevice(
            "handset",
            MediaDeviceType.AUDIO_HANDSET
        )
        val wiredHeadset = MediaDevice(
            "wired headset",
            MediaDeviceType.AUDIO_WIRED_HEADSET
        )
        val bluetoothAudio = MediaDevice(
            "bluetooth",
            MediaDeviceType.AUDIO_BLUETOOTH
        )
        val sortedDevices =
            listOf(speaker, receiver, wiredHeadset, bluetoothAudio).sortedBy { it.order }
        assertTrue(
            sortedDevices[0] == bluetoothAudio &&
                    sortedDevices[1] == wiredHeadset &&
                    sortedDevices[2] == speaker &&
                    sortedDevices[3] == receiver
        )
    }

    @Test
    fun `MediaDeviceType fromAudioDeviceInfo should return known type when defined value`() {
        val type = MediaDeviceType.fromAudioDeviceInfo(AudioDeviceInfo.TYPE_BUILTIN_SPEAKER)

        assertEquals(MediaDeviceType.AUDIO_BUILTIN_SPEAKER, type)
    }

    @Test
    fun `MediaDeviceType fromAudioDeviceInfo should return OTHER type when undefined value`() {
        val type = MediaDeviceType.fromAudioDeviceInfo(-99)

        assertEquals(MediaDeviceType.OTHER, type)
    }
}
