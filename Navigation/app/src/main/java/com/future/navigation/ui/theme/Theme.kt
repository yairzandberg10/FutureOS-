package com.future.navigation.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun NavigationTheme(isDarkMode: Boolean = true, accentColor: Color = Color(0xFF4C8EFF), content: @Composable () -> Unit) {
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
