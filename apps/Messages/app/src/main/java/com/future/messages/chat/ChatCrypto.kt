package com.future.messages.chat

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.PublicKey
import java.security.SecureRandom
import java.security.Signature
import java.security.spec.ECGenParameterSpec
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.KeyAgreement
import javax.crypto.Mac
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * הצפנה מקצה לקצה של צ'אט FutureOS. השרת (Firebase) רואה רק מי שלח למי ומתי;
 * את התוכן הוא לא יכול לקרוא.
 *
 * - לכל מכשיר שני מפתחות P-256 ב-AndroidKeyStore (בחומרה, לא ניתנים לייצוא):
 *   מפתח הסכמה (ECDH) לפענוח, ומפתח חתימה (ECDSA) לזיהוי השולח.
 * - כל הודעה מוצפנת במפתח זמני חדש (ECIES): ECDH(זמני, מפתח הנמען) →
 *   HKDF-SHA256 → AES-256-GCM. דליפה של הודעה אחת לא חושפת אחרות.
 * - השולח חותם על כל המעטפה (מזהים + מפתח זמני + צופן). הנמען מאמת מול מפתח
 *   החתימה של השולח, כך ששרת פרוץ לא יכול להתחזות או לשנות הודעה.
 */
internal object ChatCrypto {
    private const val AGREE_ALIAS = "future_chat_agree_v1"
    private const val SIGN_ALIAS = "future_chat_sign_v1"
    private const val CURVE = "secp256r1"
    private val random = SecureRandom()

    private val keyStore: KeyStore by lazy { KeyStore.getInstance("AndroidKeyStore").apply { load(null) } }

    /** יוצר את זוג המפתחות אם עוד לא קיימים. מחזיר את המפתחות הציבוריים (base64 של X.509). */
    fun ensureIdentity(): Pair<String, String> {
        if (!keyStore.containsAlias(AGREE_ALIAS)) {
            KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC, "AndroidKeyStore").apply {
                initialize(
                    KeyGenParameterSpec.Builder(AGREE_ALIAS, KeyProperties.PURPOSE_AGREE_KEY)
                        .setAlgorithmParameterSpec(ECGenParameterSpec(CURVE))
                        .build()
                )
            }.generateKeyPair()
        }
        if (!keyStore.containsAlias(SIGN_ALIAS)) {
            KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC, "AndroidKeyStore").apply {
                initialize(
                    KeyGenParameterSpec.Builder(SIGN_ALIAS, KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY)
                        .setAlgorithmParameterSpec(ECGenParameterSpec(CURVE))
                        .setDigests(KeyProperties.DIGEST_SHA256)
                        .build()
                )
            }.generateKeyPair()
        }
        return b64(keyStore.getCertificate(AGREE_ALIAS).publicKey.encoded) to
            b64(keyStore.getCertificate(SIGN_ALIAS).publicKey.encoded)
    }

    class Sealed(val eph: String, val iv: String, val ct: String, val sig: String)

    /** מצפין ל-recipientAgreeKey וחותם. context קושר את ההצפנה לשולח, לנמען ולמזהה. */
    fun seal(plaintext: ByteArray, recipientAgreeKey: String, context: String): Sealed {
        val eph = KeyPairGenerator.getInstance("EC").apply { initialize(ECGenParameterSpec(CURVE), random) }.generateKeyPair()
        val shared = KeyAgreement.getInstance("ECDH").run {
            init(eph.private)
            doPhase(publicKey(recipientAgreeKey), true)
            generateSecret()
        }
        val ephB64 = b64(eph.public.encoded)
        val key = hkdf(shared, salt = ephB64.toByteArray(), info = "FutureChat v1|$context".toByteArray())
        val iv = ByteArray(12).also(random::nextBytes)
        val ct = Cipher.getInstance("AES/GCM/NoPadding").run {
            init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(128, iv))
            updateAAD(context.toByteArray())
            doFinal(plaintext)
        }
        val ivB64 = b64(iv)
        val ctB64 = b64(ct)
        val sig = Signature.getInstance("SHA256withECDSA").run {
            initSign(keyStore.getKey(SIGN_ALIAS, null) as PrivateKey)
            update(signedBytes(context, ephB64, ivB64, ctB64))
            sign()
        }
        return Sealed(ephB64, ivB64, ctB64, b64(sig))
    }

    /** מאמת את חתימת השולח ומפענח. זורק חריגה אם משהו לא תקין. */
    fun open(sealed: Sealed, senderSignKey: String, context: String): ByteArray {
        val valid = Signature.getInstance("SHA256withECDSA").run {
            initVerify(publicKey(senderSignKey))
            update(signedBytes(context, sealed.eph, sealed.iv, sealed.ct))
            verify(unb64(sealed.sig))
        }
        require(valid) { "Bad signature" }
        val shared = KeyAgreement.getInstance("ECDH", "AndroidKeyStore").run {
            init(keyStore.getKey(AGREE_ALIAS, null) as PrivateKey)
            doPhase(publicKey(sealed.eph), true)
            generateSecret()
        }
        val key = hkdf(shared, salt = sealed.eph.toByteArray(), info = "FutureChat v1|$context".toByteArray())
        return Cipher.getInstance("AES/GCM/NoPadding").run {
            init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(128, unb64(sealed.iv)))
            updateAAD(context.toByteArray())
            doFinal(unb64(sealed.ct))
        }
    }

    /** הצפנת קובץ (תמונה) במפתח אקראי שנשלח בתוך ההודעה המוצפנת. */
    class FileKey(val key: String, val iv: String)

    fun encryptFile(data: ByteArray): Pair<ByteArray, FileKey> {
        val key = ByteArray(32).also(random::nextBytes)
        val iv = ByteArray(12).also(random::nextBytes)
        val ct = Cipher.getInstance("AES/GCM/NoPadding").run {
            init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(128, iv))
            doFinal(data)
        }
        return ct to FileKey(b64(key), b64(iv))
    }

    fun decryptFile(data: ByteArray, fileKey: FileKey): ByteArray =
        Cipher.getInstance("AES/GCM/NoPadding").run {
            init(Cipher.DECRYPT_MODE, SecretKeySpec(unb64(fileKey.key), "AES"), GCMParameterSpec(128, unb64(fileKey.iv)))
            doFinal(data)
        }

    /** טביעת אצבע קצרה של מפתח - להצגה ולזיהוי החלפת מפתח. */
    fun fingerprint(publicKeyB64: String): String =
        java.security.MessageDigest.getInstance("SHA-256").digest(unb64(publicKeyB64))
            .take(8).joinToString("") { "%02X".format(it) }.chunked(4).joinToString(" ")

    private fun signedBytes(context: String, eph: String, iv: String, ct: String) =
        "v1|$context|$eph|$iv|$ct".toByteArray()

    private fun hkdf(ikm: ByteArray, salt: ByteArray, info: ByteArray): ByteArray {
        val prk = Mac.getInstance("HmacSHA256").run { init(SecretKeySpec(salt, "HmacSHA256")); doFinal(ikm) }
        return Mac.getInstance("HmacSHA256").run {
            init(SecretKeySpec(prk, "HmacSHA256"))
            update(info)
            update(1.toByte())
            doFinal()
        }
    }

    private fun publicKey(b64: String): PublicKey =
        KeyFactory.getInstance("EC").generatePublic(X509EncodedKeySpec(unb64(b64)))

    fun b64(bytes: ByteArray): String = Base64.encodeToString(bytes, Base64.NO_WRAP)
    fun unb64(s: String): ByteArray = Base64.decode(s, Base64.NO_WRAP)
}
