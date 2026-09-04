package com.future.assistant.asr

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import java.io.File
import java.io.FileOutputStream

/**
 * מנוע Text-to-Speech מקומי (eSpeak NG, native) - כי במכשירי הבדיקה אין
 * שום מנוע TTS מותקן ברמת המערכת (android.speech.tts.TextToSpeech נכשל
 * עם "not bound to TTS engine"). קול רובוטי אבל תומך בעברית ופועל לגמרי
 * offline.
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
        playAndWait(samples)
    }

    private fun playAndWait(samples: ShortArray) {
        val minBufferSize = AudioTrack.getMinBufferSize(
            sampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT
        )
        val audioTrack = AudioTrack(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANT)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build(),
            AudioFormat.Builder()
                .setSampleRate(sampleRate)
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .build(),
            maxOf(minBufferSize, samples.size * 2),
            AudioTrack.MODE_STATIC,
            AudioManager.AUDIO_SESSION_ID_GENERATE
        )
        try {
            if (audioTrack.state != AudioTrack.STATE_INITIALIZED) return
            audioTrack.write(samples, 0, samples.size)
            audioTrack.play()
            val durationMs = (samples.size.toLong() * 1000L) / sampleRate
            Thread.sleep(durationMs + 100)
            audioTrack.stop()
        } finally {
            audioTrack.release()
        }
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
    private external fun nativeTerminate()

    companion object {
        init { System.loadLibrary("espeak_jni") }
    }
}
