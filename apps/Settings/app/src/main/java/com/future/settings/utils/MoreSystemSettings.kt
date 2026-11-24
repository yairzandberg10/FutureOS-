package com.future.settings.utils

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import com.future.sharednav.i18n.tr
import com.future.sharednav.root.RootShell

/**
 * הגדרות שחסרו כדי שההגדרות ירגישו כמו של טלפון מלא: רשתות Wi-Fi, פירוט אחסון,
 * כל האפליקציות (לא רק של FutureOS), אזור זמן ושעה ידנית.
 *
 * הכל עובר דרך root (cmd wifi / du / pm / service call alarm), כמו שאר ההגדרות
 * באפליקציה - בלי להעביר את המשתמש להגדרות של אנדרואיד, שהן מסך מגע שיוצא מ-FutureOS.
 */
object MoreSystemSettings {

    // ---------------------------------------------------------------------
    // Wi-Fi (cmd wifi, אנדרואיד 11 ומעלה)
    // ---------------------------------------------------------------------

    data class WifiStatus(val enabled: Boolean, val connectedSsid: String?)
    data class WifiNetwork(val ssid: String, val rssi: Int, val security: String) {
        /** open / owe / wpa2 / wpa3 - הערכים ש-`cmd wifi connect-network` מקבל. eap = לא נתמך. */
        val secured: Boolean get() = security != "open" && security != "owe"
    }
    data class SavedWifi(val id: String, val ssid: String)

