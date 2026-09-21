package com.future.notes.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.future.sharednav.theme.FutureMaterialTheme
import com.future.sharednav.theme.FutureTheme

/**
 * הצבעים נגזרים מ-ThemeClient (לא מ-isSystemInDarkTheme/Material You), כדי
 * שפתקים יסתנכרן עם מצב כהה/בהיר וצבע הדגשה שנקבעים מ-FutureUI. המיפוי
 * עצמו חי ב-FutureMaterialTheme של המודול המשותף (ר' DialerTheme).
 */
@Composable
fun NotesTheme(isDarkMode: Boolean = true, accentColor: Color = Color.White, content: @Composable () -> Unit) {
    FutureMaterialTheme(theme = FutureTheme(isDarkMode = isDarkMode, accentColor = accentColor), content = content)
}
