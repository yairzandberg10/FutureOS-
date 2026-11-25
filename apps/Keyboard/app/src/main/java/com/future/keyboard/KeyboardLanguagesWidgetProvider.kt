package com.future.keyboard

import android.content.Context
import com.future.sharednav.widget.FutureContentWidget
import com.future.sharednav.widget.WidgetContent

/** השפות שהופעלו במקלדת, ובכמה מהן יש חיזוי מילים. */
class KeyboardLanguagesWidgetProvider : FutureContentWidget() {
    override fun content(context: Context): WidgetContent {
        val prefs = context.getSharedPreferences(KeyboardLanguages.PREFS, Context.MODE_PRIVATE)
        val names = KeyboardLanguages.enabled(prefs).map { KeyboardLanguages.displayName(it) }.sorted()
        val predicted = KeyboardLanguages.predicted(prefs).size
        val subtitle = when {
            !prefs.getBoolean("predictive_enabled", true) -> "חיזוי מילים כבוי"
            predicted == 0 -> "חיזוי מילים כבוי בכל השפות"
            else -> "חיזוי מילים ב-$predicted שפות"
        }
        return WidgetContent(value = names.joinToString(" · "), subtitle = subtitle, title = "שפות המקלדת")
    }
}
