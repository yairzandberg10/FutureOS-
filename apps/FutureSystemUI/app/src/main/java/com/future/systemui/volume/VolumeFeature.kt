package com.future.systemui.volume

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.PixelFormat
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import com.future.systemui.CommandQueue
import com.future.systemui.Feature
import com.future.systemui.fui.statusbar.ui.VolumeOverlay
import com.future.systemui.fui.ui.theme.FutureUITheme
import com.future.systemui.statusbar.ViewOwner

/**
 * Shows FutureUI's VolumeOverlay (same composable, same timing: 1500ms visible, 260ms exit) whenever
 * AudioService announces a volume change with android.media.VOLUME_CHANGED_ACTION.
 */
class VolumeFeature : Feature {

    private val main = Handler(Looper.getMainLooper())
    private var context: Context? = null
    private var windowManager: WindowManager? = null
    private var audio: AudioManager? = null
    private var view: ComposeView? = null
    private val owner = ViewOwner()
    private val level = mutableFloatStateOf(0.5f)
    private val visible = MutableTransitionState(false)
    private val hideRunnable = Runnable { startHide() }
    private val removeRunnable = Runnable { removeNow() }

    override fun start(context: Context, commandQueue: CommandQueue) {
        try {
            this.context = context
            windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            audio = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            owner.start()
            val receiver = object : BroadcastReceiver() {
                override fun onReceive(c: Context?, intent: Intent?) {
                    try {
                        if (intent != null) handleChange(intent)
                    } catch (t: Throwable) {
                        Log.e(TAG, "volume change handling failed", t)
                    }
                }
            }
            val filter = IntentFilter().apply {
                addAction(ACTION_VOLUME_CHANGED)
                addAction(ACTION_STREAM_MUTE_CHANGED)
            }
            context.registerReceiver(receiver, filter)
            Log.i(TAG, "volume feature started")
        } catch (t: Throwable) {
            Log.e(TAG, "VolumeFeature start failed", t)
        }
    }

    private fun handleChange(intent: Intent) {
        val am = audio ?: return
        val stream = intent.getIntExtra(EXTRA_VOLUME_STREAM_TYPE, -1)
        if (stream !in SHOWN_STREAMS) return
        val max = am.getStreamMaxVolume(stream).coerceAtLeast(1)
        val value = if (intent.hasExtra(EXTRA_VOLUME_STREAM_VALUE)) {
            intent.getIntExtra(EXTRA_VOLUME_STREAM_VALUE, 0)
        } else {
            am.getStreamVolume(stream)
        }
        level.floatValue = if (am.isStreamMute(stream)) 0f else (value.toFloat() / max).coerceIn(0f, 1f)
        show()
    }

    private fun show() {
        main.removeCallbacks(hideRunnable)
        main.removeCallbacks(removeRunnable)
        visible.targetState = true
        if (view == null) addWindow()
        main.postDelayed(hideRunnable, VISIBLE_MS)
    }

    private fun addWindow() {
        val c = context ?: return
        val wm = windowManager ?: return
        val v = ComposeView(c).apply {
            owner.attachTo(this)
            setContent {
                FutureUITheme {
                    VolumeOverlay(
                        level = level.floatValue,
                        modifier = Modifier.fillMaxWidth(),
                        visibleState = visible,
                    )
                }
            }
        }
        for (type in intArrayOf(TYPE_VOLUME_OVERLAY, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY)) {
            try {
                val params = WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    type,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                    PixelFormat.TRANSLUCENT,
                ).apply {
                    gravity = Gravity.TOP
                    title = "FutureVolume"
                }
                wm.addView(v, params)
                view = v
                return
            } catch (t: Throwable) {
                Log.w(TAG, "volume addView failed for type $type", t)
            }
        }
    }

    /** Starts the exit animation; the window is removed after it. */
    private fun startHide() {
        if (view == null) return
        visible.targetState = false
        main.removeCallbacks(removeRunnable)
        main.postDelayed(removeRunnable, EXIT_MS)
    }

    private fun removeNow() {
        main.removeCallbacks(removeRunnable)
        try {
            view?.let { windowManager?.removeView(it) }
        } catch (t: Throwable) {
            Log.e(TAG, "volume removeView failed", t)
        }
        view = null
    }

    private companion object {
        const val TAG = "FutureSystemUI"
        const val VISIBLE_MS = 1500L
        const val EXIT_MS = 260L
        const val TYPE_VOLUME_OVERLAY = 2020

        // String literals: some AudioManager constants are @hide.
        const val ACTION_VOLUME_CHANGED = "android.media.VOLUME_CHANGED_ACTION"
        const val ACTION_STREAM_MUTE_CHANGED = "android.media.STREAM_MUTE_CHANGED_ACTION"
        const val EXTRA_VOLUME_STREAM_TYPE = "android.media.EXTRA_VOLUME_STREAM_TYPE"
        const val EXTRA_VOLUME_STREAM_VALUE = "android.media.EXTRA_VOLUME_STREAM_VALUE"

        val SHOWN_STREAMS = setOf(
            AudioManager.STREAM_MUSIC, AudioManager.STREAM_VOICE_CALL,
            AudioManager.STREAM_RING, AudioManager.STREAM_ALARM,
        )
    }
}
