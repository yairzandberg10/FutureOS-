package com.future.sharednav.theme

import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

/**
 * סקאלת הגדלים של המערכת. בסריקה נמצאו 29 גדלי גופן שונים בקוד
 * (7,8,9,10,11,12,13,14,15,16,17,18,19,20,22,24,26,28,32,36,40,42,48,52,
 * 56,64,70,80,96 sp) - כלומר לא הייתה סקאלה בכלל, כל מסך בחר מספר.
 *
 * עשר דרגות מכסות את כל השימושים. הערכים נבחרו מתוך האשכולות שכבר היו
 * בקוד (12sp הופיע 121 פעם, 14sp 110 פעם, 13sp 100 פעם), ושלוש הדרגות
 * המרכזיות זהות לטוקנים שכבר היו קיימים ב-FutureType/ThemeConfig
 * (summary=13, title=17, header=34), כדי שהמסכים שכבר אושרו על המכשיר
 * לא ישתנו.
 *
 * מספרי-ענק (שעון, מחשבון, טיימר) נשארים ערך מפורש במסך שלהם - הם לא
 * טיפוגרפיה של ממשק אלא תצוגה גרפית, ואין להם שני שימושים שצריך ליישר.
 */
object FutureTypography {

    /** מונה הודעות שלא נקראו. */
    val badge: TextUnit = 10.sp

    /** מקרא מקשים, תוויות זעירות. */
    val caption: TextUnit = 11.sp

    /** תוויות שדות, תגיות. */
    val label: TextUnit = 12.sp

    /** שורת הסבר מתחת לכותרת פריט. */
    val summary: TextUnit = 13.sp

    /** טקסט רץ - ברירת המחדל לגוף. */
    val body: TextUnit = 14.sp

    /** תוכן דיאלוג, שורת תפריט, שדה קלט. */
    val dialog: TextUnit = 15.sp

    /** טקסט גוף מודגש, תוכן דיאלוג. */
    val bodyLarge: TextUnit = 16.sp

    /** כותרת של פריט ברשימה. */
    val title: TextUnit = 17.sp

    /** כותרת מסך בשורה העליונה. */
    val screenTitle: TextUnit = 20.sp

    /** כותרת קטע גדולה בתוך מסך. */
    val headline: TextUnit = 24.sp

    /** כותרת ראשית של מסך (הכותרת הגדולה בראש ההגדרות). */
    val display: TextUnit = 34.sp

    /** ערך יחיד שהוא כל תוכן המסך (טיימר, סכום). */
    val hero: TextUnit = 48.sp

    /**
     * מצמיד גודל חופשי לדרגה הקרובה. קיים בשביל מיגרציה של קוד ישן
     * ובשביל הבדיקה שמוודאת את המיפוי; קוד חדש בוחר דרגה לפי תפקיד.
     * גדלים מעל [hero] מוחזרים כמו שהם - אלה מספרי-הענק שאינם ממשק.
     */
    fun snap(size: TextUnit): TextUnit = when {
        size.value <= 10f -> badge
        size.value <= 11f -> caption
        size.value <= 12f -> label
        size.value <= 13f -> summary
        size.value <= 14f -> body
        size.value <= 15f -> dialog
        size.value <= 16f -> bodyLarge
        size.value <= 18f -> title
        size.value <= 21f -> screenTitle
        size.value <= 26f -> headline
        size.value <= 40f -> display
        size.value <= 52f -> hero
        else -> size
    }

    // ---- משקלים, גובה שורה וריווח אותיות ----
    // עד עכשיו כל מסך בחר משקל בעצמו. הדיזיין סיסטם קושר משקל לדרגה:
    // כותרות 700, כותרת פריט 500, גוף 400, ומספר-ענק 300.

    /** מספר-ענק (שעון, בורר שעה) - דק בכוונה. */
    val weightLight: FontWeight = FontWeight.Light

    /** גוף, סיכום, תווית. */
    val weightRegular: FontWeight = FontWeight.Normal

    /** כותרת של פריט ברשימה. */
    val weightMedium: FontWeight = FontWeight.Medium

    /** כותרת של שורת הגדרה. */
    val weightSemibold: FontWeight = FontWeight.SemiBold

    /** כותרת מסך, כותרת ראשית, תוכן דיאלוג, מונה. */
    val weightBold: FontWeight = FontWeight.Bold

    /** גובה שורה אחיד בכל המערכת. */
    const val lineHeightRatio: Float = 1.3f

    /** ריווח אותיות - 0 בכל מקום חוץ מכותרת קטע. */
    val trackingSection: TextUnit = 1.sp

    /**
     * ערכים מספריים בלבד - פני השעון, בורר השעה, טיימר. הספרות שם משתנות
     * כל שנייה, ובגופן פרופורציונלי כל ספרה ברוחב אחר, כך שהשורה "קופצת"
     * בכל תקתוק. הגופן של המערכת הוא Roboto, ולכן החד-רווח שלה הוא
     * Roboto Mono - בדיוק מה שהעיצוב מבקש (--fos-font-mono).
     * לא לשימוש בטקסט רץ.
     */
    val monoFamily: FontFamily = FontFamily.Monospace
}
