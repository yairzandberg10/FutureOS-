package com.future.messages.ui.components

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

/**
 * הכתבה מקומית - דרך שירות התמלול של Assistant (Whisper על המכשיר), כמו
 * במקלדת. לא דרך ACTION_RECOGNIZE_SPEECH: במכשיר אין שירותי Google, והכוונה
 * הזו פתחה את מסך התמלול של Google.
 *
 * לחיצה ראשונה מתחילה להקשיב, לחיצה שנייה מסיימת והטקסט נכנס לטיוטה.
 */
class LocalDictation internal constructor(
    val available: Boolean,
    private val onToggle: () -> Unit,
) {
    var listening by mutableStateOf(false)
        internal set
    var processing by mutableStateOf(false)
        internal set

    fun toggle() = onToggle()
}

private val AssistantRecognizer = ComponentName(
    "com.future.assistant",
    "com.future.assistant.asr.AssistantRecognitionService",
)

private fun assistantRecognizerInstalled(context: Context): Boolean =
    context.packageManager.queryIntentServices(
        Intent("android.speech.RecognitionService").setPackage(AssistantRecognizer.packageName), 0,
    ).isNotEmpty()

@Composable
fun rememberLocalDictation(onText: (String) -> Unit): LocalDictation {
    val context = LocalContext.current
    val currentOnText by rememberUpdatedState(onText)
    val recognizerHolder = remember { arrayOfNulls<SpeechRecognizer>(1) }
    val available = remember { assistantRecognizerInstalled(context) }

    lateinit var dictation: LocalDictation

    fun finish() {
        dictation.listening = false
        dictation.processing = false
        recognizerHolder[0]?.destroy()
        recognizerHolder[0] = null
    }

    fun start() {
        val recognizer = SpeechRecognizer.createSpeechRecognizer(context, AssistantRecognizer)
        recognizerHolder[0] = recognizer
        recognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
            override fun onError(error: Int) {
                val message = when (error) {
                    SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "לא זוהה דיבור"
                    SpeechRecognizer.ERROR_AUDIO -> "שגיאה במיקרופון"
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "אין הרשאת מיקרופון"
                    else -> "התמלול נכשל"
                }
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                finish()
            }
            override fun onResults(results: Bundle?) {
                val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()?.trim()
                if (!text.isNullOrBlank()) currentOnText(text)
                finish()
            }
        })
        recognizer.startListening(
            Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "he-IL")
                putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
                putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            },
        )
        dictation.listening = true
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) start() else Toast.makeText(context, "צריך הרשאת מיקרופון להכתבה", Toast.LENGTH_SHORT).show()
    }

    dictation = remember {
        LocalDictation(available) {
            when {
                dictation.processing -> Unit
                dictation.listening -> {
                    // העצירה מתחילה את התמלול עצמו - על המכשיר זה לוקח כמה שניות.
                    dictation.listening = false
                    dictation.processing = true
                    recognizerHolder[0]?.stopListening()
                }
                ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                    PackageManager.PERMISSION_GRANTED -> start()
                else -> permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            recognizerHolder[0]?.destroy()
            recognizerHolder[0] = null
        }
    }
    return dictation
}
