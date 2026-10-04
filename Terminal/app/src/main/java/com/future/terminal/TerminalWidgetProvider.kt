package com.future.terminal

import android.os.SystemClock
import java.io.File
import android.content.Context
import com.future.sharednav.widget.FutureContentWidget
import com.future.sharednav.widget.WidgetContent

/** כמה זמן המכשיר פועל מאז ההפעלה, והאם יש root. */
class TerminalWidgetProvider : FutureContentWidget() {
    override fun content(context: Context): WidgetContent {
        val minutes = SystemClock.elapsedRealtime() / 60_000L
        val days = minutes / (24 * 60)
        val hours = (minutes / 60) % 24
        val uptime = when {
            days > 0 -> "$days ימים, $hours שעות"
            hours > 0 -> "$hours שעות, ${minutes % 60} דקות"
            else -> "${minutes} דקות"
        }
        val root = listOf("/system/bin/su", "/system/xbin/su", "/sbin/su", "/system/sbin/su", "/vendor/bin/su")
            .any { File(it).exists() }
        return WidgetContent(value = "פועל $uptime", subtitle = if (root) "root זמין" else "ללא root")
    }
}
