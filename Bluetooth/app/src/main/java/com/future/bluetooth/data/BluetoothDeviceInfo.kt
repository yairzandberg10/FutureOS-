package com.future.bluetooth.data

data class BluetoothDeviceInfo(
    val name: String,
    val address: String,
    val isBonded: Boolean,
)
