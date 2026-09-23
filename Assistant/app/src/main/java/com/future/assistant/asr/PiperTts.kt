package com.future.assistant.asr

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.content.Context
import android.util.Log
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.nio.FloatBuffer
import java.nio.LongBuffer
import java.util.concurrent.Executors
import java.util.concurrent.Future

/**
 * מנוע Text-to-Speech נוירוני מקומי: טקסט -> HebrewTextNormalizer (מספרים
 * למילים) -> ReNikud (הגייה, HebrewG2P) -> Piper/VITS (קול "shaul" של
 * Phonikud, אומן על אותה הגייה). שני המודלים רצים ישירות על ONNX Runtime.
 *
 * קודם זה היה Piper he_IL-saspeech דרך sherpa-onnx, שם eSpeak קבע את
 * ההגייה - ו-eSpeak לא משחזר תנועות מעברית בלי ניקוד, אז הקול היה טבעי
 * אבל המילים משובשות.
 *
 * רישיון הקול (Phonikud TTS checkpoints) הוא לא-מסחרי (CC-NC).
 */
class PiperTts(private val context: Context) {
    private var env: OrtEnvironment? = null
    private var g2p: HebrewG2P? = null
    private var voice: OrtSession? = null
    private var phonemeIds: Map<Int, Int> = emptyMap()
    private var sampleRate = 22050
    private var scales = floatArrayOf(0.667f, 1.0f, 0.8f)
    private val synthExecutor = Executors.newSingleThreadExecutor()

    /** טוען את המודלים. חוסם - יש לקרוא מ-thread ברקע. */
    fun init(): Boolean {
        return try {
            // שאריות המנוע הקודם (eSpeak לפונמיזציה של sherpa-onnx).
            File(context.filesDir, "piper/espeak-ng-data").deleteRecursively()

            val env = OrtEnvironment.getEnvironment()
            val options = OrtSession.SessionOptions().apply {
                // 4 ולא 8: המודלים קטנים, ו-threads על הליבות החלשות (A55)
                // מוסיפים בעיקר תקורת סנכרון.
                setIntraOpNumThreads(4)
                setOptimizationLevel(OrtSession.SessionOptions.OptLevel.ALL_OPT)
            }
            g2p = HebrewG2P(env, copyAssetIfNeeded("renikud/model.onnx").absolutePath, options)
            voice = env.createSession(copyAssetIfNeeded("piper/shaul.onnx").absolutePath, options)

            val config = JSONObject(context.assets.open("piper/shaul.onnx.json").bufferedReader().use { it.readText() })
            sampleRate = config.getJSONObject("audio").getInt("sample_rate")
            config.getJSONObject("inference").let {
                scales = floatArrayOf(
                    it.getDouble("noise_scale").toFloat(),
                    it.getDouble("length_scale").toFloat(),
                    it.getDouble("noise_w").toFloat(),
                )
            }
            val map = config.getJSONObject("phoneme_id_map")
            phonemeIds = map.keys().asSequence().associate { key ->
                key.codePointAt(0) to map.getJSONArray(key).getInt(0)
            }
            this.env = env
            true
        } catch (e: Exception) {
            Log.e(TAG, "Piper init failed", e)
            false
        }
    }

    /** מקריא את הטקסט. חוסם עד סוף ההשמעה - יש לקרוא מ-thread ברקע. */
    fun speak(text: String) {
        if (voice == null || text.isBlank()) return
        // משפט-משפט: המשפט הבא מסונתז בזמן שהקודם מושמע, כך שתשובה ארוכה
        // מתחילה להישמע אחרי סינתוז המשפט הראשון בלבד.
        val sentences = SENTENCE_END.split(HebrewTextNormalizer.normalize(text)).filter { it.isNotBlank() }
        var next: Future<ShortArray>? = sentences.firstOrNull()?.let { s -> synthExecutor.submit<ShortArray> { synthesize(s) } }
        for (i in sentences.indices) {
            val pcm = next!!.get()
            next = sentences.getOrNull(i + 1)?.let { s -> synthExecutor.submit<ShortArray> { synthesize(s) } }
            if (pcm.isNotEmpty()) PcmPlayback.playAndWait(pcm, sampleRate)
        }
    }

    private fun synthesize(sentence: String): ShortArray {
        val env = env ?: return ShortArray(0)
        val session = voice ?: return ShortArray(0)
        val ipa = g2p?.phonemize(sentence.trim()) ?: return ShortArray(0)

        // הפורמט של Piper: BOS, ואחרי כל פונמה (וגם אחרי ה-BOS) ריפוד, ואז EOS.
        val ids = ArrayList<Long>()
        ids += BOS; ids += PAD
        ipa.codePoints().forEach { cp ->
            val id = phonemeIds[cp] ?: return@forEach
            ids += id.toLong(); ids += PAD
        }
        ids += EOS

        val input = ids.toLongArray()
        val samples: FloatArray = OnnxTensor.createTensor(env, LongBuffer.wrap(input), longArrayOf(1, input.size.toLong())).use { inputT ->
            OnnxTensor.createTensor(env, LongBuffer.wrap(longArrayOf(input.size.toLong())), longArrayOf(1)).use { lengthsT ->
                OnnxTensor.createTensor(env, FloatBuffer.wrap(scales), longArrayOf(3)).use { scalesT ->
                    session.run(mapOf("input" to inputT, "input_lengths" to lengthsT, "scales" to scalesT)).use { out ->
                        val buf = (out.get(0) as OnnxTensor).floatBuffer
                        FloatArray(buf.remaining()).also { buf.get(it) }
                    }
                }
            }
        }
        // נרמול עוצמה כמו ב-Piper המקורי: השיא מגיע לקצה הטווח, בלי קליפינג.
        val peak = samples.maxOfOrNull { kotlin.math.abs(it) }?.coerceAtLeast(0.01f) ?: return ShortArray(0)
        val gain = 32767f / peak
        return ShortArray(samples.size) { i -> (samples[i] * gain).toInt().coerceIn(-32768, 32767).toShort() }
    }

    /** ONNX Runtime טוען מנתיב קובץ - מעתיק מ-assets לאחסון הפנימי פעם אחת. */
    private fun copyAssetIfNeeded(assetPath: String): File {
        val outFile = File(context.filesDir, assetPath)
        if (!outFile.exists() || outFile.length() == 0L) {
            outFile.parentFile?.mkdirs()
            val tmp = File(outFile.path + ".tmp")
            context.assets.open(assetPath).use { input ->
                FileOutputStream(tmp).use { output -> input.copyTo(output) }
            }
            check(tmp.renameTo(outFile)) { "rename $tmp failed" }
        }
        return outFile
    }

    private companion object {
        const val TAG = "PiperTts"
        const val PAD = 0L
        const val BOS = 1L
        const val EOS = 2L
        // הסימן נשאר בסוף המשפט (lookbehind), כדי שהקול ישמע את הנקודה/סימן השאלה.
        val SENTENCE_END = Regex("""(?<=[.!?])\s+|\n+""")
    }
}
