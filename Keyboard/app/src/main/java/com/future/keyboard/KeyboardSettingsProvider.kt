package com.future.keyboard

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.content.SharedPreferences
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri

/**
 * חושף החוצה (ל-FutureUI's מרכז הבקרה) הגדרות של מקלדת ה-T9 שגרות באותו
 * SharedPreferences ("t9_keyboard_prefs") ש-KeyboardService עצמו קורא/כותב
 * ישירות בתוך התהליך שלו - אותה תבנית בדיוק כמו ThemeProvider של FutureUI,
 * כי אפליקציות שונות הן UID נפרד ולא יכולות לשתף SharedPreferences ישירות.
 */
class KeyboardSettingsProvider : ContentProvider() {

    companion object {
        const val AUTHORITY = "com.future.keyboard.settings"
        val PREDICTIVE_URI: Uri = Uri.parse("content://$AUTHORITY/predictive")
        const val COL_PREDICTIVE_ENABLED = "predictive_enabled"
    }

    private lateinit var prefs: SharedPreferences

    override fun onCreate(): Boolean {
        prefs = context!!.getSharedPreferences("t9_keyboard_prefs", Context.MODE_PRIVATE)
        return true
    }

    override fun query(
        uri: Uri, projection: Array<out String>?, selection: String?,
        selectionArgs: Array<out String>?, sortOrder: String?,
    ): Cursor {
        val cursor = MatrixCursor(arrayOf(COL_PREDICTIVE_ENABLED))
        cursor.addRow(arrayOf(if (prefs.getBoolean(COL_PREDICTIVE_ENABLED, true)) 1 else 0))
        return cursor
    }

    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int {
        if (values == null || !values.containsKey(COL_PREDICTIVE_ENABLED)) return 0
        prefs.edit().putBoolean(COL_PREDICTIVE_ENABLED, values.getAsInteger(COL_PREDICTIVE_ENABLED) == 1).apply()
        context?.contentResolver?.notifyChange(PREDICTIVE_URI, null)
        return 1
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0
    override fun getType(uri: Uri): String? = "vnd.android.cursor.item/vnd.$AUTHORITY.predictive"
}
