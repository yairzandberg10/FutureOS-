package com.future.assistant.data

import android.content.Context
import android.provider.ContactsContract

/** חיפוש איש קשר לפי שם (התאמה חלקית) כדי לתמוך ב"התקשר לאמא" ולא רק
 * "חייג 0501234567" - בלי אינטרנט, ישירות מול ContactsContract. */
object ContactFinder {
    data class ContactResult(val name: String?, val number: String?, val hasPermission: Boolean)

    fun findByName(context: Context, query: String): ContactResult {
        val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER
        )
        return try {
            context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                val nameIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numberIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                // התאמה מדויקת קודמת. אחריה התאמה חלקית - ושם קצר (למשל "אל") לא נמצא בתוך כל שאילתה
                var partial: ContactResult? = null
                while (cursor.moveToNext()) {
                    val contactName = cursor.getString(nameIdx) ?: continue
                    val number = cursor.getString(numberIdx)
                    if (contactName.equals(query, ignoreCase = true)) {
                        return ContactResult(contactName, number, hasPermission = true)
                    }
                    if (partial == null && (contactName.contains(query, ignoreCase = true) ||
                            (contactName.length >= 3 && query.contains(contactName, ignoreCase = true)))
                    ) {
                        partial = ContactResult(contactName, number, hasPermission = true)
                    }
                }
                partial ?: ContactResult(null, null, hasPermission = true)
            } ?: ContactResult(null, null, hasPermission = true)
        } catch (e: SecurityException) {
            ContactResult(null, null, hasPermission = false)
        }
    }
}
