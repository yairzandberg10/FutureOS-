package com.future.assistant.asr

import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionService
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log

/**
 * חושף את מנוע התמלול המקומי (Whisper.cpp, ר' LocalSpeechEngine) כשירות
 * זיהוי-דיבור אמיתי של אנדרואיד (RecognitionService) - נקודת ההרחבה
 * הרשמית שהמערכת מספקת בדיוק למקרה הזה: אפליקציה אחרת (כאן: המקלדת T9,
 * ר' KeyboardService.startVoiceTranscription) יכולה לבקש תמלול ממנה
 * דרך ה-API הרגיל של SpeechRecognizer, פשוט על ידי מיקוד ComponentName
 * ספציפי במקום ברירת המחדל של המערכת - בלי צורך ב-IPC מותאם-אישית.
 *
 * שימושי כי שירות זיהוי הדיבור של המערכת (למשל של גוגל) עשוי להיות חסום/לא
 * זמין במכשיר הזה (ר' ההערה המקבילה ב-LocalSpeechEngine), בעוד שהמנוע
 * המקומי הזה תמיד עובד כי הוא לא תלוי ברשת/שירות חיצוני.
 */
class AssistantRecognitionService : RecognitionService() {

    private lateinit var speechEngine: LocalSpeechEngine
    @Volatile private var modelReady = false
    private val workerThread = java.util.concurrent.Executors.newSingleThreadExecutor()
    private var isRecording = false

    // נשמר מ-onStartListening כדי ש-onStopListening (שמקבל רק Callback, בלי ה-Intent
    // המקורי) עדיין ידע לאיזו שפה לתמלל.
    @Volatile private var currentLanguage: String = "he"

    override fun onCreate() {
        super.onCreate()
        speechEngine = LocalSpeechEngine(this)
        // טעינת המודל לוקחת זמן (העתקה מ-assets בפעם הראשונה) - עושים את זה
        // מראש ברקע כדי שהיא כבר תהיה מוכנה עד שבקשת תמלול ראשונה תגיע.
        workerThread.execute {
            try {
                speechEngine.loadModel()
                modelReady = true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load Whisper model", e)
            }
        }
    }

    override fun onStartListening(recognizerIntent: Intent?, listener: Callback) {
        // קוד השפה (ISO 639-1 דו-אותי, כמו "he"/"en"/"es") שהלקוח (המקלדת) ביקש -
        // ר' KeyboardService.localeFor, ששולח שם locale מלא (he-IL) ש-Keyboard
        // עצמו חותך לקידומת לפני השליחה (ר' startVoiceTranscription).
        currentLanguage = recognizerIntent?.getStringExtra(RecognizerIntent.EXTRA_LANGUAGE)?.substringBefore('-') ?: "he"
        try {
            listener.readyForSpeech(Bundle())
        } catch (e: Exception) {
            Log.w(TAG, "readyForSpeech callback failed", e)
        }
        workerThread.execute {
            try {
                // המודל עוד לא נטען (בקשה ראשונה, מיד אחרי onCreate) - מחכים לו
                // באותו thread רקע במקום להיכשל, כדי לא "לאבד" את הבקשה הראשונה.
                while (!modelReady) Thread.sleep(50)
                speechEngine.startRecording()
                isRecording = true
                listener.beginningOfSpeech()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start recording", e)
                safeError(listener, SpeechRecognizer.ERROR_AUDIO)
            }
        }
    }

    override fun onStopListening(listener: Callback) {
        workerThread.execute {
            if (!isRecording) {
                safeError(listener, SpeechRecognizer.ERROR_CLIENT)
                return@execute
            }
            isRecording = false
            try {
                listener.endOfSpeech()
                val language = currentLanguage
                val text = speechEngine.stopRecordingAndTranscribe(language)
                if (text.isBlank()) {
                    safeError(listener, SpeechRecognizer.ERROR_NO_MATCH)
                    return@execute
                }
                val bundle = Bundle().apply {
                    putStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION, arrayListOf(text))
                }
                listener.results(bundle)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to transcribe", e)
                safeError(listener, SpeechRecognizer.ERROR_RECOGNIZER_BUSY)
            }
        }
    }

    override fun onCancel(listener: Callback) {
        workerThread.execute {
            if (isRecording) {
                isRecording = false
                try { speechEngine.stopRecordingAndTranscribe(currentLanguage) } catch (e: Exception) { /* מבטלים - התוצאה לא רלוונטית */ }
            }
        }
    }

    private fun safeError(listener: Callback, code: Int) {
        try { listener.error(code) } catch (e: Exception) { Log.w(TAG, "error callback failed", e) }
    }

    override fun onDestroy() {
        super.onDestroy()
        workerThread.shutdownNow()
    }

    companion object {
        private const val TAG = "AssistantRecognition"
    }
}
