package com.android.sistemui.controlcenter.service

import com.android.sistemui.utils.safeText
import android.app.NotificationManager
import android.content.Intent
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.media.session.MediaSessionManager
import android.content.ComponentName
import android.content.Context
import android.media.session.MediaController
import android.util.Log
import com.android.sistemui.notificationcenter.service.HeadsUpNotificationService

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

    override fun onListenerConnected() {
        super.onListenerConnected()
        instance = this
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        instance = null
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        super.onNotificationPosted(sbn)
        if (!shouldShowHeadsUp(sbn)) return
        try {
            HeadsUpNotificationService.show(this, sbn.key)
        } catch (e: Exception) {
            Log.e("MediaControlService", "Failed to show heads-up for ${sbn.key}", e)
        }
    }

    /** שיחה שנענתה/נדחתה מוחקת את התראת הצלצול - הבאנר שלה נסגר מיד במקום להישאר 30 שניות. */
    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        super.onNotificationRemoved(sbn)
        HeadsUpNotificationService.dismiss(this, sbn.key)
    }

    /** מסנן התראות שלא צריכות להציג באנר קופץ: ההתראות של האפליקציה עצמה,
     * התראות "מתמשכות" (למשל התקדמות נגן מוזיקה, שירותים ברקע), והכל כשה-DND פעיל. */
    private fun shouldShowHeadsUp(sbn: StatusBarNotification): Boolean {
        if (sbn.packageName == packageName) return false
        // במסך הנעילה ההתראה מופיעה ברשימה שלו (עם הגדרת הפרטיות) - לא כבאנר
        if (com.android.sistemui.utils.FutureUIState.isLocked) return false
        val n = sbn.notification
        // נעול ומסך הנעילה פינה מקום לשיחה/מעורר: רק שיחות מקבלות באנר, לא תוכן הודעות
        if (com.android.sistemui.utils.FutureUIState.isSecured && n.category != android.app.Notification.CATEGORY_CALL) return false
        // שיחה נכנסת היא "מתמשכת" (isOngoing) לכל אורך הצלצול, אבל היא בדיוק ההפך
        // מהתראות מתמשכות רגילות (התקדמות נגן וכו') שהמסנן הזה נועד לחסום - היא
        // חייבת להופיע כבאנר, אחרת אין שום אינדיקציה לשיחה נכנסת מעל אפליקציה אחרת.
        // רק שיחה *מצלצלת* (עם fullScreenIntent): גם התראת "מחייג…"/"שיחה פעילה" של
        // החייגן היא CATEGORY_CALL ומתמשכת, והיא קפצה כבאנר של 30 שניות עם "טלפון = מענה"
        // בכל שיחה יוצאת ובכל שינוי מצב.
        val isRingingCall = n.category == android.app.Notification.CATEGORY_CALL && n.fullScreenIntent != null
        if (sbn.isOngoing && !isRingingCall) return false
        val title = n.safeText(android.app.Notification.EXTRA_TITLE)
        val text = n.safeText(android.app.Notification.EXTRA_TEXT)
        if (title.isNullOrBlank() && text.isNullOrBlank()) return false
        val nm = getSystemService(NotificationManager::class.java)
        if (nm != null && nm.currentInterruptionFilter != NotificationManager.INTERRUPTION_FILTER_ALL) return false
        return true
    }
}
