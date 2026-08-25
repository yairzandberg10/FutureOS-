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

    /** מסנן התראות שלא צריכות להציג באנר קופץ: ההתראות של האפליקציה עצמה,
     * התראות "מתמשכות" (למשל התקדמות נגן מוזיקה, שירותים ברקע), והכל כשה-DND פעיל. */
    private fun shouldShowHeadsUp(sbn: StatusBarNotification): Boolean {
        if (sbn.packageName == packageName) return false
        if (sbn.isOngoing) return false
        val n = sbn.notification
        val title = n.extras.getCharSequence(android.app.Notification.EXTRA_TITLE)
        val text = n.extras.getCharSequence(android.app.Notification.EXTRA_TEXT)
        if (title.isNullOrBlank() && text.isNullOrBlank()) return false
        val nm = getSystemService(NotificationManager::class.java)
        if (nm != null && nm.currentInterruptionFilter != NotificationManager.INTERRUPTION_FILTER_ALL) return false
        return true
    }
}
