package com.future.systemui.lockscreen

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.util.Log

/**
 * Talks to FutureUI's lock provider (content://com.future.futureui.systemui, "lock_*" calls).
 *
 * The PIN lives in FutureUI: only an HMAC under a Keystore key owned by FutureUI's uid is stored there, so it can
 * not be copied into this app, and it is not copied: every check is done by FutureUI. The PIN never reaches a
 * log. The provider requires com.future.futureui.permission.SYSTEM_SETTINGS (granted to us because the
 * permission is signature|privileged and we are a priv-app).
 *
 * All calls block (they may start FutureUI's process): never call them from the main thread.
 */
class FutureUiLockClient(context: Context) {
    private val resolver = context.applicationContext.contentResolver
    private val pm = context.applicationContext.packageManager

    sealed class Verify {
        object Ok : Verify()

        /** wrong PIN; [lockoutMs] is FutureUI's own lockout after this attempt (0 = none). */
        data class Wrong(val lockoutMs: Long) : Verify()

        /** FutureUI is not answering (not installed, starting, crashed). Not counted as a failed attempt. */
        object Unavailable : Verify()
    }

    data class Status(val hasPin: Boolean, val pinLength: Int, val lockoutMs: Long)

    fun isInstalled(): Boolean = runCatching { pm.getPackageInfo(FUTURE_UI, 0); true }.getOrDefault(false)

    private fun call(method: String, arg: String?, extras: Bundle?): Bundle? = try {
        resolver.call(URI, method, arg, extras)
    } catch (t: Throwable) {
        // never log arg: it can be the PIN
        Log.w(TAG, "$method failed: ${t.javaClass.simpleName}")
        null
    }

    /** Public status, readable without the PIN. null = FutureUI unreachable. */
    fun status(): Status? {
        val b = call(METHOD_STATE, null, null) ?: return null
        if (!b.containsKey("has_pin")) return null
        return Status(b.getBoolean("has_pin"), b.getInt("pin_length", 4), b.getLong("lockout_ms", 0L))
    }

    /** Checks [pin] inside FutureUI (constant-time HMAC compare, FutureUI's own lockout). On success pulls the settings once. */
    fun verify(pin: String, onSettings: (Bundle) -> Unit): Verify {
        val b = call(METHOD_VERIFY, pin, null) ?: return Verify.Unavailable
        val token = b.getString(EXTRA_TOKEN)
        if (token != null) {
            val extras = Bundle().apply { putString(EXTRA_TOKEN, token) }
            call(METHOD_STATE, null, extras)?.takeIf { it.getBoolean("authorized") }?.let { runCatching { onSettings(it) } }
            // the token is only needed for the sync above; do not leave a 10 minute settings session behind
            call(METHOD_END, null, null)
            return Verify.Ok
        }
        if (b.getString("error") == null) return Verify.Unavailable
        return Verify.Wrong(status()?.lockoutMs ?: 0L)
    }

    /** Settings when no PIN is set (then FutureUI hands them out without a token). null = unreachable / PIN set. */
    fun settingsWithoutPin(): Bundle? =
        call(METHOD_STATE, null, null)?.takeIf { it.getBoolean("authorized") && !it.getBoolean("has_pin") }

    /** True if [token] (from the Settings app) is a live FutureUI lock session. */
    fun tokenValid(token: String?): Boolean {
        if (token == null) return false
        val b = call(METHOD_STATE, null, Bundle().apply { putString(EXTRA_TOKEN, token) }) ?: return false
        return b.getBoolean("has_pin") && b.getBoolean("authorized")
    }

    companion object {
        private const val TAG = "LockClient"
        const val FUTURE_UI = "com.future.futureui"
        private val URI: Uri = Uri.parse("content://com.future.futureui.systemui")
        const val METHOD_STATE = "lock_state"
        const val METHOD_VERIFY = "lock_verify"
        const val METHOD_END = "lock_end"
        const val EXTRA_TOKEN = "token"
    }
}
