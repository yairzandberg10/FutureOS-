package com.future.settings.feedback

/**
 * מסיר פרטים אישיים מטקסט משוב ומלוג לפני שהם מוצגים בתצוגה המקדימה ונשלחים.
 * אותם כללים כמו server/feedback/src/redact.js - השרת מסנן שוב, אבל מה שלא יוצא מהמכשיר לא יכול לדלוף.
 * לוגיקה טהורה בלי Android, כדי שאפשר לבדוק אותה בבדיקת יחידה רגילה.
 */
object FeedbackRedactor {

    private val RULES: List<Pair<Regex, String>> = listOf(
        Regex("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}") to "[email]",
        // IMEI / ICCID / IMSI
        Regex("\\b\\d{14,20}\\b") to "[id]",
        Regex("(?:\\+|00)\\d{1,3}[\\s-]?\\d{1,4}(?:[\\s-]?\\d{2,4}){2,4}") to "[phone]",
        Regex("\\b0(?:5\\d|[2-4]|[89]|7\\d)[\\s-]?\\d{3}[\\s-]?\\d{4}\\b") to "[phone]",
        Regex("-?\\d{1,3}\\.\\d{4,}\\s*,\\s*-?\\d{1,3}\\.\\d{4,}") to "[location]",
        Regex("\\b(?:\\d{1,3}\\.){3}\\d{1,3}\\b") to "[ip]",
        Regex("\\b(?:[0-9A-Fa-f]{2}[:-]){5}[0-9A-Fa-f]{2}\\b") to "[mac]",
        Regex("\\b(token|apikey|api_key|key|password|secret|authorization|bearer)\\b(\\s*[:=]\\s*|\\s+)[^\\s,;\"']+", RegexOption.IGNORE_CASE) to "$1$2[secret]",
    )

    fun redact(text: String): String {
        var out = text
        for ((pattern, replacement) in RULES) out = pattern.replace(out, replacement)
        return out
    }
}
