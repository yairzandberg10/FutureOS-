package com.future.futureui.lockscreen.face

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Log
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * תבניות הפנים של המשתמש - וקטורי מאפיינים בלבד, אף פעם לא תמונות.
 * נשמרות בקובץ פרטי של FutureUI, מוצפנות ב-AES-GCM במפתח Android Keystore
 * שאינו יוצא מהמכשיר: גם מי שמעתיק את הקובץ לא יכול לקרוא אותו.
 *
 * שתי קבוצות: דגימות הרישום (קבועות) ודגימות "למידה" - כשהפנים לא זוהו
 * והמשתמש מיד הזין קוד נכון, הדגימה האחרונה נוספת (עד 10, הישנה יוצאת),
 * כך שהזיהוי משתפר עם משקפיים, זקן או תאורה חדשה - כמו באייפון.
 */
class FaceTemplateStore(context: Context) {
    private val file = File(context.filesDir, FILE_NAME)

    class Data(val enrolled: List<FloatArray>, val adaptive: List<FloatArray>, val calibration: Float) {
        val all: List<FloatArray> get() = enrolled + adaptive
    }

    @Volatile private var cache: Data? = null

    fun isEnrolled(): Boolean = file.exists() && (load()?.enrolled?.isNotEmpty() == true)

    fun load(): Data? {
        cache?.let { return it }
        if (!file.exists()) return null
        return try {
            val raw = file.readBytes()
            val iv = raw.copyOfRange(0, IV_LEN)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(128, iv))
            val plain = cipher.doFinal(raw, IV_LEN, raw.size - IV_LEN)
            decode(plain).also { cache = it }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read face templates", e)
            null
        }
    }

    fun saveEnrollment(samples: List<FloatArray>, calibration: Float) {
        write(Data(samples.take(MAX_ENROLLED), emptyList(), calibration))
    }

    fun addAdaptive(sample: FloatArray) {
        val d = load() ?: return
        write(Data(d.enrolled, (d.adaptive + listOf(sample)).takeLast(MAX_ADAPTIVE), d.calibration))
    }

    fun clear() {
        cache = null
        file.delete()
        runCatching { KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }.deleteEntry(KEY_ALIAS) }
    }

    private fun write(data: Data) {
        try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
            val enc = cipher.doFinal(encode(data))
            val tmp = File(file.parentFile, "$FILE_NAME.tmp")
            tmp.writeBytes(cipher.iv + enc)
            if (!tmp.renameTo(file)) { file.delete(); tmp.renameTo(file) }
            cache = data
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save face templates", e)
        }
    }

    /** כל ערך בהיסטוגרמה בין 0 ל-1 - נשמר כ-16 ביט (דיוק של 1/65535 - מספיק בהרבה). */
    private fun encode(d: Data): ByteArray {
        val bos = ByteArrayOutputStream()
        DataOutputStream(bos).use { out ->
            out.writeInt(VERSION)
            out.writeInt(FaceEngine.DIM)
            out.writeFloat(d.calibration)
            out.writeInt(d.enrolled.size)
            out.writeInt(d.adaptive.size)
            for (t in d.all) for (v in t) out.writeShort((v.coerceIn(0f, 1f) * 65535f).toInt())
        }
        return bos.toByteArray()
    }

    private fun decode(b: ByteArray): Data {
        DataInputStream(ByteArrayInputStream(b)).use { inp ->
            require(inp.readInt() == VERSION)
            val dim = inp.readInt()
            require(dim == FaceEngine.DIM)
            val calibration = inp.readFloat()
            val nEnrolled = inp.readInt()
            val nAdaptive = inp.readInt()
            fun read() = FloatArray(dim) { inp.readUnsignedShort() / 65535f }
            val enrolled = List(nEnrolled) { read() }
            val adaptive = List(nAdaptive) { read() }
            return Data(enrolled, adaptive, calibration)
        }
    }

    private fun getOrCreateKey(): SecretKey {
        val ks = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (ks.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }
        val gen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        gen.init(
            KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )
        return gen.generateKey()
    }

    companion object {
        private const val TAG = "FaceTemplates"
        private const val FILE_NAME = "face_templates.bin"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "futureui_lock2_face"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val IV_LEN = 12
        private const val VERSION = 1
        const val MAX_ENROLLED = 15
        const val MAX_ADAPTIVE = 10
    }
}
