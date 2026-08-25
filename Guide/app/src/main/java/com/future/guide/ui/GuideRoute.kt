package com.future.guide.ui

sealed class GuideRoute {
    object Home : GuideRoute()
    data class Detail(val appId: String) : GuideRoute()
}
