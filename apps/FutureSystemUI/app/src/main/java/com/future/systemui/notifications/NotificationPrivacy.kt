package com.future.systemui.notifications

import android.app.KeyguardManager
import android.app.Notification
import android.content.Context
import android.service.notification.StatusBarNotification
import android.util.Log
import android.view.View
import android.view.WindowManager
import com.future.systemui.fui.utils.FutureUIState

private const val TAG = "FutureSystemUI"

/** Lock-screen rules shared by the shade and the heads-up. */
object NotificationPrivacy {
    /**
     * The device is locked: the system keyguard shows, or FutureUI's lock screen is up (isLocked), or it yielded
     * to a call/alarm while the device is still locked (isSecured). Read-only use of the shared FutureUIState.
     */
    fun isLocked(context: Context): Boolean {
        try {
            val km = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
            if (km != null && km.isKeyguardLocked) return true
        } catch (t: Throwable) {
            Log.w(TAG, "isKeyguardLocked failed", t)
        }
        return try { FutureUIState.isLocked || FutureUIState.isSecured } catch (t: Throwable) { false }
    }

    /** Private/secret notifications must not show their content while locked (only the app name). */
    fun isSensitive(sbn: StatusBarNotification): Boolean =
        sbn.notification.visibility != Notification.VISIBILITY_PUBLIC
}

/** Adds [view] with TYPE_NOTIFICATION_SHADE (2040), then 2017, then TYPE_APPLICATION_OVERLAY. */
internal fun addOverlayWindow(wm: WindowManager, view: View, lp: WindowManager.LayoutParams): Boolean {
    for (type in intArrayOf(2040, 2017, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY)) {
        try {
            lp.type = type
            wm.addView(view, lp)
            return true
        } catch (t: Throwable) {
            Log.w(TAG, "addView with window type $type failed: $t")
        }
    }
    Log.e(TAG, "no window type worked for ${lp.title}")
    return false
}
