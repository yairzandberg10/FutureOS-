package com.future.bluetooth.data

import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

/**
 * לקוח OBEX Object Push (OPP) מינימלי - הפרוטוקול שבו טלפונים שולחים קבצים
 * בבלוטות'. רץ מעל ערוץ RFCOMM שכבר פתוח (ר' [TransferManager]).
 *
 * הרצף: CONNECT, ואז לכל קובץ PUT עם השם והגודל ואחריו חבילות Body, והחבילה
 * האחרונה עם End-of-Body. בסוף DISCONNECT. רוב המכשירים מעכבים את התשובה
 * לחבילת ה-PUT הראשונה עד שהמשתמש בצד השני מאשר - זה שלב "ממתין לאישור".
 *
 * כל הקריאות חוסמות. ביטול נעשה מבחוץ בסגירת הסוקט, שמעיף כאן IOException.
 */
class ObexPush(private val input: InputStream, private val output: OutputStream) {

    /** גודל החבילה המקסימלי שסוכם עם הצד השני ב-CONNECT. */
    private var maxPacket = MIN_PACKET

    /** תשובה שאינה הצלחה/המשך. [code] הוא קוד התשובה של OBEX. */
    class ObexRefused(val code: Int) : IOException("OBEX 0x" + code.toString(16))

    fun connect() {
        val packet = byteArrayOf(
            OP_CONNECT.toByte(), 0, 7,
            OBEX_VERSION, 0,
            (OUR_MAX_PACKET shr 8).toByte(), (OUR_MAX_PACKET and 0xFF).toByte(),
        )
        output.write(packet)
        output.flush()
        val response = readResponse()
        if (response.code != RESP_SUCCESS) throw ObexRefused(response.code)
        // תשובת CONNECT: גרסה, דגלים ושני בתים של גודל החבילה של הצד השני.
        if (response.body.size >= 4) {
            val theirs = ((response.body[2].toInt() and 0xFF) shl 8) or (response.body[3].toInt() and 0xFF)
            maxPacket = minOf(OUR_MAX_PACKET, theirs).coerceAtLeast(MIN_PACKET)
        }
    }

    /**
     * שולח קובץ אחד. [onAccepted] נקרא כשהצד השני אישר את הקבלה (התשובה
     * לחבילה הראשונה), ו-[onProgress] אחרי כל חבילה עם מספר הבתים שנשלחו.
     */
    fun put(
        name: String,
        mime: String?,
        length: Long,
        data: InputStream,
        onAccepted: () -> Unit,
        onProgress: (Long) -> Unit,
    ) {
        // החבילה הראשונה: רק כותרות. גוף הקובץ בחבילות שאחריה.
        val headers = java.io.ByteArrayOutputStream()
        headers.write(unicodeHeader(HI_NAME, name))
        if (!mime.isNullOrEmpty()) headers.write(byteSeqHeader(HI_TYPE, (mime + "\u0000").toByteArray(Charsets.US_ASCII)))
        if (length in 0..0xFFFFFFFFL) headers.write(int4Header(HI_LENGTH, length))
        writePacket(OP_PUT, headers.toByteArray())
        var response = readResponse()
        if (response.code != RESP_CONTINUE && response.code != RESP_SUCCESS) throw ObexRefused(response.code)
        // חלק מהמקבלים שואלים את המשתמש רק כשמגיע תוכן, ולא על חבילת הכותרות -
        // לכן "אושר" רק אחרי שחבילת התוכן הראשונה קיבלה תשובה.
        var accepted = false
        fun acknowledged() {
            if (!accepted) {
                accepted = true
                onAccepted()
            }
        }

        val chunk = maxPacket - 3 - 3
        val buffer = ByteArray(chunk)
        var sent = 0L
        while (true) {
            val read = readFully(data, buffer)
            if (read < chunk) {
                // החבילה האחרונה: End-of-Body (אולי ריק) עם ביט הסיום.
                writePacket(OP_PUT_FINAL, byteSeqHeader(HI_END_OF_BODY, buffer, read))
                sent += read
                response = readResponse()
                if (response.code != RESP_SUCCESS) throw ObexRefused(response.code)
                acknowledged()
                onProgress(sent)
                return
            }
            writePacket(OP_PUT, byteSeqHeader(HI_BODY, buffer, read))
            sent += read
            response = readResponse()
            if (response.code != RESP_CONTINUE) throw ObexRefused(response.code)
            acknowledged()
            onProgress(sent)
        }
    }

