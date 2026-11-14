package com.future.messages.data

/**
 * פענוח החלק שאחרי "sms:"/"smsto:" (כבר מפוענח מ-%XX על ידי Uri):
 * "0501234567?body=שלום" -> ("0501234567", "שלום"). כמה נמענים נשארים
 * כמו שהם ("050...,052...") - הקורא מחליט מה לעשות איתם.
 */
object SmsTarget {
    fun parse(schemeSpecificPart: String): Pair<String, String> {
        val address = schemeSpecificPart.substringBefore('?').trim()
        val query = schemeSpecificPart.substringAfter('?', "")
        val body = query.split('&')
            .firstOrNull { it.startsWith("body=") }
            ?.removePrefix("body=")
            .orEmpty()
        return address to body
    }

    fun isGroup(address: String): Boolean = address.contains(',') || address.contains(';')
}
