package com.future.translate

import android.content.Context
import com.future.sharednav.widget.FutureContentWidget
import com.future.sharednav.widget.WidgetContent
import com.future.translate.data.HistoryStore
import com.future.translate.data.Languages
import com.future.translate.data.TranslatePrefs

/** התרגום האחרון שבוצע: התוצאה, וזוג השפות והזמן. */
class TranslateLastWidgetProvider : FutureContentWidget() {
    override fun content(context: Context): WidgetContent {
        val last = HistoryStore(context).entries.value.maxByOrNull { it.time }
            ?: return WidgetContent(value = "אין תרגומים עדיין", subtitle = "תרגם משהו כדי לראות אותו כאן")
        val pair = "${Languages.of(last.from).name} ← ${Languages.of(last.to).name}"
        return WidgetContent(
            value = last.dst,
            subtitle = "$pair · ${FutureContentWidget.ago(last.time)}",
        )
    }
}

/** מספר התרגומים השמורים, וזוג השפות הנוכחי בבורר. */
class TranslateSavedWidgetProvider : FutureContentWidget() {
    override fun content(context: Context): WidgetContent {
        val saved = HistoryStore(context).entries.value.count { it.saved }
        val prefs = TranslatePrefs(context)
        val pair = "${Languages.of(prefs.from).name} ← ${Languages.of(prefs.to).name}"
        return WidgetContent(
            value = if (saved == 0) "אין תרגומים שמורים" else "$saved שמורים",
            subtitle = pair,
        )
    }
}
