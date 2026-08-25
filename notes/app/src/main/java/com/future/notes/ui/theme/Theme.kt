package com.future.notes.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * במקום isSystemInDarkTheme()/Material You (שהתעלמו לגמרי מהעיצוב המשותף של
 * FutureOS), הצבעים כאן נגזרים מ-ThemeClient.getTheme() - אותו דפוס בדיוק כמו
 * MessagesTheme, כדי שפתקים יסתנכרן עם מצב כהה/בהיר וצבע הדגשה שנקבעים
 * מ-FutureUI (קונטרול סנטר/הגדרות), לא ממצב המערכת של אנדרואיד.
 */
@Composable
fun NotesTheme(isDarkMode: Boolean = true, accentColor: Color = Color.White, content: @Composable () -> Unit) {
    val colorScheme = if (isDarkMode) {
        darkColorScheme(
            primary = accentColor,
            onPrimary = Color.Black,
            secondary = accentColor,
            background = Color.Black,
            onBackground = Color.White,
            surface = Color.White.copy(alpha = 0.08f),
            onSurface = Color.White,
            surfaceVariant = Color.White.copy(alpha = 0.16f),
            onSurfaceVariant = Color.White,
            primaryContainer = accentColor.copy(alpha = 0.24f),
            onPrimaryContainer = Color.White,
            secondaryContainer = Color.White.copy(alpha = 0.12f),
            outline = Color.White.copy(alpha = 0.3f)
        )
    } else {
        lightColorScheme(
            primary = accentColor,
            onPrimary = Color.White,
            secondary = accentColor,
            background = Color(0xFFF2F2F7),
            onBackground = Color.Black,
            surface = Color.White,
            onSurface = Color.Black,
            surfaceVariant = Color.Black.copy(alpha = 0.06f),
            onSurfaceVariant = Color.Black,
            primaryContainer = accentColor.copy(alpha = 0.18f),
            onPrimaryContainer = Color.Black,
            secondaryContainer = Color.Black.copy(alpha = 0.06f),
            outline = Color.Black.copy(alpha = 0.25f)
        )
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
