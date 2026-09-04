package com.future.assistant.asr

import android.content.Context
import java.io.File
import java.io.FileOutputStream

/**
 * עטיפת Kotlin נוחה סביב Whisper.cpp (מנוע זיהוי דיבור מקומי, native) +
 * Recorder - מקליטה ומתמללת לגמרי בתוך האפליקציה, בלי להסתמך על שירות
 * זיהוי הדיבור של המערכת (SpeechRecognizer/RecognitionService) שיכול
 * להיות חסום ברמת המכשיר (למשל ע"י Device Admin/MDM שחוסם RECORD_AUDIO
 * לאפליקציית המערכת שמבצעת את הזיהוי בפועל, גם כשלאפליקציה שלנו יש הרשאת
 * מיקרופון תקינה משלה). בניגוד לגישת TFLite הקודמת, כאן השפה (עברית)
 * נבחרת בזמן ריצה ולא צרובה בתוך המודל.
 */
class LocalSpeechEngine(private val context: Context) {
    private val recorder = Recorder(context)
    private val whisper = WhisperCpp()
    private val wavFile = File(context.filesDir, "assistant_recording.wav")

    /** טוען את המודל מה-assets (מעתיק לאחסון פנימי בפעם הראשונה). חוסם - יש לקרוא מ-thread ברקע. */
    fun loadModel() {
        val modelFile = copyAssetIfNeeded("ggml-tiny-q8_0.bin")
        check(whisper.init(modelFile.absolutePath)) { "whisper_init_from_file_with_params failed" }
    }

    fun startRecording() {
        recorder.setFilePath(wavFile.absolutePath)
        recorder.start()
    }

    /** עוצר את ההקלטה ומתמלל בעברית. חוסם - יש לקרוא מ-thread ברקע. מחזיר טקסט ריק אם נכשל. */
    fun stopRecordingAndTranscribe(): String {
        recorder.stop()
        val samples = WaveUtil.getSamples(wavFile.absolutePath)
        if (samples.isEmpty()) return ""
        return whisper.transcribe(samples, "he")
    }

    private fun copyAssetIfNeeded(name: String): File {
        val outFile = File(context.filesDir, name)
        if (!outFile.exists() || outFile.length() == 0L) {
            context.assets.open(name).use { input ->
                FileOutputStream(outFile).use { output -> input.copyTo(output) }
            }
        }
        return outFile
    }
}