    fun wifiStatus(interactor: SystemInteractor): WifiStatus {
        val out = interactor.runRootCommand("cmd wifi status").output
        val enabled = out.contains("Wifi is enabled", ignoreCase = true)
        val ssid = Regex("""connected to "(.*?)"""", RegexOption.IGNORE_CASE).find(out)?.groupValues?.get(1)
            ?: if (out.contains("is connected", ignoreCase = true)) Regex("""SSID: "(.*?)"""").find(out)?.groupValues?.get(1) else null
        return WifiStatus(enabled, ssid?.takeIf { it.isNotBlank() && !it.contains("unknown ssid") })
    }

    fun setWifiEnabled(interactor: SystemInteractor, enabled: Boolean): Boolean =
        interactor.runRootCommand("svc wifi ${if (enabled) "enable" else "disable"}").success

    fun startWifiScan(interactor: SystemInteractor) {
        interactor.runRootCommand("cmd wifi start-scan")
    }

    private val SCAN_LINE = Regex("""^\s*([0-9a-fA-F]{2}(?::[0-9a-fA-F]{2}){5})\s+(\d+)\s+(-?\d+)\S*\s+(\S+)\s+(.*?)\s*(\[.*\])?\s*$""")

    fun wifiScanResults(interactor: SystemInteractor): List<WifiNetwork> {
        val out = interactor.runRootCommand("cmd wifi list-scan-results").output
        return out.lines().mapNotNull { line ->
            val m = SCAN_LINE.find(line) ?: return@mapNotNull null
            val ssid = m.groupValues[5].trim()
            if (ssid.isEmpty()) return@mapNotNull null
            val flags = m.groupValues[6]
            val security = when {
                flags.contains("EAP") -> "eap"
                flags.contains("SAE") && !flags.contains("PSK") -> "wpa3"
                flags.contains("PSK") -> "wpa2"
                flags.contains("WEP") -> "eap" // WEP לא נתמך בחיבור דרך cmd wifi
                flags.contains("OWE") -> "owe"
                else -> "open"
            }
            WifiNetwork(ssid, m.groupValues[3].toIntOrNull() ?: -100, security)
        }
            .groupBy { it.ssid }
            .map { (_, same) -> same.maxBy { it.rssi } }
            .sortedByDescending { it.rssi }
    }

    fun connectWifi(interactor: SystemInteractor, network: WifiNetwork, password: String?): Boolean {
        val cmd = buildString {
            append("cmd wifi connect-network ")
            append(RootShell.quote(network.ssid))
            append(' ')
            append(network.security)
            if (network.secured && !password.isNullOrEmpty()) {
                append(' ')
                append(RootShell.quote(password))
            }
        }
        return interactor.runRootCommand(cmd).success
    }

    private val SAVED_LINE = Regex("""^\s*(\d+)\s+(.+?)\s{2,}(\S+)\s*$""")

    fun savedWifiNetworks(interactor: SystemInteractor): List<SavedWifi> {
        val out = interactor.runRootCommand("cmd wifi list-networks").output
        return out.lines().mapNotNull { line ->
            val m = SAVED_LINE.find(line) ?: return@mapNotNull null
            SavedWifi(m.groupValues[1], m.groupValues[2].trim())
        }
    }

    fun forgetWifi(interactor: SystemInteractor, id: String): Boolean {
        if (!id.all { it.isDigit() }) return false
        return interactor.runRootCommand("cmd wifi forget-network $id").success
    }

    fun signalLabel(rssi: Int): String = when {
        rssi >= -55 -> tr("אות חזק")
        rssi >= -70 -> tr("אות בינוני")
        else -> tr("אות חלש")
    }

    // ---------------------------------------------------------------------
    // פירוט אחסון (du דרך root, בסשן אחד)
    // ---------------------------------------------------------------------

    data class StorageCategory(val label: String, val bytes: Long)

    private const val MEDIA = "/data/media/0"

    private val STORAGE_PATHS = listOf(
        "apps" to listOf("/data/app", "/data/data"),
        "images" to listOf("$MEDIA/DCIM", "$MEDIA/Pictures"),
        "videos" to listOf("$MEDIA/Movies"),
        "audio" to listOf("$MEDIA/Music", "$MEDIA/Ringtones", "$MEDIA/Recordings", "$MEDIA/Notifications", "$MEDIA/Alarms", "$MEDIA/Podcasts"),
        "downloads" to listOf("$MEDIA/Download"),
        "documents" to listOf("$MEDIA/Documents"),
    )

    private fun storageLabel(id: String) = when (id) {
        "apps" -> tr("אפליקציות ונתונים שלהן")
        "images" -> tr("תמונות וצילומים")
        "videos" -> tr("סרטונים")
        "audio" -> tr("שמע והקלטות")
        "downloads" -> tr("הורדות")
        "documents" -> tr("מסמכים")
        else -> tr("אחר")
    }

    /** מחזיר רשימה ריקה כשאין root - אז המסך מציג רק את הסכום הכולל. */
    fun storageBreakdown(interactor: SystemInteractor, usedBytes: Long): List<StorageCategory> {
        val commands = STORAGE_PATHS.flatMap { (id, paths) ->
            listOf("echo '@@$id'", "du -sk ${paths.joinToString(" ")} 2>/dev/null")
        }
        val result = interactor.runRootCommands(commands)
        if (!result.output.contains("@@")) return emptyList()
        val sums = mutableMapOf<String, Long>()
        var current: String? = null
        for (line in result.output.lines()) {
            if (line.startsWith("@@")) {
                current = line.removePrefix("@@").trim()
                sums[current] = 0L
            } else if (current != null) {
                val kb = line.trim().split(Regex("\\s+")).firstOrNull()?.toLongOrNull() ?: continue
                sums[current] = (sums[current] ?: 0L) + kb * 1024L
            }
        }
        if (sums.values.all { it == 0L }) return emptyList()
        val categories = STORAGE_PATHS.map { (id, _) -> StorageCategory(storageLabel(id), sums[id] ?: 0L) }
        val other = (usedBytes - categories.sumOf { it.bytes }).coerceAtLeast(0L)
        return (categories + StorageCategory(storageLabel("other"), other)).sortedByDescending { it.bytes }
    }

    fun formatBytes(bytes: Long): String {
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        val gb = mb / 1024.0
        return when {
            gb >= 1.0 -> "%.1f GB".format(gb)
            mb >= 1.0 -> "%.0f MB".format(mb)
            else -> "%.0f KB".format(kb)
        }
    }

    // ---------------------------------------------------------------------
    // כל האפליקציות
    // ---------------------------------------------------------------------

    data class InstalledApp(val packageName: String, val label: String, val isSystem: Boolean)

    /** כל האפליקציות שיש להן אייקון במסך הבית (ה-<queries> של LAUNCHER במניפסט מתיר את זה). */
    fun launcherApps(context: Context): List<InstalledApp> {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return try {
            pm.queryIntentActivities(intent, 0)
                .map { it.activityInfo.applicationInfo }
                .distinctBy { it.packageName }
                .map { info ->
                    InstalledApp(
                        info.packageName,
                        info.loadLabel(pm).toString(),
                        (info.flags and ApplicationInfo.FLAG_SYSTEM) != 0,
                    )
                }
                .sortedBy { it.label.lowercase() }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun appLabel(context: Context, packageName: String): String? = try {
        val pm = context.packageManager
        pm.getApplicationInfo(packageName, 0).loadLabel(pm).toString()
    } catch (e: Exception) {
        null
    }

    fun isSystemApp(context: Context, packageName: String): Boolean = try {
        (context.packageManager.getApplicationInfo(packageName, 0).flags and ApplicationInfo.FLAG_SYSTEM) != 0
    } catch (e: Exception) {
        true
    }

    /** הסרה מותרת רק לאפליקציות משתמש שאינן חלק מ-FutureOS - כדי שאי אפשר יהיה
     *  להסיר בטעות את מסך הבית, החייגן או את ההגדרות עצמן. */
    fun canUninstall(context: Context, packageName: String): Boolean =
        !packageName.startsWith("com.future.") && !isSystemApp(context, packageName)

    fun uninstallApp(interactor: SystemInteractor, packageName: String): Boolean {
        if (!RootShell.isSafeToken(packageName)) return false
        val out = interactor.runRootCommand("pm uninstall ${RootShell.quote(packageName)}")
        return out.success && out.output.contains("Success")
    }

    data class AppStorage(val appBytes: Long, val dataBytes: Long, val cacheBytes: Long) {
        val total: Long get() = appBytes + dataBytes
    }

    /** דורש גישה לנתוני שימוש (PACKAGE_USAGE_STATS) - מחזיר null כשאין. */
    fun appStorage(context: Context, packageName: String): AppStorage? = try {
        val ssm = context.getSystemService(Context.STORAGE_STATS_SERVICE) as android.app.usage.StorageStatsManager
        val stats = ssm.queryStatsForPackage(android.os.storage.StorageManager.UUID_DEFAULT, packageName, android.os.Process.myUserHandle())
        AppStorage(stats.appBytes, stats.dataBytes, stats.cacheBytes)
    } catch (e: Exception) {
        null
    }

    fun clearAppCache(interactor: SystemInteractor, packageName: String): Boolean {
        if (!RootShell.isSafeToken(packageName)) return false
        return interactor.runRootCommand(
            "find /data/data/$packageName/cache /data/data/$packageName/code_cache $MEDIA/Android/data/$packageName/cache -mindepth 1 -delete 2>/dev/null; true"
        ).success
    }

    // ---------------------------------------------------------------------
    // תאריך, שעה ואזור זמן (IAlarmManager: setTime=2, setTimeZone=3 באנדרואיד 12)
    // ---------------------------------------------------------------------

    val TIME_ZONES = listOf(
        "Asia/Jerusalem" to tr("ישראל"),
        "UTC" to "UTC",
        "Europe/London" to tr("לונדון"),
        "Europe/Paris" to tr("פריז"),
        "Europe/Berlin" to tr("ברלין"),
        "Europe/Athens" to tr("אתונה"),
        "Europe/Moscow" to tr("מוסקבה"),
        "Asia/Dubai" to tr("דובאי"),
        "Asia/Kolkata" to tr("הודו"),
        "Asia/Bangkok" to tr("בנגקוק"),
        "Asia/Shanghai" to tr("סין"),
        "Asia/Tokyo" to tr("טוקיו"),
        "Australia/Sydney" to tr("סידני"),
        "Africa/Johannesburg" to tr("יוהנסבורג"),
        "America/Sao_Paulo" to tr("סאו פאולו"),
        "America/New_York" to tr("ניו יורק"),
        "America/Chicago" to tr("שיקגו"),
        "America/Denver" to tr("דנוור"),
        "America/Los_Angeles" to tr("לוס אנג'לס"),
    )

    fun gmtOffsetLabel(id: String): String {
        val offsetMin = java.util.TimeZone.getTimeZone(id).getOffset(System.currentTimeMillis()) / 60000
        val sign = if (offsetMin >= 0) "+" else "-"
        val abs = kotlin.math.abs(offsetMin)
        return "⁦GMT$sign%d:%02d⁩".format(abs / 60, abs % 60)
    }

    fun currentTimeZone(interactor: SystemInteractor): String =
        interactor.runRootCommand("getprop persist.sys.timezone").output.trim()
            .ifBlank { java.util.TimeZone.getDefault().id }

    /** מכבה אזור זמן אוטומטי, מחליף אזור ובודק בפועל שהשינוי נקלט. */
    fun setTimeZone(interactor: SystemInteractor, id: String): Boolean {
        if (TIME_ZONES.none { it.first == id }) return false
        interactor.runRootCommands(listOf(
            "settings put global auto_time_zone 0",
            "service call alarm 3 s16 $id",
        ))
        if (currentTimeZone(interactor) == id) return true
        // גיבוי: כתיבה ישירה של המאפיין והודעה לכל האפליקציות
        interactor.runRootCommands(listOf(
            "setprop persist.sys.timezone $id",
            "am broadcast -a android.intent.action.TIMEZONE_CHANGED --es time-zone $id",
        ))
        return currentTimeZone(interactor) == id
    }

    /** קובע את השעון למילישניות נתונות (אחרי כיבוי שעה אוטומטית) ובודק שזה נקלט. */
    fun setSystemTime(interactor: SystemInteractor, millis: Long): Boolean {
        interactor.runRootCommands(listOf(
            "settings put global auto_time 0",
            "service call alarm 2 i64 $millis",
        ))
        return kotlin.math.abs(System.currentTimeMillis() - millis) < 60_000
    }

    /** "1430" / "14:30" → (14, 30). */
    fun parseTime(input: String): Pair<Int, Int>? {
        val d = input.filter { it.isDigit() }
        if (d.length !in 3..4) return null
        val h = d.dropLast(2).toInt()
        val m = d.takeLast(2).toInt()
        return if (h in 0..23 && m in 0..59) h to m else null
    }

    /** "24112026" / "24.11.2026" → (24, 11, 2026). */
    fun parseDate(input: String): Triple<Int, Int, Int>? {
        val d = input.filter { it.isDigit() }
        if (d.length != 8) return null
        val day = d.substring(0, 2).toInt()
        val month = d.substring(2, 4).toInt()
        val year = d.substring(4, 8).toInt()
        if (month !in 1..12 || year !in 2020..2099) return null
        val maxDay = java.util.Calendar.getInstance().apply { clear(); set(year, month - 1, 1) }.getActualMaximum(java.util.Calendar.DAY_OF_MONTH)
        return if (day in 1..maxDay) Triple(day, month, year) else null
    }
}
