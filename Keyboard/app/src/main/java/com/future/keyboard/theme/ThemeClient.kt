package com.future.keyboard.theme

import android.content.Context
import android.graphics.Color
import android.net.Uri

/**
 * לקוח ל-ThemeProvider של FutureUI - מקור האמת המשותף לעיצוב (כהה/בהיר,
 * צבע הדגשה) בין כל אפליקציות FutureOS. ראו dialer/theme/ThemeClient.kt -
 * אותו דפוס בדיוק, מועתק כאן כי Content Provider לא ניתן לשתף ישירות בין
 * מודולי Gradle נפרדים בלי ספריית קוד משותפת.
 */
data class SharedTheme(val isDarkMode: Boolean, val primaryColor: Int)

object ThemeClient {
    private val THEME_URI = Uri.parse("content://com.future.futureui.theme/theme")

    /** אם FutureUI לא מותקן, מחזיר ברירת מחדל (כהה, לבן) ולא קורס. */
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
