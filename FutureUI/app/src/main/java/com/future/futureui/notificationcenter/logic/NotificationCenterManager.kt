package com.future.futureui.notificationcenter.logic

import android.app.Notification
import android.content.Context
import android.content.Intent
import android.service.notification.StatusBarNotification
import androidx.compose.runtime.mutableStateListOf
import com.future.futureui.controlcenter.logic.ControlManager
import com.future.futureui.controlcenter.service.MediaControlService

class NotificationCenterManager(private val context: Context) {
    val notifications = mutableStateListOf<StatusBarNotification>()
    val controlManager = ControlManager(context)

    /** מעביר הלאה לניקוי ה-listeners של ControlManager - יש לקרוא מ-onDestroy()/onInterrupt() של השירות שמחזיק את המופע הזה. */
    fun dispose() {
        controlManager.dispose()
    }

    fun updateNotifications() {
        if (MediaControlService.isEnabled(context)) {
            val activeNotifications = MediaControlService.instance?.activeNotifications
            notifications.clear()
            activeNotifications?.forEach { sbn ->
                if (isShowingNotification(sbn)) {
                    notifications.add(sbn)
                }
            }
        }
    }

    private fun isShowingNotification(sbn: StatusBarNotification): Boolean {
        val n = sbn.notification
        // Filter out ongoing or system notifications that might be noisy if desired
        // For now, show everything that has a title or text
        val title = n.extras.getCharSequence(Notification.EXTRA_TITLE)
        val text = n.extras.getCharSequence(Notification.EXTRA_TEXT)
        return !title.isNullOrBlank() || !text.isNullOrBlank()
    }

    /** אם ה-Listener לא מחובר (הרשאה לא ניתנה, או שהשירות עוד לא נקשר מחדש אחרי
     * הפעלה מחדש של המכשיר), מבקש את ההרשאה במקום לא לעשות כלום בשקט - זו
     * הייתה הסיבה ש"נקה הכל" ו"מחק" נראו כאילו לא עובדים בכלל. */
    private fun requestListenerAccessIfNeeded(): Boolean {
        if (MediaControlService.isEnabled(context) && MediaControlService.instance != null) return true
        try {
            val intent = Intent(android.provider.Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: Exception) {}
        return false
    }

    fun clearAll() {
        if (!requestListenerAccessIfNeeded()) return
        MediaControlService.instance?.cancelAllNotifications()
        notifications.clear()
    }

    fun dismissNotification(sbn: StatusBarNotification) {
        if (!requestListenerAccessIfNeeded()) return
        MediaControlService.instance?.cancelNotification(sbn.key)
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
