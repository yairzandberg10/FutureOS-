package com.future.assistant.asr

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import androidx.core.content.ContextCompat
import com.future.assistant.data.WakeWord
import kotlin.math.sqrt

private const val TAG = "Assistant/Wake"

/**
 * מקשיב למילת ההפעלה ("היי פיוטר", "עוזר") כל עוד חלון העוזר פתוח ולא מקשיב
 * לפקודה. הכול על המכשיר: זיהוי קטעי דיבור לפי עוצמה (VAD), וכל קטע קצר מתומלל
 * ב-Whisper המשותף ונבדק ב-[WakeWord]. קטעים ארוכים (שיחה ברקע) לא מתומללים.
 *
 * ההקלטה והתמלול רצים ב-threads נפרדים: בזמן שקטע אחד מתומלל (1-2 שניות
 * ב-small) המיקרופון ממשיך לקלוט, כך שניסיון שני של "היי פיוטר" לא הולך לאיבוד.
 * כשהתמלול עסוק, רק הקטע האחרון מחכה בתור - קטעים ישנים יותר לא רלוונטיים.
 *
 * רק הקלטה אחת בכל פעם: [stop] סוגר את המיקרופון לפני שההאזנה לפקודה מתחילה.
 * כשמילת ההפעלה מזוהה המיקרופון נסגר מיד, [onDetected] נקרא (משוב מיידי), ואז
 * [onWake] עם מה שנאמר אחריה (ריק אם כלום). שניהם מ-thread ברקע.
 */
class WakeWordListener(private val context: Context) {
    private val lock = Any()
    private var thread: Thread? = null
    private var record: AudioRecord? = null
    /** עולה בכל start/stop - קטע שתומלל אחרי stop לא מפעיל כלום. */
    @Volatile private var generation = 0
    /** הקטע הבא לתמלול (רק האחרון), וה-generation של ה-thread שמתמלל עכשיו. */
    private var pending: FloatArray? = null
    private var workerGen = -1

    val isRunning: Boolean get() = synchronized(lock) { thread != null }

