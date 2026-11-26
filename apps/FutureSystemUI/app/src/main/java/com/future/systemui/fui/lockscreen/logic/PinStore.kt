package com.future.systemui.fui.lockscreen.logic

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.os.SystemClock
import com.future.systemui.lockscreen.FutureUiLockClient
import com.future.systemui.lockscreen.LockoutGuard

/**
 * קוד הנעילה (4 עד 8 ספרות) - בגרסת SystemUI.
 *
 * הקוד עצמו נשאר ב-FutureUI (HMAC במפתח Keystore של ה-uid שלו), וכל בדיקה נעשית שם דרך
 * [FutureUiLockClient]: לא מעתיקים את הקוד ולא את ה-HMAC, והקוד הקיים של המשתמש ממשיך לעבוד.
 * כאן נשמרים רק עותק של "יש קוד / אורכו" (לפתיחה לפני ש-FutureUI ענה) ומגביל ניסיונות
 * ([LockoutGuard]) שנשמר בדיסק ועובר הפעלה מחדש.
 *
 * כל הפונקציות שפונות ל-FutureUI חוסמות - לא לקרוא להן מה-main thread.
 */
class PinStore(context: Context, private val prefs: SharedPreferences) {
    private val appContext = context.applicationContext
    private val client = FutureUiLockClient(appContext)
    private val guard = LockoutGuard(appContext)

    /** מי שמקבל את ההגדרות ש-FutureUI מחזיר אחרי קוד נכון (LockSettings). */
    @Volatile var onRemoteSettings: ((Bundle) -> Unit)? = null

    /** ההשהיה ש-FutureUI עצמו הכריז עליה (elapsedRealtime). */
    @Volatile private var remoteLockedUntil = 0L

    sealed class Result {
        object Ok : Result()
        object Wrong : Result()
        object LockedOut : Result()
        /** FutureUI לא ענה - לא נספר כניסיון. */
        object Unavailable : Result()
    }

    /**
     * יש קוד? פותח-סגור: אם עוד לא ידוע ו-FutureUI מותקן, מניחים שיש (נעול) עד שהוא עונה.
     */
    fun hasPin(): Boolean =
        if (prefs.contains(KEY_HAS_PIN)) prefs.getBoolean(KEY_HAS_PIN, true) else client.isInstalled()

    fun pinLength(): Int = prefs.getInt(KEY_PIN_LENGTH, 4)

    /** מרענן את "יש קוד / אורך / השהיה" מ-FutureUI. חוסם. false = FutureUI לא ענה. */
    fun refresh(): Boolean {
        val s = client.status() ?: return false
        prefs.edit().putBoolean(KEY_HAS_PIN, s.hasPin).putInt(KEY_PIN_LENGTH, s.pinLength.coerceIn(MIN_LENGTH, MAX_LENGTH)).apply()
        noteRemoteLockout(s.lockoutMs)
        if (!s.hasPin) {
            // אין קוד: FutureUI מוסר את ההגדרות בלי אסימון
            client.settingsWithoutPin()?.let { b -> runCatching { onRemoteSettings?.invoke(b) } }
        }
        return true
    }

    /** כמה מילישניות נותרו עד שמותר לנסות שוב; 0 כשאין חסימה. */
    fun remainingLockoutMs(): Long {
        val remote = (remoteLockedUntil - SystemClock.elapsedRealtime()).coerceAtLeast(0L)
        return maxOf(guard.remainingMs(), remote)
    }

    fun failedAttempts(): Int = guard.failures()

    /** בודק קוד ב-FutureUI. חוסם. */
    fun verify(pin: String): Result {
        if (remainingLockoutMs() > 0) return Result.LockedOut
        guard.beginAttempt() // נכתב לדיסק לפני הבדיקה
        return when (val r = client.verify(pin) { b -> onRemoteSettings?.invoke(b) }) {
            FutureUiLockClient.Verify.Ok -> {
                guard.success()
                remoteLockedUntil = 0L
                prefs.edit().putBoolean(KEY_HAS_PIN, true).apply()
                Result.Ok
            }
            is FutureUiLockClient.Verify.Wrong -> {
                noteRemoteLockout(r.lockoutMs)
                Result.Wrong
            }
            FutureUiLockClient.Verify.Unavailable -> {
                guard.refund()
                Result.Unavailable
            }
        }
    }

    private fun noteRemoteLockout(ms: Long) {
        remoteLockedUntil = if (ms > 0) SystemClock.elapsedRealtime() + ms.coerceAtMost(MAX_REMOTE_MS) else 0L
    }

    fun isKeyAvailable(): Boolean = client.status() != null

    companion object {
        private const val KEY_HAS_PIN = "remote_has_pin"
        private const val KEY_PIN_LENGTH = "remote_pin_length"
        private const val MAX_REMOTE_MS = 15 * 60_000L
        const val MIN_LENGTH = 4
        const val MAX_LENGTH = 8
    }
}
