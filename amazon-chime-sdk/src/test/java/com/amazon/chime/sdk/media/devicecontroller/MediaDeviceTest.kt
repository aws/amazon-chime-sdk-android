/*
 * Copyright (c) 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 */

package com.amazon.chime.sdk.media.devicecontroller

import android.media.AudioDeviceInfo
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaDeviceTest {
    @Test
    fun `MediaDevice should be able to sort by order`() {
        val speaker = MediaDevice("speaker", AudioDeviceInfo.TYPE_BUILTIN_SPEAKER)
        val receiver = MediaDevice("handset", AudioDeviceInfo.TYPE_TELEPHONY)
        val wiredHeadset = MediaDevice("wired headset", AudioDeviceInfo.TYPE_WIRED_HEADSET)
        val bluetoothAudio = MediaDevice("bluetooth", AudioDeviceInfo.TYPE_BLUETOOTH_SCO)
        val sortedDevices = listOf(speaker, receiver, wiredHeadset, bluetoothAudio).sortedBy { it.order }
        assertTrue(
            sortedDevices[0] == bluetoothAudio &&
                    sortedDevices[1] == wiredHeadset &&
                    sortedDevices[2] == speaker &&
                    sortedDevices[3] == receiver
        )
    }
}
