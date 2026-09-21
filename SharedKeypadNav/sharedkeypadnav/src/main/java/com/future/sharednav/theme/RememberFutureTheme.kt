package com.future.sharednav.theme

import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private const val TAG = "SharedNav/FutureTheme"

/**
 * הערכה הפעילה של המערכת, מתעדכנת בזמן אמת.
 *
 * מחליף בלוק של ~20 שורות שהועתק כמעט זהה ל-20 MainActivity שונים:
 * `var theme by remember { ThemeClient.getTheme(...) }` ועוד
 * DisposableEffect עם LifecycleEventObserver שקורא שוב ב-ON_RESUME. הבלוק
 * הזה עדכן את הערכה רק בחזרה לאפליקציה - שינוי מצב כהה/בהיר מהקונטרול
 * סנטר, שנפתח *מעל* האפליקציה בלי להוציא אותה מ-RESUMED, לא נראה עד
 * שיוצאים ונכנסים שוב.
 *
 * כאן נרשמים ל-ContentObserver על ה-Uri של ThemeProvider, שכבר קורא
 * notifyChange בכל עדכון - כך כל אפליקציה פתוחה משנה צבע ברגע השינוי.
 */
@Composable
fun rememberFutureTheme(): FutureTheme {
    val context = LocalContext.current
    var shared by remember(context) { mutableStateOf(ThemeClient.getTheme(context)) }

    DisposableEffect(context) {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                shared = ThemeClient.getTheme(context)
            }
        }
        val registered = try {
            context.contentResolver.registerContentObserver(ThemeClient.THEME_URI, false, observer)
            true
        } catch (e: Exception) {
            // FutureUI לא מותקן, או שההרשאה חסרה - הערכה נשארת זו שנקראה בהתחלה.
            Log.w(TAG, "registerContentObserver נכשל, הערכה לא תתעדכן בזמן אמת", e)
            false
        }
        onDispose {
            if (registered) context.contentResolver.unregisterContentObserver(observer)
        }
    }

    return remember(shared) {
        FutureTheme(isDarkMode = shared.isDarkMode, accentColor = Color(shared.primaryColor))
    }
}
