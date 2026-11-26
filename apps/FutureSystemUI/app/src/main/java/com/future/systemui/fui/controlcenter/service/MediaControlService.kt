package com.future.systemui.fui.controlcenter.service

import com.future.systemui.fui.utils.safeText
import android.app.NotificationManager
import android.content.Intent
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.media.session.MediaSessionManager
import android.content.ComponentName
import android.content.Context
import android.media.session.MediaController
import android.util.Log
import com.future.systemui.fui.notificationcenter.service.HeadsUpNotificationService

class MediaControlService : NotificationListenerService() {
    companion object {
        var instance: MediaControlService? = null
            private set

        /** עולה בכל התראה שנוספה/הוסרה - מרכז ההתראות מתרענן לפיו מיד (F30). */
        val changeTick = androidx.compose.runtime.mutableIntStateOf(0)

        // מפתחות שכבר הוצג להם באנר - עדכון שקט של אותה התראה (FLAG_ONLY_ALERT_ONCE) לא יקפיץ שוב
        private val bannerShown = HashSet<String>()

        fun getActiveControllers(context: Context): List<MediaController> {
            val manager = context.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
            return try {
                // ב-SystemUI (חתום פלטפורמה) ההרשאה MEDIA_CONTENT_CONTROL מחליפה את האזנה להתראות
                manager.getActiveSessions(if (hasMediaControl(context)) null else ComponentName(context, MediaControlService::class.java))
            } catch (e: Exception) {
                Log.e("MediaControlService", "Error getting active sessions", e)
                emptyList()
            }
        }
        
        /** האם התהליך מחזיק MEDIA_CONTENT_CONTROL (SystemUI) - אז אין צורך בהאזנה להתראות כדי לשלוט במדיה. */
        fun hasMediaControl(context: Context): Boolean =
            context.checkSelfPermission("android.permission.MEDIA_CONTENT_CONTROL") == android.content.pm.PackageManager.PERMISSION_GRANTED

        fun isEnabled(context: Context): Boolean {
            if (hasMediaControl(context)) return true
            val cn = ComponentName(context, MediaControlService::class.java)
            val flat = android.provider.Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
            return flat != null && flat.contains(cn.flattenToString())
        }
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        instance = this
        bumpTick()
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        instance = null
        bumpTick()
    }

    private fun bumpTick() {
        // ה-callbacks של ה-listener רצים על ה-main thread, שם גם נכתב מצב ה-Compose.
        com.future.systemui.fui.utils.FutureUIState.notificationTick.intValue++
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        super.onNotificationPosted(sbn)
        changeTick.intValue++
        bumpTick()
        if (!shouldShowHeadsUp(sbn)) return
        bannerShown.add(sbn.key)
        try {
            HeadsUpNotificationService.show(this, sbn.key)
        } catch (e: Exception) {
            Log.e("MediaControlService", "Failed to show heads-up for ${sbn.key}", e)
        }
    }

    /** שיחה שנענתה/נדחתה מוחקת את התראת הצלצול - הבאנר שלה נסגר מיד במקום להישאר 30 שניות. */
    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        super.onNotificationRemoved(sbn)
        changeTick.intValue++
        bumpTick()
        bannerShown.remove(sbn.key)
        HeadsUpNotificationService.dismiss(this, sbn.key)
    }

    /** מסנן התראות שלא צריכות להציג באנר קופץ: ההתראות של האפליקציה עצמה,
     * התראות "מתמשכות" (למשל התקדמות נגן מוזיקה, שירותים ברקע), והכל כשה-DND פעיל. */
    private fun shouldShowHeadsUp(sbn: StatusBarNotification): Boolean {
        if (sbn.packageName == packageName) return false
        // עדכון של התראה שכבר הוצגה, שהאפליקציה סימנה "להתריע פעם אחת" (SU-17)
        if (sbn.notification.flags and android.app.Notification.FLAG_ONLY_ALERT_ONCE != 0 && sbn.key in bannerShown) return false
        // במסך הנעילה ההתראה מופיעה ברשימה שלו (עם הגדרת הפרטיות) - לא כבאנר
        if (com.future.systemui.fui.utils.FutureUIState.isLocked) return false
        val n = sbn.notification
        // נעול ומסך הנעילה פינה מקום לשיחה/מעורר: רק שיחות מקבלות באנר, לא תוכן הודעות
        if (com.future.systemui.fui.utils.FutureUIState.isSecured && n.category != android.app.Notification.CATEGORY_CALL) return false
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
