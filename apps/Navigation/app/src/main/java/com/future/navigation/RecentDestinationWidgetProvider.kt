package com.future.navigation

import android.content.Context
import com.future.navigation.data.places.RecentPlaceList
import com.future.sharednav.widget.FutureContentWidget
import com.future.sharednav.widget.WidgetContent

/** היעד האחרון שנבחר בניווט - השם והכתובת. */
class RecentDestinationWidgetProvider : FutureContentWidget() {
    override fun content(context: Context): WidgetContent {
        val raw = context.applicationContext
            .getSharedPreferences("recent_places", Context.MODE_PRIVATE)
            .getString("items", null)
        val latest = RecentPlaceList.decode(raw).firstOrNull()
            ?: return WidgetContent(value = "אין יעדים אחרונים", subtitle = "לאן נוסעים?")
        return WidgetContent(
            value = latest.title.ifBlank { latest.address },
            subtitle = latest.address.takeIf { latest.title.isNotBlank() && it.isNotBlank() },
        )
    }
}
