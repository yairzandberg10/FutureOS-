package com.future.tools.data

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import com.future.tools.ui.ToolRoute

/**
 * מיפוי בין כלי בודד (ToolRoute) לבין activity-alias שמוצהר ב-Manifest -
 * כשalias מופעל (setComponentEnabledSetting) הוא נכנס ל-queryIntentActivities
 * של המערכת ומופיע בלאנצ'ר כאפליקציה נפרדת משלו, עם התווית והאייקון שמוגדרים
 * לו שם, למרות ששתי הכניסות מריצות בפועל את אותה MainActivity. MainActivity
 * מזהה איזה alias שימש להפעלה לפי intent.component ופותחת ישר במסך המתאים
 * (ראו ROUTE_BY_ALIAS שם).
 */
object ToolShortcuts {
    private const val PACKAGE = "com.future.tools"

    val ALIAS_BY_ROUTE: Map<ToolRoute, String> = mapOf(
        ToolRoute.Flashlight to "$PACKAGE.ToolShortcutFlashlight",
        ToolRoute.UnitConverter to "$PACKAGE.ToolShortcutUnitConverter",
        ToolRoute.Compass to "$PACKAGE.ToolShortcutCompass",
        ToolRoute.Level to "$PACKAGE.ToolShortcutLevel",
        ToolRoute.NoiseMeter to "$PACKAGE.ToolShortcutNoiseMeter",
        ToolRoute.LuxMeter to "$PACKAGE.ToolShortcutLuxMeter",
        ToolRoute.AngleRuler to "$PACKAGE.ToolShortcutAngleRuler",
        ToolRoute.TipSplitCalculator to "$PACKAGE.ToolShortcutTipSplit",
        ToolRoute.QuickFinanceCalculator to "$PACKAGE.ToolShortcutQuickFinance",
        ToolRoute.TimeZoneConverter to "$PACKAGE.ToolShortcutTimeZone",
        ToolRoute.QrScanner to "$PACKAGE.ToolShortcutQrScanner",
        ToolRoute.Pomodoro to "$PACKAGE.ToolShortcutPomodoro",
        ToolRoute.PasswordGenerator to "$PACKAGE.ToolShortcutPasswordGenerator",
        ToolRoute.QuickNotes to "$PACKAGE.ToolShortcutQuickNotes",
        ToolRoute.CoinDice to "$PACKAGE.ToolShortcutCoinDice",
        ToolRoute.RandomPicker to "$PACKAGE.ToolShortcutRandomPicker",
        ToolRoute.RandomNumber to "$PACKAGE.ToolShortcutRandomNumber",
        ToolRoute.TextScanner to "$PACKAGE.ToolShortcutTextScanner",
        ToolRoute.VoiceTranscribe to "$PACKAGE.ToolShortcutVoiceTranscribe"
    )

    val ROUTE_BY_ALIAS: Map<String, ToolRoute> =
        ALIAS_BY_ROUTE.entries.associate { (route, alias) -> alias to route }

    fun isPinnedToHome(context: Context, route: ToolRoute): Boolean {
        val alias = ALIAS_BY_ROUTE[route] ?: return false
        val state = context.packageManager.getComponentEnabledSetting(ComponentName(PACKAGE, alias))
        return state == PackageManager.COMPONENT_ENABLED_STATE_ENABLED
    }

    fun setPinnedToHome(context: Context, route: ToolRoute, pinned: Boolean) {
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
