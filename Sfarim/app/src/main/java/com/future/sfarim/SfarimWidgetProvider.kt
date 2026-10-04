package com.future.sfarim

import android.icu.util.HebrewCalendar
import android.content.Context
import com.future.sharednav.widget.FutureContentWidget
import com.future.sharednav.widget.WidgetContent

/** התהילים היומי לפי החלוקה החודשית המקובלת, לפי היום בחודש העברי. */
class SfarimWidgetProvider : FutureContentWidget() {
    override fun content(context: Context): WidgetContent {
        val day = HebrewCalendar().get(HebrewCalendar.DAY_OF_MONTH).coerceIn(1, 30)
        return WidgetContent(value = "תהילים ${TEHILLIM_BY_DAY[day - 1]}", subtitle = "תהילים היומי · יום $day בחודש")
    }

    private companion object {
        /** חלוקת ספר תהילים לשלושים ימי החודש. */
        val TEHILLIM_BY_DAY = listOf(
            "1-9", "10-17", "18-22", "23-28", "29-34", "35-38", "39-43", "44-48", "49-54", "55-59",
            "60-65", "66-68", "69-71", "72-76", "77-78", "79-82", "83-87", "88-89", "90-96", "97-103",
            "104-105", "106-107", "108-112", "113-118", "119:1-96", "119:97-176", "120-134", "135-139", "140-144", "145-150",
        )
    }
}
