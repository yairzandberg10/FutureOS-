package com.future.clock

import android.content.Context
import com.future.clock.logic.AlarmLogic
import com.future.sharednav.widget.FutureContentWidget
import com.future.sharednav.widget.WidgetContent
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** הטיימר: הזמן שנשאר (רץ או מושהה), ושעת הסיום כשהוא רץ. */
class TimerWidgetProvider : FutureContentWidget() {
    override fun content(context: Context): WidgetContent {
        AlarmLogic.runningTimer(context)?.let { (end, _) ->
            val endClock = SimpleDateFormat("HH:mm", Locale.US).format(Date(end))
            return WidgetContent(
                value = formatLeft(end - System.currentTimeMillis()),
                subtitle = "מסתיים ב-$endClock",
                title = "טיימר",
            )
        }
        AlarmLogic.pausedTimer(context)?.let { (left, _) ->
            return WidgetContent(value = formatLeft(left), subtitle = "מושהה", title = "טיימר")
        }
        return WidgetContent(value = "אין טיימר", subtitle = "פתח כדי להפעיל", title = "טיימר")
    }

    private fun formatLeft(millis: Long): String {
        val total = (millis / 1000).coerceAtLeast(0)
        val h = total / 3600
        val m = (total % 3600) / 60
        val s = total % 60
        return if (h > 0) String.format(Locale.US, "%d:%02d:%02d", h, m, s)
        else String.format(Locale.US, "%02d:%02d", m, s)
    }
}
