package com.future.sharednav.t9

/**
 * מיפוי הספרות (2-9) לאותיות T9, משותף לכל מקום בסוויטה שצריך לדעת "איזה
 * אותיות מייצגת הספרה הזאת על המקלדת הפיזית" - חיפוש T9 בחייגן ובנגן
 * המוזיקה, וניבוי הטקסט במקלדת. זה רק המיפוי הגולמי: לוגיקת ההתאמה/חיפוש
 * (T9Search) ולוגיקת הניבוי המילוני (T9Engine) נשארות נפרדות בכל אפליקציה
 * כי הן אלגוריתמים שונים לגמרי שרק במקרה משתמשים באותו מיפוי בסיס.
 *
 * המיפוי העברי תואם את ההדפסה בפועל על מקשי המכשיר הפיזי (ראו החלוקה
 * למקשים 2-9 למטה) - זה המיפוי הקנוני היחיד בסוויטה, ואין עותקים נפרדים
 * שצריך לשמור מסונכרנים ידנית.
 */
object T9DigitMap {
    val ENGLISH: Map<Char, String> = mapOf(
        '2' to "abc",
        '3' to "def",
        '4' to "ghi",
        '5' to "jkl",
        '6' to "mno",
        '7' to "pqrs",
        '8' to "tuv",
        '9' to "wxyz",
    )

    // מיפוי זה מתאים למקלדת הפיזית בפועל (כפי שדווח על ידי המשתמש, מקש
    // אחר מקש) - שונה מהתקן שהיה קודם בקוד. אותיות סופיות (ך ם ן ף ץ)
    // אינן מודפסות על המקשים בעצמן, ולכן הוצמדו כאן לאחר האות הרגילה
    // המקבילה על אותו מקש (למשל ם אחרי מ) - כדי שגם ניבוי המילון וגם
    // מצב ה-multi-tap ימשיכו לעבוד עבור מילים שמסתיימות באות סופית.
    val HEBREW: Map<Char, String> = mapOf(
        '2' to "דהו",
        '3' to "אבג",
        '4' to "מנםן",
        '5' to "יכלך",
        '6' to "זחט",
        '7' to "רשת",
        '8' to "צקץ",
        '9' to "סעפף",
    )

    // 15 השפות הנוספות (מעבר לעברית/אנגלית) - כל אחת מרחיבה את פריסת ה-ITU
    // E.161 הבסיסית (2:abc 3:def 4:ghi 5:jkl 6:mno 7:pqrs 8:tuv 9:wxyz) עם
    // האותיות המיוחדות של השפה, צמודות אחרי האות הבסיסית המקבילה על אותו
    // מקש - בדיוק כמו שהאותיות הסופיות בעברית צמודות לאות הרגילה שלהן למעלה.
    // אין להן עדיין מילון תדירות מלא (ר' T9Engine) - הקלדת multi-tap עצמה
    // כן עובדת, רק בלי הצעות ניבוי אוטומטיות.
    val SPANISH: Map<Char, String> = mapOf(
        '2' to "abcá", '3' to "defé", '4' to "ghií", '5' to "jkl",
        '6' to "mnoñó", '7' to "pqrs", '8' to "tuvú", '9' to "wxyzü",
    )
    val FRENCH: Map<Char, String> = mapOf(
        '2' to "abcàâæç", '3' to "deféèêë", '4' to "ghiîï", '5' to "jkl",
        '6' to "mnoôœ", '7' to "pqrs", '8' to "tuvùûü", '9' to "wxyzÿ",
    )
    val GERMAN: Map<Char, String> = mapOf(
        '2' to "abcä", '3' to "def", '4' to "ghi", '5' to "jkl",
        '6' to "mnoö", '7' to "pqrsß", '8' to "tuvü", '9' to "wxyz",
    )
    val ITALIAN: Map<Char, String> = mapOf(
        '2' to "abcà", '3' to "defèé", '4' to "ghiì", '5' to "jkl",
        '6' to "mnoò", '7' to "pqrs", '8' to "tuvù", '9' to "wxyz",
    )
    val PORTUGUESE: Map<Char, String> = mapOf(
        '2' to "abcãáàâç", '3' to "deféê", '4' to "ghií", '5' to "jkl",
        '6' to "mnoóôõ", '7' to "pqrs", '8' to "tuvúü", '9' to "wxyz",
    )
    val DUTCH: Map<Char, String> = mapOf(
        '2' to "abc", '3' to "defé", '4' to "ghiï", '5' to "jkl",
        '6' to "mnoö", '7' to "pqrs", '8' to "tuvü", '9' to "wxyz",
    )
    val POLISH: Map<Char, String> = mapOf(
        '2' to "abcą", '3' to "defę", '4' to "ghi", '5' to "jklł",
        '6' to "mnońó", '7' to "pqrsś", '8' to "tuv", '9' to "wxyzźż",
    )
    val TURKISH: Map<Char, String> = mapOf(
        '2' to "abcç", '3' to "def", '4' to "ghiığ", '5' to "jkl",
        '6' to "mnoö", '7' to "pqrsş", '8' to "tuvü", '9' to "wxyz",
    )
    val ROMANIAN: Map<Char, String> = mapOf(
        '2' to "abcăâ", '3' to "def", '4' to "ghiî", '5' to "jkl",
        '6' to "mno", '7' to "pqrsș", '8' to "tuvț", '9' to "wxyz",
    )
    val CZECH: Map<Char, String> = mapOf(
        '2' to "abcáč", '3' to "defďéě", '4' to "ghií", '5' to "jkl",
        '6' to "mnoňó", '7' to "pqrsřš", '8' to "tuvťúů", '9' to "wxyzýž",
    )
    val SWEDISH: Map<Char, String> = mapOf(
        '2' to "abcåä", '3' to "def", '4' to "ghi", '5' to "jkl",
        '6' to "mnoö", '7' to "pqrs", '8' to "tuv", '9' to "wxyz",
    )
    val NORWEGIAN: Map<Char, String> = mapOf(
        '2' to "abcå", '3' to "defæ", '4' to "ghi", '5' to "jkl",
        '6' to "mnoø", '7' to "pqrs", '8' to "tuv", '9' to "wxyz",
    )
    val DANISH: Map<Char, String> = mapOf(
        '2' to "abcå", '3' to "defæ", '4' to "ghi", '5' to "jkl",
        '6' to "mnoø", '7' to "pqrs", '8' to "tuv", '9' to "wxyz",
    )
    val FINNISH: Map<Char, String> = mapOf(
        '2' to "abcäå", '3' to "def", '4' to "ghi", '5' to "jkl",
        '6' to "mnoö", '7' to "pqrs", '8' to "tuv", '9' to "wxyz",
    )
    // מיפוי T9 קירילי סטנדרטי (בדיוק כמו שנהוג היה בטלפוני פיצ'ר רוסיים) -
    // לא הרחבה של ITU E.161 הלטיני כמו שאר השפות למעלה, כי הא"ב שונה לגמרי.
    val RUSSIAN: Map<Char, String> = mapOf(
        '2' to "абвг", '3' to "дежз", '4' to "ийкл", '5' to "мноп",
        '6' to "рсту", '7' to "фхцч", '8' to "шщъы", '9' to "ьэюя",
    )

    /** true אם התו שייך לטווח האותיות העבריות (כולל ניקוד/סופיות) - שימושי
     * כדי להחליט איזה מיפוי (עברי/אנגלי) להפעיל על טקסט נתון. */
    fun isHebrew(char: Char): Boolean = char in '֐'..'׿'
}
