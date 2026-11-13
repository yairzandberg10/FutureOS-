package com.android.sistemui.security

import android.app.AppOpsManager
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.provider.Settings
import android.util.Log
import com.future.sharednav.root.RootShell
import com.future.sharednav.security.SameSigner
import java.security.KeyStore
import java.security.cert.X509Certificate

/**
 * סורק איומים: מחפש את הדרכים שבהן נוזקות לאנדרואיד משתלטות על טלפון בפועל.
 *
 * שתי שכבות:
 *  1. נוזקות ידועות: שם החבילה או תעודת החתימה מופיעים במאגר הנוזקות (MalwareIndicators).
 *  2. יכולות מסוכנות שהופעלו לאפליקציה זרה - הצעד שכל נוזקה בנקאית או ריגול עושה כדי לפעול,
 *     גם כזו שעוד לא מופיעה באף מאגר:
 *     - שירות נגישות: קורא את כל המסך ומקשים, כולל הקוד של מסך הנעילה.
 *     - מאזין התראות: קורא הודעות וקודי אימות שמגיעים ב-SMS.
 *     - מנהל מכשיר: נועל, מוחק ומונע את הסרת האפליקציה.
 *     - קריאת SMS, חלון מעל אפליקציות (מסכי כניסה מזויפים), התקנת אפליקציות.
 *     - רשת: תעודת CA שהותקנה ידנית, שרת מתווך גלובלי, VPN, ניפוי באגים דרך Wi-Fi.
 *
 * "זרה" = לא אפליקציית מערכת ולא חתומה במפתח של FutureOS. האמון לפי התעודות עצמן
 * (SameSigner), לא לפי checkSignatures - שב-ROM של הטלפון מחזיר "תואם" לכל אפליקציה.
 *
 * כשל בקריאה של אחד המקורות לא נספר כ"נקי": שם המקור נכנס ל-unchecked ומוצג כ"לא נבדק".
 *
 * רץ ב-FutureUI כי רק לה יש QUERY_ALL_PACKAGES - בלי זה רשימת האפליקציות מסוננת
 * והסריקה הייתה מדווחת "נקי" בטעות.
 */
object ThreatScanner {
    private const val TAG = "ThreatScanner"

    /**
     * חומרה: 3 קריטי, 2 גבוה, 1 דורש בדיקה.
     * blockable: אפשר לחסום את היכולת עצמה (בלי למחוק את האפליקציה) ולהחזיר אותה אחר כך.
     */
    enum class Kind(val severity: Int, val blockable: Boolean) {
        KNOWN_MALWARE(3, false),
        ACCESSIBILITY(3, true),
        DEVICE_ADMIN(2, false),
        NOTIFICATION_LISTENER(2, true),
        USER_CA(2, false),
        PROXY(2, true),
        ADB_WIFI(2, false),
        SMS_READER(1, true),
        OVERLAY(1, true),
        INSTALLER(1, true),
        VPN(1, false),
    }

    /**
     * @param components לשירות נגישות / מאזין התראות: הרכיבים המדויקים (package/class),
     *   כדי שחסימה תסיר רק אותם ושחרור יחזיר בדיוק אותם.
     * @param detail לנוזקה ידועה: שם המשפחה במאגר.
     */
    data class Finding(
        val kind: Kind,
        val target: String,
        val label: String,
        val components: List<String> = emptyList(),
        val detail: String = "",
    ) {
        val key: String get() = "${kind.name}:$target"
    }

    data class Result(
        val findings: List<Finding>,
        val scannedApps: Int,
        /** false = לא היה אפשר לבדוק הרשאות חלון צף והתקנה (אין root). */
        val appOpsChecked: Boolean,
        /** מקורות שלא נקראו (accessibility, notifications, device_admin, apps, signatures, certificates, malware_db). */
        val unchecked: List<String>,
        val dbDate: String,
        val dbSize: Int,
    )

    private const val OP_OVERLAY = "android:system_alert_window"
    private const val OP_INSTALL = "android:request_install_packages"
    private val SMS_PERMISSIONS = setOf(android.Manifest.permission.READ_SMS, android.Manifest.permission.RECEIVE_SMS)

