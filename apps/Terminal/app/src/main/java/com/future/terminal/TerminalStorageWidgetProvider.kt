package com.future.terminal

import android.content.Context
import android.os.Environment
import android.os.StatFs
import com.future.sharednav.widget.FutureContentWidget
import com.future.sharednav.widget.WidgetContent
import java.util.Locale

/** שטח האחסון הפנוי במחיצת הנתונים, ואחוז השימוש. */
class TerminalStorageWidgetProvider : FutureContentWidget() {
    override fun content(context: Context): WidgetContent {
        val stat = runCatching { StatFs(Environment.getDataDirectory().path) }.getOrNull()
        val total = stat?.totalBytes ?: 0L
        if (stat == null || total <= 0L) return WidgetContent(value = "אחסון לא זמין", title = "אחסון")
        val free = stat.availableBytes
        val usedPercent = ((total - free) * 100 / total).coerceIn(0L, 100L)
        return WidgetContent(
            value = "פנוי ${gb(free)} GB",
            subtitle = "מתוך ${gb(total)} GB · $usedPercent% בשימוש",
            title = "אחסון",
        )
    }

    private fun gb(bytes: Long): String = String.format(Locale.US, "%.1f", bytes / 1_000_000_000.0)
}
