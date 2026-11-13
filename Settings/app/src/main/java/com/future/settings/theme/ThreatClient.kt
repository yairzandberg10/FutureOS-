package com.future.settings.theme

import android.content.Context
import android.net.Uri
import com.future.sharednav.systemui.SystemUiTarget

/**
 * תוצאות סורק האיומים של FutureUI (content://…/threats) והפעולות של ההגנה (חסום / אשר /
 * חסימה אוטומטית). הסריקה עצמה רצה ב-FutureUI כי רק לה יש QUERY_ALL_PACKAGES.
 * כל הקריאות חוסמות (סריקה + אולי סשן root) - רק מ-IO.
 */
data class ThreatReport(
    val findings: List<Threat>,
    val scannedApps: Int,
    val appOpsChecked: Boolean,
    val autoBlock: Boolean,
    val dbDate: String,
    val dbSize: Int,
    /** מקורות שלא נקראו בסריקה - "לא נבדק", לא "נקי". */
    val unchecked: List<String>,
) {
    /** רק ממצאים פתוחים: מה שנחסם או שהמשתמש אישר לא מוריד את רמת האבטחה. */
    val open: List<Threat> get() = findings.filter { it.status == Threat.OPEN }
    val worst: Int get() = open.maxOfOrNull { it.severity } ?: 0
}

/**
 * kind: שם הסוג ב-ThreatScanner.Kind. severity: 3 קריטי, 2 גבוה, 1 דורש בדיקה.
 * status: open / allowed / blocked. detail: לנוזקה ידועה - שם המשפחה במאגר.
 */
data class Threat(
    val kind: String,
    val target: String,
    val label: String,
    val severity: Int,
    val status: String,
    val detail: String,
) {
    val key: String get() = "$kind:$target"
    /** אפשר לחסום את היכולת עצמה (ולהחזיר אותה). חייב להתאים ל-ThreatScanner.Kind.blockable. */
    val blockable: Boolean get() = kind in BLOCKABLE

    companion object {
        const val OPEN = "open"
        const val ALLOWED = "allowed"
        const val BLOCKED = "blocked"
        private val BLOCKABLE = setOf("ACCESSIBILITY", "NOTIFICATION_LISTENER", "PROXY", "SMS_READER", "OVERLAY", "INSTALLER")
    }
}

object ThreatClient {
    private val BASE = Uri.parse("content://${SystemUiTarget.SETTINGS_AUTHORITY}")
    private val URI = Uri.parse("content://${SystemUiTarget.SETTINGS_AUTHORITY}/threats")

    private fun trusted(context: Context) =
        com.future.sharednav.systemui.TrustedProviders.isTrusted(context, SystemUiTarget.SETTINGS_AUTHORITY)

    /** null = הסריקה לא זמינה (FutureUI ישנה, לא מאומתת, או שגיאה). */
    fun scan(context: Context): ThreatReport? {
        if (!trusted(context)) return null
        return try {
            context.contentResolver.query(URI, null, null, null, null)?.use { c ->
                val kindCol = c.getColumnIndex("kind")
                val targetCol = c.getColumnIndex("target")
                val labelCol = c.getColumnIndex("label")
                val sevCol = c.getColumnIndex("severity")
                val statusCol = c.getColumnIndex("status")
                val detailCol = c.getColumnIndex("detail")
                // FutureUI ישנה בלי הנתיב מחזירה את הגדרות שורת המצב
                if (kindCol < 0 || targetCol < 0 || labelCol < 0 || sevCol < 0) return@use null
                var scanned = 0
                var appOps = false
                var sawMeta = false
                var autoBlock = false
                var dbDate = ""
                var dbSize = 0
                val unchecked = ArrayList<String>()
                val list = ArrayList<Threat>()
                while (c.moveToNext()) {
                    val kind = c.getString(kindCol) ?: continue
                    val target = c.getString(targetCol) ?: ""
                    val label = c.getString(labelCol) ?: ""
                    when (kind) {
                        "_meta" -> {
                            sawMeta = true
                            scanned = target.toIntOrNull() ?: 0
                            appOps = label == "1"
                        }
                        "_policy" -> autoBlock = label == "1"
                        "_db" -> { dbDate = target; dbSize = label.toIntOrNull() ?: 0 }
                        "_unchecked" -> unchecked += target
                        else -> list += Threat(
                            kind, target, label, c.getInt(sevCol),
                            if (statusCol >= 0) c.getString(statusCol) ?: Threat.OPEN else Threat.OPEN,
                            if (detailCol >= 0) c.getString(detailCol) ?: "" else "",
                        )
                    }
                }
                // FutureUI ישנה (בלי עמודת status) לא יודעת על מאגר נוזקות - זה "לא נבדק"
                if (statusCol < 0) unchecked += "malware_db"
                if (sawMeta) ThreatReport(list, scanned, appOps, autoBlock, dbDate, dbSize, unchecked) else null
            }
        } catch (e: Exception) {
            android.util.Log.w("ThreatClient", "threat scan failed", e)
            null
        }
    }

    fun block(context: Context, key: String): Boolean = call(context, "threat_block", key)
    fun allow(context: Context, key: String): Boolean = call(context, "threat_allow", key)
    fun setAutoBlock(context: Context, on: Boolean): Boolean = call(context, "threat_autoblock", if (on) "1" else "0")

    private fun call(context: Context, method: String, arg: String): Boolean {
        if (!trusted(context)) return false
        return try {
            context.contentResolver.call(BASE, method, arg, null)?.getBoolean("ok") == true
        } catch (e: Exception) {
            android.util.Log.w("ThreatClient", "$method failed", e)
            false
        }
    }
}
