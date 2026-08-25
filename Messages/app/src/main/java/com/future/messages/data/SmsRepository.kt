package com.future.messages.data

import android.app.PendingIntent
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import android.provider.Telephony
import android.telephony.SmsManager
import android.util.Log
import androidx.core.content.FileProvider
import com.future.messages.mms.ContentType
import com.future.messages.mms.pdu_alt.CharacterSets
import com.future.messages.mms.pdu_alt.EncodedStringValue
import com.future.messages.mms.pdu_alt.PduBody
import com.future.messages.mms.pdu_alt.PduComposer
import com.future.messages.mms.pdu_alt.PduHeaders
import com.future.messages.mms.pdu_alt.PduPart
import com.future.messages.mms.pdu_alt.SendReq
import com.future.messages.receiver.MmsSentReceiver
import java.io.File

/**
 * שכבת גישה אמיתית לספק ה-SMS של אנדרואיד. אין כאן שום נתון מדומה - הכל
 * נקרא/נכתב דרך content://sms ו-content://mms-sms/conversations בפועל.
 */
class SmsRepository(private val context: Context) {

    /**
     * רשימת השיחות, ממוינת מהחדש לישן. שולפת ישירות מ-content://sms ומקבצת
     * לפי thread_id בקוד - לא מסתמכת על content://sms/conversations, שהוא
     * View ישן ולא אמין בהרבה מכשירים (לפעמים ריק גם כשיש הודעות אמיתיות).
     * שאילתה אחת בלבד, לא שאילתה נפרדת לכל שיחה.
     */
    fun getConversations(): List<Conversation> {
        data class ThreadAccumulator(
            var address: String? = null,
            var lastText: String = "",
            var lastDate: Long = 0L,
            var unread: Int = 0
        )
        val byThread = LinkedHashMap<Long, ThreadAccumulator>()

        val projection = arrayOf(
            Telephony.Sms.THREAD_ID, Telephony.Sms.ADDRESS, Telephony.Sms.BODY,
            Telephony.Sms.DATE, Telephony.Sms.READ
        )
        try {
            context.contentResolver.query(
                Telephony.Sms.CONTENT_URI, projection, null, null, "${Telephony.Sms.DATE} DESC"
            )?.use { cursor ->
                val threadIdCol = cursor.getColumnIndex(Telephony.Sms.THREAD_ID)
                val addressCol = cursor.getColumnIndex(Telephony.Sms.ADDRESS)
                val bodyCol = cursor.getColumnIndex(Telephony.Sms.BODY)
                val dateCol = cursor.getColumnIndex(Telephony.Sms.DATE)
                val readCol = cursor.getColumnIndex(Telephony.Sms.READ)

                while (cursor.moveToNext()) {
                    val threadId = cursor.getLong(threadIdCol)
                    val address = cursor.getString(addressCol) ?: continue
                    val isRead = cursor.getInt(readCol) == 1

                    val acc = byThread.getOrPut(threadId) { ThreadAccumulator() }
                    // התוצאות ממוינות מהחדש לישן - הרשומה הראשונה שאנחנו רואים
                    // לכל thread_id היא ההודעה האחרונה בו.
                    if (acc.address == null) {
                        acc.address = address
                        acc.lastText = cursor.getString(bodyCol) ?: ""
                        acc.lastDate = cursor.getLong(dateCol)
                    }
                    if (!isRead) acc.unread++
                }
            }
        } catch (e: Exception) {
            Log.e("SmsRepository", "Error loading conversations", e)
        }

        return byThread.entries
            .mapNotNull { (threadId, acc) ->
                val address = acc.address ?: return@mapNotNull null
                Conversation(
                    threadId = threadId,
                    contact = resolveContact(address),
                    lastMessageText = acc.lastText,
                    lastMessageTimestamp = acc.lastDate,
                    unreadCount = acc.unread
                )
            }
            .sortedByDescending { it.lastMessageTimestamp }
    }

