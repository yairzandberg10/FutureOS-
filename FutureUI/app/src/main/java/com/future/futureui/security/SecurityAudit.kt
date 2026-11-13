package com.future.futureui.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * יומן האבטחה: מה נחסם, מתי, איזו אפליקציה, ומה היה המצב לפני כן.
 *
 * הקובץ מוצפן ב-AES-256-GCM במפתח של Android Keystore שלא יוצא מהמכשיר ולא נגיש לאף
 * אפליקציה אחרת. GCM גם מאמת: קובץ ששונה מבחוץ לא נפתח, ואז היומן מתחיל מחדש ברשומה
 * "היומן אופס" - כך שמחיקה או זיוף של היסטוריה נראים, ולא נעלמים בשקט.
 *
 * רק אירועים, בלי תוכן: לא הודעות, לא התראות ולא קודים.
 */
object SecurityAudit {
    private const val TAG = "SecurityAudit"
    private const val FILE = "security_audit.bin"
    private const val KEY_ALIAS = "futureos_security_audit_v1"
    private const val MAX_ENTRIES = 300
    private val lock = Any()

    /** סוגי אירועים. חייבים להתאים למסך ההיסטוריה בהגדרות. */
    object Type {
        const val NEW = "NEW"                 // ממצא חדש (התראה)
        const val BASELINE = "BASELINE"       // ההפעלה הראשונה: מה שכבר היה פעיל
        const val AUTO_BLOCK = "AUTO_BLOCK"   // נחסם אוטומטית
        const val BLOCK = "BLOCK"             // נחסם ידנית מההגדרות
        const val RESTORE = "RESTORE"         // הוחזר וסומן כמהימן
        const val ALLOW = "ALLOW"             // סומן כמהימן
        const val HOLD = "HOLD"               // אפליקציה הושהתה לפני ההפעלה הראשונה
        const val SIDELOAD = "SIDELOAD"       // הותקנה ממקור לא מוכר
        const val TAMPER = "TAMPER"           // הגדרות ההגנה שונו מבחוץ
        const val DB_UPDATE = "DB_UPDATE"     // מאגר הנוזקות עודכן
        const val DB_REJECT = "DB_REJECT"     // עדכון מאגר נדחה (חתימה/גרסה)
        const val LOG_RESET = "LOG_RESET"     // היומן הקודם לא נפתח (שונה או נפגם)
    }

    data class Event(
        val time: Long,
        val type: String,
        val kind: String,
        val target: String,
        val label: String,
        val detail: String,
    )

    fun record(context: Context, type: String, kind: String = "", target: String = "", label: String = "", detail: String = "") {
        synchronized(lock) {
            runCatching {
                val list = readLocked(context).toMutableList()
                list += Event(System.currentTimeMillis(), type, kind, target, label, detail)
                writeLocked(context, list.takeLast(MAX_ENTRIES))
            }.onFailure { Log.w(TAG, "record failed", it) }
        }
    }

    /** החדש קודם. */
    fun events(context: Context): List<Event> = synchronized(lock) {
        runCatching { readLocked(context) }.getOrDefault(emptyList()).asReversed()
    }

    private fun readLocked(context: Context): List<Event> {
        val f = File(context.filesDir, FILE)
        if (!f.exists()) return emptyList()
        val plain = try {
            decrypt(f.readBytes())
        } catch (e: Exception) {
            Log.w(TAG, "audit log unreadable - starting a new one", e)
            val reset = listOf(Event(System.currentTimeMillis(), Type.LOG_RESET, "", "", "", ""))
            writeLocked(context, reset)
            return reset
        }
        val arr = JSONArray(String(plain, Charsets.UTF_8))
        return (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            Event(o.optLong("t"), o.optString("y"), o.optString("k"), o.optString("g"), o.optString("l"), o.optString("d"))
        }
    }

    private fun writeLocked(context: Context, list: List<Event>) {
        val arr = JSONArray()
        list.forEach { e ->
            arr.put(JSONObject().put("t", e.time).put("y", e.type).put("k", e.kind).put("g", e.target).put("l", e.label).put("d", e.detail))
        }
        val tmp = File(context.filesDir, "$FILE.tmp")
        tmp.writeBytes(encrypt(arr.toString().toByteArray(Charsets.UTF_8)))
        if (!tmp.renameTo(File(context.filesDir, FILE))) error("rename failed")
    }

    // פורמט: [12 בתים IV][מוצפן + תג GCM]
    private fun encrypt(plain: ByteArray): ByteArray {
        val c = Cipher.getInstance("AES/GCM/NoPadding")
        c.init(Cipher.ENCRYPT_MODE, key())
        return c.iv + c.doFinal(plain)
    }

    private fun decrypt(blob: ByteArray): ByteArray {
        require(blob.size > 12 + 16)
        val c = Cipher.getInstance("AES/GCM/NoPadding")
        c.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, blob, 0, 12))
        return c.doFinal(blob, 12, blob.size - 12)
    }

    private fun key(): SecretKey {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (ks.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        val gen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        gen.init(
            KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )
        return gen.generateKey()
    }
}
