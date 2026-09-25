package com.future.assistant.asr

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * המקלדת שולחת את זה כשהיא נפתחת: טוען את מודל Whisper לזיכרון התהליך
 * כבר עכשיו, כך שכשמחזיקים 0 לתמלול המודל כבר מוכן ולא נטען מאפס
 * (264MB, כמה שניות). אם המודל כבר טעון - loadModel חוזר מיד.
 */
class AsrWarmupReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        val app = context.applicationContext
        Thread {
            try {
                LocalSpeechEngine.preload(app)
            } catch (e: Exception) {
                Log.w("AsrWarmup", "warm-up failed", e)
            } finally {
                pending.finish()
            }
        }.start()
    }

    companion object {
        const val ACTION_WARM_UP = "com.future.assistant.action.WARM_UP_ASR"
    }
}
