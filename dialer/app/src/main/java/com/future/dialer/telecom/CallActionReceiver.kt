package com.future.dialer.telecom

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.future.dialer.MainActivity

/**
 * מקבל את מקשי CALL/ENDCALL הפיזיים ששודרו גלובלית ע"י StatusBarAccessibilityService של
 * FutureUI - נחוץ כשהמכשיר נמצא בתוך אפליקציה אחרת ורק הבאנר (heads-up) מוצג, לא מסך
 * השיחה עצמו, כי אז onKeyDown של MainActivity לא מקבל את הלחיצה בכלל (הוא לא בחזית).
 *
 * גם מקבל ACTION_LAUNCH_CALL_UI - נשלח כש-FutureLauncher (מסך הבית) הוא האפליקציה
 * בחזית בזמן שיחה מצלצלת, כי ה-fullScreenIntent הרגיל של ההתראה לא מופעל אוטומטית
 * ע"י המערכת כשהמסך דלוק ולא נעול.
 */
class CallActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_ANSWER_CALL -> CallService.answer()
            ACTION_REJECT_CALL -> CallService.reject()
            ACTION_LAUNCH_CALL_UI -> {
                val launchIntent = Intent(context, MainActivity::class.java)
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                context.startActivity(launchIntent)
            }
        }
    }

    companion object {
        const val ACTION_ANSWER_CALL = "com.future.dialer.ACTION_ANSWER_CALL"
        const val ACTION_REJECT_CALL = "com.future.dialer.ACTION_REJECT_CALL"
        const val ACTION_LAUNCH_CALL_UI = "com.future.dialer.ACTION_LAUNCH_CALL_UI"
    }
}
