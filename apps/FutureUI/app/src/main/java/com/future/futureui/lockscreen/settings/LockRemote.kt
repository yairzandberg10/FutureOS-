package com.future.futureui.lockscreen.settings

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import com.future.futureui.lockscreen.face.FaceTemplateStore
import com.future.futureui.lockscreen.logic.LockCatalog
import com.future.futureui.lockscreen.logic.LockSettings
import com.future.futureui.lockscreen.logic.PinPolicy
import com.future.futureui.lockscreen.logic.PinStore
import com.future.futureui.lockscreen.logic.SecurityLog
import com.future.futureui.statusbar.service.StatusBarAccessibilityService
import com.future.sharednav.i18n.tr
import java.security.SecureRandom

/**
 * הגדרות מסך הנעילה מאפליקציית ההגדרות. המסך עצמו נמצא שם, וכאן (בתהליך של
 * FutureUI, שמחזיק את הקוד ואת נתוני הפנים) נעשית כל בדיקה וכל שינוי.
 * נקרא רק דרך SystemUiSettingsProvider.call, שבודק את הרשאת החתימה.
 *
 * כשמוגדר קוד, קודם [verify] עם הקוד הנוכחי. הוא מחזיר אסימון (token) לזמן
 * קצר, ובלעדיו אי אפשר לראות או לשנות כלום - אחרת כל מי שמחזיק את הטלפון
 * פתוח יכול לכבות את הנעילה. האסימון נשמר רק בזיכרון: הפעלה מחדש של FutureUI
 * מבטלת אותו.
 */
object LockRemote {
    const val METHOD_STATE = "lock_state"
    const val METHOD_VERIFY = "lock_verify"
    const val METHOD_ACTION = "lock_action"
    const val METHOD_SET_PIN = "lock_set_pin"
    const val METHOD_END = "lock_end"
    const val METHOD_CHECK_PIN = "lock_check_pin"

    const val EXTRA_TOKEN = "token"

    private const val SESSION_MS = 10L * 60 * 1000

    private var token: String? = null
    private var expiresAt = 0L
    private val random = SecureRandom()

    @Synchronized
    private fun newToken(): String {
        val bytes = ByteArray(16).also { random.nextBytes(it) }
        return bytes.joinToString("") { "%02x".format(it) }.also {
            token = it
            expiresAt = SystemClock.elapsedRealtime() + SESSION_MS
        }
    }

    @Synchronized
    fun isValid(candidate: String?): Boolean {
        val current = token ?: return false
        if (candidate == null || SystemClock.elapsedRealtime() > expiresAt) {
            if (SystemClock.elapsedRealtime() > expiresAt) token = null
            return false
        }
        return java.security.MessageDigest.isEqual(current.toByteArray(), candidate.toByteArray())
    }

    @Synchronized
    fun end() {
        token = null
        expiresAt = 0L
    }

    fun handle(context: Context, method: String, arg: String?, extras: Bundle?): Bundle? {
        val settings = LockSettings(context)
        val given = extras?.getString(EXTRA_TOKEN)
        return when (method) {
            METHOD_STATE -> state(context, settings, given)
            METHOD_VERIFY -> verify(context, settings, arg.orEmpty())
            METHOD_SET_PIN -> setPin(settings, given, arg.orEmpty())
            METHOD_ACTION -> action(context, settings, given, arg.orEmpty(), extras)
            METHOD_END -> { end(); Bundle() }
            // רק מדיניות הקוד (בלי לשמור ובלי לחשוף כלום), כדי לדחות קוד חלש לפני האישור
            METHOD_CHECK_PIN -> Bundle().apply { PinPolicy.weakReason(arg.orEmpty())?.let { putString("error", it) } }
            else -> null
        }
    }

    private fun authorized(settings: LockSettings, given: String?): Boolean =
        !settings.pin.hasPin() || isValid(given)

    private fun state(context: Context, settings: LockSettings, given: String?): Bundle {
        val hasPin = settings.pin.hasPin()
        val out = Bundle().apply {
            putBoolean("has_pin", hasPin)
            putInt("pin_length", settings.pin.pinLength())
            putLong("lockout_ms", settings.pin.remainingLockoutMs())
        }
        val ok = authorized(settings, given)
        out.putBoolean("authorized", ok)
        if (!ok) return out
        val enrolled = runCatching { FaceTemplateStore.shared(context).isEnrolled() }.getOrDefault(false)
        out.putBoolean("enabled", settings.enabled)
        out.putString("auto_lock", LockSettings.autoLockLabel(settings.autoLockDelayMs))
        out.putBoolean("enrolled", enrolled)
        out.putBoolean("face_on", settings.faceEnabled && enrolled)
        out.putBoolean("face_stay", settings.faceStayOnLock)
        out.putString("sensitivity", LockSettings.SENSITIVITY_LABELS.getOrElse(settings.faceSensitivity) { "" })
        out.putString("privacy", LockSettings.PRIVACY_LABELS.getOrElse(settings.notificationPrivacy) { "" })
        out.putString("clock_style", LockCatalog.clockStyles.getOrElse(settings.clockStyle) { "" })
        out.putString("clock_color", LockCatalog.clockColors.getOrElse(settings.clockColor) { "" })
        out.putString("background", LockCatalog.backgrounds.getOrElse(settings.background) { "" })
        out.putStringArrayList("widgets", ArrayList(settings.widgets.map { LockCatalog.widgetLabel(it) }))
        out.putString("left", LockCatalog.shortcutLabel(settings.leftShortcut))
        out.putString("right", LockCatalog.shortcutLabel(settings.rightShortcut))
        out.putString("message", settings.ownerMessage)
        return out
    }

