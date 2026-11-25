package com.future.assistant.widget

import android.content.Context
import com.future.sharednav.widget.FutureContentWidget
import com.future.sharednav.widget.WidgetContent
import java.io.File

/** מצב מודל הדיבור המקומי (Whisper) - מוכן לשימוש, או עוד לא נטען. */
class AssistantModelWidgetProvider : FutureContentWidget() {
    override fun content(context: Context): WidgetContent {
        // שם הקובץ זהה ל-LocalSpeechEngine.MODEL_FILE (מועתק מהנכסים אל filesDir).
        val model = File(context.filesDir, "ggml-small-q8_0.bin")
        if (!model.exists() || model.length() == 0L) {
            return WidgetContent(value = "מודל הדיבור לא נטען", subtitle = "ייטען כשהעוזר נפתח")
        }
        val megabytes = model.length() / 1_000_000L
        return WidgetContent(value = "מודל הדיבור מוכן", subtitle = "⁦$megabytes MB⁩ · אפשר לדבר עם העוזר")
    }
}
