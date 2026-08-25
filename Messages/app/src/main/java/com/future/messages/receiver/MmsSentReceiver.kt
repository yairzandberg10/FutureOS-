package com.future.messages.receiver

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.SmsManager
import android.util.Log
import android.widget.Toast
import java.io.File

/**
 * מקבל את תוצאת שליחת ה-MMS. תפקידו היחיד: להודיע למשתמש אם השליחה בפועל
 * נכשלה (לרוב בגלל שאין חבילת נתונים סלולרית/MMS פעילה), ולנקות את קובץ ה-PDU
 * הזמני מה-cache בכל מקרה. רישום ההודעה כ"נשלחה" ב-content://mms כבר קרה
 * מיידית ב-SmsRepository - זה תואם את הדפוס הקיים לגבי SMS.
 */
class MmsSentReceiver : BroadcastReceiver() {
    companion object {
        const val ACTION_MMS_SENT = "com.future.messages.action.MMS_SENT"
        const val EXTRA_FILE_PATH = "file_path"
        private const val TAG = "MmsSentReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
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
