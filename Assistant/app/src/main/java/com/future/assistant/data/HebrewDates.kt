package com.future.assistant.data

import android.icu.util.Calendar
import android.icu.util.HebrewCalendar
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * תאריך עברי וחגים - מחושבים מקומית מהלוח העברי של android.icu, בלי
 * אינטרנט. כל חג מחושב למועד הקרוב שלו (השנה או בשנה הבאה), כולל הדחיות
 * הקבועות (צומות שחלים בשבת, יום העצמאות ויום השואה).
 */
object HebrewDates {
    private enum class Shift { NONE, FAST, ESTHER_FAST, ATZMAUT, ZIKARON, SHOAH }

    private class Holiday(val names: List<String>, val month: Int, val day: Int, val shift: Shift = Shift.NONE) {
        val title get() = names.first()
    }

    private val HOLIDAYS = listOf(
        Holiday(listOf("ראש השנה"), HebrewCalendar.TISHRI, 1),
        Holiday(listOf("צום גדליה"), HebrewCalendar.TISHRI, 3, Shift.FAST),
        Holiday(listOf("יום כיפור", "יום הכיפורים", "כיפור"), HebrewCalendar.TISHRI, 10),
        Holiday(listOf("סוכות", "חג הסוכות"), HebrewCalendar.TISHRI, 15),
        Holiday(listOf("הושענא רבה"), HebrewCalendar.TISHRI, 21),
        Holiday(listOf("שמחת תורה", "שמיני עצרת"), HebrewCalendar.TISHRI, 22),
        Holiday(listOf("חנוכה", "חג החנוכה"), HebrewCalendar.KISLEV, 25),
        Holiday(listOf("עשרה בטבת", "צום עשרה בטבת"), HebrewCalendar.TEVET, 10),
        Holiday(listOf("טו בשבט"), HebrewCalendar.SHEVAT, 15),
        Holiday(listOf("תענית אסתר"), HebrewCalendar.ADAR, 13, Shift.ESTHER_FAST),
        Holiday(listOf("פורים"), HebrewCalendar.ADAR, 14),
        Holiday(listOf("שושן פורים"), HebrewCalendar.ADAR, 15),
        Holiday(listOf("פסח", "חג הפסח", "ליל הסדר"), HebrewCalendar.NISAN, 15),
        Holiday(listOf("יום השואה", "יום הזיכרון לשואה"), HebrewCalendar.NISAN, 27, Shift.SHOAH),
        Holiday(listOf("יום הזיכרון", "יום הזכרון"), HebrewCalendar.IYAR, 5, Shift.ZIKARON),
        Holiday(listOf("יום העצמאות"), HebrewCalendar.IYAR, 5, Shift.ATZMAUT),
        Holiday(listOf("פסח שני"), HebrewCalendar.IYAR, 14),
        Holiday(listOf("לג בעומר"), HebrewCalendar.IYAR, 18),
        Holiday(listOf("יום ירושלים"), HebrewCalendar.IYAR, 28),
        Holiday(listOf("שבועות", "חג השבועות"), HebrewCalendar.SIVAN, 6),
        Holiday(listOf("יז בתמוז", "שבעה עשר בתמוז"), HebrewCalendar.TAMUZ, 17, Shift.FAST),
        Holiday(listOf("תשעה באב", "ט באב"), HebrewCalendar.AV, 9, Shift.FAST),
        Holiday(listOf("טו באב"), HebrewCalendar.AV, 15),
    )

    /** מספר ה"פקודות" שהמודול הזה מוסיף: לכל חג "מתי" ו"כמה ימים עד", ועוד התאריך העברי. */
    val commandCount: Int get() = HOLIDAYS.size * 2 + 1

    private val HOLIDAY_CUES = listOf("מתי", "כמה ימים עד", "כמה זמן עד", "כמה נשאר עד", "כמה ימים נשארו עד", "באיזה תאריך", "באיזה יום")
    private val HEBREW_DATE_CUES = listOf("תאריך העברי", "תאריך עברי", "בלוח העברי", "התאריך בעברית", "איזה חודש עברי")

    fun tryAnswer(rawText: String): CommandResult? {
        val text = KnowledgeBase.normalize(rawText)
        if (HEBREW_DATE_CUES.any { text.contains(it) }) {
            val today = HebrewCalendar()
            val weekday = SimpleDateFormat("EEEE", Locale("he")).format(Date())
            return CommandResult(
                "היום $weekday, ${formatHebrew(today)}",
                speech = "היום $weekday, ${formatHebrewSpoken(today)}",
            )
        }
        if (HOLIDAY_CUES.none { text.contains(it) }) return null
        val holiday = HOLIDAYS
            .flatMap { h -> h.names.map { KnowledgeBase.normalize(it) to h } }
            .filter { (name, _) -> " $text ".contains(" $name ") || text.contains(" ב$name") || text.contains(" ל$name") || text.contains(" ו$name") }
            .maxByOrNull { it.first.length }?.second ?: return null

        val next = nextOccurrence(holiday)
        val days = daysFromToday(next)
        val gregorian = SimpleDateFormat("EEEE, d בMMMM yyyy", Locale("he")).format(next.time)
        val hebrew = formatHebrew(next)
        return when (days) {
            0L -> CommandResult("היום ${holiday.title}! $hebrew", speech = "היום ${holiday.title}! ${formatHebrewSpoken(next)}")
            1L -> CommandResult("${holiday.title} מחר, $gregorian, $hebrew", speech = "${holiday.title} מחר, $gregorian")
            else -> CommandResult(
                "${holiday.title} יחול ב$gregorian, $hebrew. בעוד $days ימים",
                speech = "${holiday.title} יחול ב$gregorian, ${formatHebrewSpoken(next)}. בעוד $days ימים",
            )
        }
    }

