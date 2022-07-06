/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.device

import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraMetadata
import android.media.AudioDeviceInfo
import io.mockk.MockKAnnotations
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.impl.annotations.MockK
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class MediaDeviceTest {
    @MockK
    private lateinit var mockCameraManager: CameraManager

    @MockK
    private lateinit var mockFrontCameraCharacteristics: CameraCharacteristics

    @MockK
    private lateinit var mockBackCameraCharacteristics: CameraCharacteristics

    @MockK
    private lateinit var mockOtherCameraCharacteristics: CameraCharacteristics

    @Before
    fun setUp() {
        MockKAnnotations.init(this, relaxUnitFun = true)

        clearAllMocks()
    }

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

    @Test
    fun `listVideoDevices converts from cameras provided from CameraManager`() {
        every { mockCameraManager.cameraIdList } returns arrayOf("0", "1", "2")
        every { mockCameraManager.getCameraCharacteristics("0") } returns mockFrontCameraCharacteristics
        every { mockCameraManager.getCameraCharacteristics("1") } returns mockBackCameraCharacteristics
        every { mockCameraManager.getCameraCharacteristics("2") } returns mockOtherCameraCharacteristics
        every { mockFrontCameraCharacteristics.get(CameraCharacteristics.LENS_FACING) } returns CameraMetadata.LENS_FACING_FRONT
        every { mockBackCameraCharacteristics.get(CameraCharacteristics.LENS_FACING) } returns CameraMetadata.LENS_FACING_BACK
        every { mockOtherCameraCharacteristics.get(CameraCharacteristics.LENS_FACING) } returns null

        val devices = MediaDevice.listVideoDevices(mockCameraManager)

        assertEquals(devices.size, 3)
        assertEquals(devices[0].id, "0")
        assertEquals(devices[1].id, "1")
        assertEquals(devices[2].id, "2")
        assertEquals(devices[0].type, MediaDeviceType.VIDEO_FRONT_CAMERA)
        assertEquals(devices[1].type, MediaDeviceType.VIDEO_BACK_CAMERA)
        assertEquals(devices[2].type, MediaDeviceType.OTHER)
    }
}
