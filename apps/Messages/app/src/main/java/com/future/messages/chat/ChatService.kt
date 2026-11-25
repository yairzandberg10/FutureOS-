package com.future.messages.chat

import com.future.sharednav.i18n.tr

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat

/**
 * מחזיק את ההאזנה לתור הצ'אט פתוחה גם כש-Messages סגורה. בפרויקט ה-Firebase
 * החינמי אין Cloud Function ששולחת Push, ולכן בלי השירות הזה הודעות צ'אט
 * היו מגיעות רק כשהאפליקציה פתוחה. שירות קדמי עם התראה שקטה, ורק כשמחוברים
 * לצ'אט; אחרת הוא נסגר מיד ולא מופיע כלום (כמו RcsService).
 */
class ChatService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, notification())
        FutureChat.init(this)
        if (!FutureChat.isActive) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }
        FutureChat.keepListening = true
        FutureChat.attach(this)
        return START_STICKY
    }

    override fun onDestroy() {
        FutureChat.keepListening = false
        FutureChat.detach()
        super.onDestroy()
    }

    private fun notification() = run {
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel(CHANNEL_ID, tr("צ'אט FutureOS"), NotificationManager.IMPORTANCE_MIN))
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.sym_action_chat)
            .setContentTitle(tr("צ'אט FutureOS פעיל"))
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setOngoing(true)
            .build()
    }

    /** מפעיל את השירות אחרי אתחול - בלי זה הודעות צ'אט לא יגיעו עד שפותחים את Messages. */
    class BootReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == Intent.ACTION_BOOT_COMPLETED) start(context)
        }
    }

    companion object {
        private const val CHANNEL_ID = "chat_service"
        private const val NOTIFICATION_ID = 7271

        /** לא מפעיל כלום כשהצ'אט לא מוגדר או שלא מחוברים. */
        fun start(context: Context) {
            if (!ChatBackend.configured) return
            FutureChat.init(context)
            if (!FutureChat.isActive) return
            try {
                context.startForegroundService(Intent(context, ChatService::class.java))
            } catch (e: Exception) {
                android.util.Log.w("ChatService", "Cannot start chat service", e)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, ChatService::class.java))
        }
    }
}
