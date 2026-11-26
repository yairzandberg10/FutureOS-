package com.future.settings.theme

import android.content.Context
import android.net.Uri
import android.os.Bundle
import com.future.sharednav.systemui.SystemUiTarget

/** מצב מסך הנעילה כפי ש-FutureUI מחזיר אותו (LockRemote). התוויות כבר מתורגמות. */
data class LockState(
    val linked: Boolean,
    val hasPin: Boolean,
    val pinLength: Int,
    val lockoutMs: Long,
    /** false = צריך קודם להזין את הקוד הנוכחי; שאר השדות ריקים. */
    val authorized: Boolean,
    val enabled: Boolean = false,
    val autoLock: String = "",
    val enrolled: Boolean = false,
    val faceOn: Boolean = false,
    val faceStay: Boolean = false,
    val sensitivity: String = "",
    val privacy: String = "",
    val clockStyle: String = "",
    val clockColor: String = "",
    val background: String = "",
    val widgets: List<String> = emptyList(),
    val left: String = "",
    val right: String = "",
    val message: String = "",
)

/** תשובה לפעולה: אסימון חדש (אחרי קוד נכון / קוד חדש), שגיאה להצגה, או הודעה קצרה. */
data class LockResult(val ok: Boolean, val token: String? = null, val error: String? = null, val toast: String? = null)

/**
 * הגדרות מסך הנעילה. הן נשמרות ב-FutureUI (שם הקוד ונתוני הפנים מוצפנים במפתח
 * Keystore שלו), אבל המסך כולו כאן - דרך ContentProvider.call של
 * SystemUiSettingsProvider, שבודק את הרשאת החתימה. כשמוגדר קוד, כל קריאה צריכה
 * את האסימון שמתקבל מ-[verify].
 */
object LockSettingsClient {
    private val URI = Uri.parse("content://${SystemUiTarget.SETTINGS_AUTHORITY}")
    const val ACTION_ENROLL = "com.future.futureui.ACTION_LOCK_SETTINGS"
    const val FUTURE_UI_PACKAGE = SystemUiTarget.PACKAGE

    private fun trusted(context: Context) =
        com.future.sharednav.systemui.TrustedProviders.isTrusted(context, SystemUiTarget.SETTINGS_AUTHORITY)

    private fun call(context: Context, method: String, arg: String? = null, extras: Bundle? = null): Bundle? {
        if (!trusted(context)) return null
        return runCatching { context.contentResolver.call(URI, method, arg, extras) }.getOrNull()
    }

    private fun tokenExtras(token: String?) = Bundle().apply { if (token != null) putString("token", token) }

    fun state(context: Context, token: String?): LockState {
        val b = call(context, "lock_state", null, tokenExtras(token))
            ?: return LockState(linked = false, hasPin = false, pinLength = 4, lockoutMs = 0L, authorized = false)
        val ok = b.getBoolean("authorized")
        val base = LockState(
            linked = true,
            hasPin = b.getBoolean("has_pin"),
            pinLength = b.getInt("pin_length", 4),
            lockoutMs = b.getLong("lockout_ms"),
            authorized = ok,
        )
        if (!ok) return base
        return base.copy(
            enabled = b.getBoolean("enabled"),
            autoLock = b.getString("auto_lock").orEmpty(),
            enrolled = b.getBoolean("enrolled"),
            faceOn = b.getBoolean("face_on"),
            faceStay = b.getBoolean("face_stay"),
            sensitivity = b.getString("sensitivity").orEmpty(),
            privacy = b.getString("privacy").orEmpty(),
            clockStyle = b.getString("clock_style").orEmpty(),
            clockColor = b.getString("clock_color").orEmpty(),
            background = b.getString("background").orEmpty(),
            widgets = b.getStringArrayList("widgets").orEmpty(),
            left = b.getString("left").orEmpty(),
            right = b.getString("right").orEmpty(),
            message = b.getString("message").orEmpty(),
        )
    }

    private fun result(b: Bundle?): LockResult {
        if (b == null) return LockResult(false, error = com.future.sharednav.i18n.tr("לא זמין"))
        val error = b.getString("error")
        return LockResult(
            ok = error == null && b.getBoolean("ok", true),
            token = b.getString("token"),
            error = error,
            toast = b.getString("toast"),
        )
    }

    fun verify(context: Context, pin: String): LockResult = result(call(context, "lock_verify", pin))

    /** מדיניות הקוד של FutureUI (1234, 0000, שנים...): הודעת שגיאה, או null כשהקוד בסדר. */
    fun checkPin(context: Context, pin: String): String? =
        call(context, "lock_check_pin", pin)?.getString("error")

    fun setPin(context: Context, token: String?, pin: String): LockResult =
        result(call(context, "lock_set_pin", pin, tokenExtras(token)))

    /** [id] כמו ב-LockRemote.action: enabled, clear_pin, auto_lock, face_on, widget וכו'. */
    fun action(context: Context, token: String?, id: String, extras: Bundle.() -> Unit = {}): LockResult =
        result(call(context, "lock_action", id, tokenExtras(token).apply(extras)))

    fun end(context: Context) { call(context, "lock_end") }
}