    fun start(onDetected: () -> Unit = {}, onWake: (rest: String) -> Unit) {
        synchronized(lock) {
            if (thread != null) return
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) return
            val gen = ++generation
            pending = null
            thread = Thread({ loop(gen, onDetected, onWake) }, "wake-word").apply { start() }
        }
    }

    /** סוגר את המיקרופון מיד (לא מחכה לתמלול שאולי רץ). */
    fun stop() {
        synchronized(lock) {
            generation++
            pending = null
            runCatching { record?.stop() }
            thread = null
        }
    }

    @SuppressLint("MissingPermission")
    private fun loop(gen: Int, onDetected: () -> Unit, onWake: (String) -> Unit) {
        val minBuf = AudioRecord.getMinBufferSize(RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
        val rec = runCatching {
            AudioRecord(MediaRecorder.AudioSource.VOICE_RECOGNITION, RATE, AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT, maxOf(minBuf, FRAME * 8))
        }.getOrNull()
        if (rec == null || rec.state != AudioRecord.STATE_INITIALIZED) {
            rec?.release()
            synchronized(lock) { if (generation == gen) thread = null }
            return
        }
        synchronized(lock) {
            if (generation != gen) { rec.release(); return }
            record = rec
        }
        Log.i(TAG, "listening")
        try {
            rec.startRecording()
            val frame = ShortArray(FRAME)
            val preRoll = ArrayDeque<FloatArray>()
            val segment = ArrayList<FloatArray>()
            var noise = 0.01f
            var inSpeech = false
            var silentFrames = 0
            var speechFrames = 0
            while (generation == gen) {
                val n = rec.read(frame, 0, FRAME)
                if (n <= 0) break
                val f = FloatArray(n) { frame[it] / 32768f }
                var sum = 0f
                for (v in f) sum += v * v
                val rms = sqrt(sum / n)
                val loud = rms > maxOf(MIN_RMS, noise * 3f)
                if (!inSpeech) {
                    // רצפת הרעש עוקבת לאט אחרי החדר (ומהר כלפי מטה)
                    noise = if (rms < noise) noise * 0.9f + rms * 0.1f else noise * 0.995f + rms * 0.005f
                    preRoll.addLast(f)
                    if (preRoll.size > PRE_ROLL_FRAMES) preRoll.removeFirst()
                    if (loud) {
                        inSpeech = true
                        segment.clear(); segment.addAll(preRoll); preRoll.clear()
                        silentFrames = 0; speechFrames = 1
                    }
                    continue
                }
                segment.add(f)
                if (loud) { speechFrames++; silentFrames = 0 } else silentFrames++
                val tooLong = segment.size > MAX_SEGMENT_FRAMES
                if (silentFrames >= END_SILENCE_FRAMES || tooLong) {
                    inSpeech = false
                    // דיבור ארוך (שיחה, טלוויזיה) - לא מילת הפעלה, לא מבזבזים עליו תמלול
                    if (!tooLong && speechFrames >= MIN_SPEECH_FRAMES) {
                        // בלי השקט שבסוף: הוא רק מאריך את ה-encoder
                        val keep = segment.size - (silentFrames - TAIL_FRAMES).coerceAtLeast(0)
                        val samples = FloatArray(segment.take(keep).sumOf { it.size })
                        var o = 0
                        for (s in segment.take(keep)) { s.copyInto(samples, o); o += s.size }
                        submit(gen, samples, onDetected, onWake)
                    }
                    segment.clear()
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "wake loop failed", e)
        } finally {
            runCatching { rec.stop() }
            rec.release()
            synchronized(lock) {
                if (record === rec) record = null
                if (generation == gen) thread = null
            }
        }
    }

    /** מעביר קטע לתמלול. thread תמלול אחד לכל generation; קטע חדש מחליף את הממתין. */
    private fun submit(gen: Int, samples: FloatArray, onDetected: () -> Unit, onWake: (String) -> Unit) {
        synchronized(lock) {
            if (generation != gen) return
            pending = samples
            if (workerGen == gen) return
            workerGen = gen
        }
        Thread({ work(gen, onDetected, onWake) }, "wake-asr").start()
    }

    private fun work(gen: Int, onDetected: () -> Unit, onWake: (String) -> Unit) {
        while (true) {
            val samples = synchronized(lock) {
                if (generation != gen) {
                    if (workerGen == gen) workerGen = -1
                    return
                }
                val p = pending
                pending = null
                if (p == null) { workerGen = -1; return }
                p
            }
            val t0 = System.currentTimeMillis()
            val text = runCatching { LocalSpeechEngine.transcribeSamples(context, samples, PROMPT, fast = true, maxTokens = WAKE_TOKENS) }.getOrDefault("")
            var rest = WakeWord.match(text)
            Log.i(TAG, "heard='$text' wake=${rest != null} in ${System.currentTimeMillis() - t0}ms (${samples.size / 16}ms audio)")
            if (rest == null) continue
            // נמצאה מילת הפעלה: סוגרים את המיקרופון (כמו stop) ונותנים משוב מיד,
            // עוד לפני התמלול המלא של הפקודה (אם נאמרה).
            synchronized(lock) {
                if (generation != gen) {
                    if (workerGen == gen) workerGen = -1
                    return
                }
                generation++
                pending = null
                runCatching { record?.stop() }
                thread = null
                workerGen = -1
            }
            onDetected()
            // נאמרה גם פקודה ("היי פיוטר מה השעה") - הבדיקה נעצרה אחרי כמה
            // טוקנים, אז הקטע הזה מתומלל שוב עד הסוף.
            if (rest.isNotBlank()) {
                val t1 = System.currentTimeMillis()
                val full = runCatching { LocalSpeechEngine.transcribeSamples(context, samples, null, fast = true) }.getOrDefault("")
                rest = WakeWord.match(full) ?: ""
                Log.i(TAG, "command='$full' in ${System.currentTimeMillis() - t1}ms")
            }
            onWake(rest)
            return
        }
    }

    private companion object {
        const val RATE = 16000
        const val FRAME = 320 // 20ms
        const val MIN_RMS = 0.012f
        const val PRE_ROLL_FRAMES = 15 // 300ms לפני תחילת הדיבור
        // 400ms שקט = סוף הקטע. היה 600ms - כל זה זמן המתנה לפני שהבדיקה בכלל מתחילה.
        const val END_SILENCE_FRAMES = 20
        const val TAIL_FRAMES = 5 // 100ms שקט נשארים בסוף הקטע
        const val MIN_SPEECH_FRAMES = 10 // לפחות 200ms של דיבור
        // 5 שניות: "היי פיוטר" + פקודה קצרה נכנסים בקטע אחד (ב-3.5 שניות פקודה כזו נזרקה)
        const val MAX_SEGMENT_FRAMES = 250
        // הקשר ניטרלי ולא מילת ההפעלה עצמה: Whisper נוטה "לחזור" על ההקשר כשהוא
        // שומע רעש, וזה היה מפעיל את העוזר לבד.
        const val PROMPT = "דיבור קצר בעברית."
        // "היי פיוטר" + מילה: מספיק כדי לדעת אם נאמרה גם פקודה. הפענוח הוא רוב
        // הזמן (כ-80ms לטוקן ב-small), אז התקרה קובעת את המהירות.
        const val WAKE_TOKENS = 8
    }
}
