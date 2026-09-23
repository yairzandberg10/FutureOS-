package com.future.translate.speech

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

/**
 * "השמע" - הקראה במנוע ההקראה של המכשיר. לא בכל מכשיר יש מנוע, ולא כל
 * מנוע מכיר כל שפה - [speak] מחזיר false ואז המסך אומר את זה.
 */
class Speaker(context: Context) {
    private val _speaking = MutableStateFlow(false)
    val speaking: StateFlow<Boolean> = _speaking.asStateFlow()

    private var ready = false
    private val tts: TextToSpeech = TextToSpeech(context.applicationContext) { status ->
        ready = status == TextToSpeech.SUCCESS
    }

    init {
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                _speaking.value = true
            }

            override fun onDone(utteranceId: String?) {
                _speaking.value = false
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                _speaking.value = false
            }

            override fun onError(utteranceId: String?, errorCode: Int) {
                _speaking.value = false
            }
        })
    }

    fun speak(text: String, languageCode: String): Boolean {
        if (!ready || text.isBlank()) return false
        val locale = Locale.forLanguageTag(languageCode)
        val available = tts.isLanguageAvailable(locale)
        if (available < TextToSpeech.LANG_AVAILABLE) return false
        tts.language = locale
        return tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "translate") == TextToSpeech.SUCCESS
    }

    fun stop() {
        tts.stop()
        _speaking.value = false
    }

    fun shutdown() {
        tts.shutdown()
    }
}

/**
 * זיהוי דיבור למצב שיחה - שירות הזיהוי של המכשיר (SpeechRecognizer). במכשיר
 * בלי שירות כזה [available] הוא false, ומצב השיחה עובר להקלדה.
 */
class Listener(private val context: Context) {
    val available: Boolean = SpeechRecognizer.isRecognitionAvailable(context)

    private val _listening = MutableStateFlow(false)
    val listening: StateFlow<Boolean> = _listening.asStateFlow()

    private var recognizer: SpeechRecognizer? = null

    /** מקשיב פעם אחת. [onResult] מקבל את הטקסט, או null כשלא נשמע כלום. */
    fun listen(languageCode: String, onResult: (String?) -> Unit) {
        if (!available) {
            onResult(null)
            return
        }
        stop()
        val r = SpeechRecognizer.createSpeechRecognizer(context)
        recognizer = r
        r.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                _listening.value = true
            }

            override fun onResults(results: Bundle?) {
                _listening.value = false
                val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                onResult(text?.takeIf { it.isNotBlank() })
            }

            override fun onError(error: Int) {
                _listening.value = false
                onResult(null)
            }

            override fun onBeginningOfSpeech() = Unit
            override fun onRmsChanged(rmsdB: Float) = Unit
            override fun onBufferReceived(buffer: ByteArray?) = Unit
            override fun onEndOfSpeech() = Unit
            override fun onPartialResults(partialResults: Bundle?) = Unit
            override fun onEvent(eventType: Int, params: Bundle?) = Unit
        })
        r.startListening(
            Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageCode)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            }
        )
        _listening.value = true
    }

    fun stop() {
        recognizer?.let {
            it.cancel()
            it.destroy()
        }
        recognizer = null
        _listening.value = false
    }
}
