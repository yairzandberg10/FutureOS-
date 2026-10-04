package com.future.assistant.asr

/** עטיפת JNI דקה סביב whisper.cpp - זיהוי דיבור מקומי (offline) עם שפה נבחרת בזמן ריצה. */
class WhisperCpp {
    private var ctx: Long = 0

    fun init(modelPath: String): Boolean {
        ctx = nativeInit(modelPath)
        return ctx != 0L
    }

    fun transcribe(samples: FloatArray, language: String): String {
        if (ctx == 0L) return ""
        // UTF-8 קטוע בסוף (תקרת הטוקנים) מתפענח לתו החלפה - מוסרים אותו.
        return String(nativeTranscribe(ctx, samples, language), Charsets.UTF_8).replace("�", "")
    }

    fun release() {
        if (ctx != 0L) {
            nativeFree(ctx)
            ctx = 0
        }
    }

    private external fun nativeInit(modelPath: String): Long
    private external fun nativeTranscribe(ctx: Long, samples: FloatArray, language: String): ByteArray
    private external fun nativeFree(ctx: Long)

    companion object {
        init { System.loadLibrary("whisper_jni") }
    }
}
