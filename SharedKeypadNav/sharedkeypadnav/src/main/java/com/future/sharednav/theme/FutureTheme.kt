package com.future.sharednav.theme

import androidx.compose.ui.graphics.Color

/**
 * עיצוב משותף אמיתי לכל אפליקציות FutureOS - הגרסה המאוחדת של 17 עותקים
 * שהיו קיימים בנפרד בכל אפליקציה (כולם עם אותם 3 הצבעים הבסיסיים, אבל כל
 * אחד עם הרחבות שונות ולא מתואמות). נטען בפועל דרך ThemeClient מה-
 * ContentProvider של FutureUI (com.future.futureui.theme).
 *
 * הבסיס (backgroundColor/surfaceColor/textColor) זהה בין-סוט-2-ביטים בכל
 * 17 העותקים המקוריים - זה "ליבת האמת" שלא זזה. dangerColor/successColor/
 * warningColor היו קיימים רק בחלק מהאפליקציות (Tools, Assistant ואחרות) -
 * כאן הם חלק מהליבה כדי שכל אפליקציה תוכל להשתמש בהם בלי לשכפל קוד.
 *
 * הרחבות שהיו ספציפיות לאפליקציה בודדת (כפתורי מחשבון, פס קלט/פלט
 * טרמינל, צבע מועדפים באנשי קשר) נשארות ספציפיות - הן חשופות כ-extension
 * properties בתחתית הקובץ, כדי לא "להדביק" אותן על אפליקציות שלא צריכות
 * אותן, אך גם לא לשכפל את החישוב שלהן.
 */
data class FutureTheme(
    val isDarkMode: Boolean = true,
    val accentColor: Color = Color.White,
) {
    val backgroundColor: Color = if (isDarkMode) Color.Black else Color(0xFFF2F2F7)
    val surfaceColor: Color = if (isDarkMode) Color(0xFF1C1C1E) else Color.White
    val textColor: Color = if (isDarkMode) Color.White else Color.Black
    val dangerColor: Color = if (isDarkMode) Color(0xFFFF6B6B) else Color(0xFFD32F2F)
    val successColor: Color = if (isDarkMode) Color(0xFF32D74B) else Color(0xFF1E8E3E)
    val warningColor: Color = if (isDarkMode) Color(0xFFFFD60A) else Color(0xFFB8860B)
}

// ---- הרחבות ספציפיות-לאפליקציה (היו משוכפלות בקוד לפני האיחוד) ----

/** שלושת צבעי כפתורי המחשבון (Calculator) - ספרה/פעולה/פוקוס. */
val FutureTheme.calcButtonColor: Color
    get() = if (isDarkMode) Color(0xFF2C2C2E) else Color(0xFFE5E5EA)

val FutureTheme.calcMutedButtonColor: Color
    get() = if (isDarkMode) Color(0xFF3A3A3C) else Color(0xFFD1D1D6)

val FutureTheme.calcButtonFocusedColor: Color
    get() = if (isDarkMode) Color(0xFF5A5A5C) else Color(0xFFB8B8BE)

/** צבעי סרגל הקלט ופלט הפקודות בטרמינל (Terminal). */
val FutureTheme.inputBarColor: Color
    get() = if (isDarkMode) Color(0xFF111111) else Color(0xFFE5E5EA)

val FutureTheme.outputTextColor: Color
    get() = if (isDarkMode) Color(0xFFD0D0D0) else Color(0xFF3A3A3C)

/** צבע כוכב המועדפים באנשי קשר (Contact) - קבוע, לא תלוי מצב כהה/בהיר. */
val FutureTheme.favoriteColor: Color
    get() = Color(0xFFFFC107)
