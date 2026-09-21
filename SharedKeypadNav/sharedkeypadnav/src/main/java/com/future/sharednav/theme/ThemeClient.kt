package com.future.sharednav.theme

import android.content.ContentValues
import android.content.Context
import android.graphics.Color
import android.net.Uri
import android.util.Log
import com.future.sharednav.systemui.SystemUiTarget

private const val TAG = "SharedNav/ThemeClient"

/**
 * לקוח משותף ל-ThemeProvider של FutureUI - מקור האמת לעיצוב (כהה/בהיר,
 * צבע הדגשה) בין כל אפליקציות FutureOS. עד לאיחוד הזה כל אפליקציה
 * מ-22 האפליקציות שהחזיקו את הקובץ הזה שמרה עותק ידני שלו (3 רמות
 * פיצ'רים שונות: 47/32/29 שורות) - כל 22 העותקים חלקו את אותו getTheme
 * בדיוק, ורק חלקם כללו גם setDarkMode/setPrimaryColor. הגרסה המאוחדת
 * חושפת את כל שלוש הפונקציות תמיד; אפליקציה שלא צריכה לכתוב עיצוב פשוט
 * לא קוראת ל-set*.
 *
 * אם FutureUI לא מותקן, query מחזיר null בשקט (במקום לחשוף שגיאה) -
 * getTheme נופל בחזרה לברירת מחדל (כהה, לבן) ולא קורס.
 */
data class SharedTheme(
    val isDarkMode: Boolean,
    val primaryColor: Int,
    val fontSizeMultiplier: Float = 1.0f,
)

object ThemeClient {
    /** ציבורי כדי ש-rememberFutureTheme יוכל להירשם לשינויים בו. */
    val THEME_URI: Uri = Uri.parse("content://${SystemUiTarget.THEME_AUTHORITY}/theme")

    fun getTheme(context: Context): SharedTheme {
        return try {
            context.contentResolver.query(THEME_URI, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val isDark = cursor.getInt(cursor.getColumnIndexOrThrow("is_dark_mode")) == 1
                    val color = cursor.getInt(cursor.getColumnIndexOrThrow("primary_color"))
                    // גרסת FutureUI ישנה לא מחזירה את העמודה הזו - getColumnIndex
                    // (ולא ...OrThrow) כדי שהאפליקציה תמשיך לעבוד מולה.
                    val fontCol = cursor.getColumnIndex("font_size_multiplier")
                    val fontMultiplier = if (fontCol >= 0) cursor.getFloat(fontCol) else 1.0f
                    SharedTheme(isDark, color, fontMultiplier)
                } else null
            } ?: SharedTheme(true, Color.WHITE)
        } catch (e: Exception) {
            Log.w(TAG, "getTheme נכשל, נופל לברירת מחדל (כהה/לבן)", e)
            SharedTheme(true, Color.WHITE)
        }
    }

    fun setDarkMode(context: Context, isDark: Boolean) {
        try {
            val values = ContentValues().apply { put("is_dark_mode", if (isDark) 1 else 0) }
            context.contentResolver.update(THEME_URI, values, null, null)
        } catch (e: Exception) {
            Log.w(TAG, "setDarkMode נכשל", e)
        }
    }

    /** מכפיל גודל הגופן בלבד - הדרך שבה רכיבים משותפים קוראים את הסקאלה
     *  בלי לשלם על בניית SharedTheme שלם (ראו FutureType.rememberFutureType). */
    fun getFontSizeMultiplier(context: Context): Float = getTheme(context).fontSizeMultiplier

    fun setFontSizeMultiplier(context: Context, multiplier: Float) {
        try {
            val values = ContentValues().apply { put("font_size_multiplier", multiplier) }
            context.contentResolver.update(THEME_URI, values, null, null)
        } catch (e: Exception) {
            Log.w(TAG, "setFontSizeMultiplier נכשל", e)
        }
    }

    fun setPrimaryColor(context: Context, color: Int) {
        try {
            val values = ContentValues().apply { put("primary_color", color) }
            context.contentResolver.update(THEME_URI, values, null, null)
        } catch (e: Exception) {
            Log.w(TAG, "setPrimaryColor נכשל", e)
        }
    }
}
