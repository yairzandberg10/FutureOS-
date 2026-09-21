package com.future.dialer.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.future.sharednav.theme.FutureMaterialTheme
import com.future.sharednav.theme.FutureTheme
import com.future.sharednav.theme.mutedTextColor

/**
 * צבעי כיוון שיחה (נכנסת/יוצאת/שלא נענתה). היו שלושה hex גולמיים מלוח
 * הצבעים של Material (ירוק/כחול/אדום), כלומר צבעים שאינם קיימים בפלטה של
 * המערכת ואינם מגיבים למצב כהה/בהיר. הפלטה כולה היא שבעה צבעים לערכה,
 * ואין בה כחול: שיחה יוצאת אינה סטטוס אלא ברירת מחדל, ולכן היא מקבלת את
 * צבע הטקסט המשני ולא צבע משלה ("An icon never carries its own brand
 * color", README של הדיזיין סיסטם).
 */
object DialerCallColors {
    val FutureTheme.incoming: Color get() = successColor
    val FutureTheme.outgoing: Color get() = mutedTextColor
    val FutureTheme.missed: Color get() = dangerColor
}

/**
 * מעטפת דקה סביב FutureMaterialTheme של המודול המשותף. הערכה הייתה כאן
 * העתק ידני של אותו ColorScheme שהופיע גם ב-Messages/notes/Navigation, עם
 * onPrimary=Color.Black קבוע (בלתי קריא על הדגשה כהה).
 */
@Composable
fun DialerTheme(isDarkMode: Boolean = true, accentColor: Color = Color.White, content: @Composable () -> Unit) {
    FutureMaterialTheme(theme = FutureTheme(isDarkMode = isDarkMode, accentColor = accentColor), content = content)
}
