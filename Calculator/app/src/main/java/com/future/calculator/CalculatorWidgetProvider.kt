package com.future.calculator

import android.content.Context
import com.future.calculator.logic.CalcHistoryFormat
import com.future.sharednav.widget.FutureContentWidget
import com.future.sharednav.widget.WidgetContent

/** התוצאה האחרונה וחישוב שלה, מההיסטוריה שהמחשבון שומר. */
class CalculatorWidgetProvider : FutureContentWidget() {
    override fun content(context: Context): WidgetContent {
        val last = CalcHistoryFormat.parse(
            context.getSharedPreferences(CalcHistoryFormat.PREFS, Context.MODE_PRIVATE)
                .getString(CalcHistoryFormat.KEY, "").orEmpty()
        ).firstOrNull()
            ?: return WidgetContent(value = "0", subtitle = context.getString(R.string.calc_empty))
        // ספרות וסימני פעולה משמאל לימין גם בווידג'ט RTL.
        return WidgetContent(value = "⁦${last.result}⁩", subtitle = "⁦${last.expression} =⁩")
    }
}
