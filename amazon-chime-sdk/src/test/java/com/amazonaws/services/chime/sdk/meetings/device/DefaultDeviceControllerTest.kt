/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.services.chime.sdk.meetings.device

import android.content.Context
import android.content.Intent
import android.media.AudioDeviceInfo
import android.media.AudioManager
import com.amazonaws.services.chime.sdk.meetings.internal.audio.AudioClientController
import com.amazonaws.services.chime.sdk.meetings.internal.video.VideoClientController
import com.xodee.client.audio.audioclient.AudioClient
import com.xodee.client.video.VideoDevice
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockkClass
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DefaultDeviceControllerTest {
    @MockK
    private lateinit var speakerInfo: AudioDeviceInfo

    @MockK
    private lateinit var receiverInfo: AudioDeviceInfo

    @MockK
    private lateinit var wiredHeadsetInfo: AudioDeviceInfo

    @MockK
    private lateinit var bluetoothInfo: AudioDeviceInfo

    @MockK
    private lateinit var context: Context

    @MockK
    private lateinit var audioClientController: AudioClientController

    @MockK
    private lateinit var videoClientController: VideoClientController

    @MockK
    private lateinit var audioManager: AudioManager

    @MockK
    private lateinit var deviceChangeObserver: DeviceChangeObserver

    private lateinit var deviceController: DefaultDeviceController

    private fun setupForNewAPILevel() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        deviceController = DefaultDeviceController(
            context,
            audioClientController,
            videoClientController,
            audioManager,
            23
        )
        commonSetup()
    }

    private fun setupForOldAPILevel() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        every { context.registerReceiver(any(), any()) } returns Intent()
        deviceController = DefaultDeviceController(
            context,
            audioClientController,
            videoClientController,
            audioManager,
            21
        )
        commonSetup()
    }

    private fun commonSetup() {
        every { speakerInfo.type } returns AudioDeviceInfo.TYPE_BUILTIN_SPEAKER
        every { speakerInfo.productName } returns "default speaker"
        every { receiverInfo.type } returns AudioDeviceInfo.TYPE_TELEPHONY
        every { receiverInfo.productName } returns "default receiver"
        every { wiredHeadsetInfo.type } returns AudioDeviceInfo.TYPE_WIRED_HEADSET
        every { wiredHeadsetInfo.productName } returns "my wired headset"
        every { bluetoothInfo.type } returns AudioDeviceInfo.TYPE_BLUETOOTH_SCO
        every { bluetoothInfo.productName } returns "my bluetooth headphone"
    }

    @Test
    fun `deviceController should register device change event when build version is high`() {
        setupForNewAPILevel()
        verify { audioManager.registerAudioDeviceCallback(any(), null) }
    }

    @Test
    fun `deviceController should register device change event when build version is low`() {
        setupForOldAPILevel()
        verify(exactly = 3) { context.registerReceiver(any(), any()) }
    }

    @Test
    fun `listAudioDevices should return a list of connected devices with product name when build version is high`() {
        setupForNewAPILevel()
        every { audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS) } returns arrayOf(
            speakerInfo, receiverInfo, bluetoothInfo
        )

        val devices = deviceController.listAudioDevices()

        assertEquals(3, devices.size)
        devices.forEach {
            assertTrue(
                it.type == MediaDeviceType.AUDIO_BUILTIN_SPEAKER &&
                        it.label == "default speaker (Speaker)" ||
                        it.type == MediaDeviceType.AUDIO_HANDSET &&
                        it.label == "default receiver (Handset)" ||
                        it.type == MediaDeviceType.AUDIO_BLUETOOTH &&
                        it.label == "my bluetooth headphone (Bluetooth)"
            )
        }
    }

    @Test
    fun `listAudioDevices should return a list of connected devices when build version is low`() {
        setupForOldAPILevel()
        every { audioManager.isBluetoothScoOn } returns true
        every { audioManager.isBluetoothA2dpOn } returns true
        every { audioManager.isWiredHeadsetOn } returns false

        val devices = deviceController.listAudioDevices()
        assertEquals(3, devices.size)
        devices.forEach {
            assertTrue(
                it.type == MediaDeviceType.AUDIO_HANDSET &&
                        it.label == "Handset" ||
                        it.type == MediaDeviceType.AUDIO_BUILTIN_SPEAKER &&
                        it.label == "Speaker" ||
                        it.type == MediaDeviceType.AUDIO_BLUETOOTH &&
                        it.label == "Bluetooth"
            )
        }
    }

    @Test
    fun `listAudioDevices should not return both wired headset and receiver when build version is high`() {
        setupForNewAPILevel()
        every { audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS) } returns arrayOf(
            speakerInfo, receiverInfo, wiredHeadsetInfo
        )

        val devices = deviceController.listAudioDevices()
        assertEquals(2, devices.size)
        devices.forEach {
            assertTrue(
                it.type == MediaDeviceType.AUDIO_WIRED_HEADSET ||
                        it.type == MediaDeviceType.AUDIO_BUILTIN_SPEAKER
            )
        }
    }

    @Test
    fun `listAudioDevices should not return both wired headset and receiver when build version is low`() {
        setupForOldAPILevel()
        every { audioManager.isBluetoothScoOn } returns false
        every { audioManager.isBluetoothA2dpOn } returns false
        every { audioManager.isWiredHeadsetOn } returns true

        val devices = deviceController.listAudioDevices()
        assertEquals(2, devices.size)
        devices.forEach {
            assertTrue(
                it.type == MediaDeviceType.AUDIO_WIRED_HEADSET ||
                        it.type == MediaDeviceType.AUDIO_BUILTIN_SPEAKER
            )
        }
    }

    @Test
    fun `chooseAudioDevice should call AudioClientController setRoute`() {
        setupForOldAPILevel()
        every { audioClientController.setRoute(any()) } returns true

        deviceController.chooseAudioDevice(
            MediaDevice(
                "speaker",
                MediaDeviceType.AUDIO_BUILTIN_SPEAKER
            )
        )

        verify { audioClientController.setRoute(AudioClient.SPK_STREAM_ROUTE_SPEAKER) }
    }

    @Test
    fun `chooseAudioDevice should call audioManager startBluetoothSco when choosing bluetooth device`() {
        setupForOldAPILevel()
        every { audioClientController.setRoute(any()) } returns true

        deviceController.chooseAudioDevice(
            MediaDevice(
                "bluetooth",
                MediaDeviceType.AUDIO_BLUETOOTH
            )
        )

        verify { audioManager.startBluetoothSco() }
    }

    @Test
    fun `chooseAudioDevice should disable speaker and bluetooth when choosing other devices`() {
        setupForOldAPILevel()
        every { audioClientController.setRoute(any()) } returns true

        deviceController.chooseAudioDevice(
            MediaDevice(
                "wired headset",
                MediaDeviceType.AUDIO_WIRED_HEADSET
            )
        )

        verify { audioManager.setSpeakerphoneOn(false) }
        verify { audioManager.setBluetoothScoOn(false) }
    }

    @Test
    fun `chooseAudioDevice should default to handset when not bluetooth, wired headset, or speaker`() {
        setupForOldAPILevel()
        every { audioClientController.setRoute(any()) } returns true

        deviceController.chooseAudioDevice(
            MediaDevice(
                "handset",
                MediaDeviceType.AUDIO_HANDSET
            )
        )

        verify { audioClientController.setRoute(AudioClient.SPK_STREAM_ROUTE_RECEIVER) }
    }

    @Test
    fun `getActiveCamera should return null when no active camera`() {
        setupForOldAPILevel()
        every { videoClientController.getActiveCamera() } returns null

        assertNull(deviceController.getActiveCamera())
    }

    @Test
    fun `getActiveCamera should return a media device when active camera existing`() {
        setupForOldAPILevel()
        val videoDevice = mockkClass(VideoDevice::class)
        every { videoDevice.name } returns "camera"
        every { videoDevice.isFrontFacing } returns true
        every { videoClientController.getActiveCamera() } returns videoDevice

        val mediaDevice = deviceController.getActiveCamera()!!

        assertEquals("camera", mediaDevice.label)
        assertEquals(MediaDeviceType.VIDEO_FRONT_CAMERA, mediaDevice.type)
    }

    @Test
    fun `switchCamera should call videoClientController switchCamera`() {
        setupForOldAPILevel()

        deviceController.switchCamera()

        verify { videoClientController.switchCamera() }
    }

    @Test
    fun `notifyAudioDeviceChange should notify added observers`() {
        setupForOldAPILevel()
        deviceController.addDeviceChangeObserver(deviceChangeObserver)
        every { audioManager.isBluetoothScoOn } returns false
        every { audioManager.isBluetoothA2dpOn } returns false
        every { audioManager.isWiredHeadsetOn } returns false

        deviceController.notifyAudioDeviceChange()

        verify { deviceChangeObserver.onAudioDeviceChanged(any()) }
    }

    @Test
    fun `notifyAudioDeviceChange should NOT notify removed observer`() {
        setupForOldAPILevel()
        deviceController.addDeviceChangeObserver(deviceChangeObserver)
        deviceController.removeDeviceChangeObserver(deviceChangeObserver)

        deviceController.notifyAudioDeviceChange()

        verify(exactly = 0) { deviceChangeObserver.onAudioDeviceChanged(any()) }
    }
}
