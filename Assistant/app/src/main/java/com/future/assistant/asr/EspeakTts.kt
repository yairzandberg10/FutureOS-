package com.future.assistant.asr

import android.content.Context
import java.io.File
import java.io.FileOutputStream

/**
 * מנוע Text-to-Speech מקומי (eSpeak NG, native) - שימש כפתרון ראשוני לכך
 * שבמכשירי הבדיקה אין שום מנוע TTS מותקן ברמת המערכת, לפני המעבר ל-Piper
 * (PiperTts, קול נוירוני טבעי יותר). נשאר כאן כגיבוי, לא בשימוש כרגע.
 */
class EspeakTts(private val context: Context) {
    private var sampleRate = 22050
    private var initialized = false

    /** מעתיק את נתוני השפה מה-assets ומאתחל את המנוע. חוסם - יש לקרוא מ-thread ברקע. */
    fun init(): Boolean {
        val dataDir = copyDataDirIfNeeded()
        val rate = nativeInit(dataDir.absolutePath)
        if (rate <= 0) return false
        sampleRate = rate
        initialized = true
        return true
    }

    /** מתמלל ומשמיע את הטקסט. חוסם עד סוף ההשמעה - יש לקרוא מ-thread ברקע. */
    fun speak(text: String) {
        if (!initialized || text.isBlank()) return
        val samples = nativeSynthesize(text)
        if (samples.isEmpty()) return
        // espeak-ng יכול לדווח קצב דגימה שונה מזה שהתקבל מ-nativeInit (למשל
        // בהתאם לקול/שפה) - צריך את הקצב האמיתי של הסינתוז הזה בדיוק, אחרת
        // ההשמעה נשמעת מהירה ומצווצת מדי (chipmunk).
        val actualRate = nativeGetSampleRate().takeIf { it > 0 } ?: sampleRate
        PcmPlayback.playAndWait(samples, actualRate)
    }

    private fun copyDataDirIfNeeded(): File {
        val outDir = File(context.filesDir, "espeak-ng-data")
        val marker = File(outDir, ".copied")
        if (!marker.exists()) {
            copyAssetDir("espeak-ng-data", outDir)
            marker.parentFile?.mkdirs()
            marker.createNewFile()
        }
        // eSpeak מצפה שנותנים לו את התיקייה שמכילה את "espeak-ng-data" (לא
        // אותה בעצמה) - הוא מוסיף את השם הזה לנתיב בעצמו.
        return outDir.parentFile!!
    }

    private fun copyAssetDir(assetPath: String, outDir: File) {
        val assets = context.assets
        val children = assets.list(assetPath) ?: emptyArray()
        if (children.isEmpty()) {
            // קובץ, לא תיקייה
            outDir.parentFile?.mkdirs()
            assets.open(assetPath).use { input ->
                FileOutputStream(outDir).use { output -> input.copyTo(output) }
            }
            return
        }
        outDir.mkdirs()
        for (child in children) {
            copyAssetDir("$assetPath/$child", File(outDir, child))
        }
    }

    private external fun nativeInit(dataPath: String): Int
    private external fun nativeSynthesize(text: String): ShortArray
    private external fun nativeGetSampleRate(): Int
    private external fun nativeTerminate()

    companion object {
        init { System.loadLibrary("espeak_jni") }
    }
}
