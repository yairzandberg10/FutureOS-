package com.future.sharednav.contacts

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.ContactsContract
import android.telephony.PhoneNumberUtils
import android.telephony.TelephonyManager
import android.util.Log
import java.util.Locale

/**
 * שאילתות אנשי קשר שהיו מועתקות בחייגן, באנשי הקשר ובהודעות: שם לפי מספר
 * (PhoneLookup - מתאים גם כשהמספר שמור בפורמט אחר, 050... מול +97250...)
 * וסימון מועדף. חוסם (IPC לספק אנשי הקשר) - לקרוא מ-thread רקע.
 */
object ContactLookup {
    private const val TAG = "ContactLookup"

    /** שם התצוגה של איש הקשר שהמספר שייך לו, או null (גם כשאין הרשאה). */
    fun nameForNumber(context: Context, number: String): String? {
        if (number.isBlank()) return null
        // במכשיר הזה PhoneLookup לא מתאים 0586943731 לאיש קשר שנשמר כ-+972586943731
        // (נמצא בבדיקה) - אז מנסים שוב בפורמט הבינלאומי לפי מדינת הרשת/SIM.
        return phoneLookup(context, number)
            ?: toE164(context, number)?.takeIf { it != number }?.let { phoneLookup(context, it) }
    }

    private fun toE164(context: Context, number: String): String? = try {
        val tm = context.getSystemService(TelephonyManager::class.java)
        val iso = listOfNotNull(tm?.networkCountryIso, tm?.simCountryIso, Locale.getDefault().country)
            .firstOrNull { it.isNotBlank() } ?: "IL"
        PhoneNumberUtils.formatNumberToE164(number, iso.uppercase(Locale.US))
    } catch (e: Exception) {
        null
    }

    private fun phoneLookup(context: Context, number: String): String? {
        return try {
            val uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number))
            context.contentResolver.query(uri, arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME), null, null, null)?.use { c ->
                if (c.moveToFirst()) c.getString(0)?.takeIf { it.isNotBlank() } else null
            }
        } catch (e: Exception) {
            Log.w(TAG, "name lookup failed", e)
            null
        }
    }

    /** מסמן/מבטל מועדף (STARRED). false אם העדכון נכשל. */
    fun setStarred(context: Context, contactId: String, starred: Boolean): Boolean = try {
        val uri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, contactId.toLong())
        val values = ContentValues().apply { put(ContactsContract.Contacts.STARRED, if (starred) 1 else 0) }
        context.contentResolver.update(uri, values, null, null) > 0
    } catch (e: Exception) {
        Log.w(TAG, "setStarred failed", e)
        false
    }
}
