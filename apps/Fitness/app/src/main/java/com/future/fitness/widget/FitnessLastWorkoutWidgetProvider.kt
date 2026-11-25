package com.future.fitness.widget

import android.content.Context
import com.future.fitness.data.WorkoutStore
import com.future.sharednav.widget.FutureContentWidget
import com.future.sharednav.widget.WidgetContent

/** האימון האחרון שהושלם (שם, משך וכמה זמן עבר). */
class FitnessLastWorkoutWidgetProvider : FutureContentWidget() {
    override fun content(context: Context): WidgetContent {
        // ההיסטוריה שמורה מהחדש לישן, אז הראשון הוא האחרון שהושלם
        val last = WorkoutStore(context).getHistory().firstOrNull()
            ?: return WidgetContent(value = "אין אימונים", subtitle = "אימון שהושלם יופיע כאן")
        return WidgetContent(
            value = last.name,
            subtitle = "${last.minutes} דקות · ${ago(last.dateMillis)}",
        )
    }
}
