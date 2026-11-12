package com.future.settings.theme

import android.content.Context
import android.net.Uri
import com.future.sharednav.systemui.SystemUiTarget

/**
 * תוצאות סורק האיומים של FutureUI (content://…/threats). הסריקה עצמה רצה ב-FutureUI
 * כי רק לה יש QUERY_ALL_PACKAGES. קריאה חוסמת (סריקה + אולי סשן root) - רק מ-IO.
 */
data class ThreatReport(
    val findings: List<Threat>,
    val scannedApps: Int,
    val appOpsChecked: Boolean,
) {
    val worst: Int get() = findings.maxOfOrNull { it.severity } ?: 0
}

/** kind: שם הסוג ב-ThreatScanner.Kind. severity: 3 קריטי, 2 גבוה, 1 דורש בדיקה. */
data class Threat(val kind: String, val target: String, val label: String, val severity: Int)

object ThreatClient {
    private val URI = Uri.parse("content://${SystemUiTarget.SETTINGS_AUTHORITY}/threats")

    /** null = הסריקה לא זמינה (FutureUI ישנה, לא מאומתת, או שגיאה). */
    fun scan(context: Context): ThreatReport? {
        if (!com.future.sharednav.systemui.TrustedProviders.isTrusted(context, SystemUiTarget.SETTINGS_AUTHORITY)) return null
        return try {
            context.contentResolver.query(URI, null, null, null, null)?.use { c ->
                val kindCol = c.getColumnIndex("kind")
                val targetCol = c.getColumnIndex("target")
                val labelCol = c.getColumnIndex("label")
                val sevCol = c.getColumnIndex("severity")
                // FutureUI ישנה בלי הנתיב מחזירה את הגדרות שורת המצב
                if (kindCol < 0 || targetCol < 0 || labelCol < 0 || sevCol < 0) return@use null
                var scanned = 0
                var appOps = false
                var sawMeta = false
                val list = ArrayList<Threat>()
                while (c.moveToNext()) {
                    val kind = c.getString(kindCol) ?: continue
                    if (kind == "_meta") {
                        sawMeta = true
                        scanned = c.getString(targetCol)?.toIntOrNull() ?: 0
                        appOps = c.getString(labelCol) == "1"
                    } else {
                        list += Threat(kind, c.getString(targetCol) ?: "", c.getString(labelCol) ?: "", c.getInt(sevCol))
                    }
                }
                if (sawMeta) ThreatReport(list, scanned, appOps) else null
            }
        } catch (e: Exception) {
            android.util.Log.w("ThreatClient", "threat scan failed", e)
            null
        }
    }
}
