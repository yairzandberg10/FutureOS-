package com.future.contact.data

import android.content.ContentProviderOperation
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.provider.ContactsContract
import java.io.ByteArrayOutputStream
import java.text.Collator
import java.util.Locale

data class Contact(
    val id: String,
    val name: String,
    val phoneNumbers: List<String>,
    /** תמונה ממוזערת (PHOTO_THUMBNAIL_URI) - לרשימות ולכרטיס. */
    val photoUri: String?,
    val isFavorite: Boolean = false,
    val lookupKey: String = "",
    /** "משפחה, פרטי" - למיון לפי שם משפחה. */
    val alternativeName: String = "",
    /** חסום: שיחות ממנו נדחות (SEND_TO_VOICEMAIL) והמספרים ברשימת החסומים של החייגן. */
    val isBlocked: Boolean = false,
    val updatedAt: Long = 0L,
)

data class ContactDetails(
    val email: String = "",
    val organization: String = "",
    val jobTitle: String = "",
    val address: String = "",
    val notes: String = ""
)

enum class ContactSort(val label: String) {
    FIRST_NAME("שם פרטי"),
    LAST_NAME("שם משפחה"),
    RECENT("עודכנו לאחרונה"),
}

/** גישה אמיתית לספק אנשי הקשר של אנדרואיד - בלי נתונים מדומים. */
class ContactsRepository(private val context: Context) {

