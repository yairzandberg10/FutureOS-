package com.future.systemui.fui.lockscreen.logic

/**
 * מדיניות קוד הנעילה (סעיף 529 בצ'קליסט): דוחה קודים שנוחשים ראשונים.
 *
 * חלה רק כשבוחרים קוד חדש (מסך "קוד חדש" בהגדרות מסך הנעילה). בבדיקת קוד
 * היא לא רצה בכלל - קוד חלש שכבר שמור במכשיר ממשיך לפתוח אותו, אחרת עדכון
 * היה נועל את המשתמש מחוץ לטלפון שלו.
 */
object PinPolicy {

    /**
     * הקודים הנפוצים ביותר שלא נתפסים בכללים למטה. 2580/0852 - עמודת האמצע
     * במקלדת הטלפון, הניחוש הראשון בטלפון מקשים.
     */
    private val COMMON = setOf(
        "2580", "0852", "1004", "2000", "1122", "1313", "6969", "1379", "1397", "2468", "1357",
        "123123", "112233", "121212", "654321", "159753", "147258", "258369", "000000",
    )

    /** null = הקוד בסדר; אחרת ההסבר שמוצג למשתמש. */
    fun weakReason(pin: String): String? {
        if (pin.length < PinStore.MIN_LENGTH) return null // האורך נבדק במקום אחר
        val digits = pin.map { it - '0' }
        if (digits.all { it == digits[0] }) return "קוד של ספרה אחת חוזרת קל לנחש"
        val steps = digits.zipWithNext { a, b -> b - a }
        if (steps.all { it == 1 } || steps.all { it == -1 }) return "קוד של ספרות עוקבות קל לנחש"
        if (isRepeatingBlock(pin)) return "קוד שחוזר על עצמו קל לנחש"
        if (pin in COMMON) return "זה אחד הקודים הנפוצים ביותר"
        if (looksLikeYear(pin)) return "שנה (למשל שנת לידה) קלה לנחש"
        return null
    }

    /** 1212, 123123, 12341234 - בלוק קצר שחוזר עד סוף הקוד. */
    private fun isRepeatingBlock(pin: String): Boolean =
        (1..pin.length / 2).any { size ->
            pin.length % size == 0 && pin.chunked(size).distinct().size == 1
        }

    /** 4 ספרות שהן שנה בין 1940 ל-2039. */
    private fun looksLikeYear(pin: String): Boolean {
        if (pin.length != 4) return false
        val year = pin.toIntOrNull() ?: return false
        return year in 1940..2039
    }
}
