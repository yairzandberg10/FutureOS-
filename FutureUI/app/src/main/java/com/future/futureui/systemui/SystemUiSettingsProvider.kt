package com.future.futureui.systemui

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import com.future.futureui.statusbar.logic.StatusBarLayoutManager

/**
 * מקור אמת יחיד להגדרות שורת המצב, נגיש מבחוץ - כדי שאפליקציית
 * ההגדרות תוכל לשלוט בהן ישירות במקום להפנות את המשתמש ל-FutureUI עצמה.
 * כותב לאותם shared_prefs שהשירותים ב-FutureUI כבר קוראים מהם (status_bar_prefs),
 * אז שינוי מבחוץ מתעדכן אצלם בלולאת הפולינג הרגילה שלהם -
 * בלי צורך במנגנון סנכרון נוסף.
 */
class SystemUiSettingsProvider : ContentProvider() {

    companion object {
        const val AUTHORITY = "com.future.futureui.systemui"
        val CONTENT_URI: Uri = Uri.parse("content://$AUTHORITY/settings")

        const val COL_SHOW_BATTERY = "show_battery"
        const val COL_SHOW_BLUETOOTH = "show_bluetooth"
        const val COL_USE_24_HOUR_CLOCK = "use_24_hour_clock"
        const val COL_SUPPRESS_SYSTEM_BARS = "suppress_system_bars"
        const val COL_BAR_STYLE = StatusBarLayoutManager.KEY_BAR_STYLE
        // קריאה בלבד: המכשיר נעול (גם כשמסך הנעילה פינה מקום לשיחה). החייגן בודק
        // את זה כדי לא לחשוף יומן/אנשי קשר מתוך שיחה במכשיר נעול.
        const val COL_DEVICE_SECURED = "device_secured"

        // content://…/security - מצב האבטחה למרכז האבטחה בהגדרות. קריאה בלבד, ורק
        // דגלים ומונים: לא הקוד, לא ה-HMAC שלו ולא אורכו.
        const val PATH_SECURITY = "security"
        val SECURITY_COLUMNS = arrayOf(
            "lock_enabled", "has_pin", "face_enabled", "notif_privacy",
            "auto_lock_ms", "failed_week", "last_failed_at", "lockout_ms",
        )

        // content://…/threats - סריקת איומים (ThreatScanner) ומצב ההגנה (ThreatGuard). שורות מיוחדות:
        //   kind="_meta": target = מספר האפליקציות שנסרקו, label = 1 אם נבדקו הרשאות חלון צף/התקנה
        //   kind="_policy": label = 1 אם חסימה אוטומטית פועלת
        //   kind="_db": target = תאריך מאגר הנוזקות, label = מספר הסימנים בו
        //   kind="_unchecked": target = מקור שלא נקרא (לא "נקי" - "לא נבדק")
        // status: open / allowed / blocked. detail: לנוזקה ידועה - שם המשפחה.
        const val PATH_THREATS = "threats"
        val THREAT_COLUMNS = arrayOf("kind", "target", "label", "severity", "status", "detail")

        // call(): פעולות של מרכז האבטחה. arg = מפתח הממצא (KIND:target) או "1"/"0".
        const val METHOD_THREAT_BLOCK = "threat_block"
        const val METHOD_THREAT_ALLOW = "threat_allow"
        const val METHOD_THREAT_AUTOBLOCK = "threat_autoblock"
    }

    private lateinit var statusBarPrefs: android.content.SharedPreferences

    override fun onCreate(): Boolean {
        statusBarPrefs = context!!.getSharedPreferences("status_bar_prefs", Context.MODE_PRIVATE)
        return true
    }

    /**
     * הרשאת החתימה של הספק נאכפת על ידי המערכת - אבל ב-ROM של הטלפון השוואת החתימות
     * מחזירה "תואם" לכל אפליקציה (ראו SameSigner). אז בודקים כאן בעצמנו: רק אפליקציה
     * בחתימה של FutureOS, FutureUI עצמה או root (adb עם su).
     */
    private fun enforceCaller() {
        val uid = android.os.Binder.getCallingUid()
        if (uid == android.os.Process.myUid() || uid == 0) return
        val ok = android.os.Binder.clearCallingIdentity().let { token ->
            try { com.future.sharednav.security.SameSigner.matchesUid(context!!, uid) }
            finally { android.os.Binder.restoreCallingIdentity(token) }
        }
        if (!ok) throw SecurityException("not a FutureOS app")
    }

