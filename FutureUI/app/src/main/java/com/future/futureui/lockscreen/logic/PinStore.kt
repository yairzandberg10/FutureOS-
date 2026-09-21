package com.future.futureui.lockscreen.logic

import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import java.security.KeyStore
import java.security.MessageDigest
import javax.crypto.KeyGenerator
import javax.crypto.Mac

/**
 * אחסון קוד הנעילה.
 *
 * המימוש הקודם שמר `SHA-256(salt + pin)` בקובץ העדפות רגיל. קוד של ארבע
 * ספרות הוא 10,000 אפשרויות בלבד, וסיבוב hash יחיד נשבר offline בשבריר
 * שנייה ברגע שמישהו מקבל את `lock_screen_prefs` - לא הייתה שום עלות
 * לניחוש, לא הייתה הגבלת ניסיונות, וההשוואה נעשתה ב-`==` על מחרוזת.
 *
 * כאן ה-HMAC נחתם במפתח שנוצר ב-Android Keystore ואינו ניתן לייצוא: גם
 * מי שמעתיק את קובץ ההעדפות לא יכול לנחש offline, כי הוא לא יכול לחשב את
 * ה-HMAC בלי החומרה של המכשיר. זו הגנה חזקה יותר מ-PBKDF2 עבור סוד של
 * 4 ספרות, ובלי תלות חיצונית נוספת.
 *
 * בנוסף: השוואה בזמן קבוע (`MessageDigest.isEqual`), ספירת ניסיונות
 * כושלים, והשהיה מצטברת אחרי חמישה כישלונות.
 */
internal class PinStore(private val prefs: SharedPreferences) {

    fun hasPin(): Boolean =
        prefs.getString(KEY_PIN_MAC, null) != null || prefs.getString(KEY_LEGACY_HASH, null) != null

    fun setPin(pin: String) {
        val mac = computeMac(pin)
        if (mac == null) {
            // Keystore לא זמין (מקרה קצה בחומרה) - נופלים למנגנון הישן כדי
            // שלא להישאר בלי נעילה בכלל, אבל לא שוכחים שזה מה שקרה.
            Log.w(TAG, "Keystore unavailable, storing legacy hash")
            prefs.edit()
                .putString(KEY_LEGACY_HASH, legacyHash(pin, getOrCreateLegacySalt()))
                .remove(KEY_PIN_MAC)
                .remove(KEY_FAILED_ATTEMPTS)
                .remove(KEY_LOCKED_UNTIL)
                .apply()
            return
        }
        prefs.edit()
            .putString(KEY_PIN_MAC, mac)
            .remove(KEY_LEGACY_HASH)
            .remove(KEY_LEGACY_SALT)
            .remove(KEY_FAILED_ATTEMPTS)
            .remove(KEY_LOCKED_UNTIL)
            .apply()
    }

    fun clearPin() {
        prefs.edit()
            .remove(KEY_PIN_MAC)
            .remove(KEY_LEGACY_HASH)
            .remove(KEY_LEGACY_SALT)
            .remove(KEY_FAILED_ATTEMPTS)
            .remove(KEY_LOCKED_UNTIL)
            .apply()
        deleteKeystoreEntry()
    }

    /** כמה מילישניות נותרו עד שאפשר לנסות שוב; 0 כשאין השהיה פעילה. */
    fun remainingLockoutMs(): Long {
        val until = prefs.getLong(KEY_LOCKED_UNTIL, 0L)
        return (until - System.currentTimeMillis()).coerceAtLeast(0L)
    }

