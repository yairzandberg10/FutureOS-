package com.future.notes.theme

import android.content.Context
import android.graphics.Color
import android.net.Uri

data class SharedTheme(val isDarkMode: Boolean, val primaryColor: Int)

/**
 * קורא את העיצוב המשותף (מצב כהה/בהיר, צבע הדגשה) מה-ContentProvider של
 * FutureUI, באותו דפוס בדיוק כמו שאר אפליקציות FutureOS (למשל Messages) -
 * כדי שפתקים יסתנכרן עם שאר המערכת במקום להשתמש במצב הכהה/בהיר של אנדרואיד.
 */
object ThemeClient {
    private val THEME_URI = Uri.parse("content://com.future.futureui.theme/theme")

    fun getTheme(context: Context): SharedTheme {
        return try {
            context.contentResolver.query(THEME_URI, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val isDark = cursor.getInt(cursor.getColumnIndexOrThrow("is_dark_mode")) == 1
                    val color = cursor.getInt(cursor.getColumnIndexOrThrow("primary_color"))
                    SharedTheme(isDark, color)
                } else null
            } ?: SharedTheme(true, Color.WHITE)
        } catch (e: Exception) {
            SharedTheme(true, Color.WHITE)
        }
    }
}
