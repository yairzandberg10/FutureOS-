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
    private var stopRequested = false
    private var onLevel: ((Float) -> Unit)? = null
    private var onAutoResult: ((String) -> Unit)? = null

    /**
     * מתחיל להאזין. [onLevel] - עוצמה 0..1 לגלים. [onAutoResult] - כשגוגל מסיים
     * לבד (שקט אחרי משפט) לפני שהמשתמש לחץ OK.
     */
    fun start(onLevel: (Float) -> Unit, onAutoResult: (String) -> Unit) {
        this.onLevel = onLevel
        this.onAutoResult = onAutoResult
        stopRequested = false
        lastPartial = ""
        val component = if (!googleBroken && isOnline()) googleComponent(context) else null
        if (component != null && startGoogle(component)) return
        startWhisper()
    }

    /** OK בזמן האזנה: עוצר ומחזיר את הטקסט. חוסם עד שהתוצאה מוכנה. */
    suspend fun stopAndTranscribe(): String {
        stopRequested = true
        if (!usingGoogle) return withContext(Dispatchers.IO) { whisper.stopRecordingAndTranscribe() }
        val deferred = result ?: return lastPartial
        withContext(Dispatchers.Main) { runCatching { recognizer?.stopListening() } }
        val text = withTimeoutOrNull(8_000) { deferred.await() } ?: lastPartial
        withContext(Dispatchers.Main) { release() }
        return text
    }

    fun cancel() {
        stopRequested = true
        if (usingGoogle) {
            runCatching { recognizer?.cancel() }
            release()
        } else {
            whisper.cancelRecording()
        }
    }

    private fun startWhisper() {
        usingGoogle = false
        val levelCb = onLevel
        Thread {
            whisper.startRecording { db -> levelCb?.invoke(((db + 2f) / 12f).coerceIn(0f, 1f)) }
        }.start()
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
            override fun onEndOfSpeech() {}
            override fun onPartialResults(partialResults: Bundle?) {
                firstResult(partialResults)?.let { if (it.isNotBlank()) lastPartial = it }
            }
            override fun onEvent(eventType: Int, params: Bundle?) {}
            override fun onResults(results: Bundle?) {
                val text = firstResult(results).orEmpty().ifBlank { lastPartial }
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
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1_500L)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1_500L)
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
