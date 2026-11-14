package com.future.flashlight.data

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import com.future.flashlight.MainActivity
import com.future.flashlight.R

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
        // ערוץ ההתראה נוצר פעם אחת כאן (FL-24), ולא בכל בניית התראה
        getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CHANNEL, getString(R.string.torch_channel_name), NotificationManager.IMPORTANCE_LOW)
        )
        // כובה ממקום אחר (מרכז הבקרה, המצלמה) - השירות נסגר איתו. רק אחרי
        // שהפנס נראה דלוק: registerTorchCallback מדווח מיד את המצב הנוכחי
        // ("כבוי"), והדיווח הזה הגיע אחרי ההדלקה - השירות נסגר וכיבה את הפנס
        // מיד, כך שהפנס לא נדלק בכלל.
        var seenOn = false
        stopObserving = controller.observe { on, _ ->
            if (on) seenOn = true else if (seenOn) stopSelf()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // הכיבוי נעשה ב-onDestroy (שנקרא מ-stopSelf), כך שאין קריאה כפולה ל-setTorch
        if (intent?.action == ACTION_OFF) {
            stopSelf()
            return START_NOT_STICKY
        }
        // קודם ההדלקה: אם היא נכשלת לא מציגים התראה "הפנס דלוק", ומודיעים למשתמש
        if (!controller.setTorch(true)) {
            android.widget.Toast.makeText(this, getString(R.string.torch_failed), android.widget.Toast.LENGTH_SHORT).show()
            stopSelf()
            return START_NOT_STICKY
        }
        startForeground(NOTIFICATION_ID, notification())
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        stopObserving()
        controller.setTorch(false)
        // FL-30: ההתראה נשארה אחרי כיבוי כי הייתה ongoing - מסירים אותה במפורש
        stopForeground(STOP_FOREGROUND_REMOVE)
        getSystemService(NotificationManager::class.java).cancel(NOTIFICATION_ID)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun notification(): Notification {
        val off = PendingIntent.getService(
            this, 0, Intent(this, TorchService::class.java).setAction(ACTION_OFF),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        // לחיצה על ההתראה פותחת את האפליקציה (FL-12); הכפתור "כבה" מכבה
        val open = PendingIntent.getActivity(
            this, 1, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        return Notification.Builder(this, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .setContentTitle(getString(R.string.torch_notif_title))
            .setContentText(getString(R.string.torch_notif_text))
            .setContentIntent(open)
            .addAction(0, getString(R.string.torch_notif_action_off), off)
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
            // הפנס יכול להיות דלוק בלי השירות (למשל מהבקרה): stopService לבדו לא מכבה אותו (FL-01)
            FlashlightController(context).setTorch(false)
            context.stopService(Intent(context, TorchService::class.java))
        }
    }
}
