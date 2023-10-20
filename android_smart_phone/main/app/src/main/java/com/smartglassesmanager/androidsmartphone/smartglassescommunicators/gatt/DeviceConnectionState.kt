
package com.smartglassesmanager.androidsmartphone.smartglassescommunicators.gatt

import android.bluetooth.BluetoothDevice

sealed class DeviceConnectionState( val state:Int ) {
    class Connected(val device: BluetoothDevice) : DeviceConnectionState(2)
    object Connecting:DeviceConnectionState(1)
    object Disconnected : DeviceConnectionState(0)
}

