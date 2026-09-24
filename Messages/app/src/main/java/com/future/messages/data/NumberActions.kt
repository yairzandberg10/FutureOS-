package com.future.messages.data

import android.content.ContentProviderOperation
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.BlockedNumberContract
import android.provider.ContactsContract
import android.util.Log

/**
 * פעולות על מספר טלפון בודד - ממסך "הודעה חדשה", כשמקלידים מספר ועומדים
 * עליו: חסימה והוספה למועדפים (חיוג/הוספה לאנשי קשר/הודעה הם Intents).
 */
object NumberActions {
    private const val TAG = "NumberActions"

    /** מספר טלפון "סביר" - ספרות, ואולי + בהתחלה ורווחים/מקפים. */
    fun looksLikePhoneNumber(text: String): Boolean {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return false
        val digits = trimmed.count { it.isDigit() }
        return digits >= 3 && trimmed.all { it.isDigit() || it in "+-() *#" }
    }

    /** מזהה איש הקשר שהמספר שייך לו, או null. */
    fun contactIdFor(context: Context, number: String): Long? = try {
        val uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number))
        context.contentResolver.query(uri, arrayOf(ContactsContract.PhoneLookup._ID), null, null, null)?.use {
            if (it.moveToFirst()) it.getLong(0) else null
        }
    } catch (e: Exception) {
        Log.e(TAG, "lookup failed", e)
        null
    }

    /** חסימה ברשימת החסומים של המערכת - מותר לאפליקציית ה-SMS המוגדרת כברירת מחדל. */
    fun block(context: Context, number: String): Boolean = try {
        if (!BlockedNumberContract.canCurrentUserBlockNumbers(context)) {
            false
        } else {
            val values = ContentValues().apply {
                put(BlockedNumberContract.BlockedNumbers.COLUMN_ORIGINAL_NUMBER, number)
            }
            context.contentResolver.insert(BlockedNumberContract.BlockedNumbers.CONTENT_URI, values) != null
        }
    } catch (e: Exception) {
        Log.e(TAG, "block failed", e)
        false
    }

    /**
     * מועדפים = STARRED של איש הקשר. מספר שאינו איש קשר נשמר קודם כאיש קשר
     * (השם הוא המספר עצמו) - אחרת אין למה לסמן כוכב.
     */
    fun addToFavorites(context: Context, number: String): Boolean = try {
        val id = contactIdFor(context, number) ?: createContact(context, number)
        if (id == null) {
            false
        } else {
            val values = ContentValues().apply { put(ContactsContract.Contacts.STARRED, 1) }
            val uri = android.content.ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, id)
            context.contentResolver.update(uri, values, null, null) > 0
        }
    } catch (e: Exception) {
        Log.e(TAG, "favorite failed", e)
        false
    }

    private fun createContact(context: Context, number: String): Long? {
        val ops = arrayListOf(
            ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
                .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
                .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null)
                .build(),
            ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, number)
                .build(),
            ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, number)
                .withValue(ContactsContract.CommonDataKinds.Phone.TYPE, ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE)
                .build(),
        )
        context.contentResolver.applyBatch(ContactsContract.AUTHORITY, ops)
        return contactIdFor(context, number)
    }
}
