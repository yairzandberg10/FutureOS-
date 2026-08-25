package com.future.calendar.data

import java.time.LocalDate

data class DafYomiEntry(val masechet: String, val daf: Int)

/**
 * מחזור דף יומי ה-14 התחיל ב-5 בינואר 2020 ומסתיים ב-7 ביוני 2027 (סיום הש"ס),
 * 2,711 דפים בסך הכל - נתונים מאומתים (לא בדויים). מכיוון שסדר המסכתות וכמות
 * הדפים בכל מחזור זהים, אפשר לחשב כל תאריך (עבר/הווה/עתיד) לפי מחזוריות.
 * מעילה/קינים/תמיד/מדות בסוף סדר קדשים משולבים ומקבלים חלוקת ימים משוערת
 * (מבוססת על טווח תאריכים מאומת של 36 יום לבלוק המשולב) - לא ודאית לגמרי
 * ברמת היום הבודד, בשונה משאר הטבלה שמאומתת במלואה.
 */
object DafYomi {
    private val CYCLE_START: LocalDate = LocalDate.of(2020, 1, 5)

    // (שם מסכת, מספר ימי לימוד - דף ב' עד דף אחרון = סה"כ_דפים-1, חוץ מהזנב המיוחד)
    private val MASECHTOS = listOf(
        "ברכות" to 63,
        "שבת" to 156,
        "עירובין" to 104,
        "פסחים" to 120,
        "שקלים" to 21,
        "יומא" to 87,
        "סוכה" to 55,
        "ביצה" to 39,
        "ראש השנה" to 34,
        "תענית" to 30,
        "מגילה" to 31,
        "מועד קטן" to 28,
        "חגיגה" to 26,
        "יבמות" to 121,
        "כתובות" to 111,
        "נדרים" to 90,
        "נזיר" to 65,
        "סוטה" to 48,
        "גיטין" to 89,
        "קידושין" to 81,
        "בבא קמא" to 118,
        "בבא מציעא" to 118,
        "בבא בתרא" to 175,
        "סנהדרין" to 112,
        "מכות" to 23,
        "שבועות" to 48,
        "עבודה זרה" to 75,
        "הוריות" to 13,
        "זבחים" to 119,
        "מנחות" to 109,
        "חולין" to 141,
        "בכורות" to 60,
        "ערכין" to 33,
        "תמורה" to 33,
        "כריתות" to 27,
        "מעילה" to 21,
        "קינים" to 3,
        "תמיד" to 8,
        "מדות" to 4,
        "נדה" to 72
    )

    private val CYCLE_LENGTH_DAYS = MASECHTOS.sumOf { it.second }

    fun forDate(date: LocalDate): DafYomiEntry? {
        val daysSinceStart = java.time.temporal.ChronoUnit.DAYS.between(CYCLE_START, date)
        var dayInCycle = daysSinceStart % CYCLE_LENGTH_DAYS
        if (dayInCycle < 0) dayInCycle += CYCLE_LENGTH_DAYS

        var remaining = dayInCycle
        for ((name, days) in MASECHTOS) {
            if (remaining < days) {
                val daf = if (name in setOf("קינים", "תמיד", "מדות")) (remaining + 1).toInt() else (remaining + 2).toInt()
                return DafYomiEntry(name, daf)
            }
            remaining -= days
        }
        return null
    }
}
