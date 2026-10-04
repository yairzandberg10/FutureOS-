package com.android.sistemui.systemui

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import com.android.sistemui.statusbar.logic.StatusBarLayoutManager

/**
 * מקור אמת יחיד להגדרות שורת המצב, נגיש מבחוץ - כדי שאפליקציית
 * ההגדרות תוכל לשלוט בהן ישירות במקום להפנות את המשתמש ל-FutureUI עצמה.
 * כותב לאותם shared_prefs שהשירותים ב-FutureUI כבר קוראים מהם (status_bar_prefs),
 * אז שינוי מבחוץ מתעדכן אצלם בלולאת הפולינג הרגילה שלהם -
 * בלי צורך במנגנון סנכרון נוסף.
 */
class SystemUiSettingsProvider : ContentProvider() {

    companion object {
        const val AUTHORITY = "com.android.sistemui.systemui"
        val CONTENT_URI: Uri = Uri.parse("content://$AUTHORITY/settings")

        const val COL_SHOW_BATTERY = "show_battery"
        const val COL_SHOW_BLUETOOTH = "show_bluetooth"
        const val COL_USE_24_HOUR_CLOCK = "use_24_hour_clock"
        const val COL_SUPPRESS_SYSTEM_BARS = "suppress_system_bars"
        const val COL_BAR_STYLE = StatusBarLayoutManager.KEY_BAR_STYLE
        // קריאה בלבד: המכשיר נעול (גם כשמסך הנעילה פינה מקום לשיחה). החייגן בודק
        // את זה כדי לא לחשוף יומן/אנשי קשר מתוך שיחה במכשיר נעול.
        const val COL_DEVICE_SECURED = "device_secured"
    }

    private lateinit var statusBarPrefs: android.content.SharedPreferences

    override fun onCreate(): Boolean {
        statusBarPrefs = context!!.getSharedPreferences("status_bar_prefs", Context.MODE_PRIVATE)
        return true
    }

    override fun query(uri: Uri, projection: Array<out String>?, selection: String?, selectionArgs: Array<out String>?, sortOrder: String?): Cursor {
        val cursor = MatrixCursor(arrayOf(COL_SHOW_BATTERY, COL_SHOW_BLUETOOTH, COL_USE_24_HOUR_CLOCK, COL_SUPPRESS_SYSTEM_BARS, COL_BAR_STYLE, COL_DEVICE_SECURED))
        cursor.addRow(
            arrayOf(
                if (statusBarPrefs.getBoolean(COL_SHOW_BATTERY, true)) 1 else 0,
                if (statusBarPrefs.getBoolean(COL_SHOW_BLUETOOTH, true)) 1 else 0,
                if (statusBarPrefs.getBoolean(COL_USE_24_HOUR_CLOCK, true)) 1 else 0,
                if (statusBarPrefs.getBoolean(COL_SUPPRESS_SYSTEM_BARS, true)) 1 else 0,
                // אותה ברירת מחדל כמו StatusBarLayoutManager.getBarStyle - אחרת ההגדרות יציגו סגנון אחר מהשורה עצמה
                statusBarPrefs.getString(COL_BAR_STYLE, StatusBarLayoutManager.STYLE_CAPSULES),
                if (com.android.sistemui.utils.FutureUIState.isSecured) 1 else 0,
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
            values.getAsString(COL_BAR_STYLE)?.takeIf { it in StatusBarLayoutManager.STYLES }?.let { putString(COL_BAR_STYLE, it) }
        }.apply()
        context?.contentResolver?.notifyChange(CONTENT_URI, null)
        return 1
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0
    override fun getType(uri: Uri): String? = "vnd.android.cursor.item/vnd.$AUTHORITY.settings"
}
