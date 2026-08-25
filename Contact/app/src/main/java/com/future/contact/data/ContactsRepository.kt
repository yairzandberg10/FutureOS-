package com.future.contact.data

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.provider.ContactsContract

data class Contact(
    val id: String,
    val name: String,
    val phoneNumbers: List<String>,
    val photoUri: String?,
    val isFavorite: Boolean = false
)

data class ContactDetails(
    val email: String = "",
    val organization: String = "",
    val jobTitle: String = "",
    val address: String = "",
    val notes: String = ""
)

/** גישה אמיתית לספק אנשי הקשר של אנדרואיד - בלי נתונים מדומים. */
class ContactsRepository(private val context: Context) {

    fun getAllContacts(): List<Contact> {
        val contacts = LinkedHashMap<String, MutableList<String>>()
        val names = HashMap<String, String>()
        val photos = HashMap<String, String?>()
        val starred = HashMap<String, Boolean>()

        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER,
            ContactsContract.CommonDataKinds.Phone.PHOTO_URI,
            ContactsContract.CommonDataKinds.Phone.STARRED
        )

        try {
            context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                projection, null, null,
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"
            )?.use { cursor ->
                val idCol = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
                val nameCol = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numberCol = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                val photoCol = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.PHOTO_URI)
                val starredCol = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.STARRED)

                while (cursor.moveToNext()) {
                    val id = cursor.getString(idCol) ?: continue
                    val name = cursor.getString(nameCol) ?: continue
                    val number = cursor.getString(numberCol) ?: ""
                    names[id] = name
                    photos[id] = if (photoCol >= 0) cursor.getString(photoCol) else null
                    starred[id] = starredCol >= 0 && cursor.getInt(starredCol) == 1
                    contacts.getOrPut(id) { mutableListOf() }.add(number)
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("ContactsRepository", "Error loading contacts", e)
        }

        return contacts.map { (id, numbers) ->
            Contact(
                id = id,
                name = names[id] ?: "",
                phoneNumbers = numbers.distinct(),
                photoUri = photos[id],
                isFavorite = starred[id] ?: false
            )
        }.sortedWith(compareByDescending<Contact> { it.isFavorite }.thenBy { it.name.lowercase() })
    }

    fun setFavorite(contactId: String, isFavorite: Boolean) {
        try {
            val uri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, contactId.toLong())
            val values = ContentValues().apply { put(ContactsContract.Contacts.STARRED, if (isFavorite) 1 else 0) }
            context.contentResolver.update(uri, values, null, null)
        } catch (e: Exception) {
            android.util.Log.e("ContactsRepository", "Error setting favorite", e)
        }
    }

    /** שולף מייל, ארגון/תפקיד, כתובת והערות מטבלת ה-Data - שדות מורחבים שלא
     * זמינים דרך שאילתת Phone.CONTENT_URI הרגילה. */
    fun getContactDetails(contactId: String): ContactDetails {
        var email = ""
        var organization = ""
        var jobTitle = ""
        var address = ""
        var notes = ""
        try {
            context.contentResolver.query(
                ContactsContract.Data.CONTENT_URI,
                null,
                "${ContactsContract.Data.CONTACT_ID} = ?",
                arrayOf(contactId),
                null
            )?.use { cursor ->
                val mimeCol = cursor.getColumnIndex(ContactsContract.Data.MIMETYPE)
                while (cursor.moveToNext()) {
                    when (cursor.getString(mimeCol)) {
                        ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE -> {
                            if (email.isEmpty()) {
                                val col = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.ADDRESS)
                                if (col >= 0) email = cursor.getString(col) ?: ""
                            }
                        }
                        ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE -> {
                            val orgCol = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Organization.COMPANY)
                            val titleCol = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Organization.TITLE)
                            if (orgCol >= 0) organization = cursor.getString(orgCol) ?: ""
                            if (titleCol >= 0) jobTitle = cursor.getString(titleCol) ?: ""
                        }
                        ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE -> {
                            if (address.isEmpty()) {
                                val col = cursor.getColumnIndex(ContactsContract.CommonDataKinds.StructuredPostal.FORMATTED_ADDRESS)
                                if (col >= 0) address = cursor.getString(col) ?: ""
                            }
                        }
                        ContactsContract.CommonDataKinds.Note.CONTENT_ITEM_TYPE -> {
                            val col = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Note.NOTE)
                            if (col >= 0) notes = cursor.getString(col) ?: ""
                        }
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("ContactsRepository", "Error loading contact details", e)
        }
        return ContactDetails(email, organization, jobTitle, address, notes)
    }

    /** מעדכן את פרטי איש הקשר בספק. מחזיר true רק אם הכתיבה הצליחה בפועל -
     * לפני התיקון הכשל היה "בולע" חריגה ומדווח הצלחה שקטה, מה שגרם ל-UI
     * להציג את הערכים החדשים גם כשהם מעולם לא נשמרו בפועל (למשל כשההרשאה
     * WRITE_CONTACTS לא ניתנה). */
    fun updateContactDetails(contactId: String, details: ContactDetails): Boolean {
        return try {
            val rawContactId = findRawContactId(contactId) ?: return false
            upsertDataRow(rawContactId, ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE) { values ->
                values.put(ContactsContract.CommonDataKinds.Email.ADDRESS, details.email)
                values.put(ContactsContract.CommonDataKinds.Email.TYPE, ContactsContract.CommonDataKinds.Email.TYPE_HOME)
            }
            upsertDataRow(rawContactId, ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE) { values ->
                values.put(ContactsContract.CommonDataKinds.Organization.COMPANY, details.organization)
                values.put(ContactsContract.CommonDataKinds.Organization.TITLE, details.jobTitle)
            }
            upsertDataRow(rawContactId, ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE) { values ->
                values.put(ContactsContract.CommonDataKinds.StructuredPostal.FORMATTED_ADDRESS, details.address)
                values.put(ContactsContract.CommonDataKinds.StructuredPostal.TYPE, ContactsContract.CommonDataKinds.StructuredPostal.TYPE_HOME)
            }
            upsertDataRow(rawContactId, ContactsContract.CommonDataKinds.Note.CONTENT_ITEM_TYPE) { values ->
                values.put(ContactsContract.CommonDataKinds.Note.NOTE, details.notes)
            }
            true
        } catch (e: Exception) {
            android.util.Log.e("ContactsRepository", "Error updating contact details", e)
            false
        }
    }

    private fun findRawContactId(contactId: String): String? {
        return context.contentResolver.query(
            ContactsContract.RawContacts.CONTENT_URI,
            arrayOf(ContactsContract.RawContacts._ID),
            "${ContactsContract.RawContacts.CONTACT_ID} = ?",
            arrayOf(contactId),
            null
        )?.use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null }
    }

    private fun upsertDataRow(rawContactId: String, mimeType: String, fill: (ContentValues) -> Unit) {
        val existingId = context.contentResolver.query(
            ContactsContract.Data.CONTENT_URI,
            arrayOf(ContactsContract.Data._ID),
            "${ContactsContract.Data.RAW_CONTACT_ID} = ? AND ${ContactsContract.Data.MIMETYPE} = ?",
            arrayOf(rawContactId, mimeType),
            null
        )?.use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null }

        val values = ContentValues().apply {
            put(ContactsContract.Data.RAW_CONTACT_ID, rawContactId)
            put(ContactsContract.Data.MIMETYPE, mimeType)
            fill(this)
        }

        if (existingId != null) {
            context.contentResolver.update(
                ContactsContract.Data.CONTENT_URI,
                values,
                "${ContactsContract.Data._ID} = ?",
                arrayOf(existingId)
            )
        } else {
            context.contentResolver.insert(ContactsContract.Data.CONTENT_URI, values)
        }
    }

    fun hasContactsPermission(): Boolean {
        return context.checkSelfPermission(android.Manifest.permission.READ_CONTACTS) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED
    }
}
