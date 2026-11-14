package com.future.messages.receiver

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.future.messages.data.SmsRepository
import java.util.concurrent.ConcurrentHashMap

/**
 * מקבל את דיווח המסירה (deliveryIntent) מהרשת - לא כל הספקים/מכשירי הצד
 * השני שולחים כזה, ואם הוא לא מגיע ההודעה פשוט נשארת במצב SENT (לא DELIVERED)
 * לצמיתות, וזה בסדר. בניגוד ל-SmsSentReceiver, אין כאן צורך לצבור לפי
 * חלקים - כל חלק שמדווח פשוט מעדכן את אותו עמודת STATUS, וזה בטוח (idempotent).
 * הודעה מפוצלת: "נמסר" רק אחרי שכל החלקים נמסרו, וכשל של חלק אחד מסמן כשל (M08).
 */
class SmsDeliveredReceiver : BroadcastReceiver() {
    companion object {
        const val ACTION_SMS_DELIVERED = "com.future.messages.action.SMS_DELIVERED"
        const val EXTRA_MESSAGE_ID = "message_id"
        const val EXTRA_PART_COUNT = "part_count"

        private val remainingParts = ConcurrentHashMap<Long, Int>()
        private val anyPartFailed = ConcurrentHashMap<Long, Boolean>()
    }

    override fun onReceive(context: Context, intent: Intent) {
        val messageId = intent.getLongExtra(EXTRA_MESSAGE_ID, -1L)
        if (messageId == -1L) return
        val partCount = intent.getIntExtra(EXTRA_PART_COUNT, 1)
        val delivered = resultCode == Activity.RESULT_OK
        if (!delivered) anyPartFailed[messageId] = true
        val stillWaiting = remainingParts.compute(messageId) { _, previous -> (previous ?: partCount) - 1 } ?: 0
        // חלק נמסר ועוד חלקים ממתינים: עדיין אין מה לעדכן
        if (stillWaiting > 0 && delivered) return
        if (stillWaiting <= 0) remainingParts.remove(messageId)
        val allDelivered = stillWaiting <= 0 && anyPartFailed[messageId] != true
        if (stillWaiting <= 0) anyPartFailed.remove(messageId)
        val pending = goAsync()
        Thread {
            try {
                SmsRepository(context.applicationContext).updateDeliveryStatus(messageId, allDelivered)
            } finally {
                pending.finish()
            }
        }.start()
    }
}
