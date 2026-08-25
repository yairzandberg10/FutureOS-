package com.future.futurelauncher.ui

import android.content.ComponentName
import android.content.Intent
import android.content.pm.ResolveInfo
import java.util.UUID

/**
 * בונה Intent שמפעיל בדיוק את הפעילות (או activity-alias) שנפתרה כאן, לפי
 * שם הרכיב המדויק - לא דרך PackageManager.getLaunchIntentForPackage(packageName)
 * שמחזיר תמיד "פעילות הפעלה" אחת לפי חבילה, ולכן מתעלם מזהות הכניסה
 * הספציפית כשלחבילה אחת יש כמה activity-alias שפתוחים ל-LAUNCHER (למשל כלים
 * שהתווספו למסך הבית כאפליקציה עצמאית משלהם, ראו com.future.tools.data.ToolShortcuts).
 */
fun ResolveInfo.launchIntent(): Intent = Intent(Intent.ACTION_MAIN).apply {
    addCategory(Intent.CATEGORY_LAUNCHER)
    component = ComponentName(activityInfo.packageName, activityInfo.name)
}

sealed class LauncherItem {
    abstract val id: String
    abstract val label: String
    abstract var customLabel: String?
    abstract var customIconPath: String?

    data class App(
        override val id: String,
        val resolveInfo: ResolveInfo,
        override val label: String,
        override var customLabel: String? = null,
        override var customIconPath: String? = null
    ) : LauncherItem()

    data class Folder(
        override val id: String,
        override var label: String,
        val apps: MutableList<App> = mutableListOf(),
        override var customLabel: String? = null,
        override var customIconPath: String? = null
    ) : LauncherItem()

    data class Widget(
        override val id: String,
        val widgetId: Int,
        override val label: String = "Widget",
        var spanX: Int = 1,
        var spanY: Int = 1,
        override var customLabel: String? = null,
        override var customIconPath: String? = null
    ) : LauncherItem()

    data class Empty(
        override val id: String = "empty:${UUID.randomUUID()}",
        override val label: String = "",
        var isOccupiedBy: String? = null,
        override var customLabel: String? = null,
        override var customIconPath: String? = null
    ) : LauncherItem()
}