    fun hasReadSmsPermission(): Boolean {
        return context.checkSelfPermission(android.Manifest.permission.READ_SMS) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    fun getMessages(threadId: Long): List<Message> {
        val messages = mutableListOf<Message>()
        val uri = Telephony.Sms.CONTENT_URI
        val projection = arrayOf(Telephony.Sms._ID, Telephony.Sms.BODY, Telephony.Sms.DATE, Telephony.Sms.TYPE, Telephony.Sms.READ)
        try {
            context.contentResolver.query(
                uri, projection, "${Telephony.Sms.THREAD_ID} = ?", arrayOf(threadId.toString()), "${Telephony.Sms.DATE} ASC"
            )?.use { cursor ->
                val idCol = cursor.getColumnIndex(Telephony.Sms._ID)
                val bodyCol = cursor.getColumnIndex(Telephony.Sms.BODY)
                val dateCol = cursor.getColumnIndex(Telephony.Sms.DATE)
                val typeCol = cursor.getColumnIndex(Telephony.Sms.TYPE)
                val readCol = cursor.getColumnIndex(Telephony.Sms.READ)
                while (cursor.moveToNext()) {
                    messages.add(
                        Message(
                            id = cursor.getLong(idCol),
                            text = cursor.getString(bodyCol) ?: "",
                            timestamp = cursor.getLong(dateCol),
                            isFromMe = cursor.getInt(typeCol) == Telephony.Sms.MESSAGE_TYPE_SENT,
                            isRead = cursor.getInt(readCol) == 1
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e("SmsRepository", "Error loading messages for thread $threadId", e)
        }

        // content://mms מחזיק תאריך בשניות (לא במילישניות כמו content://sms) - צריך
        // להכפיל ב-1000 כדי שהמיון המשולב עם הודעות SMS יהיה נכון.
        val mmsProjection = arrayOf(Telephony.Mms._ID, Telephony.Mms.DATE, Telephony.Mms.MESSAGE_BOX, Telephony.Mms.READ)
        try {
            context.contentResolver.query(
                Telephony.Mms.CONTENT_URI, mmsProjection,
                "${Telephony.Mms.THREAD_ID} = ?", arrayOf(threadId.toString()), "${Telephony.Mms.DATE} ASC"
            )?.use { cursor ->
                val idCol = cursor.getColumnIndex(Telephony.Mms._ID)
                val dateCol = cursor.getColumnIndex(Telephony.Mms.DATE)
                val boxCol = cursor.getColumnIndex(Telephony.Mms.MESSAGE_BOX)
                val readCol = cursor.getColumnIndex(Telephony.Mms.READ)
                while (cursor.moveToNext()) {
                    val mmsId = cursor.getLong(idCol)
                    val (text, imageUri) = readMmsParts(mmsId)
                    messages.add(
                        Message(
                            id = mmsId,
                            text = text,
                            timestamp = cursor.getLong(dateCol) * 1000L,
                            isFromMe = cursor.getInt(boxCol) == Telephony.Mms.MESSAGE_BOX_SENT,
                            isRead = cursor.getInt(readCol) == 1,
                            isMms = true,
                            imageUri = imageUri
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e("SmsRepository", "Error loading MMS messages for thread $threadId", e)
        }

        return messages.sortedBy { it.timestamp }
    }

    /** קורא את חלקי ה-MMS (טקסט + תמונה) לפי content://mms/part. */
    private fun readMmsParts(mmsId: Long): Pair<String, Uri?> {
        var text = ""
        var imageUri: Uri? = null
        val projection = arrayOf(Telephony.Mms.Part._ID, Telephony.Mms.Part.CONTENT_TYPE, Telephony.Mms.Part.TEXT)
        try {
            context.contentResolver.query(
                Telephony.Mms.Part.CONTENT_URI, projection,
                "${Telephony.Mms.Part.MSG_ID} = ?", arrayOf(mmsId.toString()), null
            )?.use { cursor ->
                val idCol = cursor.getColumnIndex(Telephony.Mms.Part._ID)
                val ctCol = cursor.getColumnIndex(Telephony.Mms.Part.CONTENT_TYPE)
                val textCol = cursor.getColumnIndex(Telephony.Mms.Part.TEXT)
                while (cursor.moveToNext()) {
                    val partId = cursor.getLong(idCol)
                    val contentType = cursor.getString(ctCol) ?: continue
                    when {
                        contentType == "text/plain" -> {
                            cursor.getString(textCol)?.let { if (it.isNotEmpty()) text = it }
                        }
                        contentType.startsWith("image/") -> {
                            imageUri = ContentUris.withAppendedId(Telephony.Mms.Part.CONTENT_URI, partId)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("SmsRepository", "Error loading parts for mms $mmsId", e)
        }
        return text to imageUri
    }

    /** מוחק הודעה בודדת - SMS או MMS - לפי המזהה שלה. */
    fun deleteMessage(message: Message): Boolean {
        return try {
            val uri = if (message.isMms) {
                ContentUris.withAppendedId(Telephony.Mms.CONTENT_URI, message.id)
            } else {
                ContentUris.withAppendedId(Telephony.Sms.CONTENT_URI, message.id)
            }
            context.contentResolver.delete(uri, null, null) > 0
        } catch (e: Exception) {
            Log.e("SmsRepository", "Error deleting message ${message.id}", e)
            false
        }
    }

    /**
     * שולח הודעת SMS אמיתית (מפוצלת אוטומטית אם ארוכה) ורושם אותה כ"נשלחה" בספק
     * רק אם השליחה בפועל לא זרקה חריגה (למשל: אין סים, אין כיסוי, אין הרשאה) -
     * כדי שהמשתמש לא יראה "נשלח" על הודעה שבפועל מעולם לא יצאה מהמכשיר.
     * מחזירה true אם ההודעה נשלחה ונרשמה בהצלחה, false אחרת.
     */
    fun sendMessage(address: String, text: String): Boolean {
        return try {
            val smsManager = context.getSystemService(SmsManager::class.java)
            val parts = smsManager.divideMessage(text)
            smsManager.sendMultipartTextMessage(address, null, parts, null, null)

            val threadId = Telephony.Threads.getOrCreateThreadId(context, address)
            val values = ContentValues().apply {
                put(Telephony.Sms.THREAD_ID, threadId)
                put(Telephony.Sms.ADDRESS, address)
                put(Telephony.Sms.BODY, text)
                put(Telephony.Sms.DATE, System.currentTimeMillis())
                put(Telephony.Sms.READ, 1)
                put(Telephony.Sms.TYPE, Telephony.Sms.MESSAGE_TYPE_SENT)
            }
            context.contentResolver.insert(Telephony.Sms.CONTENT_URI, values)
            true
        } catch (e: Exception) {
            Log.e("SmsRepository", "Error sending message to $address", e)
            false
        }
    }

    /**
     * שולח MMS אמיתי (טקסט + תמונה אופציונלית) דרך SmsManager.sendMultimediaMessage -
     * ה-API הציבורי הסטנדרטי מ-Android 5.0 ואילך, שבו האפליקציה בונה PDU של
     * MMS (M-Send.req) ומוסרת אותו למערכת; המערכת מבצעת בפועל את ההעלאה ל-MMSC
     * של הספק הסלולרי (כתובת, פרוקסי וכו') - לא צריך לטפל ב-HTTP/APN ידנית.
     * בניית ה-PDU עצמו (קידוד WSP בינארי) משתמשת ב-PduComposer, גרסה מותאמת של
     * ספריית android-smsmms (Apache 2.0) שמבוססת על קוד ה-MMS המקורי של AOSP -
     * לא מומצא, כי קידוד כזה ידני "בערך נכון" ייכשל בשקט מול ה-MMSC בפועל.
     *
     * מחזירה true אם המסירה למערכת הצליחה (לא מבטיחה מסירה סופית ל-MMSC בפועל -
     * זה מגיע אסינכרונית ב-MmsSentReceiver, שמציג Toast אם השליחה נכשלה).
     */
    fun sendMmsMessage(address: String, text: String, imageUri: Uri?): Boolean {
        return try {
            val threadId = Telephony.Threads.getOrCreateThreadId(context, address)

            val sendReq = SendReq()
            sendReq.addTo(EncodedStringValue(address))
            sendReq.setDate(System.currentTimeMillis() / 1000)

            val body = PduBody()
            var imageMime: String? = null

            if (imageUri != null) {
                imageMime = context.contentResolver.getType(imageUri) ?: "image/jpeg"
                val bytes = context.contentResolver.openInputStream(imageUri)?.use { it.readBytes() }
                if (bytes != null) {
                    val imagePart = PduPart()
                    imagePart.setContentType(imageMime.toByteArray())
                    imagePart.setContentLocation("image.jpg".toByteArray())
                    imagePart.setContentId("image".toByteArray())
                    imagePart.setData(bytes)
                    body.addPart(imagePart)
                }
            }
            if (text.isNotBlank()) {
                val textPart = PduPart()
                textPart.setCharset(CharacterSets.UTF_8)
                textPart.setContentType(ContentType.TEXT_PLAIN.toByteArray())
                textPart.setContentLocation("text.txt".toByteArray())
                textPart.setContentId("text".toByteArray())
                textPart.setData(text.toByteArray(Charsets.UTF_8))
                body.addPart(textPart)
            }
            if (body.getPartsNum() == 0) return false

            // חלק SMIL בראש הגוף - מגדיר איך להציג את השקופית (תמונה+טקסט) ביחד.
            // לא חובה טכנית, אבל בלעדיו חלק ממכשירי/אפליקציות MMS אחרות מציגים
            // את החלקים בנפרד או לא מציגים כלום.
            val smilPart = PduPart()
            smilPart.setContentId("smil".toByteArray())
            smilPart.setContentLocation("smil.xml".toByteArray())
            smilPart.setContentType("application/smil".toByteArray())
            smilPart.setData(buildSmilDocument(hasImage = imageUri != null, hasText = text.isNotBlank()).toByteArray())
            body.addPart(0, smilPart)

            sendReq.setBody(body)
            var totalSize = 0L
            for (i in 0 until body.getPartsNum()) totalSize += body.getPart(i).dataLength
            sendReq.setMessageSize(totalSize)
            sendReq.setMessageClass(PduHeaders.MESSAGE_CLASS_PERSONAL_STR.toByteArray())
            sendReq.setExpiry(7 * 24 * 60 * 60L)
            sendReq.setPriority(PduHeaders.PRIORITY_NORMAL)
            sendReq.setDeliveryReport(PduHeaders.VALUE_NO)
            sendReq.setReadReport(PduHeaders.VALUE_NO)

            val pduBytes = PduComposer(context, sendReq).make()
                ?: throw IllegalStateException("PduComposer.make() returned null - invalid PDU")

            val cacheDir = File(context.cacheDir, "mms").apply { mkdirs() }
            val file = File(cacheDir, "send_${System.currentTimeMillis()}.dat")
            file.writeBytes(pduBytes)

            val contentUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            // ה-MmsService של המערכת (תהליך/UID נפרד) צריך גישה לקרוא את קובץ ה-PDU
            // כדי להעלות אותו בפועל ל-MMSC - בלי המענק הזה הוא יקבל SecurityException.
            context.grantUriPermission(
                "com.android.mms.service", contentUri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )

            val sentIntent = Intent(context, MmsSentReceiver::class.java).apply {
                action = MmsSentReceiver.ACTION_MMS_SENT
                putExtra(MmsSentReceiver.EXTRA_FILE_PATH, file.absolutePath)
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context, file.name.hashCode(), sentIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val smsManager = context.getSystemService(SmsManager::class.java)
            smsManager.sendMultimediaMessage(context, contentUri, null, null, pendingIntent)

            insertSentMms(threadId, address, text, imageUri, imageMime)
            true
        } catch (e: Exception) {
            Log.e("SmsRepository", "Error sending MMS to $address", e)
            false
        }
    }

    /** שקופית SMIL בודדת עם תמונה ו/או טקסט - תואם multipart/related. */
    private fun buildSmilDocument(hasImage: Boolean, hasText: Boolean): String {
        val body = buildString {
            append("<par dur=\"5000ms\">")
            if (hasImage) append("<img src=\"image.jpg\" region=\"Image\"/>")
            if (hasText) append("<text src=\"text.txt\" region=\"Text\"/>")
            append("</par>")
        }
        return "<smil><head><layout>" +
            "<root-layout width=\"320px\" height=\"480px\"/>" +
            "<region id=\"Image\" width=\"100%\" height=\"80%\" top=\"0\" left=\"0\" fit=\"meet\"/>" +
            "<region id=\"Text\" width=\"100%\" height=\"20%\" top=\"80%\" left=\"0\" fit=\"scroll\"/>" +
            "</layout></head><body>$body</body></smil>"
    }

    /** רושם את ה-MMS שנשלח ב-content://mms כדי שיופיע מיד בהיסטוריית השיחה. */
    private fun insertSentMms(threadId: Long, address: String, text: String, imageUri: Uri?, imageMimeType: String?) {
        try {
            val mmsValues = ContentValues().apply {
                put(Telephony.Mms.THREAD_ID, threadId)
                put(Telephony.Mms.DATE, System.currentTimeMillis() / 1000)
                put(Telephony.Mms.MESSAGE_BOX, Telephony.Mms.MESSAGE_BOX_SENT)
                put(Telephony.Mms.READ, 1)
                put(Telephony.Mms.MESSAGE_TYPE, PduHeaders.MESSAGE_TYPE_SEND_REQ)
                put(Telephony.Mms.MMS_VERSION, PduHeaders.CURRENT_MMS_VERSION)
                put(Telephony.Mms.CONTENT_TYPE, ContentType.MULTIPART_RELATED)
                put(Telephony.Mms.TEXT_ONLY, if (imageUri == null) 1 else 0)
            }
            val mmsUri = context.contentResolver.insert(Telephony.Mms.CONTENT_URI, mmsValues) ?: run {
                Log.e("SmsRepository", "Failed to insert sent MMS record")
                return
            }
            val mmsId = ContentUris.parseId(mmsUri)
            val partsUri = Uri.withAppendedPath(mmsUri, "part")

            if (text.isNotBlank()) {
                val textValues = ContentValues().apply {
                    put(Telephony.Mms.Part.MSG_ID, mmsId)
                    put(Telephony.Mms.Part.CONTENT_TYPE, ContentType.TEXT_PLAIN)
                    put(Telephony.Mms.Part.CHARSET, CharacterSets.UTF_8)
                    put(Telephony.Mms.Part.TEXT, text)
                }
                context.contentResolver.insert(partsUri, textValues)
            }

            if (imageUri != null) {
                val imageValues = ContentValues().apply {
                    put(Telephony.Mms.Part.MSG_ID, mmsId)
                    put(Telephony.Mms.Part.CONTENT_TYPE, imageMimeType ?: "image/jpeg")
                    put(Telephony.Mms.Part.NAME, "image")
                }
                val partUri = context.contentResolver.insert(partsUri, imageValues)
                if (partUri != null) {
                    context.contentResolver.openOutputStream(partUri)?.use { out ->
                        context.contentResolver.openInputStream(imageUri)?.use { input -> input.copyTo(out) }
                    }
                }
            }

            val addrValues = ContentValues().apply {
                put(Telephony.Mms.Addr.ADDRESS, address)
                put(Telephony.Mms.Addr.CHARSET, CharacterSets.UTF_8)
                put(Telephony.Mms.Addr.TYPE, PduHeaders.TO)
            }
            context.contentResolver.insert(Uri.withAppendedPath(mmsUri, "addr"), addrValues)
        } catch (e: Exception) {
            Log.e("SmsRepository", "Error persisting sent MMS", e)
        }
    }

    fun markThreadRead(threadId: Long) {
        try {
            val values = ContentValues().apply { put(Telephony.Sms.READ, 1) }
            context.contentResolver.update(
                Telephony.Sms.CONTENT_URI, values,
                "${Telephony.Sms.THREAD_ID} = ? AND ${Telephony.Sms.READ} = 0", arrayOf(threadId.toString())
            )
        } catch (e: Exception) {
            Log.e("SmsRepository", "Error marking thread $threadId read", e)
        }
    }

    /** מזהה שם איש קשר אמיתי לפי מספר טלפון; אם לא נמצא, מציג את המספר עצמו. */
    fun resolveContact(address: String): Contact {
        try {
            val uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(address))
            context.contentResolver.query(uri, arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val name = cursor.getString(0)
                    if (!name.isNullOrBlank()) return Contact(name = name, phoneNumber = address)
                }
            }
        } catch (e: Exception) {
            Log.e("SmsRepository", "Error resolving contact for $address", e)
        }
        return Contact(name = address, phoneNumber = address)
    }
}
