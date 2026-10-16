package com.future.settings.feedback

/**
 * משוב מוכן לשליחה - בדיוק מה שמוצג בתצוגה המקדימה ובדיוק מה שנשלח (אחרי סינון).
 * החוזה מול השרת: POST {base}/feedback, ראו server/feedback/src/devices.js.
 * בניית ה-JSON ידנית (בלי org.json) כדי שתרוץ גם בבדיקת יחידה על ה-JVM.
 */
data class FeedbackPayload(
    val clientId: String,
    val type: FeedbackType,
    val appPkg: String,
    val appLabel: String,
    val appVersion: String?,
    val build: String,
    val model: String,
    val text: String,
    val log: String?,
) {
    fun toJson(): String = buildString {
        append('{')
        field("client_id", clientId); append(',')
        field("type", type.wire); append(',')
        field("text", text); append(',')
        field("app_pkg", appPkg); append(',')
        field("build", build)
        if (appVersion != null) { append(','); field("app_version", appVersion) }
        if (log != null) { append(','); field("log", log) }
        append('}')
    }

    private fun StringBuilder.field(name: String, value: String) {
        append('"').append(name).append("\":")
        append(jsonString(value))
    }

    companion object {
        const val MAX_TEXT = 4000
        const val MAX_LOG = 60 * 1024

        /** מחרוזת JSON עם escape לכל תו בקרה, מרכאות ולוכסן הפוך. */
        fun jsonString(value: String): String = buildString(value.length + 2) {
            append('"')
            for (c in value) {
                when {
                    c == '"' -> append("\\\"")
                    c == '\\' -> append("\\\\")
                    c == '\n' -> append("\\n")
                    c == '\r' -> append("\\r")
                    c == '\t' -> append("\\t")
                    c < ' ' || c == ' ' || c == ' ' -> append("\\u").append(c.code.toString(16).padStart(4, '0'))
                    else -> append(c)
                }
            }
            append('"')
        }

        /** הקיצור של גרסת אפליקציה לפורמט שהשרת מקבל (אותיות, ספרות, . - + רווח וסוגריים, עד 64). */
        fun cleanVersion(raw: String?): String? =
            raw?.filter { it.isLetterOrDigit() || it in "._-+ ()" }?.take(64)?.takeIf { it.isNotBlank() }

        /** חותך לוג לסוף שלו (השורות האחרונות הן הרלוונטיות) בגבול שורה. */
        fun tailLog(raw: String, maxChars: Int = MAX_LOG): String {
            if (raw.length <= maxChars) return raw
            val cut = raw.substring(raw.length - maxChars)
            val nl = cut.indexOf('\n')
            return if (nl in 0 until cut.length - 1) cut.substring(nl + 1) else cut
        }
    }
}

enum class FeedbackType(val wire: String, val label: String) {
    Bug("bug", "באג"),
    Idea("idea", "הצעה"),
    Other("other", "אחר"),
}
