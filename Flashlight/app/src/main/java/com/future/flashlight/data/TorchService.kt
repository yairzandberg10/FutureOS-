package com.future.flashlight.data

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder

/**
 * שומר על הפנס דלוק כשהמסך נכבה או ננעל. הפנס של Camera2 כבה כשהתהליך
 * שהדליק אותו נהרג, ובגרסת release המערכת הורגת תהליך ברקע מהר - לכן כל
 * עוד הפנס דלוק רץ שירות קדמי קטן, עם התראה שמכבה אותו.
 */
class TorchService : Service() {
    private lateinit var controller: FlashlightController
    private var stopObserving: () -> Unit = {}

    override fun onCreate() {
        super.onCreate()
        controller = FlashlightController(this)
        // כובה ממקום אחר (מרכז הבקרה, המצלמה) - השירות נסגר איתו.
        stopObserving = controller.observe { on, _ -> if (!on) stopSelf() }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_OFF) {
            controller.setTorch(false)
            stopSelf()
            return START_NOT_STICKY
        }
        startForeground(NOTIFICATION_ID, notification())
        if (!controller.setTorch(true)) stopSelf()
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        stopObserving()
        controller.setTorch(false)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun notification(): Notification {
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel(CHANNEL, "פנס", NotificationManager.IMPORTANCE_LOW))
        val off = PendingIntent.getService(
            this, 0, Intent(this, TorchService::class.java).setAction(ACTION_OFF),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        return Notification.Builder(this, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .setContentTitle("הפנס דלוק")
            .setContentText("לחץ כדי לכבות")
            .setContentIntent(off)
            .setOngoing(true)
            .build()
    }

    companion object {
        private const val CHANNEL = "torch"
        private const val NOTIFICATION_ID = 1
        private const val ACTION_OFF = "com.future.flashlight.OFF"

        fun turnOn(context: Context) {
            context.startForegroundService(Intent(context, TorchService::class.java))
        }

        fun turnOff(context: Context) {
            context.stopService(Intent(context, TorchService::class.java))
        }
    }
}
