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
    private val wavFile = File(context.filesDir, "assistant_recording.wav")

    /** טוען את המודל מה-assets (מעתיק לאחסון פנימי בפעם הראשונה). חוסם - יש לקרוא מ-thread ברקע. */
    fun loadModel() {
        synchronized(lock) {
            if (sharedWhisper != null) return
            // המודל הקודם (tiny) - לא נמצא יותר ב-assets, אבל העותק שלו נשאר
            // באחסון הפנימי מהתקנות קודמות.
            File(context.filesDir, "ggml-tiny-q8_0.bin").delete()
            val modelFile = copyAssetIfNeeded(MODEL_FILE)
            val whisper = WhisperCpp()
            check(whisper.init(modelFile.absolutePath)) { "whisper_init_from_file_with_params failed" }
            sharedWhisper = whisper
        }
    }

    /** [onLevel] - עוצמת הקול בזמן אמת (סקאלת onRmsChanged), מ-thread ההקלטה. */
    fun startRecording(onLevel: ((Float) -> Unit)? = null) {
        recorder.setLevelListener(onLevel?.let { cb -> Recorder.LevelListener { cb(it) } })
        recorder.setFilePath(wavFile.absolutePath)
        recorder.start()
    }

    /** עוצר את ההקלטה ומתמלל. חוסם - יש לקרוא מ-thread ברקע. מחזיר טקסט ריק אם נכשל.
     * [language] הוא קוד ISO 639-1 דו-אותי (כמו "he"/"en"/"es") - המודל
     * (ggml-small, בלי סיומת ".en") רב-לשוני, לא רק עברית (ר' AssistantRecognitionService
     * שמעביר כאן את שפת המקלדת הנוכחית כשמקלדת T9 מבקשת תמלול). */
    fun stopRecordingAndTranscribe(language: String = "he"): String {
        recorder.stop()
        val samples = WaveUtil.getSamples(wavFile.absolutePath)
        if (samples.isEmpty()) return ""
        val whisper = sharedWhisper ?: return ""
        // העוזר והשירות (AssistantRecognitionService) רצים באותו תהליך וחולקים
        // את אותו context, ש-whisper_full לא בטוח להריץ עליו במקביל.
        return synchronized(lock) { whisper.transcribe(samples, language) }
    }

    private fun copyAssetIfNeeded(name: String): File {
        val outFile = File(context.filesDir, name)
        if (!outFile.exists() || outFile.length() == 0L) {
            // העתקה לקובץ זמני ואז rename: אם התהליך נהרג באמצע ההעתקה (264MB),
            // לא נשאר קובץ חלקי שנראה תקין בהפעלה הבאה.
            val tmp = File(context.filesDir, "$name.tmp")
            context.assets.open(name).use { input ->
                FileOutputStream(tmp).use { output -> input.copyTo(output) }
            }
            check(tmp.renameTo(outFile)) { "rename $tmp failed" }
        }
        return outFile
    }

    companion object {
        /**
         * Whisper small (רב-לשוני, מכומת 8 ביט, 264MB). tiny, שהיה כאן קודם,
         * חלש מדי בעברית. small-q8_0 ולא q5_1 הקטן יותר: עם dotprod, ‏q8_0 מהיר
         * פי 4 על המכשיר (הפענוח של q5_1 כבד).
         */
        private const val MODEL_FILE = "ggml-small-q8_0.bin"

        // מודל אחד לכל התהליך - העוזר והשירות טוענים אותו שניהם, ושני עותקים
        // של המודל בזיכרון הם כחצי ג'יגה על מכשיר עם 4GB.
        private val lock = Any()
        @Volatile private var sharedWhisper: WhisperCpp? = null
    }
}
