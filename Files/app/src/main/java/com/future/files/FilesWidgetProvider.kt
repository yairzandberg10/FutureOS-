package com.future.files

import android.os.Environment
import android.os.StatFs
import java.util.Locale
import android.content.Context
import com.future.sharednav.widget.FutureContentWidget
import com.future.sharednav.widget.WidgetContent

/** נפח פנוי באחסון (סטטיסטיקת מערכת קבצים, בלי הרשאת אחסון). */
class FilesWidgetProvider : FutureContentWidget() {
    override fun content(context: Context): WidgetContent {
        val stat = StatFs(Environment.getExternalStorageDirectory().path)
        val gb = 1024.0 * 1024.0 * 1024.0
        val free = stat.availableBytes / gb
        val total = stat.totalBytes / gb
        val usedPercent = if (total > 0) ((1 - free / total) * 100).toInt() else 0
        return WidgetContent(
            value = String.format(Locale.US, "%.1fGB פנויים", free),
            subtitle = String.format(Locale.US, "מתוך %.0fGB · %d%% בשימוש", total, usedPercent),
        )
    }
}
