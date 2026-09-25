package com.future.sharednav.theme

import android.content.ContentValues
import android.content.Context
import android.database.ContentObserver
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

    /**
     * עותק אחרון של הערכה לכל התהליך, לנתיבים החמים ([cachedTheme]). כל query
     * ל-ThemeProvider הוא IPC לתהליך של FutureUI ועוד הקצאת CursorWindow -
     * ו-rememberFutureType קרא אותו פעם לכל מופע רכיב (כל כרטיס ברשימה),
     * על ה-main thread, בזמן ה-composition הראשון של המסך.
     * מתאפס ב-ContentObserver ברגע ש-FutureUI מודיע על שינוי.
     */
    @Volatile private var cached: SharedTheme? = null
    @Volatile private var observing = false
    /** עולה בכל שינוי - קריאה שהתחילה לפני השינוי לא תשמור ערך ישן במטמון. */
    @Volatile private var generation = 0

    /** קריאה טרייה מה-ThemeProvider (IPC). מעדכנת גם את המטמון. */
    fun getTheme(context: Context): SharedTheme {
        val gen = generation
        // null = FutureUI לא ענה. ברירת המחדל לא נשמרת, כדי שנפילה רגעית של
        // התהליך שלו לא תקבע ערכה שגויה עד השינוי הבא.
        val theme = queryTheme(context) ?: return SharedTheme(true, Color.WHITE)
        if (observing && gen == generation) cached = theme
        return theme
    }

    /**
     * הערכה מהמטמון, או קריאה אחת ל-ThemeProvider אם הוא ריק. טרי כמו
     * [getTheme] כל עוד ה-observer רשום; אם ההרשאה נכשלה (FutureUI לא מותקן)
     * לא שומרים כלום וכל קריאה חוזרת ל-[getTheme].
     */
    fun cachedTheme(context: Context): SharedTheme {
        cached?.let { return it }
        ensureObserving(context)
        return getTheme(context)
    }

    private fun ensureObserving(context: Context) {
        if (observing) return
        synchronized(this) {
            if (observing) return
            try {
                // handler null: ה-onChange רץ על thread של binder, לא תלוי ב-main.
                context.applicationContext.contentResolver.registerContentObserver(
                    THEME_URI, false,
                    object : ContentObserver(null) {
                        override fun onChange(selfChange: Boolean) {
                            generation++
                            cached = null
                        }
                    },
                )
                observing = true
            } catch (e: Exception) {
                Log.w(TAG, "registerContentObserver נכשל, בלי מטמון לערכה", e)
            }
        }
    }

    private fun queryTheme(context: Context): SharedTheme? {
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
            }
        } catch (e: Exception) {
            Log.w(TAG, "getTheme נכשל, נופל לברירת מחדל (כהה/לבן)", e)
            null
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
     *  מהמטמון - בלי IPC בכל מופע רכיב (ראו FutureType.rememberFutureType). */
    fun getFontSizeMultiplier(context: Context): Float = cachedTheme(context).fontSizeMultiplier

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
