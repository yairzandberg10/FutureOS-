package com.future.sfarim.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * עיצוב משותף בין כל אפליקציות FutureOS (כהה/בהיר, צבע הדגשה), נטען דרך
 * ThemeClient מה-ContentProvider של FutureUI.
 */
data class FutureTheme(
    val isDarkMode: Boolean = true,
    val accentColor: Color = Color.White
) {
    val backgroundColor: Color = if (isDarkMode) Color.Black else Color(0xFFF2F2F7)
    val surfaceColor: Color = if (isDarkMode) Color(0xFF1C1C1E) else Color.White
    val textColor: Color = if (isDarkMode) Color.White else Color.Black
}
