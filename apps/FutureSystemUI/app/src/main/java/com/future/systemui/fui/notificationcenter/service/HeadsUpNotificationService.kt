package com.future.systemui.fui.notificationcenter.service

import android.content.Context

/**
 * FutureSystemUI: FutureUI's heads-up Service is replaced by com.future.systemui.notifications.HeadsUpController.
 * This empty shell only keeps MediaControlService (fui/controlcenter, not in use in SystemUI) compiling; delete both
 * together. Not a Service and not in the manifest.
 */
class HeadsUpNotificationService {
    companion object {
        @Suppress("UNUSED_PARAMETER")
        fun show(context: Context, notificationKey: String) {
        }

        @Suppress("UNUSED_PARAMETER")
        fun dismiss(context: Context, notificationKey: String) {
        }
    }
}
