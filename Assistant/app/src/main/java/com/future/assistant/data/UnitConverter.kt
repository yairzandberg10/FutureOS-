package com.future.assistant.data

/**
 * המרת יחידות כללית - אורך, משקל, נפח, זמן, שטח, נפח מידע ומהירות. כל
 * יחידה מוגדרת ביחס ליחידת בסיס של הממד שלה, כך שכל זוג יחידות מאותו ממד
 * עובד בשני הכיוונים ("כמה גרם בקילו", "המר 5 מייל לקילומטר").
 */
object UnitConverter {
    private class Unit(val names: List<String>, val factor: Double) {
        val title get() = names.first()
    }

    private val DIMENSIONS: List<List<Unit>> = listOf(
        listOf(
            Unit(listOf("מילימטר", "מילימטרים", "ממ"), 0.001),
            Unit(listOf("סנטימטר", "סנטימטרים", "סמ", "סנטי"), 0.01),
            Unit(listOf("מטר", "מטרים"), 1.0),
            Unit(listOf("קילומטר", "קילומטרים", "קמ"), 1000.0),
            Unit(listOf("אינץ", "אינצים", "אינטש"), 0.0254),
            Unit(listOf("רגל", "רגליים", "פיט", "פוט"), 0.3048),
            Unit(listOf("יארד", "יארדים"), 0.9144),
            Unit(listOf("מייל", "מיילים"), 1609.344),
            Unit(listOf("מייל ימי", "מיילים ימיים"), 1852.0),
        ),
        listOf(
            Unit(listOf("מיליגרם", "מיליגרמים"), 0.000001),
            Unit(listOf("גרם", "גרמים"), 0.001),
            Unit(listOf("קילוגרם", "קילוגרמים", "קילו", "קג"), 1.0),
            Unit(listOf("טון", "טונות"), 1000.0),
            Unit(listOf("פאונד", "פאונדים", "ליברה", "ליברות"), 0.45359237),
            Unit(listOf("אונקיה", "אונקיות"), 0.028349523125),
        ),
        listOf(
            Unit(listOf("מיליליטר", "מיליליטרים", "מל"), 0.001),
            Unit(listOf("סמק"), 0.001),
            Unit(listOf("ליטר", "ליטרים"), 1.0),
            Unit(listOf("גלון", "גלונים"), 3.785411784),
            Unit(listOf("כוס", "כוסות"), 0.24),
            Unit(listOf("כף", "כפות"), 0.015),
            Unit(listOf("כפית", "כפיות"), 0.005),
            Unit(listOf("קוב", "מטר מעוקב", "מטרים מעוקבים"), 1000.0),
        ),
        listOf(
            Unit(listOf("שנייה", "שניות", "שניה"), 1.0),
            Unit(listOf("דקה", "דקות"), 60.0),
            Unit(listOf("שעה", "שעות"), 3600.0),
            Unit(listOf("יום", "ימים", "יממה", "יממות"), 86400.0),
            Unit(listOf("שבוע", "שבועות"), 604800.0),
            Unit(listOf("חודש", "חודשים"), 2629746.0),
            Unit(listOf("שנה", "שנים"), 31556952.0),
        ),
        listOf(
            Unit(listOf("סנטימטר רבוע", "סנטימטרים רבועים"), 0.0001),
            Unit(listOf("מטר רבוע", "מטרים רבועים", "מר"), 1.0),
            Unit(listOf("דונם", "דונמים"), 1000.0),
            Unit(listOf("הקטר", "הקטאר", "הקטרים"), 10000.0),
            Unit(listOf("קילומטר רבוע", "קילומטרים רבועים", "קמר"), 1000000.0),
            Unit(listOf("אקר", "אקרים"), 4046.8564224),
        ),
        listOf(
            Unit(listOf("ביט", "ביטים"), 0.125),
            Unit(listOf("בייט", "בייטים", "בתים"), 1.0),
            Unit(listOf("קילובייט", "קילו בייט"), 1024.0),
            Unit(listOf("מגהבייט", "מגה בייט", "מגה"), 1048576.0),
            Unit(listOf("גיגהבייט", "גיגה בייט", "גיגה", "גיגבייט"), 1073741824.0),
            Unit(listOf("טרהבייט", "טרה בייט", "טרה"), 1099511627776.0),
        ),
        listOf(
            Unit(listOf("קמש", "קילומטר לשעה", "קילומטרים לשעה"), 1 / 3.6),
            Unit(listOf("מטר לשנייה", "מטרים לשנייה"), 1.0),
            Unit(listOf("מייל לשעה", "מיילים לשעה", "מיילים בשעה"), 0.44704),
            Unit(listOf("קשר", "קשרים"), 0.514444),
        ),
    )

