package com.future.dialer.data.repository

import android.content.Context
import android.net.Uri
import android.provider.ContactsContract
import android.util.Log
import com.future.dialer.data.model.Contact
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ContactRepository(private val context: Context) {

    /** מזהה שם איש קשר אמיתי לפי מספר טלפון (למשל לשיחה נכנסת) - אם לא נמצא, מחזיר null. */
    fun findNameForNumber(number: String): String? {
        return try {
            val uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number))
            context.contentResolver.query(uri, arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            }
        } catch (e: Exception) {
            Log.e("ContactRepository", "Error resolving name for $number", e)
            null
        }
    }

    suspend fun getAllContacts(): List<Contact> = withContext(Dispatchers.IO) {
        val contacts = mutableListOf<Contact>()
        val seenIds = HashSet<String>()
        try {
            val cursor = context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(
                    ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Phone.NUMBER,
                    ContactsContract.CommonDataKinds.Phone.PHOTO_THUMBNAIL_URI,
                    ContactsContract.CommonDataKinds.Phone.STARRED
                ),
                null,
                null,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
            )

            cursor?.use {
                val idIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
                val nameIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numberIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                val photoIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.PHOTO_THUMBNAIL_URI)
                val starredIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.STARRED)

                while (it.moveToNext()) {
                    val id = if (idIndex != -1) it.getString(idIndex) else ""
                    // מספר טלפון אחד לכל איש קשר ברשימה - אנשי קשר עם כמה מספרים
                    // חוזרים כמה פעמים ב-Phone.CONTENT_URI, זה שובר את המיון האלפביתי.
                    if (id.isEmpty() || !seenIds.add(id)) continue
                    val name = if (nameIndex != -1) it.getString(nameIndex) ?: "Unknown" else "Unknown"
                    val number = if (numberIndex != -1) it.getString(numberIndex) ?: "" else ""
                    val photo = if (photoIndex != -1) it.getString(photoIndex) else null
                    val starred = starredIndex != -1 && it.getInt(starredIndex) == 1

                    contacts.add(
                        Contact(
                            id = id,
                            name = name,
                            phoneNumber = number,
                            photoUri = photo,
                            isFavorite = starred
                        )
                    )
                }
            }
        } catch (e: SecurityException) {
            Log.e("ContactRepository", "Permission denied for contacts", e)
        } catch (e: Exception) {
            Log.e("ContactRepository", "Error reading contacts", e)
        }
        contacts.sortedWith(compareByDescending<Contact> { it.isFavorite }.thenBy { it.name.lowercase() })
    }

    suspend fun setFavorite(contactId: String, isFavorite: Boolean) = withContext(Dispatchers.IO) {
        try {
            val uri = android.content.ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, contactId.toLong())
            val values = android.content.ContentValues().apply {
                put(ContactsContract.Contacts.STARRED, if (isFavorite) 1 else 0)
            }
            context.contentResolver.update(uri, values, null, null)
        } catch (e: Exception) {
            Log.e("ContactRepository", "Error setting favorite", e)
        }
    }
}
