package com.future.settings.theme

import android.content.Context
import android.net.Uri
import android.provider.Settings
import com.future.sharednav.systemui.SystemUiTarget

/**
 * מצב האבטחה של המכשיר למרכז האבטחה: מסך הנעילה של FutureOS (דרך
 * SystemUiSettingsProvider ב-FutureUI, קריאה בלבד) ומתגי המערכת שמשפיעים עליו.
 */
data class SecurityStatus(
    /** false = אין קישור מאומת ל-FutureUI, אז ערכי מסך הנעילה לא ידועים. */
    val linked: Boolean,
    val lockEnabled: Boolean,
    val hasPin: Boolean,
    val faceEnabled: Boolean,
    /** 0 הצג הכל, 1 תוכן רק אחרי זיהוי, 2 הסתר תוכן. */
    val notificationPrivacy: Int,
    val autoLockMs: Long,
    val failedLastWeek: Int,
    val lastFailedAt: Long,
    val lockoutMs: Long,
    val adbEnabled: Boolean,
    val developerOptions: Boolean,
    val androidLockSet: Boolean,
)

object SecurityStatusClient {
    private val URI = Uri.parse("content://${SystemUiTarget.SETTINGS_AUTHORITY}/security")

    fun get(context: Context): SecurityStatus {
        val resolver = context.contentResolver
        val adb = runCatching { Settings.Global.getInt(resolver, Settings.Global.ADB_ENABLED, 0) == 1 }.getOrDefault(false)
        val dev = runCatching { Settings.Global.getInt(resolver, Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 0) == 1 }.getOrDefault(false)
        val keyguard = runCatching {
            (context.getSystemService(Context.KEYGUARD_SERVICE) as android.app.KeyguardManager).isDeviceSecure
        }.getOrDefault(false)
        val unknown = SecurityStatus(false, false, false, false, 0, 0L, 0, 0L, 0L, adb, dev, keyguard)

        if (!com.future.sharednav.systemui.TrustedProviders.isTrusted(context, SystemUiTarget.SETTINGS_AUTHORITY)) return unknown
        return try {
            resolver.query(URI, null, null, null, null)?.use { c ->
                // FutureUI ישנה בלי הנתיב מחזירה את הגדרות שורת המצב - אין לה has_pin
                if (!c.moveToFirst() || c.getColumnIndex("has_pin") < 0) return@use unknown
                fun int(name: String) = c.getInt(c.getColumnIndexOrThrow(name))
                fun long(name: String) = c.getLong(c.getColumnIndexOrThrow(name))
                SecurityStatus(
                    linked = true,
                    lockEnabled = int("lock_enabled") == 1,
                    hasPin = int("has_pin") == 1,
                    faceEnabled = int("face_enabled") == 1,
                    notificationPrivacy = int("notif_privacy"),
                    autoLockMs = long("auto_lock_ms"),
                    failedLastWeek = int("failed_week"),
                    lastFailedAt = long("last_failed_at"),
                    lockoutMs = long("lockout_ms"),
                    adbEnabled = adb,
                    developerOptions = dev,
                    androidLockSet = keyguard,
                )
            } ?: unknown
        } catch (e: Exception) {
            android.util.Log.w("SecurityStatusClient", "security status failed", e)
            unknown
        }
    }
}
