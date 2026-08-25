package com.future.futurelauncher.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** FutureOS משתמשת תמיד בשפה עיצובית כהה/זכוכית אחידה בכל האפליקציות - אין מצב בהיר. */
private val FutureOsColorScheme = darkColorScheme(
    primary = Color.White,
    onPrimary = Color.Black,
    secondary = Color.White,
    background = Color.Black,
    onBackground = Color.White,
    surface = Color.White.copy(alpha = 0.08f),
    onSurface = Color.White,
    surfaceVariant = Color.White.copy(alpha = 0.16f),
    onSurfaceVariant = Color.White,
    error = Color(0xFFFF6B6B)
)

@Composable
fun FutureLauncherTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = FutureOsColorScheme,
        typography = Typography,
        content = content
    )
}
