package com.future.keyboard

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.content.SharedPreferences
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri

/**
 * חושף החוצה (למרכז הבקרה של FutureUI ולהגדרות) הגדרות של מקלדת ה-T9 שגרות
 * באותו SharedPreferences ("t9_keyboard_prefs") ש-KeyboardService עצמו
 * קורא/כותב - אותה תבנית בדיוק כמו ThemeProvider של FutureUI, כי אפליקציות
 * שונות הן UID נפרד ולא יכולות לשתף SharedPreferences ישירות.
 *
 * - /predictive: המתג הכללי של הניבוי.
 * - /languages: שורה לכל שפה - באילו שפות מקלידים (# עובר רק עליהן) ובאילו
 *   מהן יש ניבוי. עדכון: code + enabled ו/או predict.
 */
class KeyboardSettingsProvider : ContentProvider() {

    companion object {
        const val AUTHORITY = "com.future.keyboard.settings"
        val PREDICTIVE_URI: Uri = Uri.parse("content://$AUTHORITY/predictive")
        val LANGUAGES_URI: Uri = Uri.parse("content://$AUTHORITY/languages")
        const val COL_PREDICTIVE_ENABLED = "predictive_enabled"
        const val COL_CODE = "code"
        const val COL_NAME = "name"
        const val COL_ENABLED = "enabled"
        const val COL_PREDICT = "predict"
    }

    private lateinit var prefs: SharedPreferences

    override fun onCreate(): Boolean {
        prefs = context!!.getSharedPreferences(KeyboardLanguages.PREFS, Context.MODE_PRIVATE)
        return true
    }

    override fun query(
        uri: Uri, projection: Array<out String>?, selection: String?,
        selectionArgs: Array<out String>?, sortOrder: String?,
    ): Cursor {
        if (uri.lastPathSegment == "languages") {
            val enabled = KeyboardLanguages.enabled(prefs)
            val predict = KeyboardLanguages.predicted(prefs)
            val cursor = MatrixCursor(arrayOf(COL_CODE, COL_NAME, COL_ENABLED, COL_PREDICT))
            T9Engine.Language.entries.forEach { lang ->
                cursor.addRow(arrayOf(lang.name, KeyboardLanguages.displayName(lang), if (lang in enabled) 1 else 0, if (lang in predict) 1 else 0))
            }
            return cursor
        }
        val cursor = MatrixCursor(arrayOf(COL_PREDICTIVE_ENABLED))
        cursor.addRow(arrayOf(if (prefs.getBoolean(COL_PREDICTIVE_ENABLED, true)) 1 else 0))
        return cursor
    }

    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int {
        if (values == null) return 0
        if (uri.lastPathSegment == "languages") {
            val lang = values.getAsString(COL_CODE)?.let { code -> T9Engine.Language.entries.firstOrNull { it.name == code } } ?: return 0
            if (values.containsKey(COL_ENABLED)) KeyboardLanguages.setEnabled(prefs, lang, values.getAsInteger(COL_ENABLED) == 1)
            if (values.containsKey(COL_PREDICT)) KeyboardLanguages.setPredicted(prefs, lang, values.getAsInteger(COL_PREDICT) == 1)
            context?.contentResolver?.notifyChange(LANGUAGES_URI, null)
            return 1
        }
        if (!values.containsKey(COL_PREDICTIVE_ENABLED)) return 0
        prefs.edit().putBoolean(COL_PREDICTIVE_ENABLED, values.getAsInteger(COL_PREDICTIVE_ENABLED) == 1).apply()
        context?.contentResolver?.notifyChange(PREDICTIVE_URI, null)
        return 1
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0
    override fun getType(uri: Uri): String? = "vnd.android.cursor.item/vnd.$AUTHORITY.${uri.lastPathSegment}"
}

/**
 * השפות הפעילות במקלדת. ברירת המחדל: עברית ואנגלית בלבד, עם ניבוי בשתיהן -
 * כך ש-# לא עובר על 17 שפות שהמשתמש לא צריך, והניבוי לא רץ בשפות שלא ביקש.
 * תמיד נשארת לפחות שפה אחת פעילה.
 */
object KeyboardLanguages {
    const val PREFS = "t9_keyboard_prefs"
    private const val KEY_ENABLED = "enabled_languages"
    private const val KEY_PREDICT = "prediction_languages"
    private val DEFAULT = setOf(T9Engine.Language.HEBREW, T9Engine.Language.ENGLISH)

    private fun read(prefs: SharedPreferences, key: String): Set<T9Engine.Language> {
        val raw = prefs.getString(key, null) ?: return DEFAULT
        return raw.split(',').mapNotNull { code -> T9Engine.Language.entries.firstOrNull { it.name == code } }.toSet()
    }

    private fun write(prefs: SharedPreferences, key: String, set: Set<T9Engine.Language>) {
        prefs.edit().putString(key, set.joinToString(",") { it.name }).apply()
    }

    fun enabled(prefs: SharedPreferences): Set<T9Engine.Language> = read(prefs, KEY_ENABLED).ifEmpty { setOf(T9Engine.Language.HEBREW) }
    fun predicted(prefs: SharedPreferences): Set<T9Engine.Language> = read(prefs, KEY_PREDICT)

    fun setEnabled(prefs: SharedPreferences, lang: T9Engine.Language, on: Boolean) {
        val next = if (on) enabled(prefs) + lang else enabled(prefs) - lang
        if (next.isEmpty()) return
        write(prefs, KEY_ENABLED, next)
    }

    fun setPredicted(prefs: SharedPreferences, lang: T9Engine.Language, on: Boolean) {
        write(prefs, KEY_PREDICT, if (on) predicted(prefs) + lang else predicted(prefs) - lang)
    }

    fun displayName(lang: T9Engine.Language): String = when (lang) {
        T9Engine.Language.HEBREW -> "עברית"
        T9Engine.Language.ENGLISH -> "English"
        T9Engine.Language.SPANISH -> "Español"
        T9Engine.Language.FRENCH -> "Français"
        T9Engine.Language.GERMAN -> "Deutsch"
        T9Engine.Language.ITALIAN -> "Italiano"
        T9Engine.Language.PORTUGUESE -> "Português"
        T9Engine.Language.DUTCH -> "Nederlands"
        T9Engine.Language.POLISH -> "Polski"
        T9Engine.Language.TURKISH -> "Türkçe"
        T9Engine.Language.ROMANIAN -> "Română"
        T9Engine.Language.CZECH -> "Čeština"
        T9Engine.Language.SWEDISH -> "Svenska"
        T9Engine.Language.NORWEGIAN -> "Norsk"
        T9Engine.Language.DANISH -> "Dansk"
        T9Engine.Language.FINNISH -> "Suomi"
        T9Engine.Language.RUSSIAN -> "Русский"
    }
}
