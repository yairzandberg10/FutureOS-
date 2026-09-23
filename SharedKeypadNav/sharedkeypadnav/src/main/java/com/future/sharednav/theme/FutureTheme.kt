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

// ---- סוגי שיחה ביומן (חייגן) ----
// ארבעה צבעים שמזהים סוג שיחה במבט: אדום - לא נענתה, ירוק - התקבלה, כתום -
// נדחתה, כחול - חויגה. אדום וירוק הם צבעי הסטטוס של הערכה; הכתום והכחול
// לקוחים מפלטת ההדגשות (FutureAccents) ומוכהים במצב בהיר, כדי שיהיו קריאים
// על כרטיס לבן באותה מידה שהם קריאים על שחור.

val FutureTheme.callMissedColor: Color get() = dangerColor
val FutureTheme.callReceivedColor: Color get() = successColor
val FutureTheme.callRejectedColor: Color get() = if (isDarkMode) FutureAccents.Orange else Color(0xFFB85C00)
val FutureTheme.callOutgoingColor: Color get() = if (isDarkMode) FutureAccents.Cyan else Color(0xFF0A6FB0)

/**
 * חמש ההדגשות שהמשתמש יכול לבחור בהגדרות - הצבע היחיד במערכת שבשליטתו
 * (guidelines/colors-accent.html). לבן הוא ברירת המחדל, ולכן שום רכיב לא
 * רשאי להניח שההדגשה צבעונית.
 */
object FutureAccents {
    val White: Color = Color.White
    val Cyan: Color = Color(0xFF64D2FF)
    val Orange: Color = Color(0xFFFF9F0A)
    val Green: Color = Color(0xFF30D158)
    val Purple: Color = Color(0xFFBF5AF2)
    val presets: List<Color> = listOf(White, Cyan, Orange, Green, Purple)
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
