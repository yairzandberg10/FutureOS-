package com.future.futureui.systemui

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri

/**
 * מקור אמת יחיד להגדרות שורת המצב ומסך הנעילה, נגיש מבחוץ - כדי שאפליקציית
 * ההגדרות תוכל לשלוט בהן ישירות במקום להפנות את המשתמש ל-FutureUI עצמה.
 * כותב לאותם shared_prefs שהשירותים ב-FutureUI כבר קוראים מהם (status_bar_prefs,
 * lock_screen_prefs), אז שינוי מבחוץ מתעדכן אצלם בלולאת הפולינג הרגילה שלהם -
 * בלי צורך במנגנון סנכרון נוסף. לא חושף כאן כלום שקשור לקוד PIN בכוונה - זה
 * נשאר רגיש מדי בשביל ContentProvider משותף בלי הרשאות ייעודיות.
 */
class SystemUiSettingsProvider : ContentProvider() {

    companion object {
        const val AUTHORITY = "com.future.futureui.systemui"
        val CONTENT_URI: Uri = Uri.parse("content://$AUTHORITY/settings")

        const val COL_SHOW_BATTERY = "show_battery"
        const val COL_SHOW_BLUETOOTH = "show_bluetooth"
        const val COL_USE_24_HOUR_CLOCK = "use_24_hour_clock"
        const val COL_SUPPRESS_SYSTEM_BARS = "suppress_system_bars"
        const val COL_CLOCK_STYLE = "clock_style"
    }

    private lateinit var statusBarPrefs: android.content.SharedPreferences
    private lateinit var lockScreenPrefs: android.content.SharedPreferences

    override fun onCreate(): Boolean {
        statusBarPrefs = context!!.getSharedPreferences("status_bar_prefs", Context.MODE_PRIVATE)
        lockScreenPrefs = context!!.getSharedPreferences("lock_screen_prefs", Context.MODE_PRIVATE)
        return true
    }

    override fun query(uri: Uri, projection: Array<out String>?, selection: String?, selectionArgs: Array<out String>?, sortOrder: String?): Cursor {
        val cursor = MatrixCursor(arrayOf(COL_SHOW_BATTERY, COL_SHOW_BLUETOOTH, COL_USE_24_HOUR_CLOCK, COL_SUPPRESS_SYSTEM_BARS, COL_CLOCK_STYLE))
        cursor.addRow(
            arrayOf(
                if (statusBarPrefs.getBoolean(COL_SHOW_BATTERY, true)) 1 else 0,
                if (statusBarPrefs.getBoolean(COL_SHOW_BLUETOOTH, true)) 1 else 0,
                if (statusBarPrefs.getBoolean(COL_USE_24_HOUR_CLOCK, true)) 1 else 0,
                if (statusBarPrefs.getBoolean(COL_SUPPRESS_SYSTEM_BARS, true)) 1 else 0,
                lockScreenPrefs.getInt(COL_CLOCK_STYLE, 0)
            )
        )
        return cursor
    }

    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int {
        if (values == null) return 0
        statusBarPrefs.edit().apply {
            if (values.containsKey(COL_SHOW_BATTERY)) putBoolean(COL_SHOW_BATTERY, values.getAsInteger(COL_SHOW_BATTERY) == 1)
            if (values.containsKey(COL_SHOW_BLUETOOTH)) putBoolean(COL_SHOW_BLUETOOTH, values.getAsInteger(COL_SHOW_BLUETOOTH) == 1)
            if (values.containsKey(COL_USE_24_HOUR_CLOCK)) putBoolean(COL_USE_24_HOUR_CLOCK, values.getAsInteger(COL_USE_24_HOUR_CLOCK) == 1)
            if (values.containsKey(COL_SUPPRESS_SYSTEM_BARS)) putBoolean(COL_SUPPRESS_SYSTEM_BARS, values.getAsInteger(COL_SUPPRESS_SYSTEM_BARS) == 1)
        }.apply()
        if (values.containsKey(COL_CLOCK_STYLE)) {
            lockScreenPrefs.edit().putInt(COL_CLOCK_STYLE, values.getAsInteger(COL_CLOCK_STYLE)).apply()
        }
        context?.contentResolver?.notifyChange(CONTENT_URI, null)
        return 1
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0
    override fun getType(uri: Uri): String? = "vnd.android.cursor.item/vnd.$AUTHORITY.settings"
}
