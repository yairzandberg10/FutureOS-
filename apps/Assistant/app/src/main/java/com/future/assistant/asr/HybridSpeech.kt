package com.future.assistant.asr

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognitionService
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

private const val TAG = "Assistant/HybridSpeech"

/**
 * זיהוי דיבור לעוזר: קודם שירות הזיהוי של גוגל בעברית (he-IL) כשהוא מותקן ויש
 * אינטרנט - בעברית הוא מדויק בהרבה מ-Whisper small שרץ על המכשיר - ו-Whisper
 * המקומי כגיבוי. אם השירות של גוגל נכשל (חסום ע"י MDM, אין רשת, שגיאת לקוח),
 * עוברים ל-Whisper באותה האזנה, בלי שהמשתמש צריך ללחוץ שוב, וזוכרים את זה עד
 * סוף התהליך.
 *
 * כל הקריאות ל-[start]/[cancel] מה-main thread (SpeechRecognizer דורש זאת).
 */
class HybridSpeech(private val context: Context, private val whisper: LocalSpeechEngine) {
    private var recognizer: SpeechRecognizer? = null
    private var usingGoogle = false
    private var result: CompletableDeferred<String>? = null
    private var lastPartial = ""
    private var endOfSpeechAt = 0L
    @Volatile private var stopRequested = false
    private var onLevel: ((Float) -> Unit)? = null
    private var onAutoResult: ((String) -> Unit)? = null
    private var onSpeechEnd: (() -> Unit)? = null
    /** נועל את ההתחלה וההפסקה של הקלטת Whisper - מונע מרוץ בין thread ההקלטה ל-cancel. */
    private val lock = Any()

    /**
     * מתחיל להאזין. [onLevel] - עוצמה 0..1 לגלים. [onAutoResult] - כשגוגל מסיים
     * לבד (שקט אחרי משפט) לפני שהמשתמש לחץ OK.
     */
    fun start(onLevel: (Float) -> Unit, onSpeechEnd: () -> Unit = {}, onAutoResult: (String) -> Unit) {
        this.onLevel = onLevel
        this.onAutoResult = onAutoResult
        this.onSpeechEnd = onSpeechEnd
        stopRequested = false
        lastPartial = ""
        val component = if (!googleBroken && isOnline()) googleComponent(context) else null
        if (component != null && startGoogle(component)) return
        startWhisper()
    }

    /** OK בזמן האזנה: עוצר ומחזיר את הטקסט. חוסם עד שהתוצאה מוכנה. */
    suspend fun stopAndTranscribe(): String {
        synchronized(lock) { stopRequested = true }
        if (!usingGoogle) return withContext(Dispatchers.IO) { whisper.stopRecordingAndTranscribe() }
        val deferred = result ?: return lastPartial
        withContext(Dispatchers.Main) { runCatching { recognizer?.stopListening() } }
        val text = withTimeoutOrNull(8_000) { deferred.await() } ?: lastPartial
        withContext(Dispatchers.Main) { release() }
        return text
    }

    fun cancel() {
        if (usingGoogle) {
            stopRequested = true
            runCatching { recognizer?.cancel() }
            release()
        } else {
            // בתוך הנעילה: thread ההקלטה לא יכול להתחיל אחרי הביטול ולהשאיר מיקרופון פתוח
            synchronized(lock) {
                stopRequested = true
                whisper.cancelRecording()
            }
        }
    }

    private fun startWhisper() {
        usingGoogle = false
        val levelCb = onLevel
        val vad = EndOfSpeech()
        Thread {
            // OK/BACK שהגיעו לפני שה-thread התחיל - לא פותחים הקלטה שאף אחד לא יעצור
            synchronized(lock) {
                if (!stopRequested) {
                    whisper.startRecording { db ->
                        val level = ((db + 2f) / 12f).coerceIn(0f, 1f)
                        levelCb?.invoke(level)
                        if (vad.update(level)) Thread { finishWhisperAuto() }.start()
                    }
                }
            }
        }.start()
    }

    /** Whisper: המשתמש סיים לדבר (שקט אחרי דיבור, או שתיקה ארוכה) - מתמללים בלי OK. */
    private fun finishWhisperAuto() {
        synchronized(lock) {
            if (stopRequested) return
            stopRequested = true
        }
        onSpeechEnd?.invoke()
        val t0 = System.currentTimeMillis()
        val text = runCatching { whisper.stopRecordingAndTranscribe() }.getOrDefault("")
        Log.i(TAG, "whisper auto-end: transcribed in ${System.currentTimeMillis() - t0}ms")
        onAutoResult?.invoke(text)
    }

