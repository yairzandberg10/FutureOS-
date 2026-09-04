package com.future.remote.ui

sealed class RemoteRoute {
    object Home : RemoteRoute()
    object AddDevice : RemoteRoute()
    object AcPresets : RemoteRoute()
    data class Device(val deviceId: String) : RemoteRoute()
    data class AddButton(val deviceId: String) : RemoteRoute()
}
