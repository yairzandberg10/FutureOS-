package com.android.sistemui.recents.logic

import android.app.ActivityManager
import android.app.AppOpsManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Process
import androidx.core.graphics.drawable.toBitmap

data class RecentAppInfo(
    val packageName: String,
    val label: String,
    val icon: Drawable,
    val lastTimeUsed: Long,
    /** הצבע הממוצע של האייקון - הרקע של כרטיס החלון (אין גישה לצילומי משימות). */
    val tint: Int = 0xFF2C2C2E.toInt(),
)

/**
 * רשימת האפליקציות האחרונות - **רק מה שהמשתמש באמת פתח**.
 *
 * קודם הרשימה נבנתה מ-UsageStatsManager של שלושת הימים האחרונים, ולכן הופיעו
 * בה גם אפליקציות שרק "עלו" לרגע ברקע (שירות שפתח פעילות, בורר מערכת, אפליקציה
 * שנפתחה מקישור בלי שהמשתמש ביקש) ואפליקציות שנסגרו מזמן. עכשיו שירות שורת
 * המצב מדווח כאן ([record]) על כל חלון שעולה לחזית, ונרשם רק חלון שהוא
 * פעילות אמיתית (getActivityInfo מצליח - לא דיאלוג, מקלדת או חלון מערכת) של
 * אפליקציה עם מסך פתיחה. הסדר נשמר ב-SharedPreferences, כך שהרשימה שורדת
 * הפעלה מחדש של השירות. סגירה ([remove]) מוציאה מהרשימה.
 */
class RecentAppsManager(private val context: Context) {

    private val prefs = context.getSharedPreferences("recents", Context.MODE_PRIVATE)
    private val pm = context.packageManager

    private val excludedPackages = setOf(
        context.packageName,
        "com.future.futurelauncher",
        "com.future.keyboard",
        "android",
        "com.android.systemui",
    )

    /** החבילות לפי הסדר, האחרונה ראשונה. */
    private val order: MutableList<String> = prefs.getString(KEY_ORDER, "")
        .orEmpty()
        .split(',')
        .filter { it.isNotBlank() }
        .toMutableList()

    /** האם רכיב הוא פעילות של אפליקציה שאפשר לפתוח - נשמר כדי לא לשאול שוב על כל חלון. */
    private val verdicts = HashMap<String, Boolean>()

    private val home: String? by lazy {
        runCatching {
            pm.resolveActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), PackageManager.MATCH_DEFAULT_ONLY)
                ?.activityInfo?.packageName
        }.getOrNull()
    }

    fun hasUsageAccess(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= 29) {
            appOps.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    /** חלון עלה לחזית (TYPE_WINDOW_STATE_CHANGED). */
    fun record(packageName: String?, className: String?) {
        if (packageName.isNullOrBlank() || className.isNullOrBlank()) return
        if (packageName in excludedPackages || packageName == home) return
        val key = "$packageName/$className"
        val isAppActivity = verdicts.getOrPut(key) {
            runCatching { pm.getActivityInfo(ComponentName(packageName, className), 0) }.isSuccess &&
                pm.getLaunchIntentForPackage(packageName) != null
        }
        if (!isAppActivity) return
        if (order.firstOrNull() == packageName) return
        order.remove(packageName)
        order.add(0, packageName)
        while (order.size > MAX_APPS) order.removeAt(order.lastIndex)
        save()
    }

    fun remove(packageName: String) {
        if (order.remove(packageName)) save()
    }

    fun clear() {
        order.clear()
        save()
    }

    fun getRecentApps(limit: Int = MAX_APPS): List<RecentAppInfo> {
        val now = System.currentTimeMillis()
        var dropped = false
        val result = order.toList().mapIndexedNotNull { index, pkg ->
            try {
                val appInfo = pm.getApplicationInfo(pkg, 0)
                if (!appInfo.enabled || pm.getLaunchIntentForPackage(pkg) == null) {
                    order.remove(pkg); dropped = true
                    return@mapIndexedNotNull null
                }
                val icon = pm.getApplicationIcon(appInfo)
                RecentAppInfo(
                    packageName = pkg,
                    label = pm.getApplicationLabel(appInfo).toString(),
                    icon = icon,
                    lastTimeUsed = now - index,
                    tint = averageColor(icon),
                )
            } catch (e: PackageManager.NameNotFoundException) {
                // הוסרה מאז.
                order.remove(pkg); dropped = true
                null
            } catch (e: Exception) {
                null
            }
        }.take(limit)
        if (dropped) save()
        return result
    }

    /** "זיכרון פנוי X | Y" לכותרת, כמו במסך האחרונות של Redmi. */
    fun memorySummary(): String? = runCatching {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val info = ActivityManager.MemoryInfo().also { am.getMemoryInfo(it) }
        fun gb(bytes: Long) = String.format(java.util.Locale.US, "%.1fGB", bytes / (1024.0 * 1024 * 1024))
        "פנוי ${gb(info.availMem)} | ${gb(info.totalMem)}"
    }.getOrNull()

    private fun save() {
        prefs.edit().putString(KEY_ORDER, order.joinToString(",")).apply()
    }

    private fun averageColor(icon: Drawable): Int = runCatching {
        val small = icon.toBitmap(12, 12, Bitmap.Config.ARGB_8888)
        var r = 0L; var g = 0L; var b = 0L; var n = 0L
        for (x in 0 until small.width) for (y in 0 until small.height) {
            val c = small.getPixel(x, y)
            if ((c ushr 24) < 0x80) continue
            r += (c shr 16) and 0xFF; g += (c shr 8) and 0xFF; b += c and 0xFF; n++
        }
        if (n == 0L) return@runCatching 0xFF2C2C2E.toInt()
        // כהה מעט, כדי שהאייקון והטקסט הלבן יבלטו על הכרטיס.
        val k = 0.55
        (0xFF shl 24) or ((r / n * k).toInt() shl 16) or ((g / n * k).toInt() shl 8) or (b / n * k).toInt()
    }.getOrDefault(0xFF2C2C2E.toInt())

    private companion object {
        const val KEY_ORDER = "order"
        const val MAX_APPS = 20
    }
}
