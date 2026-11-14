package com.future.calendar.data

import java.time.LocalDate
import java.time.ZoneId
import java.util.Date
import java.util.Locale

/** תאריך עברי אמיתי דרך android.icu (מובנה באנדרואיד, לא ספריה חיצונית). */
object HebrewDateFormatter {
    private val HEBREW_LOCALE = Locale.forLanguageTag("he-u-ca-hebrew-nu-hebr")

    fun format(date: LocalDate): String {
        return try {
            val javaDate = Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant())
            val fmt = android.icu.text.DateFormat.getDateInstance(android.icu.text.DateFormat.LONG, HEBREW_LOCALE)
            fmt.format(javaDate)
        } catch (e: Exception) {
            ""
        }
    }

    /** רק שם החודש העברי + שנה, לכותרת תצוגת חודש/שנה. */
    fun formatMonthYear(date: LocalDate): String {
        return try {
            val javaDate = Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant())
            val fmt = android.icu.text.SimpleDateFormat("MMMM yyyy", HEBREW_LOCALE)
            fmt.format(javaDate)
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * יום החודש העברי הגולמי (מספר, לא מחרוזת מפורמטת) - כדי שאפשר יהיה להעביר
     * אותו ל-HebrewNumerals.toHebrew ולקבל ספרור באותיות (למשל ט״ו) בתאי לוח
     * השנה, ולא רק כתת-כותרת מפורמטת מראש כמו format()/formatMonthYear().
     */
    fun hebrewDayOfMonth(date: LocalDate): Int {
        return try {
            val javaDate = Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant())
            val cal = android.icu.util.Calendar.getInstance(android.icu.util.ULocale.forLanguageTag("he-u-ca-hebrew"))
            cal.time = javaDate
            cal.get(android.icu.util.Calendar.DAY_OF_MONTH)
        } catch (e: Exception) {
            date.dayOfMonth
        }
    }
}
