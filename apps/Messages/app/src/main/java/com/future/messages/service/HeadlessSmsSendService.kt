package com.future.messages.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.future.messages.data.SmsRepository

private const val ACTION_RESPOND_VIA_MESSAGE = "android.intent.action.RESPOND_VIA_MESSAGE"

/**
 * מטפל ב-ACTION_RESPOND_VIA_MESSAGE - כשאפליקציה אחרת (למשל חייגן, כשיש שיחה
 * נכנסת) מבקשת לשלוח "תשובה מהירה" בלי לפתוח את Messages. נדרש כדי שהמערכת
 * תסכים לראות באפליקציה הזו ברירת מחדל תקינה למסרונים.
 */
class HeadlessSmsSendService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_RESPOND_VIA_MESSAGE) {
            // smsto:0501234567?body=... - הכתובת היא מה שלפני ה-'?', השאר הוא גוף ההודעה.
            val address = intent.data?.schemeSpecificPart?.substringBefore('?')
            val text = intent.getStringExtra(Intent.EXTRA_TEXT)
            if (!address.isNullOrBlank() && !text.isNullOrBlank()) {
                SmsRepository(applicationContext).sendMessage(address, text)
            }
        }
        stopSelf(startId)
        return START_NOT_STICKY
    }
}
