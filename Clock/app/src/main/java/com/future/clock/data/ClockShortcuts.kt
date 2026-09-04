package com.future.clock.data

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import com.future.clock.ui.ClockRoute

/**
 * מיפוי בין מסך בודד (ClockRoute) לבין activity-alias שמוצהר ב-Manifest -
 * כשalias מופעל (setComponentEnabledSetting) הוא נכנס ל-queryIntentActivities
 * של המערכת ומופיע בלאנצ'ר כאפליקציה נפרדת משלו, עם התווית שמוגדרת לו שם,
 * למרות ששתי הכניסות מריצות בפועל את אותה MainActivity - אותו דפוס בדיוק
 * כמו ב-ToolShortcuts באפליקציית Tools.
 */
object ClockShortcuts {
    private const val PACKAGE = "com.future.clock"

    val ALIAS_BY_ROUTE: Map<ClockRoute, String> = mapOf(
        ClockRoute.Stopwatch to "$PACKAGE.ClockShortcutStopwatch",
        ClockRoute.Timer to "$PACKAGE.ClockShortcutTimer"
    )

    val ROUTE_BY_ALIAS: Map<String, ClockRoute> =
        ALIAS_BY_ROUTE.entries.associate { (route, alias) -> alias to route }

    fun isPinnedToHome(context: Context, route: ClockRoute): Boolean {
        val alias = ALIAS_BY_ROUTE[route] ?: return false
        val state = context.packageManager.getComponentEnabledSetting(ComponentName(PACKAGE, alias))
        return state == PackageManager.COMPONENT_ENABLED_STATE_ENABLED
    }

    fun setPinnedToHome(context: Context, route: ClockRoute, pinned: Boolean) {
        val alias = ALIAS_BY_ROUTE[route] ?: return
        val newState = if (pinned) {
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        } else {
            PackageManager.COMPONENT_ENABLED_STATE_DEFAULT
        }
        context.packageManager.setComponentEnabledSetting(
            ComponentName(PACKAGE, alias), newState, PackageManager.DONT_KILL_APP
        )
    }
}
