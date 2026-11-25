package com.future.flashlight.widget

import android.content.Context
import com.future.flashlight.data.TorchStateStore
import com.future.sharednav.widget.FutureContentWidget
import com.future.sharednav.widget.WidgetContent

/** כמה זמן הפנס היה דלוק היום, וכמה פעמים הודלק. */
class FlashlightTimeWidgetProvider : FutureContentWidget() {
    override fun content(context: Context): WidgetContent {
        val state = TorchStateStore.read(context)
        if (state.minutesToday == 0L && state.sessionsToday == 0) {
            return WidgetContent(value = "הפנס לא הודלק היום", subtitle = null)
        }
        val minutes = if (state.minutesToday == 1L) "דקה אחת" else "${state.minutesToday} דקות"
        val sessions = if (state.sessionsToday == 1) "הדלקה אחת" else "${state.sessionsToday} הדלקות"
        return WidgetContent(value = "$minutes דלוק היום", subtitle = sessions)
    }
}
