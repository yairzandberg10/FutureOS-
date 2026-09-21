package com.android.sistemui.notificationcenter.logic

import android.app.Notification
import android.content.Context
import android.content.Intent
import android.service.notification.StatusBarNotification
import androidx.compose.runtime.mutableStateListOf
import com.android.sistemui.controlcenter.logic.ControlManager
import com.android.sistemui.controlcenter.service.MediaControlService

class NotificationCenterManager(private val context: Context) {
    val notifications = mutableStateListOf<StatusBarNotification>()
    val controlManager = ControlManager(context)

    /** מעביר הלאה לניקוי ה-listeners של ControlManager - יש לקרוא מ-onDestroy()/onInterrupt() של השירות שמחזיק את המופע הזה. */
    fun dispose() {
        controlManager.dispose()
    }

    /** מקבץ התראות לפי אפליקציה (כדי שכמה התראות מאותה אפליקציה יופיעו רצופות
     *  עם כותרת קבוצה אחת, במקום מפוזרות ברשימה שטוחה לפי סדר הגעה) - קבוצת
     *  האפליקציה עם ההתראה החדשה ביותר עולה ראשונה, ובתוך כל קבוצה הכי חדש קודם. */
    fun updateNotifications() {
        if (MediaControlService.isEnabled(context)) {
            val activeNotifications = MediaControlService.instance?.activeNotifications
            val visible = activeNotifications?.filter { isShowingNotification(it) } ?: emptyList()
            val grouped = visible
                .groupBy { it.packageName }
                .toList()
                .sortedByDescending { (_, sbns) -> sbns.maxOf { it.postTime } }
                .flatMap { (_, sbns) -> sbns.sortedByDescending { it.postTime } }
            notifications.clear()
            notifications.addAll(grouped)
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

    /** מסמן כ"נצפתה" בלי לבטל אותה - setNotificationsShown הוא ה-API הציבורי התקני
     *  של NotificationListenerService בדיוק לזה (לא hack; ההתראה עצמה נשארת ברשימה). */
    fun markAsRead(sbn: StatusBarNotification) {
        try {
            MediaControlService.instance?.setNotificationsShown(arrayOf(sbn.key))
        } catch (t: Throwable) {}
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