    fun getAllContacts(sort: ContactSort = ContactSort.FIRST_NAME): List<Contact> {
        val phones = HashMap<String, MutableList<String>>()
        try {
            context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(ContactsContract.CommonDataKinds.Phone.CONTACT_ID, ContactsContract.CommonDataKinds.Phone.NUMBER),
                null, null, null
            )?.use { c ->
                while (c.moveToNext()) {
                    val id = c.getString(0) ?: continue
                    val number = c.getString(1) ?: continue
                    phones.getOrPut(id) { mutableListOf() }.add(number)
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("ContactsRepository", "Error loading phones", e)
        }

        val result = mutableListOf<Contact>()
        try {
            context.contentResolver.query(
                ContactsContract.Contacts.CONTENT_URI,
                arrayOf(
                    ContactsContract.Contacts._ID,
                    ContactsContract.Contacts.DISPLAY_NAME_PRIMARY,
                    ContactsContract.Contacts.DISPLAY_NAME_ALTERNATIVE,
                    ContactsContract.Contacts.PHOTO_THUMBNAIL_URI,
                    ContactsContract.Contacts.STARRED,
                    ContactsContract.Contacts.LOOKUP_KEY,
                    ContactsContract.Contacts.SEND_TO_VOICEMAIL,
                    ContactsContract.Contacts.CONTACT_LAST_UPDATED_TIMESTAMP,
                ),
                null, null, null
            )?.use { c ->
                while (c.moveToNext()) {
                    val id = c.getString(0) ?: continue
                    val name = c.getString(1)?.takeIf { it.isNotBlank() } ?: phones[id]?.firstOrNull() ?: continue
                    result += Contact(
                        id = id,
                        name = name,
                        phoneNumbers = phones[id].orEmpty().map { it.trim() }.distinctBy { it.filter(Char::isDigit) },
                        photoUri = c.getString(3),
                        isFavorite = c.getInt(4) == 1,
                        lookupKey = c.getString(5).orEmpty(),
                        alternativeName = c.getString(2).orEmpty(),
                        isBlocked = c.getInt(6) == 1,
                        updatedAt = c.getLong(7),
                    )
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("ContactsRepository", "Error loading contacts", e)
        }
        return sortContacts(result, sort)
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

    /**
     * חסימה: SEND_TO_VOICEMAIL על איש הקשר (הטלפון דוחה את שיחותיו), ובנוסף
     * בקשה לחייגן - אפליקציית ברירת המחדל לשיחות, היחידה שמורשית לכתוב
     * לרשימת החסומים של המערכת - להוסיף/להסיר את המספרים שלו שם.
     */
    fun setBlocked(contact: Contact, blocked: Boolean): Boolean = try {
        val uri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, contact.id.toLong())
        val values = ContentValues().apply { put(ContactsContract.Contacts.SEND_TO_VOICEMAIL, if (blocked) 1 else 0) }
        context.contentResolver.update(uri, values, null, null)
        contact.phoneNumbers.forEach { number ->
            context.sendBroadcast(
                Intent(ACTION_SET_BLOCKED)
                    .setPackage(DIALER_PACKAGE)
                    .putExtra(EXTRA_NUMBER, number)
                    .putExtra(EXTRA_BLOCKED, blocked),
                SYSTEM_PERMISSION,
            )
        }
        true
    } catch (e: Exception) {
        android.util.Log.e("ContactsRepository", "Error setting blocked", e)
        false
    }

    /**
     * תמונת פרופיל: נכתבת כ-DisplayPhoto של ה-RawContact, כלומר בספק אנשי
     * הקשר עצמו. משם היא מופיעה בחייגן ובהודעות, ואם איש הקשר שייך לחשבון
     * מסונכרן (Google) - גם בכל מכשיר אחר של אותו חשבון.
     */
    fun setPhoto(contactId: String, image: Uri): Boolean = try {
        val rawId = findRawContactId(contactId)?.toLong() ?: error("no raw contact")
        val source = ImageDecoder.createSource(context.contentResolver, image)
        val bitmap = ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
            val scale = maxOf(1, minOf(info.size.width, info.size.height) / PHOTO_PX)
            decoder.setTargetSize(info.size.width / scale, info.size.height / scale)
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
        }
        val bytes = ByteArrayOutputStream().use { out ->
            centerSquare(bitmap).compress(Bitmap.CompressFormat.JPEG, 88, out)
            out.toByteArray()
        }
        val photoUri = Uri.withAppendedPath(
            ContentUris.withAppendedId(ContactsContract.RawContacts.CONTENT_URI, rawId),
            ContactsContract.RawContacts.DisplayPhoto.CONTENT_DIRECTORY
        )
        context.contentResolver.openAssetFileDescriptor(photoUri, "rw")?.use { fd ->
            fd.createOutputStream().use { it.write(bytes) }
        } ?: error("no photo descriptor")
        true
    } catch (e: Exception) {
        android.util.Log.e("ContactsRepository", "Error setting photo", e)
        false
    }

    fun removePhoto(contactId: String): Boolean = try {
        val rawId = findRawContactId(contactId) ?: error("no raw contact")
        context.contentResolver.delete(
            ContactsContract.Data.CONTENT_URI,
            "${ContactsContract.Data.RAW_CONTACT_ID} = ? AND ${ContactsContract.Data.MIMETYPE} = ?",
            arrayOf(rawId, ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE)
        )
        true
    } catch (e: Exception) {
        false
    }

    /** vCard של איש הקשר - מה שנשלח כשמשתפים אותו. */
    fun vcardUri(contact: Contact): Uri? =
        contact.lookupKey.takeIf { it.isNotBlank() }?.let { Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_VCARD_URI, it) }

    /** איש קשר חדש בלי חשבון (מקומי במכשיר) - שם ומספר. */
    fun addContact(name: String, number: String): Boolean = try {
        val ops = arrayListOf(
            ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
                .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
                .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null)
                .build(),
            ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, name)
                .build(),
        )
        if (number.isNotBlank()) {
            ops += ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, number)
                .withValue(ContactsContract.CommonDataKinds.Phone.TYPE, ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE)
                .build()
        }
        context.contentResolver.applyBatch(ContactsContract.AUTHORITY, ops)
        true
    } catch (e: Exception) {
        android.util.Log.e("ContactsRepository", "Error adding contact", e)
        false
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

    /** מעדכן את פרטי איש הקשר בספק. מחזיר true רק אם הכתיבה הצליחה בפועל. */
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

    fun deleteContact(contactId: String): Boolean = try {
        context.contentResolver.delete(Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI, contactId), null, null) > 0
    } catch (e: Exception) {
        false
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
            context.contentResolver.update(ContactsContract.Data.CONTENT_URI, values, "${ContactsContract.Data._ID} = ?", arrayOf(existingId))
        } else {
            context.contentResolver.insert(ContactsContract.Data.CONTENT_URI, values)
        }
    }

    fun hasContactsPermission(): Boolean {
        return context.checkSelfPermission(android.Manifest.permission.READ_CONTACTS) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    private fun centerSquare(bitmap: Bitmap): Bitmap {
        val side = minOf(bitmap.width, bitmap.height)
        return Bitmap.createBitmap(bitmap, (bitmap.width - side) / 2, (bitmap.height - side) / 2, side, side)
    }

    companion object {
        /** צד התמונה שנשמרת לאיש הקשר - גודל DisplayPhoto רגיל. */
        private const val PHOTO_PX = 720

        const val DIALER_PACKAGE = "com.future.dialer"
        const val ACTION_SET_BLOCKED = "com.future.dialer.ACTION_SET_BLOCKED"
        const val EXTRA_NUMBER = "number"
        const val EXTRA_BLOCKED = "blocked"
        const val SYSTEM_PERMISSION = "com.future.futureui.permission.SYSTEM_SETTINGS"

        /**
         * סדר האותיות: שמות בעברית קודם, אחריהם שמות באותיות לועזיות, ובסוף
         * כל השאר (ספרות, סימנים). בתוך כל קבוצה - סדר מילוני של השפה.
         */
        fun sortContacts(list: List<Contact>, sort: ContactSort): List<Contact> {
            val collator = Collator.getInstance(Locale.forLanguageTag("he")).apply { strength = Collator.PRIMARY }
            if (sort == ContactSort.RECENT) return list.sortedByDescending { it.updatedAt }
            val key: (Contact) -> String = {
                if (sort == ContactSort.LAST_NAME) it.alternativeName.ifBlank { it.name } else it.name
            }
            return list.sortedWith(
                compareBy<Contact> { scriptBucket(key(it)) }.thenComparator { a, b -> collator.compare(key(a), key(b)) }
            )
        }

        fun scriptBucket(name: String): Int {
            val first = name.trim().firstOrNull() ?: return 3
            return when {
                first in '֐'..'׿' -> 0
                first.isLetter() && first.code < 0x0250 -> 1
                first.isLetter() -> 2
                else -> 3
            }
        }
    }
}
