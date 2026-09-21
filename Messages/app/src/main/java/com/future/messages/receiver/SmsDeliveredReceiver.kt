package com.future.messages.receiver

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.future.messages.data.SmsRepository

/**
 * מקבל את דיווח המסירה (deliveryIntent) מהרשת - לא כל הספקים/מכשירי הצד
 * השני שולחים כזה, ואם הוא לא מגיע ההודעה פשוט נשארת במצב SENT (לא DELIVERED)
 * לצמיתות, וזה בסדר. בניגוד ל-SmsSentReceiver, אין כאן צורך לצבור לפי
 * חלקים - כל חלק שמדווח פשוט מעדכן את אותו עמודת STATUS, וזה בטוח (idempotent).
 */
class SmsDeliveredReceiver : BroadcastReceiver() {
    companion object {
        const val ACTION_SMS_DELIVERED = "com.future.messages.action.SMS_DELIVERED"
        const val EXTRA_MESSAGE_ID = "message_id"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val messageId = intent.getLongExtra(EXTRA_MESSAGE_ID, -1L)
        if (messageId == -1L) return
        val delivered = resultCode == Activity.RESULT_OK
        SmsRepository(context.applicationContext).updateDeliveryStatus(messageId, delivered)
    }
}