    /** סגירה מנומסת. שגיאה כאן לא משנה כלום - הקבצים כבר נשלחו. */
    fun disconnect() {
        runCatching {
            output.write(byteArrayOf(OP_DISCONNECT.toByte(), 0, 3))
            output.flush()
            readResponse()
        }
    }

    // ---- חבילות ----

    private class Response(val code: Int, val body: ByteArray)

    private fun writePacket(opcode: Int, payload: ByteArray) {
        val total = 3 + payload.size
        output.write(byteArrayOf(opcode.toByte(), (total shr 8).toByte(), (total and 0xFF).toByte()))
        output.write(payload)
        output.flush()
    }

    private fun readResponse(): Response {
        val head = ByteArray(3)
        if (readFully(input, head) < 3) throw IOException("OBEX: connection closed")
        val length = ((head[1].toInt() and 0xFF) shl 8) or (head[2].toInt() and 0xFF)
        val body = ByteArray((length - 3).coerceAtLeast(0))
        if (readFully(input, body) < body.size) throw IOException("OBEX: short response")
        // ביט הסיום (0x80) תמיד דלוק בתשובות; משווים בלי לגעת בו.
        return Response(head[0].toInt() and 0xFF, body)
    }

    private fun readFully(stream: InputStream, into: ByteArray): Int {
        var total = 0
        while (total < into.size) {
            val n = stream.read(into, total, into.size - total)
            if (n < 0) break
            total += n
        }
        return total
    }

    private fun unicodeHeader(id: Int, text: String): ByteArray {
        val bytes = (text + "\u0000").toByteArray(Charsets.UTF_16BE)
        return byteSeqHeader(id, bytes)
    }

    private fun byteSeqHeader(id: Int, bytes: ByteArray, count: Int = bytes.size): ByteArray {
        val total = 3 + count
        val out = ByteArray(total)
        out[0] = id.toByte()
        out[1] = (total shr 8).toByte()
        out[2] = (total and 0xFF).toByte()
        System.arraycopy(bytes, 0, out, 3, count)
        return out
    }

    private fun int4Header(id: Int, value: Long): ByteArray = byteArrayOf(
        id.toByte(),
        (value shr 24).toByte(), (value shr 16).toByte(), (value shr 8).toByte(), value.toByte(),
    )

    companion object {
        private const val OP_CONNECT = 0x80
        private const val OP_DISCONNECT = 0x81
        private const val OP_PUT = 0x02
        private const val OP_PUT_FINAL = 0x82

        private const val HI_NAME = 0x01
        private const val HI_TYPE = 0x42
        private const val HI_LENGTH = 0xC3
        private const val HI_BODY = 0x48
        private const val HI_END_OF_BODY = 0x49

        const val RESP_CONTINUE = 0x90
        const val RESP_SUCCESS = 0xA0
        /** הצד השני דחה את הקובץ (המשתמש לחץ "דחה", או שהסוג לא נתמך). */
        const val RESP_FORBIDDEN = 0xC3
        const val RESP_UNAUTHORIZED = 0xC1
        const val RESP_NOT_ACCEPTABLE = 0xC6
        const val RESP_UNSUPPORTED_TYPE = 0xCF
        const val RESP_ENTITY_TOO_LARGE = 0xCD
        const val RESP_DATABASE_FULL = 0xE0

        private const val OBEX_VERSION: Byte = 0x10
        private const val OUR_MAX_PACKET = 0x3FFE
        private const val MIN_PACKET = 255
    }
}
