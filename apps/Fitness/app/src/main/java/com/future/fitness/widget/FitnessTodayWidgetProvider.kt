package com.future.fitness.widget

import android.content.Context
import com.future.fitness.data.WorkoutStore
import com.future.sharednav.widget.FutureContentWidget
import com.future.sharednav.widget.WidgetContent

/** דקות האימון ביום הנוכחי, קלוריות היום, והרצף. */
class FitnessTodayWidgetProvider : FutureContentWidget() {
    override fun content(context: Context): WidgetContent {
        val stats = WorkoutStore(context).getStats()
        val streak = if (stats.streakDays == 1) "יום אחד" else "${stats.streakDays} ימים"
        if (stats.activeMinutesToday == 0) {
            return WidgetContent(value = "לא התאמנת היום", subtitle = "רצף: $streak")
        }
        return WidgetContent(
            value = "${stats.activeMinutesToday} דקות היום",
            subtitle = "${stats.caloriesToday} קק\"ל · רצף: $streak",
        )
    }
}
