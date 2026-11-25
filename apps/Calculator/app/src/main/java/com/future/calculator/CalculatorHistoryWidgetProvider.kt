package com.future.calculator

import android.content.Context
import com.future.calculator.logic.CalcHistoryFormat
import com.future.sharednav.widget.FutureContentWidget
import com.future.sharednav.widget.WidgetContent

/** מספר החישובים השמורים בהיסטוריה, והביטוי האחרון שחושב. */
class CalculatorHistoryWidgetProvider : FutureContentWidget() {
    override fun content(context: Context): WidgetContent {
        val entries = CalcHistoryFormat.parse(
            context.getSharedPreferences(CalcHistoryFormat.PREFS, Context.MODE_PRIVATE)
                .getString(CalcHistoryFormat.KEY, "").orEmpty()
        )
        if (entries.isEmpty()) return WidgetContent(value = "אין חישובים", subtitle = "ההיסטוריה ריקה")
        val count = entries.size
        val countText = when (count) {
            1 -> "חישוב אחד בהיסטוריה"
            else -> "$count חישובים בהיסטוריה"
        }
        // ספרות וסימני פעולה משמאל לימין גם בווידג'ט RTL.
        return WidgetContent(value = countText, subtitle = "⁦${entries.first().expression}⁩")
    }
}