    private fun startOfToday(): Calendar = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }

    private fun daysFromToday(date: Calendar): Long =
        TimeUnit.MILLISECONDS.toDays(date.timeInMillis - startOfToday().timeInMillis + TimeUnit.HOURS.toMillis(12))

    private fun nextOccurrence(holiday: Holiday): HebrewCalendar {
        val today = startOfToday()
        val year = HebrewCalendar().get(Calendar.EXTENDED_YEAR)
        for (y in year..year + 1) {
            val date = dateInYear(holiday, y)
            if (!date.before(today)) return date
        }
        return dateInYear(holiday, year + 1)
    }

    private fun dateInYear(holiday: Holiday, year: Int): HebrewCalendar {
        val c = HebrewCalendar(year, holiday.month, holiday.day)
        c.set(Calendar.HOUR_OF_DAY, 0); c.set(Calendar.MINUTE, 0); c.set(Calendar.SECOND, 0); c.set(Calendar.MILLISECOND, 0)
        val dow = c.get(Calendar.DAY_OF_WEEK)
        val shift = when (holiday.shift) {
            Shift.NONE -> 0
            Shift.FAST -> if (dow == Calendar.SATURDAY) 1 else 0
            Shift.ESTHER_FAST -> if (dow == Calendar.SATURDAY) -2 else 0
            Shift.SHOAH -> when (dow) { Calendar.FRIDAY -> -1; Calendar.SUNDAY -> 1; else -> 0 }
            Shift.ATZMAUT, Shift.ZIKARON -> {
                val atzmaut = when (dow) { Calendar.FRIDAY -> -1; Calendar.SATURDAY -> -2; Calendar.MONDAY -> 1; else -> 0 }
                if (holiday.shift == Shift.ZIKARON) atzmaut - 1 else atzmaut
            }
        }
        if (shift != 0) c.add(Calendar.DATE, shift)
        return c
    }

    private fun monthName(c: HebrewCalendar): String {
        val leap = isLeapYear(c.get(Calendar.EXTENDED_YEAR))
        return when (c.get(Calendar.MONTH)) {
            HebrewCalendar.TISHRI -> "תשרי"
            HebrewCalendar.HESHVAN -> "חשוון"
            HebrewCalendar.KISLEV -> "כסלו"
            HebrewCalendar.TEVET -> "טבת"
            HebrewCalendar.SHEVAT -> "שבט"
            HebrewCalendar.ADAR_1 -> "אדר א׳"
            HebrewCalendar.ADAR -> if (leap) "אדר ב׳" else "אדר"
            HebrewCalendar.NISAN -> "ניסן"
            HebrewCalendar.IYAR -> "אייר"
            HebrewCalendar.SIVAN -> "סיוון"
            HebrewCalendar.TAMUZ -> "תמוז"
            HebrewCalendar.AV -> "אב"
            else -> "אלול"
        }
    }

    private fun isLeapYear(year: Int) = ((7 * year + 1) % 19) < 7

    fun formatHebrew(c: HebrewCalendar): String =
        "${hebrewNumeral(c.get(Calendar.DAY_OF_MONTH))} ב${monthName(c)} ${hebrewNumeral(c.get(Calendar.EXTENDED_YEAR) % 1000)}"

    /** לקול: היום כמספר (המנרמל הופך אותו למילים), השנה באותיות בלי גרשיים. */
    private fun formatHebrewSpoken(c: HebrewCalendar): String =
        "${c.get(Calendar.DAY_OF_MONTH)} ב${monthName(c).replace("׳", "")} ${hebrewNumeral(c.get(Calendar.EXTENDED_YEAR) % 1000).replace("״", "").replace("׳", "")}"

    /** 1..999 באותיות עבריות, עם גרש/גרשיים (טו/טז במקום יה/יו). */
    fun hebrewNumeral(n: Int): String {
        var rest = n
        val sb = StringBuilder()
        while (rest >= 400) { sb.append('ת'); rest -= 400 }
        HUNDREDS.forEach { (value, letter) -> if (rest >= value) { sb.append(letter); rest -= value } }
        if (rest == 15) { sb.append("טו"); rest = 0 }
        if (rest == 16) { sb.append("טז"); rest = 0 }
        TENS.forEach { (value, letter) -> if (rest >= value) { sb.append(letter); rest -= value } }
        if (rest > 0) sb.append(ONES[rest - 1])
        return if (sb.length == 1) "$sb׳" else sb.insert(sb.length - 1, '״').toString()
    }

    private val HUNDREDS = listOf(300 to 'ש', 200 to 'ר', 100 to 'ק')
    private val TENS = listOf(90 to 'צ', 80 to 'פ', 70 to 'ע', 60 to 'ס', 50 to 'נ', 40 to 'מ', 30 to 'ל', 20 to 'כ', 10 to 'י')
    private const val ONES = "אבגדהוזחט"
}
