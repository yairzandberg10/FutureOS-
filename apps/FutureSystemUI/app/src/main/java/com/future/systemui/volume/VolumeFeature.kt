package com.future.systemui.volume

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.util.Log
import com.future.systemui.CommandQueue
import com.future.systemui.Feature

/**
 * מציג חלון ווליום קטן וזמני כשעוצמת צליל משתנה (מקשי הווליום או הגדרה אחרת).
 * נרשם לשידורי המערכת של AudioService; אין כאן מגע במסך.
 */
class VolumeFeature : Feature {

    private var receiver: BroadcastReceiver? = null
    private var overlay: VolumeOverlayWindow? = null

    override fun start(context: Context, commandQueue: CommandQueue) {
        try {
            val audio = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val window = VolumeOverlayWindow(context)
            overlay = window

            val r = object : BroadcastReceiver() {
                override fun onReceive(ctx: Context?, intent: Intent?) {
                    if (intent == null) return
                    try {
                        handleChange(audio, window, intent)
                    } catch (t: Throwable) {
                        Log.e(TAG, "volume change handling failed", t)
                    }
                }
            }
            val filter = IntentFilter().apply {
                addAction(ACTION_VOLUME_CHANGED)
                addAction(ACTION_STREAM_MUTE_CHANGED)
            }
            context.registerReceiver(r, filter)
            receiver = r
            Log.i(TAG, "volume feature started")
        } catch (t: Throwable) {
            Log.e(TAG, "VolumeFeature start failed", t)
        }
    }

    private fun handleChange(audio: AudioManager, window: VolumeOverlayWindow, intent: Intent) {
        val stream = intent.getIntExtra(EXTRA_VOLUME_STREAM_TYPE, -1)
        val label = streamLabel(stream) ?: return

        val max = audio.getStreamMaxVolume(stream).coerceAtLeast(1)
        val muted = audio.isStreamMute(stream)
        val value = if (intent.hasExtra(EXTRA_VOLUME_STREAM_VALUE)) {
            intent.getIntExtra(EXTRA_VOLUME_STREAM_VALUE, 0)
        } else {
            audio.getStreamVolume(stream)
        }
        val level = if (muted) 0f else value.toFloat() / max
        window.show(label, level.coerceIn(0f, 1f))
    }

    /** שם הערוץ בעברית; ערוצים שלא מוצגים (מערכת, DTMF, נגישות...) מחזירים null. */
    private fun streamLabel(stream: Int): String? = when (stream) {
        AudioManager.STREAM_RING -> "צלצול"
        AudioManager.STREAM_MUSIC -> "מדיה"
        AudioManager.STREAM_VOICE_CALL -> "שיחה"
        AudioManager.STREAM_NOTIFICATION -> "התראות"
        AudioManager.STREAM_ALARM -> "שעון מעורר"
        else -> null
    }

    companion object {
        const val TAG = "FutureSystemUI"

        // מחרוזות literal ולא קבועי AudioManager: חלקם מוסתרים (@hide) בערכת הפיתוח.
        private const val ACTION_VOLUME_CHANGED = "android.media.VOLUME_CHANGED_ACTION"
        private const val ACTION_STREAM_MUTE_CHANGED = "android.media.STREAM_MUTE_CHANGED_ACTION"
        private const val EXTRA_VOLUME_STREAM_TYPE = "android.media.EXTRA_VOLUME_STREAM_TYPE"
        private const val EXTRA_VOLUME_STREAM_VALUE = "android.media.EXTRA_VOLUME_STREAM_VALUE"
    }
}
