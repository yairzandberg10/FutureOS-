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
 * מה שהמוקד הטלפוני מבקש ואפשר להקיש בשבילך: מספר כרטיס הרב-קו. פרטי
 * אשראי לא נשמרים בכוונה - מקישים אותם בשיחה עצמה, והאפליקציה אף פעם לא
 * רואה אותם.
 */
@Serializable
data class PaymentProfile(
    val ravKavNumber: String = "",
) {
    val isEmpty: Boolean get() = ravKavNumber.isBlank()
}

/**
 * נשמר בקובץ פרטי של האפליקציה, מוצפן ב-AES-GCM במפתח Android Keystore שלא
 * יוצא מהמכשיר - אותה תבנית של FaceTemplateStore במסך הנעילה.
 */
class PaymentProfileStore(context: Context) {
    private val file = File(context.filesDir, FILE_NAME)
    private val json = Json { ignoreUnknownKeys = true }

    private val _profile = MutableStateFlow(load())
    val profile: StateFlow<PaymentProfile> = _profile.asStateFlow()

    fun save(profile: PaymentProfile) {
        val clean = profile.copy(ravKavNumber = profile.ravKavNumber.filter { it.isDigit() })
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

    private companion object {
        const val TAG = "PaymentProfile"
        const val FILE_NAME = "payment_profile.bin"
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val KEY_ALIAS = "navigation_payment_profile"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val IV_LEN = 12
    }
}
