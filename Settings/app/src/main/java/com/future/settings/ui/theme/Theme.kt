package com.future.settings.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF007AFF),
    background = Color.Black,
    surface = Color(0xFF1C1C1E),
    onBackground = Color.White,
    onSurface = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF007AFF),
    background = Color.White,
    surface = Color.White,
    onBackground = Color.Black,
    onSurface = Color.Black
)

@Composable
fun SettingsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    primaryColor: Color? = null,
    backgroundColor: Color? = null,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) {
        DarkColorScheme.copy(
            primary = primaryColor ?: DarkColorScheme.primary,
            background = backgroundColor ?: Color.Black
        )
    } else {
        LightColorScheme.copy(
            primary = primaryColor ?: LightColorScheme.primary,
            background = backgroundColor ?: Color.White
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
