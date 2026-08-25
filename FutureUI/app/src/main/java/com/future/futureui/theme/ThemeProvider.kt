package com.future.futureui.theme

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.content.SharedPreferences
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri

/**
 * מקור אמת יחיד לעיצוב (מצב כהה/בהיר, צבע הדגשה) שמשותף בין כל אפליקציות
 * FutureOS - כל אפליקציה היא UID נפרד, אז SharedPreferences רגילות לא
 * משותפות ביניהן. ContentProvider הוא הדרך הסטנדרטית של אנדרואיד לשתף
 * נתונים כאלה בין אפליקציות שונות.
 *
 * שימוש מאפליקציה אחרת: ThemeClient(context).getTheme() / setTheme(...)
 */
class ThemeProvider : ContentProvider() {

    companion object {
        const val AUTHORITY = "com.future.futureui.theme"
        val CONTENT_URI: Uri = Uri.parse("content://$AUTHORITY/theme")

        const val COL_IS_DARK_MODE = "is_dark_mode"
        const val COL_PRIMARY_COLOR = "primary_color"
    }

    private lateinit var prefs: SharedPreferences

    override fun onCreate(): Boolean {
        prefs = context!!.getSharedPreferences("shared_theme_prefs", Context.MODE_PRIVATE)
        return true
    }

    override fun query(
        uri: Uri, projection: Array<out String>?, selection: String?,
        selectionArgs: Array<out String>?, sortOrder: String?
    ): Cursor {
        val cursor = MatrixCursor(arrayOf(COL_IS_DARK_MODE, COL_PRIMARY_COLOR))
        val isDark = if (prefs.getBoolean(COL_IS_DARK_MODE, true)) 1 else 0
        val color = prefs.getInt(COL_PRIMARY_COLOR, android.graphics.Color.WHITE)
        cursor.addRow(arrayOf(isDark, color))
        return cursor
    }

    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int {
        if (values == null) return 0
        val editor = prefs.edit()
        if (values.containsKey(COL_IS_DARK_MODE)) {
            editor.putBoolean(COL_IS_DARK_MODE, values.getAsInteger(COL_IS_DARK_MODE) == 1)
        }
        if (values.containsKey(COL_PRIMARY_COLOR)) {
            editor.putInt(COL_PRIMARY_COLOR, values.getAsInteger(COL_PRIMARY_COLOR))
        }
        editor.apply()
        // מודיע לכל מי שרשום כ"צופה" ב-Uri הזה (למשל ContentObserver) שהעיצוב השתנה
        context?.contentResolver?.notifyChange(CONTENT_URI, null)
        return 1
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0
    override fun getType(uri: Uri): String? = "vnd.android.cursor.item/vnd.$AUTHORITY.theme"
}
