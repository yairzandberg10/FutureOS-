package com.future.assistant.data

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager

data class LaunchableApp(val label: String, val packageName: String)

/** מאתר אפליקציות מותקנות (LAUNCHER) לפי שם תצוגה, כדי לאפשר פקודה קולית
 * "פתח X" - משתמש ב-queries שהוצהר במניפסט לראות את כל האפליקציות. */
object AppLauncher {
    fun listApps(context: Context): List<LaunchableApp> {
        val intent = Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_LAUNCHER) }
        val resolveInfos = try {
            context.packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
        } catch (e: Exception) {
            emptyList()
        }
        return resolveInfos
            .filter { it.activityInfo.packageName != context.packageName }
            .mapNotNull { info ->
                val label = try {
                    info.loadLabel(context.packageManager).toString()
                } catch (e: Exception) {
                    null
                }
                label?.let { LaunchableApp(it, info.activityInfo.packageName) }
            }
            .distinctBy { it.packageName }
    }

    /** התאמה מטושטשת: קודם התאמה מדויקת, אחר כך הכלה בשני הכיוונים. */
    fun findBestMatch(apps: List<LaunchableApp>, query: String): LaunchableApp? {
        val normalizedQuery = query.trim()
        if (normalizedQuery.isEmpty()) return null
        apps.firstOrNull { it.label.trim() == normalizedQuery }?.let { return it }
        apps.firstOrNull { it.label.contains(normalizedQuery, ignoreCase = true) }?.let { return it }
        return apps.firstOrNull { normalizedQuery.contains(it.label.trim(), ignoreCase = true) }
    }

    fun launch(context: Context, packageName: String): Boolean {
        val intent = context.packageManager.getLaunchIntentForPackage(packageName) ?: return false
        return try {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            false
        }
    }
}