    /**
     * @param withAppOps בדיקת חלון צף/התקנה. אנדרואיד לא נותן לאפליקציה לקרוא את
     *   ההרשאות האלה של אפליקציה אחרת, אז כשהבדיקה הרגילה נחסמת עוברים ל-root
     *   (סשן su אחד לכל הסריקה).
     */
    fun scan(context: Context, withAppOps: Boolean): Result {
        val pm = context.packageManager
        val self = context.packageName
        val cr = context.contentResolver
        val findings = ArrayList<Finding>()
        val unchecked = ArrayList<String>()

        val selfDigests = SameSigner.self(context)
        if (selfDigests.isEmpty()) unchecked += "signatures"

        val packages: List<PackageInfo> = runCatching {
            pm.getInstalledPackages(PackageManager.GET_PERMISSIONS or PackageManager.GET_SIGNING_CERTIFICATES)
        }.getOrElse { Log.w(TAG, "installed packages", it); emptyList() }
        if (packages.isEmpty()) unchecked += "apps"
        val byPkg = packages.associateBy { it.packageName }

        val trustCache = HashMap<String, Boolean>()
        // null = לא מותקנת (רשומה ישנה בהגדרות), true = מהימנה, false = זרה
        fun trust(pkg: String): Boolean? {
            trustCache[pkg]?.let { return it }
            val info = byPkg[pkg] ?: runCatching {
                pm.getPackageInfo(pkg, PackageManager.GET_SIGNING_CERTIFICATES)
            }.getOrNull() ?: return null
            val ai = info.applicationInfo ?: return null
            val ok = pkg == self ||
                ai.flags and ApplicationInfo.FLAG_SYSTEM != 0 ||
                (selfDigests.isNotEmpty() && SameSigner.digests(info) == selfDigests)
            trustCache[pkg] = ok
            return ok
        }

        fun label(pkg: String): String = runCatching {
            pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
        }.getOrDefault(pkg)

        /** רכיבים מופעלים לפי חבילה. null = לא היה אפשר לקרוא את ההגדרה. */
        fun components(setting: String): Map<String, List<String>>? =
            runCatching { Settings.Secure.getString(cr, setting) ?: "" }
                .onFailure { Log.w(TAG, "read $setting", it) }
                .getOrNull()
                ?.split(':')
                ?.mapNotNull { flat -> ComponentName.unflattenFromString(flat)?.let { it.packageName to it.flattenToString() } }
                ?.groupBy({ it.first }, { it.second })

        val a11y = components(Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
        if (a11y == null) unchecked += "accessibility"
        a11y.orEmpty().forEach { (pkg, comps) ->
            if (trust(pkg) == false) findings += Finding(Kind.ACCESSIBILITY, pkg, label(pkg), comps)
        }
        val listeners = components("enabled_notification_listeners")
        if (listeners == null) unchecked += "notifications"
        listeners.orEmpty().forEach { (pkg, comps) ->
            if (trust(pkg) == false) findings += Finding(Kind.NOTIFICATION_LISTENER, pkg, label(pkg), comps)
        }
        runCatching {
            val dpm = context.getSystemService(DevicePolicyManager::class.java) ?: error("no DevicePolicyManager")
            dpm.activeAdmins.orEmpty().map { it.packageName }.toSet().forEach { pkg ->
                if (trust(pkg) == false) findings += Finding(Kind.DEVICE_ADMIN, pkg, label(pkg))
            }
        }.onFailure { Log.w(TAG, "device admins", it); unchecked += "device_admin" }

        // אפליקציות: נוזקות ידועות, קריאת SMS, ומועמדות לבדיקת חלון צף/התקנה
        val db = MalwareIndicators.load(context)
        if (db == null) unchecked += "malware_db"
        val opCandidates = ArrayList<PackageInfo>()
        for (info in packages) {
            val pkg = info.packageName
            if (trust(pkg) != false) continue
            if (db != null) {
                MalwareIndicators.match(db, pkg, SameSigner.sha1(info))?.let { family ->
                    findings += Finding(Kind.KNOWN_MALWARE, pkg, label(pkg), detail = family)
                }
            }
            val requested = info.requestedPermissions ?: continue
            val flags = info.requestedPermissionsFlags
            val readsSms = requested.indices.any { i ->
                requested[i] in SMS_PERMISSIONS &&
                    flags != null && flags[i] and PackageInfo.REQUESTED_PERMISSION_GRANTED != 0
            }
            if (readsSms) findings += Finding(Kind.SMS_READER, pkg, label(pkg))
            if (android.Manifest.permission.SYSTEM_ALERT_WINDOW in requested ||
                android.Manifest.permission.REQUEST_INSTALL_PACKAGES in requested
            ) opCandidates += info
        }

        var appOpsChecked = true
        if (withAppOps && opCandidates.isNotEmpty()) {
            appOpsChecked = checkAppOps(context, opCandidates) { kind, pkg ->
                findings += Finding(kind, pkg, label(pkg))
            }
        } else if (!withAppOps) {
            appOpsChecked = false
        }

        // רשת
        val certs = userCertificates()
        if (certs == null) unchecked += "certificates"
        certs.orEmpty().forEach { (alias, name) -> findings += Finding(Kind.USER_CA, alias, name) }
        val proxy = runCatching { Settings.Global.getString(cr, Settings.Global.HTTP_PROXY) }.getOrNull()
        if (!proxy.isNullOrBlank() && proxy != ":0") findings += Finding(Kind.PROXY, proxy, proxy)
        if (runCatching { Settings.Global.getInt(cr, "adb_wifi_enabled", 0) }.getOrDefault(0) == 1) {
            findings += Finding(Kind.ADB_WIFI, "adb_wifi", "")
        }
        if (vpnActive(context)) findings += Finding(Kind.VPN, "vpn", "")

        return Result(
            findings.distinctBy { it.key }.sortedByDescending { it.kind.severity },
            packages.size,
            appOpsChecked,
            unchecked,
            db?.date.orEmpty(),
            db?.size ?: 0,
        )
    }

    /** מחזיר false אם חלק מהאפליקציות לא נבדקו. */
    private fun checkAppOps(context: Context, candidates: List<PackageInfo>, add: (Kind, String) -> Unit): Boolean {
        val appOps: AppOpsManager? = context.getSystemService(AppOpsManager::class.java)
        val blocked = ArrayList<PackageInfo>()
        for (info in candidates) {
            val uid = info.applicationInfo?.uid ?: continue
            val requested = info.requestedPermissions ?: continue
            try {
                if (appOps == null) throw IllegalStateException("no AppOpsManager")
                if (android.Manifest.permission.SYSTEM_ALERT_WINDOW in requested &&
                    appOps.unsafeCheckOpNoThrow(OP_OVERLAY, uid, info.packageName) == AppOpsManager.MODE_ALLOWED
                ) add(Kind.OVERLAY, info.packageName)
                if (android.Manifest.permission.REQUEST_INSTALL_PACKAGES in requested &&
                    appOps.unsafeCheckOpNoThrow(OP_INSTALL, uid, info.packageName) == AppOpsManager.MODE_ALLOWED
                ) add(Kind.INSTALLER, info.packageName)
            } catch (e: Exception) {
                // אם אנדרואיד חוסם קריאת הרשאות של uid אחר (SecurityException מהשרת) - עוברים ל-root
                blocked += info
            }
        }
        if (blocked.isEmpty()) return true

        val safe = blocked.map { it.packageName }.filter { RootShell.isSafeToken(it) }
        if (safe.isEmpty()) return false
        // סשן su אחד לכל המועמדות. כל חבילה עוברת RootShell.quote, והסמן מפריד בין הפלטים
        val commands = safe.flatMap { pkg ->
            val q = RootShell.quote(pkg)
            listOf("echo '@@'$q", "appops get $q SYSTEM_ALERT_WINDOW", "appops get $q REQUEST_INSTALL_PACKAGES")
        }
        val result = runCatching { RootShell.run(commands) }.getOrNull()
        if (result == null || result.isShellUnavailable) return false
        var current: String? = null
        for (line in result.output.lineSequence()) {
            val t = line.trim()
            when {
                t.startsWith("@@") -> current = t.removePrefix("@@").takeIf { it in safe }
                current == null -> Unit
                t.startsWith("SYSTEM_ALERT_WINDOW: allow") -> add(Kind.OVERLAY, current!!)
                t.startsWith("REQUEST_INSTALL_PACKAGES: allow") -> add(Kind.INSTALLER, current!!)
            }
        }
        return safe.size == blocked.size
    }

    /** תעודות CA שהמשתמש (או מישהו אחר) התקין ידנית - מה שמאפשר לפענח תעבורת HTTPS. null = לא נקרא. */
    private fun userCertificates(): List<Pair<String, String>>? = runCatching {
        val store = KeyStore.getInstance("AndroidCAStore").apply { load(null) }
        store.aliases().toList().filter { it.startsWith("user:") }.map { alias ->
            val cert = store.getCertificate(alias) as? X509Certificate
            val name = cert?.subjectX500Principal?.name
                ?.split(',')
                ?.firstOrNull { it.trim().startsWith("CN=") }
                ?.substringAfter("CN=")
                ?: alias
            alias to name
        }
    }.getOrElse { Log.w(TAG, "CA store", it); null }

    private fun vpnActive(context: Context): Boolean = runCatching {
        val cm = context.getSystemService(ConnectivityManager::class.java) ?: return false
        @Suppress("DEPRECATION")
        cm.allNetworks.any { cm.getNetworkCapabilities(it)?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true }
    }.getOrDefault(false)
}
