package com.future.recorder

import android.content.Context
import com.future.recorder.data.Recordings
import com.future.recorder.data.formatDuration
import com.future.sharednav.widget.FutureContentWidget
import com.future.sharednav.widget.WidgetContent

/** מספר ההקלטות השמורות, וסך משך ההקלטות. */
class RecordingsCountWidgetProvider : FutureContentWidget() {
    override fun content(context: Context): WidgetContent {
        val recordings = Recordings.list(context)
        if (recordings.isEmpty()) return WidgetContent(value = "אין הקלטות", subtitle = "הקלטה חדשה במסך הראשי")
        val totalMs = recordings.sumOf { it.durationMs }
        return WidgetContent(
            value = if (recordings.size == 1) "הקלטה אחת" else "${recordings.size} הקלטות",
            subtitle = "סה\"כ ${formatDuration(totalMs)}",
        )
    }
}
