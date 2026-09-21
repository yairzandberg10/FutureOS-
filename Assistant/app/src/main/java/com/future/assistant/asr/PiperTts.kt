package com.future.assistant.asr

import android.content.Context
import com.k2fsa.sherpa.onnx.OfflineTts
import com.k2fsa.sherpa.onnx.OfflineTtsConfig
import com.k2fsa.sherpa.onnx.OfflineTtsModelConfig
import com.k2fsa.sherpa.onnx.OfflineTtsVitsModelConfig
import java.io.File
import java.io.FileOutputStream

/**
 * מנוע Text-to-Speech נוירוני מקומי (Piper/VITS, דרך sherpa-onnx) - קול
 * הרבה יותר טבעי מ-eSpeak NG (שנשאר בקוד כגיבוי, לא בשימוש כרגע). המודל
 * (he_IL-saspeech-medium, קול עברי) נטען ישירות מ-assets/piper, אבל
 * espeak-ng-data (המשמש את Piper לפונמיזציה) חייב להיות מועתק לדיסק אמיתי
 * קודם - הספרייה הנייטיבית פותחת את הקבצים ישירות (fopen), לא דרך
 * AssetManager, וגם בונה את הנתיב בעצמה כ-"<dataDir>/espeak-ng-data", אז
 * dataDir צריך להצביע על התיקייה שמכילה את espeak-ng-data ולא עליה עצמה.
 */
class PiperTts(private val context: Context) {
    private var tts: OfflineTts? = null

    /** טוען את המודל. חוסם - יש לקרוא מ-thread ברקע. */
    fun init(): Boolean {
        return try {
            val dataParentDir = copyEspeakDataDirIfNeeded()
            val vits = OfflineTtsVitsModelConfig(
                model = "piper/he_IL-saspeech-medium.onnx",
                tokens = "piper/tokens.txt",
                dataDir = dataParentDir.absolutePath,
                noiseScale = 0.667f,
                noiseScaleW = 0.8f,
                lengthScale = 1.0f,
            )
            val config = OfflineTtsConfig(
                model = OfflineTtsModelConfig(vits = vits, numThreads = 2, provider = "cpu"),
            )
            tts = OfflineTts(context.assets, config)
            true
        } catch (e: Exception) {
            tts = null
            false
        }
    }

    private fun copyEspeakDataDirIfNeeded(): File {
        val parentDir = File(context.filesDir, "piper")
        val outDir = File(parentDir, "espeak-ng-data")
        val marker = File(outDir, ".copied")
        if (!marker.exists()) {
            copyAssetDir("piper/espeak-ng-data", outDir)
            marker.createNewFile()
        }
        return parentDir
    }

    private fun copyAssetDir(assetPath: String, outDir: File) {
        val assets = context.assets
        val children = assets.list(assetPath) ?: emptyArray()
        if (children.isEmpty()) {
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

    /** מתמלל ומשמיע את הטקסט. חוסם עד סוף ההשמעה - יש לקרוא מ-thread ברקע. */
    fun speak(text: String) {
        val engine = tts ?: return
        if (text.isBlank()) return
        val audio = engine.generate(text, 0, 1.0f)
        if (audio.samples.isEmpty()) return
        val pcm = ShortArray(audio.samples.size) { i ->
            (audio.samples[i] * 32767f).toInt().coerceIn(-32768, 32767).toShort()
        }
        PcmPlayback.playAndWait(pcm, audio.sampleRate)
    }
}
