package com.amazon.chime.sdk.media.devicecontroller

import android.content.Context
import android.content.Intent
import android.media.AudioDeviceInfo
import android.media.AudioManager
import com.amazon.chime.sdk.media.clientcontroller.AudioClientController
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import org.junit.Assert.assertEquals
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
    private lateinit var audioManager: AudioManager

    private lateinit var deviceController: DefaultDeviceController

    private fun setupForNewAPILevel() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        deviceController = DefaultDeviceController(
            context,
            audioClientController,
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
                it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER &&
                        it.label == "default speaker (Speaker)" ||
                        it.type == AudioDeviceInfo.TYPE_TELEPHONY &&
                        it.label == "default receiver (Handset)" ||
                        it.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO &&
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
                it.type == AudioDeviceInfo.TYPE_TELEPHONY &&
                        it.label == "Handset" ||
                        it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER &&
                        it.label == "Speaker" ||
                        it.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO &&
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
                it.type == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
                        it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER
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
                it.type == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
                        it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER
            )
        }
    }

    @Test
    fun `should call AudioClientController setRoute`() {
        setupForOldAPILevel()
        every { audioClientController.setRoute(any()) } returns true

        deviceController.chooseAudioDevice(MediaDevice("speaker", AudioDeviceInfo.TYPE_BUILTIN_SPEAKER))

        verify { audioClientController.setRoute(2) }
    }

    @Test
    fun `chooseAudioDevice should call audioManager startBluetoothSco when choosing bluetooth device`() {
        setupForOldAPILevel()
        every { audioClientController.setRoute(any()) } returns true

        deviceController.chooseAudioDevice(MediaDevice("bluetooth", AudioDeviceInfo.TYPE_BLUETOOTH_SCO))

        verify { audioManager.startBluetoothSco() }
    }

    @Test
    fun `chooseAudioDevice should disable speaker and bluetooth when choosing other devices`() {
        setupForOldAPILevel()
        every { audioClientController.setRoute(any()) } returns true

        deviceController.chooseAudioDevice(MediaDevice("wired headset", AudioDeviceInfo.TYPE_WIRED_HEADSET))

        verify { audioManager.setSpeakerphoneOn(false) }
        verify { audioManager.setBluetoothScoOn(false) }
    }
}
