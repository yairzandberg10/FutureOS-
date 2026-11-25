package com.future.guide

import android.content.Context
import com.future.guide.data.GUIDE_APPS
import com.future.sharednav.widget.FutureContentWidget
import com.future.sharednav.widget.WidgetContent

/** כמה נושאים וטיפים יש במדריך, ושמות שלושת הנושאים הראשונים. */
class GuideTopicsWidgetProvider : FutureContentWidget() {
    override fun content(context: Context): WidgetContent {
        if (GUIDE_APPS.isEmpty()) return WidgetContent(value = "אין נושאים במדריך", title = "נושאים")
        val tipCount = GUIDE_APPS.sumOf { it.tips.size }
        val names = GUIDE_APPS.take(3).joinToString(" · ") { it.name }
        return WidgetContent(value = "${GUIDE_APPS.size} נושאים במדריך", subtitle = "$names · $tipCount טיפים", title = "נושאים")
    }
}
