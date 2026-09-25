package com.future.remote.ui

sealed class RemoteRoute {
    object Home : RemoteRoute()
    data class Ac(val deviceId: String) : RemoteRoute()
}