    override fun query(uri: Uri, projection: Array<out String>?, selection: String?, selectionArgs: Array<out String>?, sortOrder: String?): Cursor {
        enforceCaller()
        if (uri.lastPathSegment == PATH_SECURITY) return securityStatus()
        if (uri.lastPathSegment == PATH_THREATS) return threats()
        val cursor = MatrixCursor(arrayOf(COL_SHOW_BATTERY, COL_SHOW_BLUETOOTH, COL_USE_24_HOUR_CLOCK, COL_SUPPRESS_SYSTEM_BARS, COL_BAR_STYLE, COL_DEVICE_SECURED))
        cursor.addRow(
            arrayOf(
                if (statusBarPrefs.getBoolean(COL_SHOW_BATTERY, true)) 1 else 0,
                if (statusBarPrefs.getBoolean(COL_SHOW_BLUETOOTH, true)) 1 else 0,
                // ברירת המחדל מהגדרת 12/24 של המערכת, כמו בשורת המצב עצמה (F18)
                if (StatusBarLayoutManager(context!!).getUse24HourClock()) 1 else 0,
                if (statusBarPrefs.getBoolean(COL_SUPPRESS_SYSTEM_BARS, true)) 1 else 0,
                statusBarPrefs.getString(COL_BAR_STYLE, StatusBarLayoutManager.STYLE_CLASSIC),
                // גם נעילת המערכת עצמה (KeyguardManager), לא רק הדגל בזיכרון שמתאפס עם התהליך (F14)
                if (com.future.futureui.utils.FutureUIState.isSecured || systemLocked()) 1 else 0,
            )
        )
        return cursor
    }

    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int {
        enforceCaller()
        if (values == null) return 0
        statusBarPrefs.edit().apply {
            if (values.containsKey(COL_SHOW_BATTERY)) putBoolean(COL_SHOW_BATTERY, values.getAsInteger(COL_SHOW_BATTERY) == 1)
            if (values.containsKey(COL_SHOW_BLUETOOTH)) putBoolean(COL_SHOW_BLUETOOTH, values.getAsInteger(COL_SHOW_BLUETOOTH) == 1)
            if (values.containsKey(COL_USE_24_HOUR_CLOCK)) putBoolean(COL_USE_24_HOUR_CLOCK, values.getAsInteger(COL_USE_24_HOUR_CLOCK) == 1)
            if (values.containsKey(COL_SUPPRESS_SYSTEM_BARS)) putBoolean(COL_SUPPRESS_SYSTEM_BARS, values.getAsInteger(COL_SUPPRESS_SYSTEM_BARS) == 1)
            values.getAsString(COL_BAR_STYLE)?.takeIf { it in StatusBarLayoutManager.STYLES }?.let { putString(COL_BAR_STYLE, it) }
        }.apply()
        context?.contentResolver?.notifyChange(CONTENT_URI, null)
        return 1
    }

    private fun securityStatus(): Cursor {
        val lock = com.future.futureui.lockscreen.logic.LockSettings(context!!)
        val log = com.future.futureui.lockscreen.logic.SecurityLog(context!!)
        return MatrixCursor(SECURITY_COLUMNS).apply {
            addRow(
                arrayOf<Any>(
                    if (lock.enabled) 1 else 0,
                    if (lock.pin.hasPin()) 1 else 0,
                    if (lock.faceEnabled) 1 else 0,
                    lock.notificationPrivacy,
                    lock.autoLockDelayMs,
                    log.failuresLastWeek(),
                    log.lastFailureAt(),
                    lock.pin.remainingLockoutMs(),
                )
            )
        }
    }

    private fun threats(): Cursor {
        val ctx = context!!
        val guard = com.future.futureui.security.ThreatGuard
        val result = com.future.futureui.security.ThreatScanner.scan(ctx, withAppOps = true)
        val allowed = guard.allowed(ctx)
        val keys = result.findings.map { it.key }.toSet()
        return MatrixCursor(THREAT_COLUMNS).apply {
            addRow(arrayOf<Any>("_meta", result.scannedApps.toString(), if (result.appOpsChecked) "1" else "0", 0, "", ""))
            addRow(arrayOf<Any>("_policy", "", if (guard.autoBlock(ctx)) "1" else "0", 0, "", ""))
            addRow(arrayOf<Any>("_db", result.dbDate, result.dbSize.toString(), 0, "", ""))
            result.unchecked.forEach { addRow(arrayOf<Any>("_unchecked", it, "", 0, "", "")) }
            result.findings.forEach { f ->
                addRow(arrayOf<Any>(f.kind.name, f.target, f.label, f.kind.severity, guard.status(allowed, f), f.detail))
            }
            // מה שנחסם כבר לא מופיע בסריקה (היכולת בוטלה) - מוצג כדי שאפשר יהיה להחזיר
            guard.blocked(ctx).values.filter { it.key !in keys }.forEach { e ->
                addRow(arrayOf<Any>(e.kind.name, e.target, e.label, e.kind.severity, guard.STATUS_BLOCKED, ""))
            }
        }
    }

    override fun call(method: String, arg: String?, extras: android.os.Bundle?): android.os.Bundle? {
        enforceCaller()
        val ctx = context!!
        val guard = com.future.futureui.security.ThreatGuard
        val ok = when (method) {
            METHOD_THREAT_BLOCK -> arg != null && guard.block(ctx, arg)
            METHOD_THREAT_ALLOW -> arg != null && guard.allow(ctx, arg)
            METHOD_THREAT_AUTOBLOCK -> guard.setAutoBlock(ctx, arg == "1")
            else -> return null
        }
        return android.os.Bundle().apply { putBoolean("ok", ok) }
    }

    private fun systemLocked(): Boolean = runCatching {
        (context?.getSystemService(Context.KEYGUARD_SERVICE) as? android.app.KeyguardManager)?.isDeviceLocked == true
    }.getOrDefault(false)

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0
    override fun getType(uri: Uri): String? = "vnd.android.cursor.item/vnd.$AUTHORITY.settings"
}
