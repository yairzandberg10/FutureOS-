package com.future.keyboard

import com.future.sharednav.t9.T9DigitMap

/**
 * מנוע ניבוי טקסט T9 - ממיר רצף לחיצות על מקשי הספרות הפיזיים (2-9) למילים
 * אמיתיות, בדיוק כמו בטלפונים ישנים: כל מקש מייצג כמה אותיות, והמנוע בוחר
 * את המילה הכי נפוצה שמתאימה לרצף הספרות מתוך מילון מובנה. אם אין התאמה
 * במילון, נופלים חזרה למצב "לחיצות מרובות" (multi-tap) - לחיצות חוזרות על
 * אותו מקש עוברות בין האותיות שלו, בדיוק כמו הקלדת SMS ישנה.
 */
class T9Engine(
    private val language: Language,
    // מקור מועמדים חיצוני (למשל [HebrewDictionaryDb]) - כשמסופק ומחזיר תוצאה
    // לא-null, עוקף את המילון הפנימי הקטן בזיכרון עבור השפה הזו. החזרת null
    // (להבדיל מרשימה ריקה) נופלת חזרה למילון הפנימי - כך KeyboardService יכול
    // להזין כאן את מסד הנתונים העברי המלא (כ-8.6 מיליון מילים, גדול מכדי
    // להיטען כולו לזיכרון על מכשיר חלש - ראו HebrewDictionaryDb.kt) ועדיין
    // לקבל ניבוי סביר מהמילון הפנימי בחלון הקצר שבו ה-DB עוד לא מוכן.
    private val externalCandidates: ((String) -> List<String>?)? = null,
) {

    enum class Language { HEBREW, ENGLISH }

    companion object {
        // מיפוי אנגלי/עברי משותף לכל הסוויטה - חי ב-SharedKeypadNav (T9DigitMap),
        // כדי שלא יידרש יותר לשמור אותו מסונכרן ידנית מול T9Search.kt של החייגן/המוזיקה.
        private val ENGLISH_MAP = T9DigitMap.ENGLISH
        private val HEBREW_MAP = T9DigitMap.HEBREW

        // מילון אנגלי אמיתי בכ-370,000 מילים (dwyl/english-words, words_alpha.txt) -
        // נטען מתוך משאב חבוי, בדיוק כמו HEBREW_WORDS למטה. השורות הראשונות בקובץ
        // הן רשימת "פופולריות שיחתית" משוערת שנשמרה מהמילון הקטן שהיה כאן קודם (כדי
        // שרצפי ספרות נפוצים ימשיכו להציג את המילה השימושית קודם), ואחריה כל שאר
        // המילון בסדר אלפביתי - למקור הזה אין נתוני תדירות אמיתיים כמו ל-cc100 העברי.
        private val ENGLISH_WORDS: List<String> by lazy { loadWordList("dict_en.txt") }

        // מילון עברי אמיתי בכ-20,000 מילים, ממוין לפי תדירות שימוש בפועל בעברית
        // (קורפוס cc100) - נטען פעם אחת מתוך קובץ משאב חבוי בתוך ה-APK, במקום
        // הרשימה הקטנה שהייתה כאן קודם (כ-230 מילות "הדגמה"). סדר השורות בקובץ
        // הוא סדר הפופולריות, בדיוק כמו שההיגיון הקיים ב-digitIndex מצפה.
        private val HEBREW_WORDS: List<String> by lazy { loadWordList("dict_he.txt") }

        /** טוען רשימת מילים (שורה למילה) ממשאב חבוי ב-classpath - עובד גם בתוך
         * ה-APK בזמן ריצה וגם בבדיקות יחידה מקומיות (שתיהן חולקות את אותו classpath). */
        private fun loadWordList(resourceName: String): List<String> {
            val stream = T9Engine::class.java.classLoader?.getResourceAsStream(resourceName)
                ?: error("missing bundled word list resource: $resourceName")
            return stream.bufferedReader(Charsets.UTF_8).useLines { lines -> lines.filter { it.isNotBlank() }.toList() }
        }

        /** ממיר מילה לרצף הספרות שהיא הייתה מייצרת - המפתח לחיפוש מהיר במילון. */
        fun digitsFor(word: String, language: Language): String {
            val map = if (language == Language.HEBREW) HEBREW_MAP else ENGLISH_MAP
            val builder = StringBuilder()
            for (ch in word) {
                val digit = map.entries.firstOrNull { ch in it.value }?.key ?: return ""
                builder.append(digit)
            }
            return builder.toString()
        }

        // אינדקס: רצף ספרות -> רשימת מילים תואמות, ממוין לפי סדר הופעה במילון (=פופולריות).
        // ברמת ה-companion (לא לכל מופע T9Engine) כי KeyboardService יוצר מופע T9Engine
        // חדש בכל לחיצה קצרה על # (גם במעבר בין שני מצבי העברית) - עם מילון של כ-20,000
        // מילים, בניית האינדקס מחדש בכל לחיצה כזו הייתה מורגשת במכשיר חלש.
        private val hebrewDigitIndex: Map<String, List<String>> by lazy { buildDigitIndex(HEBREW_WORDS, Language.HEBREW) }
        private val englishDigitIndex: Map<String, List<String>> by lazy { buildDigitIndex(ENGLISH_WORDS, Language.ENGLISH) }

        private fun buildDigitIndex(words: List<String>, language: Language): Map<String, List<String>> =
            // .distinct() שומר על סדר ההופעה הראשון (=פופולריות) אבל מסיר כפילויות
            // ממשיות במילון - בלעדיו המשתמש רואה שני צ'יפים זהים לאותו רצף ספרות.
            words.distinct().groupBy { digitsFor(it, language) }.filterKeys { it.isNotEmpty() }
    }

    private val keyMap = if (language == Language.HEBREW) HEBREW_MAP else ENGLISH_MAP
    private val digitIndex: Map<String, List<String>> = if (language == Language.HEBREW) hebrewDigitIndex else englishDigitIndex

    fun lettersFor(digit: Char): String = keyMap[digit] ?: ""

    /**
     * כל המילים במילון שתואמות לרצף הספרות שהוזן עד כה. סדר ברירת המחדל הוא
     * פופולריות המילון (הכי נפוצה קודם), אבל [wordFrequency] - מספר הפעמים
     * שהמשתמש עצמו כבר הקליד/בחר את המילה - מזיז מילים שנלמדו בפועל קדימה
     * (מיון stable: מילים בעלות אותה תדירות נשארות בסדר המילון המקורי ביניהן).
     */
    fun candidatesFor(digits: String, wordFrequency: (String) -> Int = { 0 }): List<String> {
        if (digits.isEmpty()) return emptyList()
        val base = externalCandidates?.invoke(digits) ?: digitIndex[digits] ?: return emptyList()
        return base.sortedByDescending { wordFrequency(it) }
    }

}
