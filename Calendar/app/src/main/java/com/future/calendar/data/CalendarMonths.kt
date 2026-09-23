package com.future.calendar.data

import android.icu.util.Calendar
import android.icu.util.ULocale
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Date
import java.util.Locale

/** איזה לוח הוא הבסיס לתצוגה - נבחר בהגדרות. */
enum class CalendarKind(val label: String) {
    /** חודשים עבריים (תשרי-אלול), ימים באותיות. */
    HEBREW("עברי"),

    /** חודשים לועזיים, ימים במספרים. */
    GREGORIAN("לועזי"),

    /** חודשים לועזיים, ובכל יום גם התאריך העברי שלו. */
    COMBINED("משולב"),
}

/**
 * חודש אחד בתצוגה - לועזי או עברי. [first] הוא היום הראשון שלו, [length]
 * מספר הימים. הרשת, הכותרת ותצוגת השנה בנויות מזה ולא מ-YearMonth, כדי
 * שחודש עברי (שמתחיל באמצע חודש לועזי) יוצג כחודש שלם משלו.
 */
data class CalMonth(
    val first: LocalDate,
    val length: Int,
    val title: String,
    val subtitle: String,
) {
    val last: LocalDate get() = first.plusDays((length - 1).toLong())
    operator fun contains(date: LocalDate): Boolean = !date.isBefore(first) && !date.isAfter(last)
}

object CalendarMonths {
    private val HE = Locale.forLanguageTag("he")
    private val HEBREW_ULOCALE = ULocale.forLanguageTag("he-u-ca-hebrew")
    private val HEBREW_NAMES = Locale.forLanguageTag("he-u-ca-hebrew-nu-hebr")

    fun gregorian(month: YearMonth, kind: CalendarKind): CalMonth {
        val first = month.atDay(1)
        val last = month.atEndOfMonth()
        return CalMonth(
            first = first,
            length = month.lengthOfMonth(),
            title = "${month.month.getDisplayName(TextStyle.FULL, HE)} ${month.year}",
            // במשולב הכותרת המשנית היא החודשים העבריים שהחודש הלועזי חוצה.
            subtitle = if (kind == CalendarKind.COMBINED) hebrewSpan(first, last) else "",
        )
    }

    /** החודש העברי שמכיל את [date]. */
    fun hebrewContaining(date: LocalDate): CalMonth {
        val cal = hebrewCal(date)
        val day = cal.get(Calendar.DAY_OF_MONTH)
        val length = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val first = date.minusDays((day - 1).toLong())
        return CalMonth(
            first = first,
            length = length,
            title = HebrewDateFormatter.formatMonthYear(first),
            subtitle = gregorianSpan(first, first.plusDays((length - 1).toLong())),
        )
    }

    /** חודשי השנה העברית שמכילה את [date], מתשרי עד אלול (13 בשנה מעוברת). */
    fun hebrewYear(date: LocalDate): List<CalMonth> {
        val months = mutableListOf<CalMonth>()
        var month = hebrewContaining(date)
        // אחורה עד תשרי: החודש הקודם שייך לשנה אחרת כשהשנה העברית שלו שונה.
        val year = hebrewCal(date).get(Calendar.EXTENDED_YEAR)
        while (true) {
            val prev = hebrewContaining(month.first.minusDays(1))
            if (hebrewCal(prev.first).get(Calendar.EXTENDED_YEAR) != year) break
            month = prev
        }
        while (hebrewCal(month.first).get(Calendar.EXTENDED_YEAR) == year) {
            months += month
            month = hebrewContaining(month.last.plusDays(1))
        }
        return months
    }

    /** "תשפ״ז" - השנה העברית של [date]. */
    fun hebrewYearLabel(date: LocalDate): String = try {
        val fmt = android.icu.text.SimpleDateFormat("yyyy", HEBREW_NAMES)
        fmt.format(toDate(date))
    } catch (e: Exception) {
        ""
    }

    /** שם החודש העברי בלבד ("תשרי"). */
    fun hebrewMonthName(date: LocalDate): String = try {
        android.icu.text.SimpleDateFormat("MMMM", HEBREW_NAMES).format(toDate(date))
    } catch (e: Exception) {
        ""
    }

    /** ט״ו - היום בחודש העברי באותיות. */
    fun hebrewDayLabel(date: LocalDate): String =
        HebrewNumerals.toHebrew(HebrewDateFormatter.hebrewDayOfMonth(date))

    private fun hebrewSpan(first: LocalDate, last: LocalDate): String {
        val a = hebrewMonthName(first)
        val b = hebrewMonthName(last)
        val year = hebrewYearLabel(last)
        return if (a == b) "$a $year" else "$a–$b $year"
    }

    private fun gregorianSpan(first: LocalDate, last: LocalDate): String {
        val a = first.month.getDisplayName(TextStyle.FULL, HE)
        val b = last.month.getDisplayName(TextStyle.FULL, HE)
        return if (first.month == last.month) "$a ${last.year}"
        else if (first.year == last.year) "$a–$b ${last.year}"
        else "$a ${first.year}–$b ${last.year}"
    }

    private fun hebrewCal(date: LocalDate): Calendar =
        Calendar.getInstance(HEBREW_ULOCALE).apply { time = toDate(date) }

    private fun toDate(date: LocalDate): Date =
        Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant())
}
