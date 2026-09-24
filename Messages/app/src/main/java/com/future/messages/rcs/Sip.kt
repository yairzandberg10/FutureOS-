package com.future.messages.rcs

import java.security.SecureRandom

/**
 * בניה ופירוק של הודעות SIP (RFC 3261) - רק מה ש-RCS במצב Pager (הודעה
 * עצמאית, OMA CPM Standalone Messaging) צריך: בקשת MESSAGE יוצאת, תשובות
 * לבקשות נכנסות, וזיהוי תשובות לבקשות שלנו.
 *
 * הפורמט הוא זה של android.telephony.ims.SipMessage: שורת הפתיחה מסתיימת
 * ב-CRLF, כל כותרת מסתיימת ב-CRLF, ו-SipMessage מוסיף בעצמו את השורה הריקה.
 */
internal object Sip {
    const val CRLF = "\r\n"

    /** התג של שירות ההודעות העצמאיות של RCS - הוא שנרשם ב-delegate והוא
     * שמנתב אלינו הודעות נכנסות. */
    const val ICSI_CPM_MSG = "urn%3Aurn-7%3A3gpp-service.ims.icsi.oma.cpm.msg"
    const val FEATURE_TAG_CPM_MSG = "+g.3gpp.icsi-ref=\"$ICSI_CPM_MSG\""
    private const val PREFERRED_SERVICE = "urn:urn-7:3gpp-service.ims.icsi.oma.cpm.msg"

    private val random = SecureRandom()

    fun token(bytes: Int = 8): String {
        val b = ByteArray(bytes)
        random.nextBytes(b)
        return b.joinToString("") { "%02x".format(it) }
    }

    /** branch חייב להתחיל ב-"z9hG4bK" (magic cookie של RFC 3261). */
    fun newBranch() = "z9hG4bK" + token(10)

    fun uuid(): String = java.util.UUID.randomUUID().toString()

    class Request(val message: SipMessageData, val branch: String, val callId: String)

    fun buildMessageRequest(
        config: ImsSipConfig,
        targetUri: String,
        cpimBody: ByteArray,
        conversationId: String,
        contributionId: String,
    ): Request {
        val branch = newBranch()
        val callId = uuid() + "@" + hostOf(config)
        val self = config.publicUserId ?: throw IllegalStateException("No IMS public identity")
        val headers = StringBuilder()
        // ב-UDP הודעה מעל ~1300 בתים חייבת לעבור ל-TCP (RFC 3261 18.1.1).
        val transport = if (config.transport.equals("UDP", true) && cpimBody.size > 1300) "TCP" else config.transport.uppercase()
        headers.header("Via", "SIP/2.0/$transport ${hostPort(config)};branch=$branch;rport")
        headers.header("Max-Forwards", "70")
        config.serviceRoute?.takeIf { it.isNotBlank() }?.let { headers.header("Route", it) }
        headers.header("From", "<$self>;tag=${token()}")
        headers.header("To", "<$targetUri>")
        headers.header("Call-ID", callId)
        headers.header("CSeq", "1 MESSAGE")
        headers.header("P-Preferred-Identity", "<$self>")
        headers.header("P-Preferred-Service", PREFERRED_SERVICE)
        config.accessNetworkInfo?.takeIf { it.isNotBlank() }?.let { headers.header("P-Access-Network-Info", it) }
        headers.header("Accept-Contact", "*;$FEATURE_TAG_CPM_MSG")
        headers.header("Contact", "<${contactUri(config)}>;$FEATURE_TAG_CPM_MSG")
        config.userAgent?.takeIf { it.isNotBlank() }?.let { headers.header("User-Agent", it) }
        headers.header("Conversation-ID", conversationId)
        headers.header("Contribution-ID", contributionId)
        headers.header("Content-Type", "message/cpim")
        headers.header("Content-Length", cpimBody.size.toString())
        return Request(
            SipMessageData("MESSAGE $targetUri SIP/2.0$CRLF", headers.toString(), cpimBody),
            branch,
            callId,
        )
    }

