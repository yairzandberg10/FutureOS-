#include <jni.h>
#include <string>
#include <thread>
#include <algorithm>

#include "whisper.h"

extern "C" JNIEXPORT jlong JNICALL
Java_com_future_assistant_asr_WhisperCpp_nativeInit(JNIEnv *env, jobject /*thiz*/, jstring modelPath) {
    const char *path = env->GetStringUTFChars(modelPath, nullptr);
    struct whisper_context_params cparams = whisper_context_default_params();
    struct whisper_context *ctx = whisper_init_from_file_with_params(path, cparams);
    env->ReleaseStringUTFChars(modelPath, path);
    return reinterpret_cast<jlong>(ctx);
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_future_assistant_asr_WhisperCpp_nativeTranscribe(JNIEnv *env, jobject /*thiz*/, jlong ctxPtr,
                                                           jfloatArray samples, jstring language) {
    auto *ctx = reinterpret_cast<struct whisper_context *>(ctxPtr);
    if (ctx == nullptr) return env->NewStringUTF("");

    jsize n = env->GetArrayLength(samples);
    jfloat *data = env->GetFloatArrayElements(samples, nullptr);
    const char *lang = env->GetStringUTFChars(language, nullptr);

    struct whisper_full_params wparams = whisper_full_default_params(WHISPER_SAMPLING_GREEDY);
    wparams.language = lang;
    wparams.translate = false;
    wparams.no_timestamps = true;
    wparams.print_progress = false;
    wparams.print_realtime = false;
    wparams.print_special = false;
    wparams.single_segment = false;
    // בלי "temperature fallback": כשהמודל לא בטוח (קורה הרבה בעברית) ברירת
    // המחדל מפענחת מחדש עד 5 פעמים בטמפרטורות עולות, עם 5 מפענחים בכל פעם -
    // זה מה שהפך תמלול של משפט קצר לשניות ארוכות. פענוח greedy יחיד מספיק.
    wparams.temperature_inc = 0.0f;
    wparams.greedy.best_of = 1;
    // כל הליבות: במדידה על ה-MT6768 (2 ליבות A75 + 6 A55) 8 threads היו
    // מהירים מ-4 ומ-6, למרות שהליבות הקטנות איטיות.
    wparams.n_threads = std::max(1, static_cast<int>(std::thread::hardware_concurrency()));
    // Whisper מרפד כל הקלטה ל-30 שניות, ולכן ה-encoder עולה אותו דבר על
    // משפט של 3 שניות ועל חצי דקה. audio_ctx מקצר את החלון לאורך ההקלטה
    // בפועל (1500 פריימים = 30 שניות, כלומר 50 לשנייה) + מרווח: במודל
    // small זה הוריד את ה-encoder מ-8.7 שניות ל-1.1 שניות למשפט קצר.
    wparams.audio_ctx = std::min(1500, static_cast<int>(static_cast<long long>(n) * 50 / 16000) + 64);

    int result = whisper_full(ctx, wparams, data, n);

    env->ReleaseFloatArrayElements(samples, data, JNI_ABORT);
    env->ReleaseStringUTFChars(language, lang);

    if (result != 0) return env->NewStringUTF("");

    std::string out;
    const int n_segments = whisper_full_n_segments(ctx);
    for (int i = 0; i < n_segments; i++) {
        out += whisper_full_get_segment_text(ctx, i);
    }
    return env->NewStringUTF(out.c_str());
}

extern "C" JNIEXPORT void JNICALL
Java_com_future_assistant_asr_WhisperCpp_nativeFree(JNIEnv * /*env*/, jobject /*thiz*/, jlong ctxPtr) {
    auto *ctx = reinterpret_cast<struct whisper_context *>(ctxPtr);
    if (ctx != nullptr) whisper_free(ctx);
}
