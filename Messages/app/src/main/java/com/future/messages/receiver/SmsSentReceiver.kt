package com.future.messages.receiver

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.future.messages.data.SmsRepository
import java.util.concurrent.ConcurrentHashMap

/**
 * מקבל את תוצאת השליחה האמיתית של כל חלק ב-SMS (sentIntent שהועבר ל-
 * SmsManager.sendMultipartTextMessage). הודעה ארוכה מתפצלת למספר חלקים,
 * שכל אחד מדווח בנפרד ולא בהכרח באותו סדר - לכן אוספים את התוצאות לפי
 * messageId (ב-remainingParts/anyPartFailed) ומעדכנים את השורה בספק
 * ל-SENT/FAILED רק אחרי שכל החלקים דיווחו. זה מה שנותן למשתמש משוב אמיתי
 * אם ההודעה יצאה מהמכשיר בפועל, בניגוד להתנהגות הקודמת שהניחה הצלחה מיד
 * עם הקריאה ל-SmsManager.
 */
class SmsSentReceiver : BroadcastReceiver() {
    companion object {
        const val ACTION_SMS_SENT = "com.future.messages.action.SMS_SENT"
        const val EXTRA_MESSAGE_ID = "message_id"
        const val EXTRA_PART_COUNT = "part_count"

        private val remainingParts = ConcurrentHashMap<Long, Int>()
        private val anyPartFailed = ConcurrentHashMap<Long, Boolean>()
    }

    override fun onReceive(context: Context, intent: Intent) {
        val messageId = intent.getLongExtra(EXTRA_MESSAGE_ID, -1L)
        if (messageId == -1L) return
        val partCount = intent.getIntExtra(EXTRA_PART_COUNT, 1)
        val succeeded = resultCode == Activity.RESULT_OK

        if (!succeeded) anyPartFailed[messageId] = true

        val stillWaiting = remainingParts.compute(messageId) { _, previous -> (previous ?: partCount) - 1 } ?: 0
        if (stillWaiting <= 0) {
            remainingParts.remove(messageId)
            val failed = anyPartFailed.remove(messageId) ?: false
            SmsRepository(context.applicationContext).updateSentMessageStatus(messageId, success = !failed)
        }
    }
}
