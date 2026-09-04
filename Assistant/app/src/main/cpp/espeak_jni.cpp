#include <jni.h>
#include <android/log.h>
#include <vector>
#include <cstring>

#include "espeak-ng/speak_lib.h"

#define LOG_TAG "EspeakJni"
// ANDROID_LOG_ERROR (לא DEBUG) - למכשיר הזה סף לוג ברירת מחדל של INFO, אז
// לוגים ברמת DEBUG נשמטים לפני שהם מגיעים ל-logcat בכלל.
#define LOGD(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

namespace {
std::vector<int16_t> g_samples;
int g_sampleRate = 22050;

int synthCallback(short *wav, int numsamples, espeak_EVENT *events) {
    // espeak-ng יכול לדווח על קצב הדגימה בפועל דרך espeakEVENT_SAMPLERATE -
    // הוא לא בהכרח זהה לערך שהתקבל מ-espeak_Initialize, ואם מתעלמים ממנו
    // וממירים ל-AudioTrack בקצב השגוי, השמע נשמע מהיר/צווח ("chipmunk").
    if (events != nullptr) {
        for (espeak_EVENT *e = events; e->type != espeakEVENT_LIST_TERMINATED; e++) {
            if (e->type == espeakEVENT_SAMPLERATE) {
                g_sampleRate = e->id.number;
                LOGD("espeakEVENT_SAMPLERATE -> %d", g_sampleRate);
            }
        }
    }
    if (wav != nullptr && numsamples > 0) {
        g_samples.insert(g_samples.end(), wav, wav + numsamples);
    }
    return 0; // 0 = continue synthesis
}
}

extern "C" JNIEXPORT jint JNICALL
Java_com_future_assistant_asr_EspeakTts_nativeInit(JNIEnv *env, jobject /*thiz*/, jstring dataPath) {
    const char *path = env->GetStringUTFChars(dataPath, nullptr);
    LOGD("espeak_Initialize path=%s", path);
    int sampleRate = espeak_Initialize(AUDIO_OUTPUT_RETRIEVAL, 0, path, 0);
    env->ReleaseStringUTFChars(dataPath, path);
    LOGD("espeak_Initialize -> sampleRate=%d", sampleRate);
    if (sampleRate <= 0) return -1;
    g_sampleRate = sampleRate;

    espeak_SetSynthCallback(synthCallback);
    espeak_ERROR voiceErr = espeak_SetVoiceByName("he");
    LOGD("espeak_SetVoiceByName(he) -> %d", (int) voiceErr);
    if (voiceErr != EE_OK) return -1;

    return sampleRate;
}

extern "C" JNIEXPORT jshortArray JNICALL
Java_com_future_assistant_asr_EspeakTts_nativeSynthesize(JNIEnv *env, jobject /*thiz*/, jstring text) {
    const char *utf8 = env->GetStringUTFChars(text, nullptr);

    g_samples.clear();
    espeak_ERROR err = espeak_Synth(utf8, strlen(utf8) + 1, 0, POS_CHARACTER, 0,
                 espeakCHARS_UTF8, nullptr, nullptr);
    espeak_Synchronize();
    LOGD("espeak_Synth(\"%s\") -> err=%d, samples=%zu, sampleRate=%d", utf8, (int) err, g_samples.size(), g_sampleRate);

    env->ReleaseStringUTFChars(text, utf8);

    jshortArray result = env->NewShortArray(static_cast<jsize>(g_samples.size()));
    if (!g_samples.empty()) {
        env->SetShortArrayRegion(result, 0, static_cast<jsize>(g_samples.size()), g_samples.data());
    }
    return result;
}

extern "C" JNIEXPORT jint JNICALL
Java_com_future_assistant_asr_EspeakTts_nativeGetSampleRate(JNIEnv * /*env*/, jobject /*thiz*/) {
    return g_sampleRate;
}

extern "C" JNIEXPORT void JNICALL
Java_com_future_assistant_asr_EspeakTts_nativeTerminate(JNIEnv * /*env*/, jobject /*thiz*/) {
    espeak_Terminate();
}
