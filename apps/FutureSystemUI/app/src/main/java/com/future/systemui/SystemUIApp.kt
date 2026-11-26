package com.future.systemui

import android.app.Application

class SystemUIApp : Application() {
    val commandQueue by lazy { CommandQueue() }
    var started = false

    /** What IStatusBarService.registerStatusBar() returned (a hidden RegisterStatusBarResult), read reflectively. */
    @Volatile
    var registerResult: Any? = null

    /** Every feature, in start order. Agents add their feature here (one line each). */
    val features: List<Feature> by lazy {
        listOf<Feature>(
            com.future.systemui.HandoverFeature(),
            com.future.systemui.lockscreen.LockScreenFeature(),
            com.future.systemui.statusbar.StatusBarFeature(),
            com.future.systemui.controlcenter.ControlCenterFeature(),
            com.future.systemui.controlcenter.RecentsFeature(),
            com.future.systemui.notifications.NotificationShadeFeature(),
            com.future.systemui.volume.VolumeFeature(),
            com.future.systemui.power.PowerMenuFeature(),
            com.future.systemui.power.ToastFeature(),
        )
    }
}
