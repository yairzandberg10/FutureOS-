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
    wparams.n_threads = std::max(1, static_cast<int>(std::thread::hardware_concurrency()));

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
