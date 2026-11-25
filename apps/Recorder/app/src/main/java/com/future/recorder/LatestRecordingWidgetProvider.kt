package com.future.recorder

import android.content.Context
import com.future.recorder.data.Recordings
import com.future.recorder.data.formatDuration
import com.future.sharednav.widget.FutureContentWidget
import com.future.sharednav.widget.WidgetContent

/** ההקלטה האחרונה: השם, משכה, ומתי נשמרה. */
class LatestRecordingWidgetProvider : FutureContentWidget() {
    override fun content(context: Context): WidgetContent {
        val latest = Recordings.list(context).firstOrNull()
            ?: return WidgetContent(value = "אין הקלטות", subtitle = "הקלטה חדשה במסך הראשי")
        return WidgetContent(
            value = latest.name,
            subtitle = "${formatDuration(latest.durationMs)} · ${ago(latest.modified)}",
        )
    }
}
