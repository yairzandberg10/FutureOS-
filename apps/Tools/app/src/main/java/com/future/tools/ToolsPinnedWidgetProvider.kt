package com.future.tools

import android.content.Context
import com.future.sharednav.widget.FutureContentWidget
import com.future.sharednav.widget.WidgetContent
import com.future.tools.data.ToolShortcuts
import com.future.tools.ui.ToolRoute

/** שמות הכלים שנעוצים כאפליקציה נפרדת במסך הבית - שניים ראשונים, והשאר כספירה. */
class ToolsPinnedWidgetProvider : FutureContentWidget() {
    override fun content(context: Context): WidgetContent {
        val names = ToolShortcuts.ALIAS_BY_ROUTE.keys
            .filter { ToolShortcuts.isPinnedToHome(context, it) }
            .mapNotNull { labelRes(it) }
            .map { context.getString(it) }
        if (names.isEmpty()) return WidgetContent(value = "אין כלים נעוצים", subtitle = "נעץ כלי ממסך הכלים")
        val rest = names.size - 2
        return WidgetContent(
            value = names.take(2).joinToString(" · "),
            subtitle = if (rest > 0) "ועוד $rest נעוצים במסך הבית" else "נעוצים במסך הבית",
        )
    }

    private fun labelRes(route: ToolRoute): Int? = when (route) {
        ToolRoute.UnitConverter -> R.string.tool_unit_converter
        ToolRoute.Compass -> R.string.tool_compass
        ToolRoute.Level -> R.string.tool_level
        ToolRoute.NoiseMeter -> R.string.tool_noise_meter
        ToolRoute.LuxMeter -> R.string.tool_lux_meter
        ToolRoute.AngleRuler -> R.string.tool_angle_ruler
        ToolRoute.GuitarTuner -> R.string.tool_guitar_tuner
        ToolRoute.TipSplitCalculator -> R.string.tool_tip_split
        ToolRoute.QuickFinanceCalculator -> R.string.tool_quick_finance
        ToolRoute.TimeZoneConverter -> R.string.tool_time_zone
        ToolRoute.QrScanner -> R.string.tool_qr_scanner
        ToolRoute.Pomodoro -> R.string.tool_pomodoro
        ToolRoute.PasswordGenerator -> R.string.tool_password_generator
        ToolRoute.CoinDice -> R.string.tool_coin_dice
        ToolRoute.RandomPicker -> R.string.tool_random_picker
        ToolRoute.RandomNumber -> R.string.tool_random_number
        ToolRoute.TextScanner -> R.string.tool_text_scanner
        else -> null
    }
}
