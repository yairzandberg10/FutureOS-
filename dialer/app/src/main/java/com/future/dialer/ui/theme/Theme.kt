package com.future.dialer.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * צבעי כיוון שיחה (נכנסת/יוצאת/שלא נענתה) - קבועים סמנטיים אחידים, כדי שלא
 * יוקלדו כ-hex גולמי בכל מקום שמציג רשומת שיחה (למשל CallHistoryItem).
 */
object DialerCallColors {
    val incoming = Color(0xFF4CAF50)
    val outgoing = Color(0xFF2196F3)
    val missed = Color(0xFFF44336)
}

@Composable
fun DialerTheme(isDarkMode: Boolean = true, accentColor: Color = Color.White, content: @Composable () -> Unit) {
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
            outline = Color.Black.copy(alpha = 0.2f)
        )
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