    /** תשובה לבקשה נכנסת: מעתיקים Via/From/Call-ID/CSeq כמו שהם, ו-To מקבל
     * tag אם אין לו (RFC 3261 8.2.6.2). */
    fun buildResponse(request: Parsed, code: Int, reason: String): SipMessageData {
        val headers = StringBuilder()
        request.all("Via").forEach { headers.header("Via", it) }
        request.first("From")?.let { headers.header("From", it) }
        request.first("To")?.let { to ->
            headers.header("To", if (to.contains(";tag=")) to else "$to;tag=${token()}")
        }
        request.first("Call-ID")?.let { headers.header("Call-ID", it) }
        request.first("CSeq")?.let { headers.header("CSeq", it) }
        headers.header("Content-Length", "0")
        return SipMessageData("SIP/2.0 $code $reason$CRLF", headers.toString(), ByteArray(0))
    }

    /** הודעת SIP מפורקת. כותרות לפי שם מנורמל (כולל הצורה המקוצרת). */
    class Parsed(val startLine: String, private val headers: List<Pair<String, String>>, val body: ByteArray) {
        val isResponse get() = startLine.startsWith("SIP/2.0")
        val statusCode: Int get() = startLine.split(' ').getOrNull(1)?.toIntOrNull() ?: 0
        val method: String get() = if (isResponse) (first("CSeq")?.substringAfter(' ')?.trim() ?: "") else startLine.substringBefore(' ')

        fun first(name: String): String? = headers.firstOrNull { it.first.equals(canonical(name), true) }?.second
        fun all(name: String): List<String> = headers.filter { it.first.equals(canonical(name), true) }.map { it.second }

        val branch: String? get() = first("Via")?.let { Regex(";\\s*branch=([^;,\\s]+)").find(it)?.groupValues?.get(1) }
    }

    fun parse(message: SipMessageData): Parsed {
        val headers = mutableListOf<Pair<String, String>>()
        for (raw in unfold(message.headerSection).split(CRLF, "\n")) {
            val line = raw.trimEnd('\r')
            val colon = line.indexOf(':')
            if (colon <= 0) continue
            val name = canonical(line.substring(0, colon).trim())
            val value = line.substring(colon + 1).trim()
            // Via מרובה בשורה אחת - מפצלים כדי שכל ערך יועתק לתשובה בנפרד.
            if (name.equals("Via", true)) value.split(Regex(",(?=\\s*SIP/)")).forEach { headers += name to it.trim() }
            else headers += name to value
        }
        return Parsed(message.startLine.trimEnd('\r', '\n'), headers, message.content)
    }

    /** מחלץ את המספר מ-URI של SIP/TEL: "<tel:+97250...>;tag=x" → "+97250...". */
    fun numberFromUri(value: String): String? {
        val uri = Regex("<([^>]+)>").find(value)?.groupValues?.get(1) ?: value.substringBefore(';').trim()
        val user = when {
            uri.startsWith("tel:", true) -> uri.substring(4).substringBefore(';')
            uri.startsWith("sip:", true) || uri.startsWith("sips:", true) ->
                uri.substringAfter(':').substringBefore('@').substringBefore(';')
            else -> return null
        }
        val digits = user.filter { it.isDigit() || it == '+' }
        return digits.ifEmpty { null }
    }

    private fun unfold(s: String) = s.replace(Regex("\r?\n[ \t]+"), " ")

    private val compactForms = mapOf(
        "v" to "Via", "f" to "From", "t" to "To", "i" to "Call-ID", "m" to "Contact",
        "l" to "Content-Length", "c" to "Content-Type", "a" to "Accept-Contact", "k" to "Supported",
    )

    private fun canonical(name: String) = compactForms[name.lowercase()] ?: name

    private fun StringBuilder.header(name: String, value: String) {
        append(name).append(": ").append(value).append(CRLF)
    }

    private fun hostOf(config: ImsSipConfig) = config.localIp.ifEmpty { "localhost" }

    private fun hostPort(config: ImsSipConfig): String {
        val host = if (config.localIp.contains(':')) "[${config.localIp}]" else hostOf(config)
        return if (config.localPort > 0) "$host:${config.localPort}" else host
    }

    private fun contactUri(config: ImsSipConfig): String {
        config.publicGruu?.takeIf { it.isNotBlank() }?.let { return it }
        val user = config.contactUser ?: config.publicUserId?.let { numberFromUri(it) } ?: "anonymous"
        return "sip:$user@${hostPort(config)}"
    }
}
