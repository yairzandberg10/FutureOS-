package com.future.systemui.statusbar

import android.app.ActivityManager
import android.content.Context
import android.util.Log
import com.future.systemui.fui.utils.FutureUIState
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Replaces the accessibility TYPE_WINDOW_STATE_CHANGED events FutureUI used to learn which app is in front.
 * SystemUI reads the top task of ActivityManager (we hold REAL_GET_TASKS) whenever the status bar callbacks
 * (app transitions, system-bar attributes) or the screen state suggest it may have changed.
 *
 * Besides filling [FutureUIState.foregroundPackage] it lets other features (recents, lock screen) subscribe:
 * `ForegroundTracker.addListener { pkg, cls -> ... }` - always called on the main thread.
 */
object ForegroundTracker {
    private const val TAG = "FutureSystemUI"

    fun interface Listener {
        fun onForegroundChanged(pkg: String?, cls: String?)
    }

    private val listeners = CopyOnWriteArrayList<Listener>()

    @Volatile
    var foregroundClass: String? = null
        private set

    fun addListener(l: Listener) { listeners.addIfAbsent(l) }
    fun removeListener(l: Listener) { listeners.remove(l) }

    /** Reads the top task and publishes it if it changed. Returns true when the foreground app changed. */
    internal fun refresh(context: Context): Boolean {
        return try {
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager ?: return false
            @Suppress("DEPRECATION")
            val top = am.getRunningTasks(1)?.firstOrNull()?.topActivity ?: return false
            publish(top.packageName, top.className)
        } catch (t: Throwable) {
            Log.w(TAG, "foreground refresh failed", t)
            false
        }
    }

    private fun publish(pkg: String?, cls: String?): Boolean {
        if (pkg == FutureUIState.foregroundPackage && cls == foregroundClass) return false
        FutureUIState.foregroundPackage = pkg
        foregroundClass = cls
        for (l in listeners) {
            try {
                l.onForegroundChanged(pkg, cls)
            } catch (t: Throwable) {
                Log.e(TAG, "foreground listener failed", t)
            }
        }
        return true
    }
}
