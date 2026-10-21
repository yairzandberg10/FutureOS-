package com.future.messages.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.future.messages.MainActivity

/**
 * מקבל WAP_PUSH_DELIVER_ACTION - נדרש כדי שהמערכת תסכים לראות באפליקציה הזו
 * ברירת מחדל תקינה למסרונים. פענוח והורדת MMS מלאים לא ממומשים כרגע (רק SMS),
 * אבל MMS נכנס כבר לא נבלע בשקט: מוצגת התראה שפותחת את האפליקציה (M29).
 */
class MmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val channelId = "sms_incoming"
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(NotificationChannel(channelId, "הודעות נכנסות", NotificationManager.IMPORTANCE_HIGH))
        }
        val openIntent = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val pendingIntent = PendingIntent.getActivity(
            context, MMS_NOTIFICATION_ID, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.sym_action_chat)
            .setContentTitle("הודעת MMS נכנסה")
            .setContentText("הורדה והצגה של MMS עדיין לא נתמכות במכשיר הזה")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()
        runCatching { manager.notify(MMS_NOTIFICATION_ID, notification) }
    }

    private companion object {
        const val MMS_NOTIFICATION_ID = 0x4d4d53
    }
}