    /**
     * זיהוי סוף דיבור לפי עוצמה (0..1, בערך 25 פעמים בשנייה): רצפת רעש מהרגעים
     * הראשונים, דיבור = מעליה בבירור, וסוף = 800ms שקט אחרי דיבור. בלי דיבור
     * בכלל - 6 שניות. מחזיר true פעם אחת, כשצריך לעצור.
     */
    private class EndOfSpeech {
        private val startedAt = System.currentTimeMillis()
        private var floor = -1f
        private var speechSeen = false
        private var lastLoudAt = 0L
        private var fired = false

        fun update(level: Float): Boolean {
            if (fired) return false
            val now = System.currentTimeMillis()
            floor = if (floor < 0f) level else if (level < floor) level else floor + (level - floor) * 0.01f
            if (level > maxOf(floor + 0.22f, 0.3f)) {
                speechSeen = true
                lastLoudAt = now
            }
            val done = if (speechSeen) now - lastLoudAt > 800 else now - startedAt > 6_000
            if (done) fired = true
            return done
        }
    }

    private fun startGoogle(component: ComponentName): Boolean = try {
        val r = SpeechRecognizer.createSpeechRecognizer(context, component)
        val deferred = CompletableDeferred<String>()
        result = deferred
        r.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) { onLevel?.invoke(((rmsdB + 2f) / 12f).coerceIn(0f, 1f)) }
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() { endOfSpeechAt = System.currentTimeMillis() }
            override fun onPartialResults(partialResults: Bundle?) {
                firstResult(partialResults)?.let { if (it.isNotBlank()) lastPartial = it }
            }
            override fun onEvent(eventType: Int, params: Bundle?) {}
            override fun onResults(results: Bundle?) {
                val text = firstResult(results).orEmpty().ifBlank { lastPartial }
                if (endOfSpeechAt > 0) Log.i(TAG, "google result ${System.currentTimeMillis() - endOfSpeechAt}ms after end of speech")
                endOfSpeechAt = 0L
                deferred.complete(text)
                // סיים לבד (שקט אחרי משפט) לפני שהמשתמש לחץ OK.
                if (!stopRequested) {
                    release()
                    onAutoResult?.invoke(text)
                }
            }
            override fun onError(error: Int) {
                Log.w(TAG, "google recognizer error $error")
                val heardNothing = lastPartial.isBlank()
                if (!stopRequested && heardNothing && error in FALLBACK_ERRORS) {
                    // השירות לא זמין במכשיר הזה (או ברשת) - ממשיכים להאזין עם Whisper.
                    if (error in PERMANENT_ERRORS) googleBroken = true
                    release()
                    deferred.cancel()
                    startWhisper()
                    return
                }
                deferred.complete(lastPartial)
                if (!stopRequested) {
                    release()
                    onAutoResult?.invoke(lastPartial)
                }
            }
        })
        recognizer = r
        usingGoogle = true
        r.startListening(
            Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "he-IL")
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "he-IL")
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                // משפט פקודה עם הפסקה קצרה באמצע לא נחתך באמצע.
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1_000L)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1_000L)
            },
        )
        true
    } catch (e: Exception) {
        Log.w(TAG, "google recognizer unavailable", e)
        googleBroken = true
        release()
        false
    }

    private fun release() {
        runCatching { recognizer?.destroy() }
        recognizer = null
    }

    private fun firstResult(bundle: Bundle?): String? =
        bundle?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()

    private fun isOnline(): Boolean {
        val cm = context.getSystemService(ConnectivityManager::class.java) ?: return false
        val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    companion object {
        /** השירות של גוגל נכשל באופן קבוע בתהליך הזה - לא מנסים שוב עד שהעוזר נסגר. */
        @Volatile private var googleBroken = false

        private val PERMANENT_ERRORS = setOf(
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS,
            SpeechRecognizer.ERROR_CLIENT,
            SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED,
            SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE,
        )
        private val FALLBACK_ERRORS = PERMANENT_ERRORS + setOf(
            SpeechRecognizer.ERROR_NETWORK,
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT,
            SpeechRecognizer.ERROR_SERVER,
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY,
            SpeechRecognizer.ERROR_SERVER_DISCONNECTED,
            SpeechRecognizer.ERROR_TOO_MANY_REQUESTS,
        )

        /** שירות הזיהוי של גוגל, אם מותקן - לא שלנו (AssistantRecognitionService). */
        fun googleComponent(context: Context): ComponentName? {
            val services = context.packageManager.queryIntentServices(Intent(RecognitionService.SERVICE_INTERFACE), 0)
                .map { it.serviceInfo }
                .filter { it.packageName != context.packageName }
            val preferred = listOf("com.google.android.googlequicksearchbox", "com.google.android.tts")
            val match = preferred.firstNotNullOfOrNull { pkg -> services.firstOrNull { it.packageName == pkg } }
                ?: services.firstOrNull { it.packageName.startsWith("com.google") }
            return match?.let { ComponentName(it.packageName, it.name) }
        }
    }
}
