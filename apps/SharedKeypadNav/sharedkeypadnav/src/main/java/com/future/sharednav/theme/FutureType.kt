package com.future.sharednav.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

/**
 * סקאלת טיפוגרפיה אחת למערכת. עד כה היא הייתה קיימת רק בתוך ThemeConfig של
 * אפליקציית ההגדרות, ולכן מחוון גודל הגופן שבהגדרות השפיע רק על מסך
 * ההגדרות עצמו - הרכיבים המשותפים קבעו גדלים קשיחים (ScreenTopBar 20.sp,
 * EmptyState 17.sp, ConfirmDialog 15.sp) ולא הגיבו לו כלל.
 *
 * עכשיו המכפיל נשמר ב-ThemeProvider של FutureUI יחד עם שאר העיצוב (ראו
 * ThemeClient.getTheme), וכל רכיב משותף קורא אותו דרך [rememberFutureType].
 */
data class FutureType(val fontSizeMultiplier: Float = 1.0f) {

    private fun scaled(size: TextUnit): TextUnit = (size.value * fontSizeMultiplier).sp

    // הסקאלה עצמה חיה ב-FutureTypography (ערכים קבועים, בלי מכפיל), וכאן
    // רק מוכפלת. כך אותה סקאלה בדיוק משמשת גם קוד שקורא טוקן ישירות
    // (FutureTypography.body) וגם רכיב שצריך להגיב למחוון גודל הגופן.
    val badge: TextUnit = scaled(FutureTypography.badge)
    val caption: TextUnit = scaled(FutureTypography.caption)
    val label: TextUnit = scaled(FutureTypography.label)
    val summary: TextUnit = scaled(FutureTypography.summary)
    val body: TextUnit = scaled(FutureTypography.body)
    val dialog: TextUnit = scaled(FutureTypography.dialog)
    val bodyLarge: TextUnit = scaled(FutureTypography.bodyLarge)
    val title: TextUnit = scaled(FutureTypography.title)
    val screenTitle: TextUnit = scaled(FutureTypography.screenTitle)
    val headline: TextUnit = scaled(FutureTypography.headline)
    val display: TextUnit = scaled(FutureTypography.display)
    val hero: TextUnit = scaled(FutureTypography.hero)

    // ---- השמות שהיו כאן קודם, כדי שאף קריאה קיימת לא תישבר ----
    val baseFontSize: TextUnit get() = bodyLarge
    val titleFontSize: TextUnit get() = title
    val summaryFontSize: TextUnit get() = summary
    val headerFontSize: TextUnit get() = display
    val screenTitleFontSize: TextUnit get() = screenTitle
    val bodyFontSize: TextUnit get() = body

    /**
     * 15sp. היה מיושר ל-bodyLarge (16sp) כשהסקאלה לא הכירה את הדרגה
     * הזו, אבל tokens/typography.css מונה אותה כאחת משבע הדרגות -
     * תוכן דיאלוג, שורת תפריט ושדה קלט כולם יושבים עליה.
     */
    val dialogFontSize: TextUnit get() = dialog
}

/**
 * null פירושו "אף אחד לא סיפק סקאלה בעץ הזה" - ואז [rememberFutureType]
 * קורא את המכפיל מהמערכת בעצמו. כך רכיב משותף עובד נכון גם במסך שלא עטוף
 * ב-ScreenScaffold, בלי שאף אפליקציה תצטרך לשנות את הקריאה שלה.
 */
val LocalFutureType = compositionLocalOf<FutureType?> { null }

/**
 * הסקאלה הפעילה. קריאת ה-ContentProvider קורית פעם אחת לכל מופע רכיב
 * (remember) ולא בכל recomposition. מסך שעטוף ב-ScreenScaffold מקבל ערך
 * מסופק ולא שואל בכלל.
 */
@Composable
fun rememberFutureType(): FutureType {
    LocalFutureType.current?.let { return it }
    val context = LocalContext.current
    return remember(context) { FutureType(ThemeClient.getFontSizeMultiplier(context)) }
}