    private fun verify(context: Context, settings: LockSettings, pin: String): Bundle {
        val out = Bundle()
        if (!settings.pin.hasPin()) {
            out.putString(EXTRA_TOKEN, newToken())
            return out
        }
        val lock = settings.pin.remainingLockoutMs()
        if (lock > 0) {
            out.putString("error", tr("נסה שוב בעוד {0} שניות", (lock + 999) / 1000))
            return out
        }
        val before = settings.pin.failedAttempts()
        if (settings.pin.verifyPin(pin)) {
            settings.onStrongAuth()
            out.putString(EXTRA_TOKEN, newToken())
        } else {
            // גם ניחוש בכניסה להגדרות הנעילה נרשם ביומן
            if (settings.pin.failedAttempts() > before) runCatching { SecurityLog(context).onFailedPin() }
            out.putString("error", tr("קוד שגוי"))
        }
        return out
    }

    private fun setPin(settings: LockSettings, given: String?, pin: String): Bundle {
        val out = Bundle()
        if (!authorized(settings, given)) { out.putString("error", tr("קוד שגוי")); return out }
        if (pin.length !in PinStore.MIN_LENGTH..PinStore.MAX_LENGTH || !pin.all { it.isDigit() }) {
            out.putString("error", tr("לפחות {0} ספרות", PinStore.MIN_LENGTH))
            return out
        }
        // מדיניות קוד: 1234, 0000, 2580, שנים וכו' נדחים גם כאן, לא רק במסך
        PinPolicy.weakReason(pin)?.let { out.putString("error", it); return out }
        if (runCatching { settings.pin.setPin(pin) }.getOrDefault(false)) {
            settings.onStrongAuth()
            out.putString(EXTRA_TOKEN, newToken())
            out.putString("toast", tr("הקוד נשמר"))
        } else {
            out.putString("error", tr("הקודים לא תואמים"))
        }
        return out
    }

    private fun action(context: Context, settings: LockSettings, given: String?, id: String, extras: Bundle?): Bundle {
        val out = Bundle()
        if (!authorized(settings, given)) { out.putBoolean("ok", false); return out }
        val on = extras?.getBoolean("value") ?: false
        val hasPin = settings.pin.hasPin()
        val faceStore = FaceTemplateStore.shared(context)
        when (id) {
            "enabled" -> settings.enabled = on
            "clear_pin" -> if (hasPin) {
                settings.pin.clearPin()
                settings.faceEnabled = false
                end()
                out.putString("toast", tr("הקוד הוסר"))
            }
            "auto_lock" -> settings.autoLockDelayMs = LockCatalog.cycle(LockSettings.AUTO_LOCK_OPTIONS, settings.autoLockDelayMs, 1)
            "face_on" -> settings.faceEnabled = on && hasPin && faceStore.isEnrolled()
            "face_stay" -> settings.faceStayOnLock = on
            "sensitivity" -> settings.faceSensitivity = LockCatalog.cycleIndex(3, settings.faceSensitivity, 1)
            "delete_face" -> {
                faceStore.clear()
                settings.faceEnabled = false
                out.putString("toast", tr("נתוני הפנים נמחקו"))
            }
            "privacy" -> settings.notificationPrivacy = LockCatalog.cycleIndex(3, settings.notificationPrivacy, 1)
            "clock_style" -> settings.clockStyle = LockCatalog.cycleIndex(LockCatalog.clockStyles.size, settings.clockStyle, 1)
            "clock_color" -> settings.clockColor = LockCatalog.cycleIndex(LockCatalog.clockColors.size, settings.clockColor, 1)
            "background" -> settings.background = LockCatalog.cycleIndex(LockCatalog.backgrounds.size, settings.background, 1)
            "widget" -> {
                val slot = extras?.getInt("slot", -1) ?: -1
                if (slot in 0 until LockSettings.WIDGET_SLOTS) {
                    settings.widgets = settings.widgets.toMutableList().also { it[slot] = LockCatalog.cycle(LockCatalog.widgets, it[slot], 1) }
                }
            }
            "left" -> settings.leftShortcut = LockCatalog.cycle(LockCatalog.shortcuts, settings.leftShortcut, 1)
            "right" -> settings.rightShortcut = LockCatalog.cycle(LockCatalog.shortcuts, settings.rightShortcut, 1)
            "message" -> settings.ownerMessage = extras?.getString("text").orEmpty().take(60)
            "lock_now" -> {
                end()
                context.sendBroadcast(Intent(StatusBarAccessibilityService.ACTION_LOCK_NOW).setPackage(context.packageName))
            }
            else -> { out.putBoolean("ok", false); return out }
        }
        out.putBoolean("ok", true)
        return out
    }
}
