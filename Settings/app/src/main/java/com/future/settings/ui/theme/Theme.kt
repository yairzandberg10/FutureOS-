package com.future.settings.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.future.sharednav.theme.FutureMaterialTheme
import com.future.sharednav.theme.FutureTheme

/**
 * מעטפת דקה סביב FutureMaterialTheme של המודול המשותף. הערכה כאן הייתה
 * עם primary כחול-iOS קבוע (0xFF007AFF) כברירת מחדל ורקע לבן במצב בהיר -
 * שני ערכים שלא הופיעו באף אפליקציה אחרת, כך שרכיבי Material בהגדרות
 * (Switch, Slider, AlertDialog) נראו שונה מכל השאר.
 *
 * [backgroundColor] נשאר בחתימה כדי שהקריאה ב-MainActivity לא תשתנה; הוא
 * תמיד שווה לרקע ש-ThemeConfig גוזר מאותו isDarkMode, ולכן אין צורך בו.
 */
@Composable
fun SettingsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    primaryColor: Color? = null,
    @Suppress("UNUSED_PARAMETER") backgroundColor: Color? = null,
    content: @Composable () -> Unit
) {
    FutureMaterialTheme(
        theme = FutureTheme(isDarkMode = darkTheme, accentColor = primaryColor ?: Color.White),
        content = content,
    )
}
