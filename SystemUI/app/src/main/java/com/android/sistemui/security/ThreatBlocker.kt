package com.android.sistemui.security

import android.content.ComponentName
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.provider.Settings
import android.util.Log
import com.future.sharednav.root.RootShell
import com.future.sharednav.security.SameSigner
import org.json.JSONArray
import org.json.JSONObject

/**
 * חוסם יכולת מסוכנת אחת של אפליקציה זרה, ויודע להחזיר אותה בדיוק כמו שהייתה.
 *
 * חוסמים את היכולת ולא את האפליקציה: לא מסירים, לא משביתים ולא נוגעים בנתונים שלה.
 * כל חסימה נשמרת עם מה שצריך כדי לבטל אותה (הרכיבים המדויקים, ההרשאות שהיו, הערך הקודם).
 *
 * גבולות קשיחים, גם אם מישהו יבקש אחרת:
 *  - אף פעם לא אפליקציה שלנו (אותו חותם) ולא אפליקציית מערכת. מסך הנעילה של FutureOS
 *    יושב באותה רשימת שירותי נגישות, אז מסירים רק את הרכיבים של החבילה הזרה.
 *  - לא נוגעים בניפוי באגים (USB או Wi-Fi), במנהלי מכשיר ובתעודות: אין להם חסימה הפיכה.
 *  - שמות חבילה עוברים RootShell.isSafeToken, וכל ערך בפקודה עובר RootShell.quote.
 */
object ThreatBlocker {
    private const val TAG = "ThreatBlocker"
    private const val A11Y = Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
    private const val LISTENERS = "enabled_notification_listeners"
    private val SMS_PERMISSIONS = listOf(android.Manifest.permission.READ_SMS, android.Manifest.permission.RECEIVE_SMS)

    /** חסימה שבוצעה. prior: לשרת מתווך - הערך הקודם; ל-SMS - ההרשאות שהיו מאושרות. */
    data class Entry(
        val kind: ThreatScanner.Kind,
        val target: String,
        val label: String,
        val components: List<String>,
        val prior: List<String>,
        val at: Long,
    ) {
        val key: String get() = "${kind.name}:$target"

        fun toJson(): JSONObject = JSONObject()
            .put("kind", kind.name).put("target", target).put("label", label)
            .put("components", JSONArray(components)).put("prior", JSONArray(prior)).put("at", at)

        companion object {
            fun fromJson(o: JSONObject): Entry? = runCatching {
                Entry(
                    ThreatScanner.Kind.valueOf(o.getString("kind")),
                    o.getString("target"),
                    o.optString("label"),
                    o.optJSONArray("components").strings(),
                    o.optJSONArray("prior").strings(),
                    o.optLong("at"),
                )
            }.getOrNull()

            private fun JSONArray?.strings(): List<String> =
                if (this == null) emptyList() else (0 until length()).map { getString(it) }
        }
    }

    /** null = לא נחסם (לא ניתן לחסום, אפליקציה מוגנת, אין root, או שהפקודה נכשלה). */
    fun block(context: Context, f: ThreatScanner.Finding): Entry? {
        if (!f.kind.blockable) return null
        val cr = context.contentResolver
        val now = System.currentTimeMillis()
        return when (f.kind) {
            ThreatScanner.Kind.ACCESSIBILITY -> {
                if (!foreignPackage(context, f.target)) return null
                val parts = read(context, A11Y) ?: return null
                val removed = parts.filter { owner(it) == f.target }
                if (removed.isEmpty()) return null
                if (!writeList(A11Y, parts - removed.toSet())) return null
                val left = read(context, A11Y)?.filter { owner(it) == f.target }
                if (left == null || left.isNotEmpty()) return null
                Entry(f.kind, f.target, f.label, removed, emptyList(), now)
            }
            ThreatScanner.Kind.NOTIFICATION_LISTENER -> {
                if (!foreignPackage(context, f.target)) return null
                val removed = read(context, LISTENERS)?.filter { owner(it) == f.target }.orEmpty()
                if (removed.isEmpty()) return null
                val ok = root(removed.map { "cmd notification disallow_listener ${RootShell.quote(it)}" })
                val left = read(context, LISTENERS)?.filter { owner(it) == f.target }
                if (!ok || left == null || left.isNotEmpty()) return null
                Entry(f.kind, f.target, f.label, removed, emptyList(), now)
            }
            ThreatScanner.Kind.SMS_READER -> {
                if (!foreignPackage(context, f.target)) return null
                val pm = context.packageManager
                val granted = SMS_PERMISSIONS.filter { pm.checkPermission(it, f.target) == PackageManager.PERMISSION_GRANTED }
                if (granted.isEmpty()) return null
                val q = RootShell.quote(f.target)
                if (!root(granted.map { "pm revoke $q $it" })) return null
                Entry(f.kind, f.target, f.label, emptyList(), granted, now)
            }
            ThreatScanner.Kind.OVERLAY, ThreatScanner.Kind.INSTALLER -> {
                if (!foreignPackage(context, f.target)) return null
                val op = if (f.kind == ThreatScanner.Kind.OVERLAY) "SYSTEM_ALERT_WINDOW" else "REQUEST_INSTALL_PACKAGES"
                if (!root(listOf("appops set ${RootShell.quote(f.target)} $op ignore"))) return null
                Entry(f.kind, f.target, f.label, emptyList(), emptyList(), now)
            }
            ThreatScanner.Kind.PROXY -> {
                val prior = runCatching { Settings.Global.getString(cr, Settings.Global.HTTP_PROXY) }.getOrNull()
                if (prior.isNullOrBlank() || prior == ":0") return null
                if (!root(listOf("settings put global http_proxy :0"))) return null
                Entry(f.kind, f.target, f.label, emptyList(), listOf(prior), now)
            }
            else -> null
        }
    }

