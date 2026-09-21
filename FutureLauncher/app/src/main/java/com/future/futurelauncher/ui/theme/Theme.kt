package com.future.futurelauncher.ui.theme

import androidx.compose.runtime.Composable
import com.future.sharednav.theme.FutureMaterialTheme
import com.future.sharednav.theme.FutureTheme
import com.future.sharednav.theme.rememberFutureTheme

/**
 * מסך הבית נשאר תמיד כהה בכוונה - הוא מצויר מעל טפט, והטקסט עליו
 * (OnWallpaperColor) מניח רקע כהה. אבל צבע ההדגשה, הצורות והטיפוגרפיה
 * מגיעים עכשיו מהערכה המשותפת, כמו בכל אפליקציה אחרת; קודם ההדגשה כאן
 * הייתה לבן קבוע והתעלמה מבחירת המשתמש.
 */
@Composable
fun FutureLauncherTheme(content: @Composable () -> Unit) {
    val shared = rememberFutureTheme()
    FutureMaterialTheme(
        theme = FutureTheme(isDarkMode = true, accentColor = shared.accentColor),
        content = content,
    )
}
