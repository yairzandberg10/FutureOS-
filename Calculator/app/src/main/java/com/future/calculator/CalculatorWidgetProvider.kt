package com.future.calculator

import android.content.Context
import com.future.sharednav.widget.FutureContentWidget
import com.future.sharednav.widget.WidgetContent

/** התוצאה האחרונה וחישוב שלה, מההיסטוריה שהמחשבון שומר. */
class CalculatorWidgetProvider : FutureContentWidget() {
    override fun content(context: Context): WidgetContent {
        val last = context.getSharedPreferences("calculator", Context.MODE_PRIVATE)
            .getString("history", "").orEmpty().lineSequence().firstOrNull { it.isNotEmpty() }
            ?.split("\u001F")?.takeIf { it.size == 2 }
            ?: return WidgetContent(value = "0", subtitle = "אין חישובים")
        // ספרות וסימני פעולה משמאל לימין גם בווידג'ט RTL.
        return WidgetContent(value = "\u2066${last[1]}\u2069", subtitle = "\u2066${last[0]} =\u2069")
    }
}
