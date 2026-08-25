package com.future.terminal.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * עיצוב משותף לכל אפליקציות FutureOS - נטען מתוך ThemeClient (מקור האמת
 * הוא ה-ContentProvider של FutureUI). מצומצם למה שהטרמינל צריך בפועל.
 */
data class FutureTheme(
    val isDarkMode: Boolean = true,
    val accentColor: Color = Color(0xFF32D74B)
) {
    val backgroundColor: Color = if (isDarkMode) Color.Black else Color(0xFFF2F2F7)
    val surfaceColor: Color = if (isDarkMode) Color(0xFF1C1C1E) else Color.White
    val textColor: Color = if (isDarkMode) Color.White else Color.Black
}
