#include <jni.h>
#include <vector>
#include <cstring>

#include "espeak-ng/speak_lib.h"

namespace {
std::vector<int16_t> g_samples;

int synthCallback(short *wav, int numsamples, espeak_EVENT * /*events*/) {
    if (wav != nullptr && numsamples > 0) {
        g_samples.insert(g_samples.end(), wav, wav + numsamples);
    }
    return 0; // 0 = continue synthesis
}
}

extern "C" JNIEXPORT jint JNICALL
Java_com_future_assistant_asr_EspeakTts_nativeInit(JNIEnv *env, jobject /*thiz*/, jstring dataPath) {
    const char *path = env->GetStringUTFChars(dataPath, nullptr);
    int sampleRate = espeak_Initialize(AUDIO_OUTPUT_RETRIEVAL, 0, path, 0);
    env->ReleaseStringUTFChars(dataPath, path);
    if (sampleRate <= 0) return -1;

    espeak_SetSynthCallback(synthCallback);
    if (espeak_SetVoiceByName("he") != EE_OK) return -1;

    return sampleRate;
}

extern "C" JNIEXPORT jshortArray JNICALL
Java_com_future_assistant_asr_EspeakTts_nativeSynthesize(JNIEnv *env, jobject /*thiz*/, jstring text) {
    const char *utf8 = env->GetStringUTFChars(text, nullptr);

    g_samples.clear();
    espeak_Synth(utf8, strlen(utf8) + 1, 0, POS_CHARACTER, 0,
                 espeakCHARS_UTF8, nullptr, nullptr);
    espeak_Synchronize();

    env->ReleaseStringUTFChars(text, utf8);

    jshortArray result = env->NewShortArray(static_cast<jsize>(g_samples.size()));
    if (!g_samples.empty()) {
        env->SetShortArrayRegion(result, 0, static_cast<jsize>(g_samples.size()), g_samples.data());
    }
    return result;
}

extern "C" JNIEXPORT void JNICALL
Java_com_future_assistant_asr_EspeakTts_nativeTerminate(JNIEnv * /*env*/, jobject /*thiz*/) {
    espeak_Terminate();
}
