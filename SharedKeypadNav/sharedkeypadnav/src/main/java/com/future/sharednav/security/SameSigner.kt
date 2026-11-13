package com.future.sharednav.security

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import java.security.MessageDigest

/**
 * "האם האפליקציה הזו חתומה באותו מפתח כמו שלנו" - בלי PackageManager.checkSignatures.
 *
 * בטלפון של FutureOS (ROM עם root) checkSignatures מחזיר SIGNATURE_MATCH לכל זוג
 * אפליקציות, גם כשהתעודות שונות לגמרי (נמדד: Play-apps מול FutureUI). כל בדיקת
 * "זו באמת אפליקציה של FutureOS" שנשענה עליו עברה לכל אפליקציה. כאן משווים את
 * התעודות עצמן: SHA-256 של כל חותם ב-signingInfo, שנקרא מהקובץ המותקן.
 *
 * אם אי אפשר לקרוא תעודות (שגיאה, חבילה לא קיימת) - התשובה "לא", אף פעם לא "כן".
 */
object SameSigner {

    @Volatile private var selfCache: Pair<String, Set<String>>? = null

    /** SHA-256 (hex, אותיות גדולות) של החותמים הנוכחיים. ריק = לא ידוע. */
    fun digests(info: PackageInfo?): Set<String> = runCatching {
        val signing = info?.signingInfo ?: return@runCatching emptySet<String>()
        // apkContentsSigners = מי שחתם בפועל על הקובץ המותקן (גם אחרי רוטציית מפתחות)
        signing.apkContentsSigners.orEmpty()
            .map { hex(MessageDigest.getInstance("SHA-256").digest(it.toByteArray())) }.toSet()
    }.getOrDefault(emptySet())

    /** SHA-1 של תעודות החתימה - הפורמט של רשימות IOC ציבוריות. */
    fun sha1(info: PackageInfo?): Set<String> = runCatching {
        val signing = info?.signingInfo ?: return@runCatching emptySet<String>()
        val all = (signing.apkContentsSigners.orEmpty().toList() + signing.signingCertificateHistory.orEmpty().toList())
        all.map { hex(MessageDigest.getInstance("SHA-1").digest(it.toByteArray())) }.toSet()
    }.getOrDefault(emptySet())

    fun digests(context: Context, pkg: String): Set<String> = runCatching {
        digests(context.packageManager.getPackageInfo(pkg, PackageManager.GET_SIGNING_CERTIFICATES))
    }.getOrDefault(emptySet())

    /** החותמים של האפליקציה שלנו. ריק = משהו שבור, ואז שום אפליקציה לא נחשבת "שלנו". */
    fun self(context: Context): Set<String> {
        val pkg = context.packageName
        selfCache?.let { (p, d) -> if (p == pkg && d.isNotEmpty()) return d }
        val d = digests(context, pkg)
        if (d.isNotEmpty()) selfCache = pkg to d
        return d
    }

    fun matches(context: Context, other: String): Boolean {
        if (other == context.packageName) return true
        val mine = self(context)
        return mine.isNotEmpty() && digests(context, other) == mine
    }

    fun matches(context: Context, info: PackageInfo): Boolean {
        if (info.packageName == context.packageName) return true
        val mine = self(context)
        return mine.isNotEmpty() && digests(info) == mine
    }

    /** האם ה-uid (למשל של מי שקורא לספק תוכן) שייך לאפליקציה בחתימה שלנו. */
    fun matchesUid(context: Context, uid: Int): Boolean =
        runCatching { context.packageManager.getPackagesForUid(uid) }.getOrNull()
            ?.any { matches(context, it) } == true

    private fun hex(bytes: ByteArray): String {
        val chars = "0123456789ABCDEF"
        val sb = StringBuilder(bytes.size * 2)
        for (b in bytes) {
            val v = b.toInt() and 0xFF
            sb.append(chars[v ushr 4]).append(chars[v and 0x0F])
        }
        return sb.toString()
    }
}
