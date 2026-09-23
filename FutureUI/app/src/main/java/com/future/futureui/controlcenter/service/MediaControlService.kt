package com.future.futureui.controlcenter.service

import android.app.NotificationManager
import android.content.Intent
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.media.session.MediaSessionManager
import android.content.ComponentName
import android.content.Context
import android.media.session.MediaController
import android.util.Log
import com.future.futureui.notificationcenter.service.HeadsUpNotificationService

class MediaControlService : NotificationListenerService() {
    companion object {
        var instance: MediaControlService? = null
            private set

        fun getActiveControllers(context: Context): List<MediaController> {
            val manager = context.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
            return try {
                manager.getActiveSessions(ComponentName(context, MediaControlService::class.java))
            } catch (e: Exception) {
                Log.e("MediaControlService", "Error getting active sessions", e)
                emptyList()
            }
        }
        
        fun isEnabled(context: Context): Boolean {
            val cn = ComponentName(context, MediaControlService::class.java)
            val flat = android.provider.Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
            return flat != null && flat.contains(cn.flattenToString())
        }
    }

    /** מפתחות שכבר הקפיצו באנר - עדכון שלהם לא מקפיץ שוב כשהם "התרע פעם אחת". */
    private val alerted = LinkedHashSet<String>()

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        super.onNotificationRemoved(sbn)
        alerted.remove(sbn.key)
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        instance = this
        activeNotifications?.forEach { alerted.add(it.key) }
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        instance = null
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        super.onNotificationPosted(sbn)
        if (!shouldShowHeadsUp(sbn)) return
        try {
            val host = com.future.futureui.statusbar.service.StatusBarAccessibilityService.instance
            if (host != null) host.showHeadsUp(sbn) else HeadsUpNotificationService.show(this, sbn.key)
        } catch (e: Exception) {
            Log.e("MediaControlService", "Failed to show heads-up for ${sbn.key}", e)
        }
    }

    /** מסנן התראות שלא צריכות להציג באנר קופץ: ההתראות של האפליקציה עצמה,
     * התראות "מתמשכות" (למשל התקדמות נגן מוזיקה, שירותים ברקע), והכל כשה-DND פעיל. */
    private fun shouldShowHeadsUp(sbn: StatusBarNotification): Boolean {
        if (sbn.packageName == packageName) return false
        val n = sbn.notification
        // שיחה נכנסת היא "מתמשכת" (isOngoing) לכל אורך הצלצול, אבל היא בדיוק ההפך
        // מהתראות מתמשכות רגילות (התקדמות נגן וכו') שהמסנן הזה נועד לחסום - היא
        // חייבת להופיע כבאנר, אחרת אין שום אינדיקציה לשיחה נכנסת מעל אפליקציה אחרת.
        if (sbn.isOngoing && n.category != android.app.Notification.CATEGORY_CALL) return false
        // סיכום קבוצה, ועדכון של התראה שכבר הוצגה עם "התרע פעם אחת" (התקדמות
        // הורדה, נגן) - לא באנר חדש בכל עדכון.
        if (n.flags and android.app.Notification.FLAG_GROUP_SUMMARY != 0) return false
        val isUpdate = !alerted.add(sbn.key)
        if (isUpdate && n.flags and android.app.Notification.FLAG_ONLY_ALERT_ONCE != 0) return false
        // כמו במערכת: רק התראות בחשיבות גבוהה קופצות, או הודעה/שיחה/שעון מעורר.
        val ranking = NotificationListenerService.Ranking()
        val importance = if (currentRanking?.getRanking(sbn.key, ranking) == true) ranking.importance else NotificationManager.IMPORTANCE_HIGH
        val urgentCategory = n.category in setOf(
            android.app.Notification.CATEGORY_MESSAGE,
            android.app.Notification.CATEGORY_CALL,
            android.app.Notification.CATEGORY_ALARM,
            android.app.Notification.CATEGORY_EMAIL,
            android.app.Notification.CATEGORY_REMINDER,
            android.app.Notification.CATEGORY_EVENT,
        )
        if (importance < NotificationManager.IMPORTANCE_HIGH && !urgentCategory) return false
        val title = n.extras.getCharSequence(android.app.Notification.EXTRA_TITLE)
        val text = n.extras.getCharSequence(android.app.Notification.EXTRA_TEXT)
        if (title.isNullOrBlank() && text.isNullOrBlank()) return false
        val nm = getSystemService(NotificationManager::class.java)
        if (nm != null && nm.currentInterruptionFilter != NotificationManager.INTERRUPTION_FILTER_ALL) return false
        return true
    }
}
