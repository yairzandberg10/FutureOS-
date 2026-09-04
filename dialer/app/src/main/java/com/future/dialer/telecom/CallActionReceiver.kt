package com.future.dialer.telecom

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * מקבל את מקשי CALL/ENDCALL הפיזיים ששודרו גלובלית ע"י LockScreenAccessibilityService של
 * FutureUI - נחוץ כשהמכשיר נמצא בתוך אפליקציה אחרת ורק הבאנר (heads-up) מוצג, לא מסך
 * השיחה עצמו, כי אז onKeyDown של MainActivity לא מקבל את הלחיצה בכלל (הוא לא בחזית).
 */
class CallActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_ANSWER_CALL -> CallService.answer()
            ACTION_REJECT_CALL -> CallService.reject()
        }
    }

    companion object {
        const val ACTION_ANSWER_CALL = "com.future.dialer.ACTION_ANSWER_CALL"
        const val ACTION_REJECT_CALL = "com.future.dialer.ACTION_REJECT_CALL"
    }
}
