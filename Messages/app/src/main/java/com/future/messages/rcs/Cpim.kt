package com.future.messages.rcs

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * עטיפת CPIM (RFC 3862) עם כותרות IMDN (RFC 5438) - הגוף של כל הודעת RCS.
 * ב-RCS Universal Profile, ב-1:1 שדות ה-From/To של ה-CPIM הם אנונימיים:
 * הזהות האמיתית היא זו של שכבת ה-SIP (P-Asserted-Identity).
 */
internal object Cpim {
    private const val ANONYMOUS = "<sip:anonymous@anonymous.invalid>"
    const val TEXT_PLAIN = "text/plain"
    const val IMDN_XML = "message/imdn+xml"
    const val FT_HTTP_XML = "application/vnd.gsma.rcs-ft-http+xml"

    class Parsed(
        val messageId: String?,
        val dateTime: String?,
        val dispositionNotification: Set<String>,
        val contentType: String,
        val content: ByteArray,
    ) {
        val text: String get() = String(content, Charsets.UTF_8)
    }

    fun buildText(messageId: String, text: String): ByteArray =
        build(messageId, "$TEXT_PLAIN;charset=UTF-8", text.toByteArray(Charsets.UTF_8), requestNotifications = true)

    fun buildImdn(messageId: String, xml: String): ByteArray =
        build(messageId, IMDN_XML, xml.toByteArray(Charsets.UTF_8), requestNotifications = false, isNotification = true)

    private fun build(
        messageId: String,
        contentType: String,
        content: ByteArray,
        requestNotifications: Boolean,
        isNotification: Boolean = false,
    ): ByteArray {
        val head = StringBuilder()
            .append("From: ").append(ANONYMOUS).append(Sip.CRLF)
            .append("To: ").append(ANONYMOUS).append(Sip.CRLF)
            .append("NS: imdn <urn:ietf:params:imdn>").append(Sip.CRLF)
            .append("imdn.Message-ID: ").append(messageId).append(Sip.CRLF)
            .append("DateTime: ").append(now()).append(Sip.CRLF)
        if (requestNotifications) head.append("imdn.Disposition-Notification: positive-delivery, display").append(Sip.CRLF)
        head.append(Sip.CRLF)
            .append("Content-Type: ").append(contentType).append(Sip.CRLF)
        // RFC 5438 סעיף 7.1.1: הודעת IMDN מסומנת כ-notification.
        if (isNotification) head.append("Content-Disposition: notification").append(Sip.CRLF)
        head.append("Content-Length: ").append(content.size).append(Sip.CRLF)
            .append(Sip.CRLF)
        return head.toString().toByteArray(Charsets.UTF_8) + content
    }

    /** מפרק גוף message/cpim: כותרות CPIM, שורה ריקה, כותרות MIME, שורה ריקה, תוכן. */
    fun parse(body: ByteArray): Parsed? {
        val firstEnd = indexOfBlankLine(body, 0) ?: return null
        val cpimHeaders = headers(String(body, 0, firstEnd.first, Charsets.UTF_8))
        val secondEnd = indexOfBlankLine(body, firstEnd.second) ?: return null
        val mimeHeaders = headers(String(body, firstEnd.second, secondEnd.first - firstEnd.second, Charsets.UTF_8))
        var content = body.copyOfRange(secondEnd.second, body.size)
        mimeHeaders["content-length"]?.toIntOrNull()?.let { if (it in 0..content.size) content = content.copyOf(it) }
        // שם ה-namespace של IMDN מוגדר בכותרת NS ואינו חייב להיות "imdn".
        val prefix = cpimHeaders["ns"]?.let { Regex("^(\\S+)\\s*<urn:ietf:params:imdn>").find(it)?.groupValues?.get(1) } ?: "imdn"
        return Parsed(
            messageId = cpimHeaders["$prefix.message-id"],
            dateTime = cpimHeaders["datetime"],
            dispositionNotification = cpimHeaders["$prefix.disposition-notification"]
                ?.split(',')?.map { it.trim().lowercase() }?.toSet() ?: emptySet(),
            contentType = mimeHeaders["content-type"]?.substringBefore(';')?.trim()?.lowercase() ?: TEXT_PLAIN,
            content = content,
        )
    }

    // --- IMDN ---

    enum class Disposition { DELIVERED, DISPLAYED }

    class Notification(val messageId: String, val disposition: Disposition?, val failed: Boolean)

    fun imdnXml(originalMessageId: String, originalDateTime: String?, disposition: Disposition): String {
        val (element, status) = when (disposition) {
            Disposition.DELIVERED -> "delivery-notification" to "<delivered/>"
            Disposition.DISPLAYED -> "display-notification" to "<displayed/>"
        }
        return """<?xml version="1.0" encoding="UTF-8"?>
<imdn xmlns="urn:ietf:params:xml:ns:imdn">
<message-id>$originalMessageId</message-id>
<datetime>${originalDateTime ?: now()}</datetime>
<$element><status>$status</status></$element>
</imdn>"""
    }

    fun parseImdn(xml: String): Notification? {
        val id = Regex("<(?:\\w+:)?message-id>\\s*([^<\\s]+)\\s*</").find(xml)?.groupValues?.get(1) ?: return null
        val status = Regex("<(?:\\w+:)?status>\\s*<(?:\\w+:)?([a-z-]+)").find(xml)?.groupValues?.get(1)
        val disposition = when (status) {
            "delivered" -> Disposition.DELIVERED
            "displayed" -> Disposition.DISPLAYED
            else -> null
        }
        val failed = status in setOf("failed", "forbidden", "error")
        return Notification(id, disposition, failed)
    }

    /** קובץ שהגיע ב-File Transfer over HTTP: שם הקובץ וקישור ההורדה. */
    fun parseFileTransfer(xml: String): Pair<String?, String?> {
        val fileInfo = Regex("<file-info\\s+type=\"file\"[\\s\\S]*?</file-info>").find(xml)?.value ?: xml
        val name = Regex("<file-name>([^<]*)</file-name>").find(fileInfo)?.groupValues?.get(1)
        val url = Regex("<data\\s+[^>]*url=\"([^\"]+)\"").find(fileInfo)?.groupValues?.get(1)
        return name to url?.replace("&amp;", "&")
    }

    private fun headers(block: String): Map<String, String> =
        block.split("\r\n", "\n").mapNotNull { line ->
            val colon = line.indexOf(':')
            if (colon <= 0) null else line.substring(0, colon).trim().lowercase() to line.substring(colon + 1).trim()
        }.toMap()

    /** מחזיר (תחילת השורה הריקה, תחילת מה שאחריה), תומך גם ב-LF בודד. */
    private fun indexOfBlankLine(data: ByteArray, from: Int): Pair<Int, Int>? {
        var i = from
        while (i < data.size) {
            if (data[i] == '\n'.code.toByte()) {
                if (i + 1 < data.size && data[i + 1] == '\n'.code.toByte()) return i to i + 2
                if (i + 2 < data.size && data[i + 1] == '\r'.code.toByte() && data[i + 2] == '\n'.code.toByte()) {
                    val start = if (i > 0 && data[i - 1] == '\r'.code.toByte()) i - 1 else i
                    return start to i + 3
                }
            }
            i++
        }
        return null
    }

    private fun now(): String = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        .apply { timeZone = TimeZone.getTimeZone("UTC") }
        .format(Date())
}
