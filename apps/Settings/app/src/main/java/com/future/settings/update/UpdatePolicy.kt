package com.future.settings.update

import java.security.KeyFactory
import java.security.MessageDigest
import java.security.Signature
import java.security.spec.X509EncodedKeySpec
import java.util.Base64

/**
 * הכללים של מנוע העדכונים, בלי ספריות Android, כדי שאפשר לבדוק אותם בבדיקת יחידה על ה-JVM.
 *
 * שלוש שכבות הגנה, כל אחת בנפרד:
 * 1. ה-manifest חתום ב-ECDSA P-256 בשרת, והמפתח הציבורי בתוך הבנייה (future.update.pubkey).
 * 2. ה-APK שהורד חייב להתאים ל-SHA-256 ולגודל שבתוך ה-manifest החתום.
 * 3. ה-APK חייב להיות חתום באותו מפתח כמו האפליקציה המותקנת וכמו Settings (נבדק ב-UpdateRepository).
 *    זו השכבה שמגנה גם אם השרת עצמו נפרץ: הוא לא מחזיק את מפתח החתימה של ה-APK.
 */
object UpdatePolicy {

    /** חלון סובלנות לשעון של הטלפון מול השרת. */
    const val CLOCK_SKEW_MS = 10L * 60L * 1000L

    /** תקרה לקובץ עדכון אחד (כמו MAX_PAYLOAD בשרת). */
    const val MAX_SIZE = 90L * 1024L * 1024L

    private val PATH_RE = Regex("""^update/download/[0-9a-f-]{36}$""")
    private val SHA_RE = Regex("""^[0-9a-f]{64}$""")
    private val FUTURE_PKG_RE = Regex("""^com\.future\.[a-z0-9_.]{1,64}$""")

    data class Offer(
        val releaseId: String,
        val kind: String,
        val pkg: String,
        val versionCode: Long,
        val versionName: String,
        val size: Long,
        val sha256: String,
        val notesHe: String,
        val path: String,
    )

    /** למה הצעה נדחתה. null = ההצעה תקינה. */
    enum class Rejection { NotFuturePackage, NotInstalled, NotNewer, BadSize, BadHash, BadPath, UnsupportedKind }

    /**
     * בודק חתימה של השרת על מחרוזת ה-manifest כמו שהגיעה.
     * החתימה מ-WebCrypto היא r||s (64 בתים), ו-Java מצפה ל-DER, לכן ממירים.
     */
    fun verifySignature(manifest: String, signatureB64: String, publicKeySpkiB64: String): Boolean = try {
        val keyBytes = Base64.getDecoder().decode(publicKeySpkiB64.trim())
        val key = KeyFactory.getInstance("EC").generatePublic(X509EncodedKeySpec(keyBytes))
        val raw = Base64.getDecoder().decode(signatureB64.trim())
        val der = if (raw.size == 64) p1363ToDer(raw) else raw
        Signature.getInstance("SHA256withECDSA").run {
            initVerify(key)
            update(manifest.toByteArray(Charsets.UTF_8))
            verify(der)
        }
    } catch (e: Exception) {
        false
    }

    /** ממיר חתימת ECDSA בפורמט r||s (IEEE P1363) ל-DER (SEQUENCE של שני INTEGER). */
    fun p1363ToDer(raw: ByteArray): ByteArray {
        require(raw.size % 2 == 0 && raw.isNotEmpty())
        val half = raw.size / 2
        val r = derInteger(raw.copyOfRange(0, half))
        val s = derInteger(raw.copyOfRange(half, raw.size))
        val body = r + s
        return byteArrayOf(0x30) + derLength(body.size) + body
    }

    private fun derInteger(value: ByteArray): ByteArray {
        var start = 0
        while (start < value.size - 1 && value[start] == 0.toByte()) start++
        var v = value.copyOfRange(start, value.size)
        if (v[0].toInt() and 0x80 != 0) v = byteArrayOf(0) + v // מספר חיובי: בלי ביט סימן דולק
        return byteArrayOf(0x02) + derLength(v.size) + v
    }

    private fun derLength(len: Int): ByteArray =
        if (len < 0x80) byteArrayOf(len.toByte()) else byteArrayOf(0x81.toByte(), len.toByte())

    /** manifest שנחתם למכשיר אחר, או שפג תוקפו, נדחה כולו. */
    fun manifestValid(deviceIdInManifest: String, ownDeviceId: String, issuedAt: Long, expiresAt: Long, now: Long): Boolean =
        deviceIdInManifest == ownDeviceId &&
            issuedAt <= now + CLOCK_SKEW_MS &&
            expiresAt + CLOCK_SKEW_MS >= now &&
            expiresAt > issuedAt

    /**
     * בודק הצעה אחת מול מה שמותקן. installedVersionCode = null כשהחבילה לא מותקנת:
     * המנוע מעדכן רק אפליקציות קיימות, ולא מתקין אפליקציות חדשות.
     */
    fun checkOffer(offer: Offer, installedVersionCode: Long?): Rejection? = when {
        offer.kind != "apk" -> Rejection.UnsupportedKind
        !FUTURE_PKG_RE.matches(offer.pkg) -> Rejection.NotFuturePackage
        installedVersionCode == null -> Rejection.NotInstalled
        offer.versionCode <= installedVersionCode -> Rejection.NotNewer
        offer.size <= 0 || offer.size > MAX_SIZE -> Rejection.BadSize
        !SHA_RE.matches(offer.sha256) -> Rejection.BadHash
        !PATH_RE.matches(offer.path) -> Rejection.BadPath
        else -> null
    }

    /** כתובת ההורדה תמיד יחסית לשרת שלנו: ההצעה לא יכולה להפנות לשרת אחר. */
    fun downloadUrl(base: String, offer: Offer): String? =
        if (PATH_RE.matches(offer.path)) "${base.trimEnd('/')}/${offer.path}" else null

    /** HTTPS בלבד. ב-debug מותר גם 127.0.0.1 (שרת הפיתוח דרך adb reverse). */
    fun transportAllowed(url: String, debug: Boolean): Boolean =
        url.startsWith("https://") || (debug && url.startsWith("http://127.0.0.1:"))

    fun sha256Hex(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

    /** אותה קבוצת חותמים בדיוק (לפי SHA-256 של כל תעודה). קבוצה ריקה לעולם לא תואמת. */
    fun sameSigners(a: Set<String>, b: Set<String>): Boolean = a.isNotEmpty() && a == b

    /** טקסט קצר בעברית למצב "נכשל", לפי קוד השגיאה הפנימי. */
    fun failureLabel(code: String): String = when {
        code == "hash" || code == "size" -> "הקובץ שהורד פגום. נסה שוב"
        code == "signature" -> "העדכון לא חתום במפתח של FutureOS ולא הותקן"
        code == "package" || code == "version" -> "העדכון לא מתאים לאפליקציה המותקנת"
        code == "network" -> "אין חיבור לשרת. נסה שוב מאוחר יותר"
        code == "storage" -> "אין מספיק מקום פנוי"
        code == "aborted" -> "ההתקנה בוטלה"
        code.startsWith("install") -> "ההתקנה נכשלה"
        else -> "העדכון נכשל"
    }
}
