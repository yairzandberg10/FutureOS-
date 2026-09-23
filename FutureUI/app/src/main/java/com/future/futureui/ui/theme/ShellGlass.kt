package com.future.futureui.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.border
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import com.future.sharednav.theme.FutureDimens
import com.future.sharednav.theme.FutureMotion
import com.future.sharednav.theme.FutureTheme
import com.future.sharednav.theme.LocalFutureTheme

/**
 * שפת העיצוב של מעטפת המערכת (מרכז בקרה, התראות, אחרונות, ווליום): זכוכית.
 *
 * - הרקע הוא הכהיה שקופה מעל האפליקציה שמתחת - לא משטח אטום ולבן.
 * - אריחים שקופים בשני שלישים: שכבה דקה של הטקסט מעל ההכהיה.
 * - פוקוס = מסגרת בלבד, בצבע הטקסט, בלי מילוי בצבע ההדגשה. צבע ההדגשה לא
 *   צובע את המעטפת - גם מצב "דלוק" הוא ניגוד (אריח בצבע הטקסט), כך שהמעטפת
 *   נראית זהה לכל בחירת הדגשה.
 *
 * עוקב אחרי מצב כהה/בהיר: בבהיר הזכוכית חלבית מעל הכהיה עדינה, בכהה - אפלה.
 */
object ShellGlass {
    /** ההכהיה מאחורי כל המשטח - האפליקציה שמתחת נשארת מורגשת. */
    fun scrim(theme: FutureTheme): Color =
        if (theme.isDarkMode) Color.Black.copy(alpha = 0.62f) else Color(0xFF1C1C22).copy(alpha = 0.38f)

    /** אריח במנוחה - שקוף בשני שלישים. */
    fun tile(theme: FutureTheme): Color =
        if (theme.isDarkMode) Color.White.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.58f)

    /** אריח ממוקד - מעט יותר אטום, יחד עם המסגרת. */
    fun tileFocused(theme: FutureTheme): Color =
        if (theme.isDarkMode) Color.White.copy(alpha = 0.20f) else Color.White.copy(alpha = 0.78f)

    /** משטח גדול (כרטיס מדיה, התראה). */
    fun panel(theme: FutureTheme): Color =
        if (theme.isDarkMode) Color(0xFF1C1C1E).copy(alpha = 0.66f) else Color.White.copy(alpha = 0.70f)

    /** דלוק: ניגוד מלא, לא צבע ההדגשה. */
    fun on(theme: FutureTheme): Color = theme.textColor
    fun onInk(theme: FutureTheme): Color = if (theme.isDarkMode) Color.Black else Color.White

    /** מסגרת הפוקוס. */
    fun ring(theme: FutureTheme): Color = theme.textColor.copy(alpha = 0.92f)

    /** הטקסט על הזכוכית - בבהיר כהה, בכהה בהיר (אותו צבע טקסט של הערכה). */
    fun ink(theme: FutureTheme): Color = theme.textColor
    fun inkMuted(theme: FutureTheme): Color = theme.textColor.copy(alpha = 0.62f)
}

/** מסגרת פוקוס של המעטפת: 2dp בצבע הטקסט, נכנסת ויוצאת ב-focusColorSpec. */
fun Modifier.shellFocusRing(isFocused: Boolean, shape: Shape): Modifier = composed {
    val theme = LocalFutureTheme.current
    val ring by animateColorAsState(
        if (isFocused) ShellGlass.ring(theme) else ShellGlass.ring(theme).copy(alpha = 0f),
        FutureMotion.focusColorSpec,
        label = "shellRing",
    )
    border(FutureDimens.focusBorderControl, ring, shape)
}

@Composable
fun shellTile(isFocused: Boolean): Color {
    val theme = LocalFutureTheme.current
    val c by animateColorAsState(
        if (isFocused) ShellGlass.tileFocused(theme) else ShellGlass.tile(theme),
        FutureMotion.focusColorSpec,
        label = "shellTile",
    )
    return c
}
