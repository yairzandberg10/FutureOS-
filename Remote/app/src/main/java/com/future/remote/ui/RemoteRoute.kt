package com.future.remote.ui
import com.future.sharednav.theme.subtleTextColor

sealed class RemoteRoute {
    object Home : RemoteRoute()
    object AddDevice : RemoteRoute()
    object AcPresets : RemoteRoute()
    data class Device(val deviceId: String) : RemoteRoute()
    data class AddButton(val deviceId: String) : RemoteRoute()
}
