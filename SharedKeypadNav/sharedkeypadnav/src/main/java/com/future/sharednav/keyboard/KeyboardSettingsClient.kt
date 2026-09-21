package com.future.sharednav.keyboard

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.util.Log

private const val TAG = "SharedNav/KeyboardSettingsClient"

/**
 * לקוח ל-KeyboardSettingsProvider - כרגע רק מתג ניבוי הטקסט (T9), נקרא/נכתב
 * ממרכז הבקרה של FutureUI. אם אפליקציית המקלדת לא מותקנת, query מחזיר null
 * בשקט ו-isPredictiveEnabled נופל לברירת המחדל (true) בלי לקרוס - אותה תבנית
 * בדיוק כמו ThemeClient.
 */
object KeyboardSettingsClient {
    private val PREDICTIVE_URI: Uri = Uri.parse("content://com.future.keyboard.settings/predictive")

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
}
