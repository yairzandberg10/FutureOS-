package com.future.calendar.data

import com.future.sharednav.i18n.tr
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
        tr("ברכות") to 63,
        tr("שבת") to 156,
        tr("עירובין") to 104,
        tr("פסחים") to 120,
        tr("שקלים") to 21,
        tr("יומא") to 87,
        tr("סוכה") to 55,
        tr("ביצה") to 39,
        tr("ראש השנה") to 34,
        tr("תענית") to 30,
        tr("מגילה") to 31,
        tr("מועד קטן") to 28,
        tr("חגיגה") to 26,
        tr("יבמות") to 121,
        tr("כתובות") to 111,
        tr("נדרים") to 90,
        tr("נזיר") to 65,
        tr("סוטה") to 48,
        tr("גיטין") to 89,
        tr("קידושין") to 81,
        tr("בבא קמא") to 118,
        tr("בבא מציעא") to 118,
        tr("בבא בתרא") to 175,
        tr("סנהדרין") to 112,
        tr("מכות") to 23,
        tr("שבועות") to 48,
        tr("עבודה זרה") to 75,
        tr("הוריות") to 13,
        tr("זבחים") to 119,
        tr("מנחות") to 109,
        tr("חולין") to 141,
        tr("בכורות") to 60,
        tr("ערכין") to 33,
        tr("תמורה") to 33,
        tr("כריתות") to 27,
        tr("מעילה") to 21,
        tr("קינים") to 3,
        tr("תמיד") to 8,
        tr("מדות") to 4,
        tr("נדה") to 72
    )

    private val CYCLE_LENGTH_DAYS = MASECHTOS.sumOf { it.second }

    private val FIRST_DAF = mapOf(tr("קינים") to 23, tr("תמיד") to 26, tr("מדות") to 34)

    /**
     * @param afterNightfall יום הדף היומי מתחלף בצאת הכוכבים, לא בחצות - אם
     * ה"עכשיו" בפועל כבר אחרי צאת הכוכבים המחושב של date, יש להעביר true
     * כדי לקבל את הדף של היום הבא (העברת day+1 ולא רק שינוי ה-date שנשלח).
     */
    fun forDate(date: LocalDate, afterNightfall: Boolean = false): DafYomiEntry? {
        val effectiveDate = if (afterNightfall) date.plusDays(1) else date
        val daysSinceStart = java.time.temporal.ChronoUnit.DAYS.between(CYCLE_START, effectiveDate)
        var dayInCycle = daysSinceStart % CYCLE_LENGTH_DAYS
        if (dayInCycle < 0) dayInCycle += CYCLE_LENGTH_DAYS

        var remaining = dayInCycle
        for ((name, days) in MASECHTOS) {
            if (remaining < days) {
                // קינים, תמיד ומדות ממשיכים את מספור הדפים של מעילה בש"ס וילנא (קינים
                // מ-22, תמיד מ-25:, מדות מ-34) - הדף היומי שלהם הוא קינים 23-25, תמיד
                // 26-33 ומדות 34-37, לא "דף 1" (כך הוצג קודם).
                val first = FIRST_DAF[name] ?: 2
                val daf = (remaining + first).toInt()
                return DafYomiEntry(name, daf)
            }
            remaining -= days
        }
        return null
    }
}
