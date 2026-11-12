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
import javax.crypto.SecretKey

/**
 * קוד הנעילה (4 עד 8 ספרות).
 *
 * הקוד עצמו לא נשמר בשום מקום - רק HMAC-SHA256 שלו, במפתח שנוצר ב-Android
 * Keystore ואינו ניתן לייצוא. מי שמעתיק את קובץ ההעדפות לא יכול לנחש offline
 * (10,000 אפשרויות לקוד של 4 ספרות נשברות בשבריר שנייה מול hash רגיל), כי
 * בלי החומרה של המכשיר אי אפשר לחשב את ה-HMAC.
 *
 * ההשוואה בזמן קבוע, וכל כישלון נספר: אחרי 5 ניסיונות מתחילה השהיה מצטברת
 * (30 שנ', ואז הכפלה עד 15 דקות) - בדיוק כמו אייפון/סמסונג.
 */
class PinStore(private val prefs: SharedPreferences) {

    fun hasPin(): Boolean = prefs.getString(KEY_PIN_MAC, null) != null

    /** אורך הקוד - מסך הנעילה בודק אוטומטית ברגע שהוזנו מספיק ספרות. */
    fun pinLength(): Int = prefs.getInt(KEY_PIN_LENGTH, 4)

    fun setPin(pin: String): Boolean {
        require(pin.length in MIN_LENGTH..MAX_LENGTH && pin.all { it.isDigit() })
        val mac = computeMac(pin) ?: return false
        prefs.edit()
            .putString(KEY_PIN_MAC, mac)
            .putInt(KEY_PIN_LENGTH, pin.length)
            .remove(KEY_FAILED_ATTEMPTS)
            .remove(KEY_LOCKED_UNTIL)
            .apply()
        return true
    }

    fun clearPin() {
        prefs.edit()
            .remove(KEY_PIN_MAC)
            .remove(KEY_PIN_LENGTH)
            .remove(KEY_FAILED_ATTEMPTS)
            .remove(KEY_LOCKED_UNTIL)
            .apply()
        runCatching { KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }.deleteEntry(KEY_ALIAS) }
    }

    /** כמה מילישניות נותרו עד שמותר לנסות שוב; 0 כשאין חסימה. */
    fun remainingLockoutMs(): Long {
        val until = prefs.getLong(KEY_LOCKED_UNTIL, 0L)
        val left = until - System.currentTimeMillis()
        // שעון שהוזז אחורה לא יכול להאריך חסימה לנצח
        return if (left > MAX_LOCKOUT_MS) MAX_LOCKOUT_MS else left.coerceAtLeast(0L)
    }

    fun failedAttempts(): Int = prefs.getInt(KEY_FAILED_ATTEMPTS, 0)

    fun verifyPin(pin: String): Boolean {
        if (remainingLockoutMs() > 0) return false
        val stored = prefs.getString(KEY_PIN_MAC, null) ?: return false
        val candidate = computeMac(pin)
        val matched = candidate != null &&
            MessageDigest.isEqual(stored.toByteArray(Charsets.UTF_8), candidate.toByteArray(Charsets.UTF_8))
        if (matched) {
            prefs.edit().remove(KEY_FAILED_ATTEMPTS).remove(KEY_LOCKED_UNTIL).apply()
        } else {
            recordFailure()
        }
        return matched
    }

    private fun recordFailure() {
        val attempts = prefs.getInt(KEY_FAILED_ATTEMPTS, 0) + 1
        val editor = prefs.edit().putInt(KEY_FAILED_ATTEMPTS, attempts)
        if (attempts >= FREE_ATTEMPTS) {
            val step = (attempts - FREE_ATTEMPTS).coerceAtMost(MAX_BACKOFF_STEPS)
            val delayMs = (BASE_LOCKOUT_MS shl step).coerceAtMost(MAX_LOCKOUT_MS)
            editor.putLong(KEY_LOCKED_UNTIL, System.currentTimeMillis() + delayMs)
        }
        editor.apply()
    }

    private fun computeMac(pin: String): String? = try {
        val mac = Mac.getInstance(MAC_ALGORITHM)
        mac.init(getOrCreateKey())
        Base64.encodeToString(mac.doFinal(pin.toByteArray(Charsets.UTF_8)), Base64.NO_WRAP)
    } catch (e: Exception) {
        Log.e(TAG, "PIN MAC failed", e)
        null
    }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_HMAC_SHA256, ANDROID_KEYSTORE)
        generator.init(KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_SIGN).build())
        return generator.generateKey()
    }

    companion object {
        private const val TAG = "LockPin"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        // שם חדש בכוונה: מסך הנעילה הקודם (נמחק ב-24/09) השתמש ב-futureui_lockscreen_pin
        // ואם נשאר ממנו קוד ישן על המכשיר - הוא לא אמור לנעול את המשתמש בחוץ.
        private const val KEY_ALIAS = "futureui_lock2_pin"
        private const val MAC_ALGORITHM = "HmacSHA256"

        private const val KEY_PIN_MAC = "pin_mac"
        private const val KEY_PIN_LENGTH = "pin_length"
        private const val KEY_FAILED_ATTEMPTS = "pin_failed_attempts"
        private const val KEY_LOCKED_UNTIL = "pin_locked_until"

        const val MIN_LENGTH = 4
        const val MAX_LENGTH = 8
        private const val FREE_ATTEMPTS = 5
        private const val BASE_LOCKOUT_MS = 30_000L
        private const val MAX_LOCKOUT_MS = 15 * 60_000L
        private const val MAX_BACKOFF_STEPS = 5
    }
}
