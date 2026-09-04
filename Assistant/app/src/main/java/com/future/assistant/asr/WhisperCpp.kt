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
        return nativeTranscribe(ctx, samples, language)
    }

    fun release() {
        if (ctx != 0L) {
            nativeFree(ctx)
            ctx = 0
        }
    }

    private external fun nativeInit(modelPath: String): Long
    private external fun nativeTranscribe(ctx: Long, samples: FloatArray, language: String): String
    private external fun nativeFree(ctx: Long)

    companion object {
        init { System.loadLibrary("whisper_jni") }
    }
}
