package com.future.assistant.asr

/**
 * מכין טקסט להקראה: ספרות, שעות, אחוזים ועשרוניים הופכים למילים עבריות.
 * הקול (Piper) אומן על טקסט בלי ספרות, ו-ReNikud מעביר ספרות כמו שהן -
 * בלי השלב הזה "רמת הסוללה 80 אחוז" נשמע ממולמל בדיוק במקום של המספר.
 *
 * המספרים נקראים בצורת הספירה (נקבה: "שלוש", "עשרים ואחת"), הצורה
 * הרגילה להקראת מספר בפני עצמו.
 */
object HebrewTextNormalizer {
    private val TIME = Regex("""\b(\d{1,2}):(\d{2})\b""")
    private val DECIMAL = Regex("""(\d+)\.(\d+)""")
    private val NUMBER = Regex("""\d+""")
    // "ב-5", "ה-3": המקף בין אות שימוש למספר נעלם בהקראה ("בחמש").
    private val PREFIX_HYPHEN = Regex("""(?<=[א-ת])[-־](?=\d)""")

    fun normalize(text: String): String {
        var s = text.replace(PREFIX_HYPHEN, "")
        s = s.replace(TIME) { m ->
            val hours = m.groupValues[1].toInt()
            val minutes = m.groupValues[2].toInt()
            if (minutes == 0) number(hours.toLong()) else "${number(hours.toLong())} ו${number(minutes.toLong())}"
        }
        s = s.replace("%", " אחוז")
        s = s.replace(DECIMAL) { m -> "${digitsOrNumber(m.groupValues[1])} נקודה ${digits(m.groupValues[2])}" }
        s = s.replace(NUMBER) { m -> digitsOrNumber(m.value) }
        return s
    }

    /** מספרי טלפון וקודים (אפס מוביל, או 7 ספרות ומעלה) נקראים ספרה-ספרה. */
    private fun digitsOrNumber(d: String): String =
        if ((d.length > 1 && d[0] == '0') || d.length >= 7) digits(d) else number(d.toLong())

    private fun digits(d: String) = d.map { ONES[it - '0'] }.joinToString(" ")

    fun number(n: Long): String {
        if (n == 0L) return "אפס"
        val parts = ArrayList<String>()
        var rest = n
        if (rest >= 1_000_000) {
            val millions = rest / 1_000_000
            parts += if (millions == 1L) "מיליון" else "${number(millions)} מיליון"
            rest %= 1_000_000
        }
        if (rest >= 1000) {
            val thousands = (rest / 1000).toInt()
            parts += when {
                thousands == 1 -> "אלף"
                thousands == 2 -> "אלפיים"
                thousands <= 10 -> "${THOUSANDS_CONSTRUCT[thousands]} אלפים"
                else -> "${number(thousands.toLong())} אלף"
            }
            rest %= 1000
        }
        if (rest >= 100) {
            parts += HUNDREDS[(rest / 100).toInt()]
            rest %= 100
        }
        when {
            rest == 0L -> {}
            rest < 10 -> parts += ONES[rest.toInt()]
            rest < 20 -> parts += TEENS[(rest - 10).toInt()]
            else -> {
                parts += TENS[(rest / 10).toInt()]
                if (rest % 10 != 0L) parts += ONES[(rest % 10).toInt()]
            }
        }
        // ו' החיבור באה לפני הרכיב האחרון: "מאה עשרים וחמש", "אלף וחמש מאות".
        if (parts.size > 1) parts[parts.size - 1] = "ו" + parts.last()
        return parts.joinToString(" ")
    }

    private val ONES = listOf("אפס", "אחת", "שתיים", "שלוש", "ארבע", "חמש", "שש", "שבע", "שמונה", "תשע")
    private val TEENS = listOf(
        "עשר", "אחת עשרה", "שתים עשרה", "שלוש עשרה", "ארבע עשרה",
        "חמש עשרה", "שש עשרה", "שבע עשרה", "שמונה עשרה", "תשע עשרה",
    )
    private val TENS = listOf("", "", "עשרים", "שלושים", "ארבעים", "חמישים", "שישים", "שבעים", "שמונים", "תשעים")
    private val HUNDREDS = listOf(
        "", "מאה", "מאתיים", "שלוש מאות", "ארבע מאות", "חמש מאות",
        "שש מאות", "שבע מאות", "שמונה מאות", "תשע מאות",
    )
    private val THOUSANDS_CONSTRUCT = listOf(
        "", "", "", "שלושת", "ארבעת", "חמשת", "ששת", "שבעת", "שמונת", "תשעת", "עשרת",
    )
}