    fun verifyPin(pin: String): Boolean {
        if (remainingLockoutMs() > 0) return false

        val storedMac = prefs.getString(KEY_PIN_MAC, null)
        val matched = when {
            storedMac != null -> {
                val candidate = computeMac(pin)
                candidate != null && constantTimeEquals(storedMac, candidate)
            }
            else -> verifyLegacy(pin)
        }

        if (matched) {
            prefs.edit().remove(KEY_FAILED_ATTEMPTS).remove(KEY_LOCKED_UNTIL).apply()
            // שדרוג שקט: קוד שנשמר בשיטה הישנה עובר ל-HMAC בפעם הראשונה
            // שהמשתמש מזין אותו נכון.
            if (storedMac == null) setPin(pin)
        } else {
            recordFailure()
        }
        return matched
    }

    private fun verifyLegacy(pin: String): Boolean {
        val stored = prefs.getString(KEY_LEGACY_HASH, null) ?: return false
        val salt = prefs.getString(KEY_LEGACY_SALT, null) ?: return false
        return constantTimeEquals(stored, legacyHash(pin, salt))
    }

    private fun recordFailure() {
        val attempts = prefs.getInt(KEY_FAILED_ATTEMPTS, 0) + 1
        val editor = prefs.edit().putInt(KEY_FAILED_ATTEMPTS, attempts)
        if (attempts >= FREE_ATTEMPTS) {
            // 30 שניות אחרי הכישלון החמישי, ואז הכפלה עד תקרה של חמש דקות -
            // מספיק כדי להפוך ניחוש של 10,000 קודים לבלתי אפשרי בפועל, ולא
            // מספיק כדי לנעול משתמש אמיתי מחוץ למכשיר שלו.
            val step = (attempts - FREE_ATTEMPTS).coerceAtMost(MAX_BACKOFF_STEPS)
            val delayMs = (BASE_LOCKOUT_MS shl step).coerceAtMost(MAX_LOCKOUT_MS)
            editor.putLong(KEY_LOCKED_UNTIL, System.currentTimeMillis() + delayMs)
        }
        editor.apply()
    }

    private fun computeMac(pin: String): String? = try {
        val mac = Mac.getInstance(MAC_ALGORITHM)
        mac.init(getOrCreateKeystoreKey())
        Base64.encodeToString(mac.doFinal(pin.toByteArray(Charsets.UTF_8)), Base64.NO_WRAP)
    } catch (e: Exception) {
        Log.e(TAG, "Failed computing PIN MAC", e)
        null
    }

    private fun getOrCreateKeystoreKey(): javax.crypto.SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_HMAC_SHA256, ANDROID_KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_SIGN).build()
        )
        return generator.generateKey()
    }

    private fun deleteKeystoreEntry() {
        try {
            KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }.deleteEntry(KEY_ALIAS)
        } catch (e: Exception) {
            Log.w(TAG, "Failed deleting PIN key", e)
        }
    }

    private fun getOrCreateLegacySalt(): String {
        prefs.getString(KEY_LEGACY_SALT, null)?.let { return it }
        val salt = java.util.UUID.randomUUID().toString()
        prefs.edit().putString(KEY_LEGACY_SALT, salt).apply()
        return salt
    }

    private fun legacyHash(pin: String, salt: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest((salt + pin).toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
    }

    private fun constantTimeEquals(a: String, b: String): Boolean =
        MessageDigest.isEqual(a.toByteArray(Charsets.UTF_8), b.toByteArray(Charsets.UTF_8))

    companion object {
        private const val TAG = "PinStore"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "futureui_lockscreen_pin"
        private const val MAC_ALGORITHM = "HmacSHA256"

        private const val KEY_PIN_MAC = "pin_mac"
        private const val KEY_LEGACY_HASH = "pin_hash"
        private const val KEY_LEGACY_SALT = "pin_salt"
        private const val KEY_FAILED_ATTEMPTS = "pin_failed_attempts"
        private const val KEY_LOCKED_UNTIL = "pin_locked_until"

        private const val FREE_ATTEMPTS = 5
        private const val BASE_LOCKOUT_MS = 30_000L
        private const val MAX_LOCKOUT_MS = 300_000L
        private const val MAX_BACKOFF_STEPS = 4
    }
}
