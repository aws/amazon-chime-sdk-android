package com.amazonaws.services.chime.sdk.meetings.device

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothHeadset
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context

/**
 * [BluetoothDeviceController] will get bluetooth devices
 */
class BluetoothDeviceController(
    private val context: Context,
    private val btAdapter: BluetoothAdapter = (context.getSystemService(
        Context.BLUETOOTH_SERVICE
    ) as BluetoothManager).adapter
) {
    // This will hold reference to headset so we can get list of updated bluetooth devices
    private var bluetoothHeadset: BluetoothHeadset? = null

    private val serviceListener: BluetoothProfile.ServiceListener = object :
        BluetoothProfile.ServiceListener {
        override fun onServiceDisconnected(profile: Int) {
            bluetoothHeadset = null
        }

        // Get initial connected bluetooth devices
        override fun onServiceConnected(profile: Int, proxy: BluetoothProfile?) {
            (proxy as? BluetoothHeadset)?.let { btHeadset ->
                bluetoothHeadset = btHeadset
            }
        }
    }

    fun startListening() {
        btAdapter.getProfileProxy(context, serviceListener, BluetoothProfile.HEADSET)
    }

    fun stopListening() {
        btAdapter.closeProfileProxy(BluetoothProfile.HEADSET, bluetoothHeadset)
    }

    /**
     * Get the name of bluetooth device. If there is multiple bluetooth, it is hard to tell
     * which has bluetooth connection, so it will only return "Bluetooth"
     */
    fun getBluetoothName(): String {
        var bluetoothName: String? = null
        val btDevices = bluetoothHeadset?.connectedDevices
        val size = btDevices?.size ?: 0
        // if there is multiple bluetooth device we cannot tell unless it has SCO connection
        if (size >= 2) {
            bluetoothName = btDevices?.firstOrNull { btDevice ->
                bluetoothHeadset?.isAudioConnected(btDevice) ?: false
            }?.name
        } else if (size == 1) {
            bluetoothName = btDevices?.get(0)?.name
        }
        return bluetoothName ?: "Bluetooth"
    }
}
