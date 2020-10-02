package com.amazonaws.services.chime.sdk.meetings.device

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothProfile
import android.content.Context
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import org.junit.Assert
import org.junit.Before
import org.junit.Test

class BluetoothDeviceControllerTest {
    @MockK
    private lateinit var context: Context

    @MockK
    private lateinit var btAdapter: BluetoothAdapter

    private lateinit var bluetoothDeviceController: BluetoothDeviceController

    @Before
    fun setup() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        bluetoothDeviceController = BluetoothDeviceController(context, btAdapter)
    }

    @Test
    fun `bluetoothController should start proxy to get bluetooth headset`() {
        every { btAdapter.getProfileProxy(any(), any(), any()) }.returns(true)
        bluetoothDeviceController.startListening()
        verify(exactly = 1) { btAdapter.getProfileProxy(any(), any(), any()) }
    }

    @Test
    fun `bluetoothController should stop proxy to get bluetooth headset`() {
        every { btAdapter.closeProfileProxy(any(), any()) }.returns(Unit)
        bluetoothDeviceController.stopListening()
        verify(exactly = 1) { btAdapter.closeProfileProxy(BluetoothProfile.HEADSET, any()) }
    }

    @Test
    fun `bluetoothController should get "Bluetooth" as name when bluetoothHeadset is null`() {
        val name = bluetoothDeviceController.getBluetoothName()
        Assert.assertEquals("Bluetooth", name)
    }
}