    /** מחזיר את מה שנחסם. false = לא הצליח (אין root, או שהאפליקציה כבר לא מותקנת). */
    fun restore(context: Context, e: Entry): Boolean {
        val installed = runCatching { context.packageManager.getApplicationInfo(e.target, 0) }.isSuccess
        return when (e.kind) {
            ThreatScanner.Kind.ACCESSIBILITY -> {
                if (!installed || !foreignPackage(context, e.target)) return false
                val parts = read(context, A11Y) ?: return false
                val comps = e.components.filter { owner(it) == e.target && it !in parts }
                comps.isEmpty() || writeList(A11Y, parts + comps)
            }
            ThreatScanner.Kind.NOTIFICATION_LISTENER -> {
                if (!installed || !foreignPackage(context, e.target)) return false
                val comps = e.components.filter { owner(it) == e.target }
                comps.isEmpty() || root(comps.map { "cmd notification allow_listener ${RootShell.quote(it)}" })
            }
            ThreatScanner.Kind.SMS_READER -> {
                if (!installed || !foreignPackage(context, e.target)) return false
                val q = RootShell.quote(e.target)
                val perms = e.prior.filter { it in SMS_PERMISSIONS }
                perms.isEmpty() || root(perms.map { "pm grant $q $it" })
            }
            ThreatScanner.Kind.OVERLAY, ThreatScanner.Kind.INSTALLER -> {
                if (!installed || !foreignPackage(context, e.target)) return false
                val op = if (e.kind == ThreatScanner.Kind.OVERLAY) "SYSTEM_ALERT_WINDOW" else "REQUEST_INSTALL_PACKAGES"
                root(listOf("appops set ${RootShell.quote(e.target)} $op allow"))
            }
            ThreatScanner.Kind.PROXY -> {
                val prior = e.prior.firstOrNull() ?: return false
                root(listOf("settings put global http_proxy ${RootShell.quote(prior)}"))
            }
            else -> false
        }
    }

    /** רק חבילה זרה, מותקנת, עם שם תקין - אף פעם לא שלנו או של המערכת. */
    private fun foreignPackage(context: Context, pkg: String): Boolean {
        if (!RootShell.isSafeToken(pkg) || pkg == context.packageName) return false
        val ai = runCatching { context.packageManager.getApplicationInfo(pkg, 0) }.getOrNull() ?: return false
        if (ai.flags and ApplicationInfo.FLAG_SYSTEM != 0) return false
        // בלי חותמים שלנו אין דרך לדעת מה "שלנו" - לא חוסמים כלום
        if (SameSigner.self(context).isEmpty()) return false
        return !SameSigner.matches(context, pkg)
    }

    private fun owner(flat: String): String? = ComponentName.unflattenFromString(flat)?.packageName

    private fun read(context: Context, key: String): List<String>? =
        runCatching { Settings.Secure.getString(context.contentResolver, key) ?: "" }.getOrNull()
            ?.split(':')?.filter { it.isNotBlank() }

    private fun writeList(key: String, parts: List<String>): Boolean {
        val cmd = if (parts.isEmpty()) "settings delete secure $key"
        else "settings put secure $key ${RootShell.quote(parts.joinToString(":"))}"
        return root(listOf(cmd))
    }

    private fun root(commands: List<String>): Boolean {
        val r = runCatching { RootShell.run(commands.joinToString(" && ")) }.getOrNull()
        if (r == null || !r.success) Log.w(TAG, "root failed: ${r?.exitCode} ${r?.error}")
        return r?.success == true
    }
}
