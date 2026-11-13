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
import java.security.KeyStore
import java.security.cert.X509Certificate

/**
 * סורק איומים: מחפש את הדרכים שבהן נוזקות לאנדרואיד משתלטות על טלפון בפועל.
 *
 * זה לא אנטי-וירוס ואין כאן חתימות של נוזקות. הסורק בודק יכולות מסוכנות שהופעלו
 * לאפליקציה זרה - כל אחת מהן היא הצעד שנוזקות בנקאיות וריגול עושות כדי לפעול:
 *  - שירות נגישות: קורא את כל המסך ומקשים, כולל הקוד של מסך הנעילה.
 *  - מאזין התראות: קורא הודעות וקודי אימות שמגיעים ב-SMS.
 *  - מנהל מכשיר: נועל, מוחק ומונע את הסרת האפליקציה.
 *  - קריאת SMS, חלון מעל אפליקציות (מסכי כניסה מזויפים), התקנת אפליקציות.
 *  - רשת: תעודת CA שהותקנה ידנית, שרת מתווך גלובלי, VPN, ניפוי באגים דרך Wi-Fi.
 *
 * "זרה" = לא אפליקציית מערכת ולא חתומה במפתח של FutureOS. האמון לפי חתימה ולא לפי
 * שם: כל אחד יכול לקרוא לחבילה com.future.something.
 *
 * רץ ב-FutureUI כי רק לה יש QUERY_ALL_PACKAGES - בלי זה רשימת האפליקציות מסוננת
 * והסריקה הייתה מדווחת "נקי" בטעות.
 */
object ThreatScanner {
    private const val TAG = "ThreatScanner"

    /** חומרה: 3 קריטי, 2 גבוה, 1 דורש בדיקה. */
    enum class Kind(val severity: Int) {
        ACCESSIBILITY(3),
        DEVICE_ADMIN(2),
        NOTIFICATION_LISTENER(2),
        USER_CA(2),
        PROXY(2),
        ADB_WIFI(2),
        SMS_READER(1),
        OVERLAY(1),
        INSTALLER(1),
        VPN(1),
    }

    data class Finding(val kind: Kind, val target: String, val label: String) {
        val key: String get() = "${kind.name}:$target"
    }

    data class Result(
        val findings: List<Finding>,
        val scannedApps: Int,
        /** false = לא היה אפשר לבדוק הרשאות חלון צף והתקנה (אין root). */
        val appOpsChecked: Boolean,
    )

    private const val OP_OVERLAY = "android:system_alert_window"
    private const val OP_INSTALL = "android:request_install_packages"
    private val SMS_PERMISSIONS = setOf(android.Manifest.permission.READ_SMS, android.Manifest.permission.RECEIVE_SMS)

    /**
     * @param withAppOps בדיקת חלון צף/התקנה. אנדרואיד לא נותן לאפליקציה לקרוא את
     *   ההרשאות האלה של אפליקציה אחרת, אז כשהבדיקה הרגילה נחסמת עוברים ל-root
     *   (סשן su אחד לכל הסריקה). השומר ברקע לא מבקש את זה, כדי לא להריץ su שוב ושוב.
     */
    fun scan(context: Context, withAppOps: Boolean): Result {
        val pm = context.packageManager
        val self = context.packageName
        val cr = context.contentResolver
        val findings = ArrayList<Finding>()
        val trustCache = HashMap<String, Boolean?>()

        // null = לא מותקנת (רשומה ישנה בהגדרות), true = מהימנה, false = זרה
        fun trust(pkg: String): Boolean? = trustCache.getOrPut(pkg) {
            val ai = runCatching { pm.getApplicationInfo(pkg, 0) }.getOrNull() ?: return@getOrPut null
            pkg == self ||
                ai.flags and ApplicationInfo.FLAG_SYSTEM != 0 ||
                runCatching { pm.checkSignatures(self, pkg) == PackageManager.SIGNATURE_MATCH }.getOrDefault(false)
        }

        fun label(pkg: String): String = runCatching {
            pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
        }.getOrDefault(pkg)

        fun components(setting: String): Set<String> =
            (runCatching { Settings.Secure.getString(cr, setting) }.getOrNull() ?: "")
                .split(':')
                .mapNotNull { ComponentName.unflattenFromString(it)?.packageName }
                .toSet()

        for (pkg in components(Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)) {
            if (trust(pkg) == false) findings += Finding(Kind.ACCESSIBILITY, pkg, label(pkg))
        }
        for (pkg in components("enabled_notification_listeners")) {
            if (trust(pkg) == false) findings += Finding(Kind.NOTIFICATION_LISTENER, pkg, label(pkg))
        }
        runCatching {
            val dpm = context.getSystemService(DevicePolicyManager::class.java)
            dpm?.activeAdmins.orEmpty().map { it.packageName }.toSet().forEach { pkg ->
                if (trust(pkg) == false) findings += Finding(Kind.DEVICE_ADMIN, pkg, label(pkg))
            }
        }.onFailure { Log.w(TAG, "device admins", it) }

        // אפליקציות: קריאת SMS, ומועמדות לבדיקת חלון צף/התקנה
        val packages: List<PackageInfo> = runCatching {
            pm.getInstalledPackages(PackageManager.GET_PERMISSIONS)
        }.getOrElse { Log.w(TAG, "installed packages", it); emptyList() }
        val opCandidates = ArrayList<PackageInfo>()
        for (info in packages) {
            val pkg = info.packageName
            val requested = info.requestedPermissions ?: continue
            if (trust(pkg) != false) continue
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
        userCertificates().forEach { (alias, name) -> findings += Finding(Kind.USER_CA, alias, name) }
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

    /** תעודות CA שהמשתמש (או מישהו אחר) התקין ידנית - מה שמאפשר לפענח תעבורת HTTPS. */
    private fun userCertificates(): List<Pair<String, String>> = runCatching {
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
    }.getOrElse { Log.w(TAG, "CA store", it); emptyList() }

    private fun vpnActive(context: Context): Boolean = runCatching {
        val cm = context.getSystemService(ConnectivityManager::class.java) ?: return false
        @Suppress("DEPRECATION")
        cm.allNetworks.any { cm.getNetworkCapabilities(it)?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true }
    }.getOrDefault(false)
}