    /** כל זוג יחידות שונות מאותו ממד = פקודה. */
    val commandCount: Int get() = DIMENSIONS.sumOf { it.size * (it.size - 1) }

    private data class Mention(val index: Int, val dim: Int, val unit: Unit)

    // ה' הידיעה לא מוסרת בכוונה - "השעה"/"היום" הן לא בקשות המרה.
    private val PREFIXES = "בלמו"
    private val NAME_INDEX: Map<String, Pair<Int, Unit>> = buildMap {
        DIMENSIONS.forEachIndexed { dim, units -> units.forEach { u -> u.names.forEach { put(it, dim to u) } } }
    }
    private const val MAX_WORDS = 3

    private fun findMentions(words: List<String>): List<Mention> {
        val found = mutableListOf<Mention>()
        var i = 0
        while (i < words.size) {
            var hit: Pair<Int, Pair<Int, Unit>>? = null
            for (n in minOf(MAX_WORDS, words.size - i) downTo 1) {
                val phrase = words.subList(i, i + n).joinToString(" ")
                val match = variants(phrase).firstNotNullOfOrNull { NAME_INDEX[it] }
                if (match != null) { hit = n to match; break }
            }
            if (hit != null) {
                found += Mention(i, hit.second.first, hit.second.second)
                i += hit.first
            } else i++
        }
        return found
    }

    private fun variants(phrase: String): List<String> {
        val out = mutableListOf(phrase)
        var s = phrase
        repeat(2) {
            if (s.length > 2 && s[0] in PREFIXES) { s = s.substring(1); out += s }
        }
        return out
    }

    private val DIGITS = Regex("\\d+(\\.\\d+)?")

    fun tryConvert(rawText: String): CommandResult? {
        val text = KnowledgeBase.normalize(rawText.replace(Regex("(\\d),(\\d)"), "$1$2"))
        val words = text.split(" ").filter { it.isNotBlank() }
        val mentions = findMentions(words)
        if (mentions.size < 2) return null
        val first = mentions[0]
        val second = mentions.drop(1).firstOrNull { it.dim == first.dim && it.unit !== first.unit } ?: return null

        // מיקום המספר קובע מה היחידה שממנה ממירים: היחידה שבאה אחריו.
        var numberIndex = words.indexOfFirst { DIGITS.containsMatchIn(it) }
        var value = if (numberIndex >= 0) DIGITS.find(words[numberIndex])!!.value.toDouble() else null
        if (value == null) {
            numberIndex = words.indexOfFirst { HebrewNumbers.findNumbers(it, 1).isNotEmpty() }
            if (numberIndex >= 0) value = HebrewNumbers.findNumbers(words.drop(numberIndex).joinToString(" "), 1).first().toDouble()
        }
        val (from, to) = when {
            value != null && numberIndex >= 0 -> {
                if (second.index > numberIndex && first.index < numberIndex) second to first else first to second
            }
            words.firstOrNull() == "כמה" -> second to first
            else -> first to second
        }
        val amount = value ?: 1.0
        val result = amount * from.unit.factor / to.unit.factor
        return CommandResult("${format(amount)} ${from.unit.title} זה ${format(result)} ${to.unit.title}")
    }

    fun format(value: Double): String {
        if (value == Math.floor(value) && !value.isInfinite() && kotlin.math.abs(value) < 1e15) return value.toLong().toString()
        val pattern = if (kotlin.math.abs(value) >= 1) "%.2f" else "%.4f"
        return pattern.format(java.util.Locale.US, value).trimEnd('0').trimEnd('.')
    }
}
