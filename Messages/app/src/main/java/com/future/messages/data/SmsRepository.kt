package com.future.messages.data

import android.app.PendingIntent
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
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
import com.future.messages.receiver.SmsDeliveredReceiver
import com.future.messages.receiver.SmsSentReceiver
import java.io.ByteArrayOutputStream
import java.io.File

/**
 * שכבת גישה אמיתית לספק ה-SMS של אנדרואיד. אין כאן שום נתון מדומה - הכל
 * נקרא/נכתב דרך content://sms ו-content://mms-sms/conversations בפועל.
 */
class SmsRepository(private val context: Context) {

    /**
     * מטמון שם איש קשר לפי מספר. בלעדיו getConversations ביצעה שאילתת
     * ContentResolver נפרדת לכל שיחה (N+1) - בתיבה עם מאות שיחות זה היה
     * הרכיב היקר ביותר בטעינה. המפה נשארת חיה לכל אורך חיי ה-repository,
     * ומתרוקנת מפורשות כשרשימת אנשי הקשר יכולה היה להשתנות (ראו
     * clearContactCache).
     */
    private val contactCache = java.util.concurrent.ConcurrentHashMap<String, Contact>()

    /** לקרוא אחרי הוספה/עריכה של איש קשר, כדי ששם חדש יופיע בשיחות. */
    fun clearContactCache() = contactCache.clear()

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
        val projection = arrayOf(
            Telephony.Sms._ID, Telephony.Sms.BODY, Telephony.Sms.DATE, Telephony.Sms.TYPE,
            Telephony.Sms.READ, Telephony.Sms.STATUS
        )
        try {
            context.contentResolver.query(
                uri, projection, "${Telephony.Sms.THREAD_ID} = ?", arrayOf(threadId.toString()), "${Telephony.Sms.DATE} ASC"
            )?.use { cursor ->
                val idCol = cursor.getColumnIndex(Telephony.Sms._ID)
                val bodyCol = cursor.getColumnIndex(Telephony.Sms.BODY)
                val dateCol = cursor.getColumnIndex(Telephony.Sms.DATE)
                val typeCol = cursor.getColumnIndex(Telephony.Sms.TYPE)
                val readCol = cursor.getColumnIndex(Telephony.Sms.READ)
                val statusCol = cursor.getColumnIndex(Telephony.Sms.STATUS)
                while (cursor.moveToNext()) {
                    val type = cursor.getInt(typeCol)
                    messages.add(
                        Message(
                            id = cursor.getLong(idCol),
                            text = cursor.getString(bodyCol) ?: "",
                            timestamp = cursor.getLong(dateCol),
                            isFromMe = type != Telephony.Sms.MESSAGE_TYPE_INBOX,
                            isRead = cursor.getInt(readCol) == 1,
                            status = smsStatusFor(type, cursor.getInt(statusCol))
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
                    val box = cursor.getInt(boxCol)
                    val (text, imageUri) = readMmsParts(mmsId)
                    messages.add(
                        Message(
                            id = mmsId,
                            text = text,
                            timestamp = cursor.getLong(dateCol) * 1000L,
                            isFromMe = box != Telephony.Mms.MESSAGE_BOX_INBOX,
                            isRead = cursor.getInt(readCol) == 1,
                            isMms = true,
                            imageUri = imageUri,
                            status = mmsStatusFor(box)
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

    /** ממפה TYPE+STATUS של שורת SMS למצב תצוגה - null להודעות נכנסות, שאין
     * להן "סטטוס שליחה" בכלל. */
    private fun smsStatusFor(type: Int, smsStatus: Int): MessageStatus? = when (type) {
        Telephony.Sms.MESSAGE_TYPE_OUTBOX, Telephony.Sms.MESSAGE_TYPE_QUEUED -> MessageStatus.SENDING
        Telephony.Sms.MESSAGE_TYPE_FAILED -> MessageStatus.FAILED
        Telephony.Sms.MESSAGE_TYPE_SENT ->
            if (smsStatus == Telephony.Sms.STATUS_COMPLETE) MessageStatus.DELIVERED else MessageStatus.SENT
        else -> null
    }

    /** ממפה MESSAGE_BOX של שורת MMS למצב תצוגה - אין ל-MMS דיווח מסירה כמו
     * ל-SMS, אז אין מצב DELIVERED כאן. */
    private fun mmsStatusFor(box: Int): MessageStatus? = when (box) {
        Telephony.Mms.MESSAGE_BOX_OUTBOX -> MessageStatus.SENDING
        Telephony.Mms.MESSAGE_BOX_FAILED -> MessageStatus.FAILED
        Telephony.Mms.MESSAGE_BOX_SENT -> MessageStatus.SENT
        else -> null
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
     * שולח הודעת SMS אמיתית (מפוצלת אוטומטית אם ארוכה). ההודעה נרשמת מיד
     * בספק במצב MESSAGE_TYPE_OUTBOX ("שולח...") - לא כ"נשלחה" - ורק
     * SmsSentReceiver, שמקבל את תוצאת השליחה האמיתית מהמערכת (sentIntent
     * לכל חלק), מעדכן אותה בפועל ל-SENT/FAILED. כך למשתמש יש משוב אמיתי אם
     * ההודעה באמת יצאה מהמכשיר, לא רק ש-SmsManager הסכים לקבל אותה.
     * מחזירה את מזהה השורה שנוצרה, או null אם אפילו הרישום/הקריאה ל-SmsManager נכשלו.
     */
    fun sendMessage(address: String, text: String): Long? {
        return try {
            val threadId = Telephony.Threads.getOrCreateThreadId(context, address)
            val values = ContentValues().apply {
                put(Telephony.Sms.THREAD_ID, threadId)
                put(Telephony.Sms.ADDRESS, address)
                put(Telephony.Sms.BODY, text)
                put(Telephony.Sms.DATE, System.currentTimeMillis())
                put(Telephony.Sms.READ, 1)
                put(Telephony.Sms.TYPE, Telephony.Sms.MESSAGE_TYPE_OUTBOX)
            }
            val insertedUri = context.contentResolver.insert(Telephony.Sms.CONTENT_URI, values) ?: return null
            val messageId = ContentUris.parseId(insertedUri)

            val smsManager = context.getSystemService(SmsManager::class.java)
            val parts = smsManager.divideMessage(text)
            val sentIntents = ArrayList(parts.indices.map { index ->
                sentPendingIntent(messageId, index, parts.size)
            })
            val deliveryIntents = ArrayList(parts.indices.map { index ->
                deliveryPendingIntent(messageId, index)
            })
            smsManager.sendMultipartTextMessage(address, null, parts, sentIntents, deliveryIntents)
            messageId
        } catch (e: Exception) {
            Log.e("SmsRepository", "Error sending message to $address", e)
            null
        }
    }

    private fun sentPendingIntent(messageId: Long, partIndex: Int, partCount: Int): PendingIntent {
        val intent = Intent(context, SmsSentReceiver::class.java).apply {
            action = SmsSentReceiver.ACTION_SMS_SENT
            putExtra(SmsSentReceiver.EXTRA_MESSAGE_ID, messageId)
            putExtra(SmsSentReceiver.EXTRA_PART_COUNT, partCount)
        }
        // requestCode חייב להיות שונה לכל (הודעה, חלק) כדי ש-PendingIntent.getBroadcast
        // לא יחזיר מופע ממוחזר עם extras של שליחה קודמת (FLAG_UPDATE_CURRENT מחליף
        // extras על אותו requestCode - בלי הפרדה כזו חלקים/הודעות שונות "יתבלבלו").
        val requestCode = (messageId * 16 + partIndex).toInt()
        return PendingIntent.getBroadcast(
            context, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun deliveryPendingIntent(messageId: Long, partIndex: Int): PendingIntent {
        val intent = Intent(context, SmsDeliveredReceiver::class.java).apply {
            action = SmsDeliveredReceiver.ACTION_SMS_DELIVERED
            putExtra(SmsDeliveredReceiver.EXTRA_MESSAGE_ID, messageId)
        }
        val requestCode = (messageId * 16 + partIndex).toInt()
        return PendingIntent.getBroadcast(
            context, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /** נקראת מ-SmsSentReceiver ברגע שידוע אם החלק/החלקים האחרונים של ההודעה
     * באמת נשלחו או לא - מעדכנת את שורת ה-SMS בספק מ-OUTBOX ל-SENT/FAILED. */
    fun updateSentMessageStatus(messageId: Long, success: Boolean) {
        try {
            val values = ContentValues().apply {
                put(
                    Telephony.Sms.TYPE,
                    if (success) Telephony.Sms.MESSAGE_TYPE_SENT else Telephony.Sms.MESSAGE_TYPE_FAILED
                )
            }
            context.contentResolver.update(ContentUris.withAppendedId(Telephony.Sms.CONTENT_URI, messageId), values, null, null)
        } catch (e: Exception) {
            Log.e("SmsRepository", "Error updating sent status for message $messageId", e)
        }
    }

    /** נקראת מ-SmsDeliveredReceiver כשמגיע דיווח מסירה מהרשת (לא כל הספקים/
     * מכשירים שולחים כזה - אם הוא לא מגיע, ההודעה פשוט נשארת ב-SENT). */
    fun updateDeliveryStatus(messageId: Long, delivered: Boolean) {
        try {
            val values = ContentValues().apply {
                put(Telephony.Sms.STATUS, if (delivered) Telephony.Sms.STATUS_COMPLETE else Telephony.Sms.STATUS_FAILED)
            }
            context.contentResolver.update(ContentUris.withAppendedId(Telephony.Sms.CONTENT_URI, messageId), values, null, null)
        } catch (e: Exception) {
            Log.e("SmsRepository", "Error updating delivery status for message $messageId", e)
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

            var imageBytes: ByteArray? = null
            if (imageUri != null) {
                // ה-MmsService דוחה PDU שגדול ממגבלת הספק (לרוב 300KB-1MB) עם
                // MMS_ERROR_IO_ERROR - תמונת מצלמה מקורית (כמה MB) תמיד נכשלת. לכן
                // מקטינים ודוחסים ל-JPEG שנכנס במגבלה, עם מרווח לטקסט/SMIL/כותרות.
                val budget = (mmsMaxMessageSize() - 8 * 1024 - text.toByteArray(Charsets.UTF_8).size)
                    .coerceAtLeast(50 * 1024)
                imageBytes = compressImageForMms(imageUri, budget)
                imageMime = "image/jpeg"
                val bytes = imageBytes ?: return false
                val imagePart = PduPart()
                imagePart.setContentType(imageMime.toByteArray())
                imagePart.setContentLocation("image.jpg".toByteArray())
                imagePart.setContentId("image".toByteArray())
                imagePart.setData(bytes)
                body.addPart(imagePart)
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
            smilPart.setData(buildSmilDocument(hasImage = imageBytes != null, hasText = text.isNotBlank()).toByteArray())
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

            // נרשם מיד כ-OUTBOX ("שולח...") - לא כ-SENT - כדי שהמשתמש יראה
            // משוב אמיתי; MmsSentReceiver מעדכן ל-SENT/FAILED לפי תוצאת השליחה
            // בפועל שמגיעה אסינכרונית מהמערכת.
            val mmsId = insertPendingMms(threadId, address, text, imageBytes, imageMime) ?: return false

            val sentIntent = Intent(context, MmsSentReceiver::class.java).apply {
                action = MmsSentReceiver.ACTION_MMS_SENT
                putExtra(MmsSentReceiver.EXTRA_FILE_PATH, file.absolutePath)
                putExtra(MmsSentReceiver.EXTRA_MMS_ID, mmsId)
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context, file.name.hashCode(), sentIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val smsManager = context.getSystemService(SmsManager::class.java)
            smsManager.sendMultimediaMessage(context, contentUri, null, null, pendingIntent)
            true
        } catch (e: Exception) {
            Log.e("SmsRepository", "Error sending MMS to $address", e)
            false
        }
    }

    /** נקראת מ-MmsSentReceiver ברגע שידועה תוצאת שליחת ה-MMS בפועל - מעדכנת
     * את שורת ה-MMS בספק מ-OUTBOX ל-SENT/FAILED. */
    fun updateMmsStatus(mmsId: Long, success: Boolean) {
        try {
            val values = ContentValues().apply {
                put(
                    Telephony.Mms.MESSAGE_BOX,
                    if (success) Telephony.Mms.MESSAGE_BOX_SENT else Telephony.Mms.MESSAGE_BOX_FAILED
                )
            }
            context.contentResolver.update(ContentUris.withAppendedId(Telephony.Mms.CONTENT_URI, mmsId), values, null, null)
        } catch (e: Exception) {
            Log.e("SmsRepository", "Error updating MMS status for $mmsId", e)
        }
    }

    /** מגבלת גודל ה-MMS של הספק (מה-carrier config של המערכת); 300KB אם לא ידוע.
     * חסום ב-1MB כי ספקים מסוימים מדווחים גבוה ממה שה-MMSC באמת מקבל. */
    private fun mmsMaxMessageSize(): Int {
        val fromCarrier = runCatching {
            context.getSystemService(SmsManager::class.java)
                .carrierConfigValues.getInt(SmsManager.MMS_CONFIG_MAX_MESSAGE_SIZE, 0)
        }.getOrDefault(0)
        return if (fromCarrier > 0) fromCarrier.coerceAtMost(1024 * 1024) else 300 * 1024
    }

    /** מפענח את התמונה (ImageDecoder מיישם את סיבוב ה-EXIF), מקטין לצלע ארוכה
     * של 1280px לכל היותר, ודוחס ל-JPEG - מוריד איכות ואז רזולוציה עד שנכנס ב-maxBytes. */
    private fun compressImageForMms(imageUri: Uri, maxBytes: Int): ByteArray? {
        return try {
            var maxSide = 1280
            val source = ImageDecoder.createSource(context.contentResolver, imageUri)
            var bitmap = ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                val w = info.size.width
                val h = info.size.height
                val scale = maxSide.toFloat() / maxOf(w, h)
                if (scale < 1f) decoder.setTargetSize((w * scale).toInt().coerceAtLeast(1), (h * scale).toInt().coerceAtLeast(1))
            }
            var result: ByteArray? = null
            while (result == null && maxSide >= 240) {
                for (quality in intArrayOf(95, 90, 85, 75, 65, 50)) {
                    val out = ByteArrayOutputStream()
                    bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
                    if (out.size() <= maxBytes) { result = out.toByteArray(); break }
                }
                if (result != null) break
                maxSide = (maxSide * 0.75f).toInt()
                val scale = maxSide.toFloat() / maxOf(bitmap.width, bitmap.height)
                bitmap = Bitmap.createScaledBitmap(
                    bitmap,
                    (bitmap.width * scale).toInt().coerceAtLeast(1),
                    (bitmap.height * scale).toInt().coerceAtLeast(1),
                    true
                )
            }
            result
        } catch (e: Exception) {
            Log.e("SmsRepository", "Error compressing image for MMS", e)
            null
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

    /** רושם את ה-MMS ב-content://mms במצב OUTBOX ("שולח...") כדי שיופיע מיד
     * בהיסטוריית השיחה, עוד לפני שידועה תוצאת השליחה בפועל. מחזירה את מזהה
     * השורה שנוצרה כדי ש-sendMmsMessage יוכל להעביר אותו ל-MmsSentReceiver. */
    private fun insertPendingMms(threadId: Long, address: String, text: String, imageBytes: ByteArray?, imageMimeType: String?): Long? {
        try {
            val mmsValues = ContentValues().apply {
                put(Telephony.Mms.THREAD_ID, threadId)
                put(Telephony.Mms.DATE, System.currentTimeMillis() / 1000)
                put(Telephony.Mms.MESSAGE_BOX, Telephony.Mms.MESSAGE_BOX_OUTBOX)
                put(Telephony.Mms.READ, 1)
                put(Telephony.Mms.MESSAGE_TYPE, PduHeaders.MESSAGE_TYPE_SEND_REQ)
                put(Telephony.Mms.MMS_VERSION, PduHeaders.CURRENT_MMS_VERSION)
                put(Telephony.Mms.CONTENT_TYPE, ContentType.MULTIPART_RELATED)
                put(Telephony.Mms.TEXT_ONLY, if (imageBytes == null) 1 else 0)
            }
            val mmsUri = context.contentResolver.insert(Telephony.Mms.CONTENT_URI, mmsValues) ?: run {
                Log.e("SmsRepository", "Failed to insert pending MMS record")
                return null
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

            if (imageBytes != null) {
                val imageValues = ContentValues().apply {
                    put(Telephony.Mms.Part.MSG_ID, mmsId)
                    put(Telephony.Mms.Part.CONTENT_TYPE, imageMimeType ?: "image/jpeg")
                    put(Telephony.Mms.Part.NAME, "image")
                }
                val partUri = context.contentResolver.insert(partsUri, imageValues)
                if (partUri != null) {
                    context.contentResolver.openOutputStream(partUri)?.use { out -> out.write(imageBytes) }
                }
            }

            val addrValues = ContentValues().apply {
                put(Telephony.Mms.Addr.ADDRESS, address)
                put(Telephony.Mms.Addr.CHARSET, CharacterSets.UTF_8)
                put(Telephony.Mms.Addr.TYPE, PduHeaders.TO)
            }
            context.contentResolver.insert(Uri.withAppendedPath(mmsUri, "addr"), addrValues)
            return mmsId
        } catch (e: Exception) {
            Log.e("SmsRepository", "Error persisting pending MMS", e)
            return null
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
        contactCache[address]?.let { return it }
        val resolved = lookupContact(address)
        contactCache[address] = resolved
        return resolved
    }

    private fun lookupContact(address: String): Contact {
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

    /** חיפוש אנשי קשר לפי שם/מספר בזמן הקלדה - להתחלת שיחה חדשה בלי לזכור מספר
     * בעל-פה. CONTENT_FILTER_URI של Phone (לא של Contacts) מחזיר כבר את שם + מספר
     * בשורה אחת לכל תוצאה, כולל התאמה חלקית על שם. */
    fun searchContacts(query: String, limit: Int = 20): List<Contact> {
        if (query.isBlank()) return emptyList()
        val results = mutableListOf<Contact>()
        try {
            val uri = Uri.withAppendedPath(ContactsContract.CommonDataKinds.Phone.CONTENT_FILTER_URI, Uri.encode(query))
            context.contentResolver.query(
                uri,
                arrayOf(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME, ContactsContract.CommonDataKinds.Phone.NUMBER),
                null, null, null,
            )?.use { cursor ->
                val seenNumbers = mutableSetOf<String>()
                while (cursor.moveToNext() && results.size < limit) {
                    val name = cursor.getString(0) ?: continue
                    val number = cursor.getString(1) ?: continue
                    if (seenNumbers.add(number)) {
                        results.add(Contact(name = name, phoneNumber = number))
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("SmsRepository", "Error searching contacts for '$query'", e)
        }
        return results
    }

    /** כל אנשי הקשר עם מספר טלפון, ממוינים לפי שם - לבחירת נמענים במסך
     * הודעה קבוצתית (לא שאילתת חיפוש חלקי כמו searchContacts, אלא הרשימה
     * המלאה שהמשתמש מסמן ממנה). */
    fun getAllContacts(): List<Contact> {
        val results = mutableListOf<Contact>()
        try {
            context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME, ContactsContract.CommonDataKinds.Phone.NUMBER),
                null, null,
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC",
            )?.use { cursor ->
                val seenNumbers = mutableSetOf<String>()
                while (cursor.moveToNext()) {
                    val name = cursor.getString(0) ?: continue
                    val number = cursor.getString(1) ?: continue
                    if (seenNumbers.add(number)) {
                        results.add(Contact(name = name, phoneNumber = number))
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("SmsRepository", "Error loading all contacts", e)
        }
        return results
    }

    /** מוחקת שיחה שלמה (כל ה-SMS/MMS שלה) לפי thread_id - אותו URI תקני
     * (content://mms-sms/conversations/<id>) שאפליקציות מסרונים סטנדרטיות
     * משתמשות בו למחיקת שיחה שלמה בבת אחת. */
    fun deleteThread(threadId: Long): Boolean {
        return try {
            context.contentResolver.delete(
                ContentUris.withAppendedId(Telephony.Threads.CONTENT_URI, threadId), null, null
            ) >= 0
        } catch (e: Exception) {
            Log.e("SmsRepository", "Error deleting thread $threadId", e)
            false
        }
    }
}
