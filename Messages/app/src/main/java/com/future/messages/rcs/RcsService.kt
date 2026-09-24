package com.future.messages.rcs

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat

/**
 * מחזיק את ה-SipDelegate בחיים: הודעות RCS נכנסות מגיעות רק כל עוד ה-delegate
 * פתוח, ולכן הוא לא יכול לחיות רק בזמן שהמסך פתוח. שירות קדמי עם התראה
 * שקטה - ורק כשיש בכלל RCS במכשיר; אחרת הוא נסגר מיד ולא מופיע כלום.
 */
class RcsService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, notification())
        if (!RcsStack.start(this)) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }
        return START_STICKY
    }

    override fun onDestroy() {
        RcsStack.stop()
        super.onDestroy()
    }

    private fun notification() = run {
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel(CHANNEL_ID, "צ'אט RCS", NotificationManager.IMPORTANCE_MIN))
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.sym_action_chat)
            .setContentTitle("צ'אט RCS פעיל")
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setOngoing(true)
            .build()
    }

    /** מפעיל את השירות אחרי אתחול - בלי זה הודעות RCS לא יגיעו עד שהמשתמש
     * פותח את Messages. */
    class BootReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == Intent.ACTION_BOOT_COMPLETED) start(context)
        }
    }

    companion object {
        private const val CHANNEL_ID = "rcs_service"
        private const val NOTIFICATION_ID = 7270

        /** לא מפעיל כלום כשאין הרשאה - כך במכשיר בלי RCS אין שירות ואין התראה. */
        fun start(context: Context) {
            if (!RcsStack.isPermitted(context)) return
            try {
                context.startForegroundService(Intent(context, RcsService::class.java))
            } catch (e: Exception) {
                android.util.Log.w("RcsService", "Cannot start RCS service", e)
            }
        }
    }
}
