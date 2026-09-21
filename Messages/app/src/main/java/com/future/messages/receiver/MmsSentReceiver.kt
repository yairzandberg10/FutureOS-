package com.future.messages.receiver

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.SmsManager
import android.util.Log
import android.widget.Toast
import com.future.messages.data.SmsRepository
import java.io.File

/**
 * מקבל את תוצאת שליחת ה-MMS בפועל ומעדכן את שורת ה-MMS ב-content://mms
 * מ-OUTBOX ("שולח...") ל-SENT/FAILED בהתאם - זה מה שנותן למשתמש משוב אמיתי
 * בבועת ההודעה עצמה, לא רק Toast חד-פעמי. גם מציג Toast עם סיבת הכישלון
 * (לרוב אין חבילת נתונים סלולרית/MMS פעילה), ומנקה את קובץ ה-PDU הזמני
 * מה-cache בכל מקרה.
 */
class MmsSentReceiver : BroadcastReceiver() {
    companion object {
        const val ACTION_MMS_SENT = "com.future.messages.action.MMS_SENT"
        const val EXTRA_FILE_PATH = "file_path"
        const val EXTRA_MMS_ID = "mms_id"
        private const val TAG = "MmsSentReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val succeeded = resultCode == Activity.RESULT_OK
        val mmsId = intent.getLongExtra(EXTRA_MMS_ID, -1L)
        if (mmsId != -1L) {
            SmsRepository(context.applicationContext).updateMmsStatus(mmsId, success = succeeded)
        }

        if (resultCode != Activity.RESULT_OK) {
            val reason = when (resultCode) {
                SmsManager.MMS_ERROR_UNSPECIFIED -> "שגיאה לא מזוהה"
                SmsManager.MMS_ERROR_INVALID_APN -> "הגדרות APN שגויות"
                SmsManager.MMS_ERROR_UNABLE_CONNECT_MMS -> "אין אפשרות להתחבר לשרת ה-MMS"
                SmsManager.MMS_ERROR_HTTP_FAILURE -> "שגיאת רשת בשליחה"
                SmsManager.MMS_ERROR_IO_ERROR -> "שגיאת קלט/פלט"
                SmsManager.MMS_ERROR_RETRY -> "נדרש ניסיון חוזר"
                SmsManager.MMS_ERROR_CONFIGURATION_ERROR -> "שגיאת תצורה"
                SmsManager.MMS_ERROR_NO_DATA_NETWORK -> "אין חיבור לרשת סלולרית לנתונים"
                else -> "קוד שגיאה $resultCode"
            }
            Log.e(TAG, "MMS send failed: $reason")
            Toast.makeText(context, "שליחת ה-MMS נכשלה: $reason", Toast.LENGTH_LONG).show()
        }

        intent.getStringExtra(EXTRA_FILE_PATH)?.let { path ->
            runCatching { File(path).delete() }
        }
    }
}
