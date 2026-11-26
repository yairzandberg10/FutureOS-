package com.future.systemui.fui.notificationcenter.logic

import com.future.systemui.fui.utils.safeText
import android.app.Notification
import android.content.Context
import android.content.Intent
import android.service.notification.StatusBarNotification
import androidx.compose.runtime.mutableStateListOf
import com.future.systemui.fui.controlcenter.logic.ControlManager
import com.future.systemui.notifications.NotificationRepository

class NotificationCenterManager(private val context: Context) {
    val notifications = mutableStateListOf<StatusBarNotification>()
    val controlManager = ControlManager(context)

    /** מעביר הלאה לניקוי ה-listeners של ControlManager - יש לקרוא מ-onDestroy()/onInterrupt() של השירות שמחזיק את המופע הזה. */
    fun dispose() {
        controlManager.dispose()
    }

    /** FutureSystemUI: הנתונים מגיעים מ-FutureNotificationListener (NotificationRepository), לא מ-MediaControlService. */
    fun updateNotifications() {
        val active = NotificationRepository.active.value
        notifications.clear()
        active.forEach { sbn ->
            if (isShowingNotification(sbn)) {
                notifications.add(sbn)
            }
        }
    }

    private fun isShowingNotification(sbn: StatusBarNotification): Boolean {
        val n = sbn.notification
        // Filter out ongoing or system notifications that might be noisy if desired
        // For now, show everything that has a title or text
        val title = n.safeText(Notification.EXTRA_TITLE)
        val text = n.safeText(Notification.EXTRA_TEXT)
        return !title.isNullOrBlank() || !text.isNullOrBlank()
    }

    /** ה-Listener מופעל בעצמו ע"י SystemUI; אם הוא עוד לא מחובר מבקשים קישור מחדש (אין מי שיאשר הרשאה במסך). */
    private fun requestListenerAccessIfNeeded(): Boolean {
        if (NotificationRepository.listener != null) return true
        try {
            NotificationRepository.requestRebind(context)
        } catch (e: Exception) {
            android.util.Log.w("FutureSystemUI", "requestListenerAccessIfNeeded failed", e)
        }
        return false
    }

    fun clearAll() {
        if (!requestListenerAccessIfNeeded()) return
        NotificationRepository.cancelAll()
        notifications.clear()
    }

    fun dismissNotification(sbn: StatusBarNotification) {
        if (!requestListenerAccessIfNeeded()) return
        NotificationRepository.cancel(sbn.key)
        notifications.remove(sbn)
    }

    fun toggleDnd() {
        controlManager.handleControlToggle("dnd")
    }

    fun launchApp(packageName: String) {
        try {
            val intent = context.packageManager.getLaunchIntentForPackage(packageName)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            }
        } catch (e: Exception) {
            android.util.Log.e("NotificationCenter", "Failed to launch app: $packageName", e)
        }
    }

    fun openNotificationSettings(packageName: String) {
        try {
            val intent = Intent().apply {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    action = android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS
                    putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, packageName)
                } else {
                    action = "android.settings.APP_NOTIFICATION_SETTINGS"
                    putExtra("app_package", packageName)
                    putExtra("app_uid", context.packageManager.getApplicationInfo(packageName, 0).uid)
                }
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            android.util.Log.e("NotificationCenter", "Failed to open settings: $packageName", e)
        }
    }
}
