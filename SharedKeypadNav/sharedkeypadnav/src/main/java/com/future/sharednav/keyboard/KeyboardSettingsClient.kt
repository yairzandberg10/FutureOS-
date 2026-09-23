package com.future.sharednav.keyboard

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.util.Log

private const val TAG = "SharedNav/KeyboardSettingsClient"

/** שפת מקלדת כפי שהמקלדת מדווחת: האם מקלידים בה (# עובר עליה) והאם יש בה ניבוי. */
data class KeyboardLanguage(val code: String, val name: String, val enabled: Boolean, val predict: Boolean)

/**
 * לקוח ל-KeyboardSettingsProvider - מתג הניבוי הכללי ושפות ההקלדה/הניבוי,
 * נקרא/נכתב ממרכז הבקרה של FutureUI ומההגדרות. אם אפליקציית המקלדת לא
 * מותקנת, query מחזיר null בשקט ונופלים לברירת מחדל בלי לקרוס - אותה תבנית
 * בדיוק כמו ThemeClient.
 */
object KeyboardSettingsClient {
    private val PREDICTIVE_URI: Uri = Uri.parse("content://com.future.keyboard.settings/predictive")
    private val LANGUAGES_URI: Uri = Uri.parse("content://com.future.keyboard.settings/languages")

    fun isPredictiveEnabled(context: Context): Boolean {
        return try {
            context.contentResolver.query(PREDICTIVE_URI, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getInt(cursor.getColumnIndexOrThrow("predictive_enabled")) == 1 else null
            } ?: true
        } catch (e: Exception) {
            Log.w(TAG, "isPredictiveEnabled נכשל, נופל לברירת מחדל (מופעל)", e)
            true
        }
    }

    fun setPredictiveEnabled(context: Context, enabled: Boolean) {
        try {
            val values = ContentValues().apply { put("predictive_enabled", if (enabled) 1 else 0) }
            context.contentResolver.update(PREDICTIVE_URI, values, null, null)
        } catch (e: Exception) {
            Log.w(TAG, "setPredictiveEnabled נכשל", e)
        }
    }

    /** כל שפות המקלדת עם המצב שלהן; רשימה ריקה אם המקלדת לא מותקנת. */
    fun languages(context: Context): List<KeyboardLanguage> {
        return try {
            context.contentResolver.query(LANGUAGES_URI, null, null, null, null)?.use { c ->
                val code = c.getColumnIndexOrThrow("code")
                val name = c.getColumnIndexOrThrow("name")
                val enabled = c.getColumnIndexOrThrow("enabled")
                val predict = c.getColumnIndexOrThrow("predict")
                buildList {
                    while (c.moveToNext()) add(KeyboardLanguage(c.getString(code), c.getString(name), c.getInt(enabled) == 1, c.getInt(predict) == 1))
                }
            } ?: emptyList()
        } catch (e: Exception) {
            Log.w(TAG, "languages נכשל", e)
            emptyList()
        }
    }

    fun setLanguage(context: Context, code: String, enabled: Boolean? = null, predict: Boolean? = null) {
        try {
            val values = ContentValues().apply {
                put("code", code)
                enabled?.let { put("enabled", if (it) 1 else 0) }
                predict?.let { put("predict", if (it) 1 else 0) }
            }
            context.contentResolver.update(LANGUAGES_URI, values, null, null)
        } catch (e: Exception) {
            Log.w(TAG, "setLanguage נכשל", e)
        }
    }

    /**
     * מחזור המצבים של שפה אחת במקש OK: כבויה → הקלדה בלבד → הקלדה וניבוי → כבויה.
     * מחזיר את השפה אחרי השינוי (או כפי שהייתה, אם זו השפה הפעילה האחרונה).
     */
    fun cycleLanguage(context: Context, lang: KeyboardLanguage): KeyboardLanguage {
        val next = when {
            !lang.enabled -> lang.copy(enabled = true, predict = false)
            !lang.predict -> lang.copy(predict = true)
            else -> lang.copy(enabled = false, predict = false)
        }
        setLanguage(context, lang.code, enabled = next.enabled, predict = next.predict)
        // המקלדת לא מכבה את השפה הפעילה האחרונה - קוראים שוב את המצב האמיתי.
        return languages(context).firstOrNull { it.code == lang.code } ?: next
    }

    /** תיאור קצר של מצב השפה לתצוגה. */
    fun stateLabel(lang: KeyboardLanguage): String = when {
        !lang.enabled -> "כבויה"
        lang.predict -> "הקלדה וניבוי"
        else -> "הקלדה בלבד"
    }
}
