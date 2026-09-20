package com.example.data.model

import android.bluetooth.BluetoothDevice

data class HocoDevice(
    val device: BluetoothDevice,
    val name: String,
    val address: String,
    val isConnected: Boolean = false,
    val isBonded: Boolean = false,
    val rssi: Int = 0
)
