package com.future.systemui.lockscreen

import android.content.Context
import android.os.SystemClock
import android.provider.Settings

/**
 * SystemUI-side brute-force limiter, independent of (and in addition to) FutureUI's own counter.
 *
 * - The attempt is written to disk (commit) BEFORE the PIN is checked and removed only after a success, so
 *   killing the process / cutting power right after a guess can not give free guesses.
 * - Delays escalate: 5 free tries, then 30s, 1m, 2m, 4m, 8m, 15m (cap) for every further failure.
 * - Survives reboots: wall clock deadline after a reboot, elapsedRealtime within the same boot (so moving the clock
 *   forward does not shorten it); the wall-clock deadline is capped, so moving it back can not extend it forever.
 */
class LockoutGuard(context: Context) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences("lock_guard", Context.MODE_PRIVATE)

    @Synchronized
    fun remainingMs(): Long {
        val now = SystemClock.elapsedRealtime()
        val sameBoot = prefs.getInt(K_BOOT, -1) == bootCount() && prefs.getLong(K_ELAPSED_BASE, 0L) <= now
        val left = if (sameBoot) {
            prefs.getLong(K_UNTIL_ELAPSED, 0L) - now
        } else {
            prefs.getLong(K_UNTIL_WALL, 0L) - System.currentTimeMillis()
        }
        return left.coerceIn(0L, MAX_DELAY_MS)
    }

    fun failures(): Int = prefs.getInt(K_FAILS, 0)

    /** Call before checking a PIN. Persists the attempt and the resulting delay. */
    @Synchronized
    fun beginAttempt() {
        val fails = prefs.getInt(K_FAILS, 0) + 1
        val e = prefs.edit().putInt(K_FAILS, fails)
        if (fails >= FREE_ATTEMPTS) {
            val step = (fails - FREE_ATTEMPTS).coerceAtMost(MAX_STEPS)
            val delay = (BASE_DELAY_MS shl step).coerceAtMost(MAX_DELAY_MS)
            val now = SystemClock.elapsedRealtime()
            e.putLong(K_UNTIL_ELAPSED, now + delay)
            e.putLong(K_ELAPSED_BASE, now)
            e.putLong(K_UNTIL_WALL, System.currentTimeMillis() + delay)
            e.putInt(K_BOOT, bootCount())
        }
        e.commit()
    }

    /** The PIN was right. */
    @Synchronized
    fun success() {
        prefs.edit().clear().commit()
    }

    /** The check could not run (FutureUI unreachable): give the attempt back. */
    @Synchronized
    fun refund() {
        val fails = (prefs.getInt(K_FAILS, 0) - 1).coerceAtLeast(0)
        val e = prefs.edit().putInt(K_FAILS, fails)
        if (fails < FREE_ATTEMPTS) e.remove(K_UNTIL_ELAPSED).remove(K_UNTIL_WALL)
        e.commit()
    }

    private fun bootCount(): Int =
        runCatching { Settings.Global.getInt(appContext.contentResolver, Settings.Global.BOOT_COUNT) }.getOrDefault(0)

    companion object {
        private const val K_FAILS = "fails"
        private const val K_UNTIL_ELAPSED = "until_elapsed"
        private const val K_ELAPSED_BASE = "elapsed_base"
        private const val K_UNTIL_WALL = "until_wall"
        private const val K_BOOT = "boot"
        private const val FREE_ATTEMPTS = 5
        private const val MAX_STEPS = 5
        private const val BASE_DELAY_MS = 30_000L
        private const val MAX_DELAY_MS = 15 * 60_000L
    }
}
