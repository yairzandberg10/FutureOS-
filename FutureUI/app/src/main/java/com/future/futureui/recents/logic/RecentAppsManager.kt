package com.future.futureui.recents.logic

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Process

data class RecentAppInfo(
    val packageName: String,
    val label: String,
    val icon: Drawable,
    val lastTimeUsed: Long
)

/** רשימת אפליקציות אחרונות אמיתית, מבוססת UsageStatsManager - בלי GLOBAL_ACTION_RECENTS. */
class RecentAppsManager(private val context: Context) {

    private val excludedPackages = setOf(
        context.packageName,
        "com.future.futurelauncher",
        "android"
    )

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

    fun getRecentApps(limit: Int = 15): List<RecentAppInfo> {
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
            ?: return emptyList()
        val pm = context.packageManager
        // מסך הבית הפעיל (גם כשזה לא FutureLauncher) הוא לא "אפליקציה אחרונה".
        val home = try {
            pm.resolveActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), PackageManager.MATCH_DEFAULT_ONLY)
                ?.activityInfo?.packageName
        } catch (e: Exception) {
            null
        }
        val end = System.currentTimeMillis()
        val start = end - 1000L * 60 * 60 * 24 * 3

        val stats = try {
            usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_BEST, start, end)
        } catch (e: Exception) {
            null
        } ?: return emptyList()

        // נקרא על ה-main thread של שירות הנגישות (זה שמסנן את כל המקשים), אז
        // אייקונים נטענים רק ל-limit הראשונות - sequence עוצר ב-take במקום לטעון
        // אייקון לכל אפליקציה שרצה בשלושת הימים האחרונים.
        return stats
            .asSequence()
            .filter { it.lastTimeUsed > 0 && it.packageName !in excludedPackages && it.packageName != home && it.totalTimeInForeground > 0 }
            .sortedByDescending { it.lastTimeUsed }
            .distinctBy { it.packageName }
            .mapNotNull { usage ->
                try {
                    val appInfo = pm.getApplicationInfo(usage.packageName, 0)
                    if (pm.getLaunchIntentForPackage(usage.packageName) == null) return@mapNotNull null
                    RecentAppInfo(
                        packageName = usage.packageName,
                        label = pm.getApplicationLabel(appInfo).toString(),
                        icon = pm.getApplicationIcon(appInfo),
                        lastTimeUsed = usage.lastTimeUsed
                    )
                } catch (e: Exception) {
                    null
                }
            }
            .take(limit)
            .toList()
    }
}
