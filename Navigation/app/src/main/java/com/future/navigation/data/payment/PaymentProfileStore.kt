package com.future.navigation.data.payment

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * הפרטים שהדפדפן ממלא לבד בטפסי התשלום (ר' PaymentWebScreen). אין כאן
 * פרטי כרטיס אשראי בכוונה: את אלה מקלידים רק בעמוד של גורם התשלום עצמו,
 * והאפליקציה אף פעם לא רואה או שומרת אותם.
 */
@Serializable
data class PaymentProfile(
    val firstName: String = "",
    val lastName: String = "",
    val idNumber: String = "",
    val phone: String = "",
    val email: String = "",
    val ravKavNumber: String = "",
) {
    val isEmpty: Boolean
        get() = listOf(firstName, lastName, idNumber, phone, email, ravKavNumber).all { it.isBlank() }

    val fullName: String get() = listOf(firstName, lastName).filter { it.isNotBlank() }.joinToString(" ")
}

/**
 * הפרטים נשמרים בקובץ פרטי של האפליקציה, מוצפנים ב-AES-GCM במפתח Android
 * Keystore שלא יוצא מהמכשיר - אותה תבנית של FaceTemplateStore במסך הנעילה.
 * תעודת זהות ומספר טלפון הם בדיוק המידע שלא אמור לשבת כטקסט גלוי בקובץ.
 */
class PaymentProfileStore(context: Context) {
    private val file = File(context.filesDir, FILE_NAME)
    private val json = Json { ignoreUnknownKeys = true }

    private val _profile = MutableStateFlow(load())
    val profile: StateFlow<PaymentProfile> = _profile.asStateFlow()

    fun save(profile: PaymentProfile) {
        val clean = profile.copy(
            firstName = profile.firstName.trim(),
            lastName = profile.lastName.trim(),
            idNumber = profile.idNumber.filter { it.isDigit() },
            phone = profile.phone.filter { it.isDigit() || it == '+' },
            email = profile.email.trim(),
            ravKavNumber = profile.ravKavNumber.filter { it.isDigit() },
        )
        try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
            val enc = cipher.doFinal(json.encodeToString(PaymentProfile.serializer(), clean).toByteArray(Charsets.UTF_8))
            val tmp = File(file.parentFile, "$FILE_NAME.tmp")
            tmp.writeBytes(cipher.iv + enc)
            if (!tmp.renameTo(file)) { file.delete(); tmp.renameTo(file) }
            _profile.value = clean
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save payment profile", e)
        }
    }

    fun clear() {
        file.delete()
        _profile.value = PaymentProfile()
    }

    private fun load(): PaymentProfile {
        if (!file.exists()) return PaymentProfile()
        return try {
            val raw = file.readBytes()
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(128, raw.copyOfRange(0, IV_LEN)))
            val plain = cipher.doFinal(raw, IV_LEN, raw.size - IV_LEN)
            json.decodeFromString(PaymentProfile.serializer(), plain.toString(Charsets.UTF_8))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read payment profile", e)
            PaymentProfile()
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
        private const val TAG = "PaymentProfile"
        private const val FILE_NAME = "payment_profile.bin"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "navigation_payment_profile"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val IV_LEN = 12

        /**
         * ספרת הביקורת של תעודת זהות ישראלית (משקלים 1,2 לסירוגין וסכום
         * ספרות). מספר קצר מ-9 ספרות מרופד באפסים משמאל, כמו בטפסים הרשמיים.
         */
        fun isValidIsraeliId(id: String): Boolean {
            val digits = id.filter { it.isDigit() }
            if (digits.isEmpty() || digits.length > 9 || digits.length != id.trim().length) return false
            val padded = digits.padStart(9, '0')
            val sum = padded.mapIndexed { i, c ->
                val v = (c - '0') * (if (i % 2 == 0) 1 else 2)
                if (v > 9) v - 9 else v
            }.sum()
            return sum % 10 == 0
        }
    }
}
