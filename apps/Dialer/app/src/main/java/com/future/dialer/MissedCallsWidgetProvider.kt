package com.future.dialer

import android.content.pm.PackageManager
import android.content.Context
import android.provider.CallLog
import com.future.sharednav.widget.FutureContentWidget
import com.future.sharednav.widget.WidgetContent

/** שיחות שלא נענו ב-24 השעות האחרונות: מספרן, והשיחה האחרונה שבהן. */
class MissedCallsWidgetProvider : FutureContentWidget() {
    override fun content(context: Context): WidgetContent {
        val title = "שיחות שלא נענו"
        if (context.checkSelfPermission(android.Manifest.permission.READ_CALL_LOG) != PackageManager.PERMISSION_GRANTED) {
            return WidgetContent(value = title, subtitle = "פתח כדי לאשר גישה", title = title)
        }
        // בלי IS_READ: האפליקציה לא מסמנת שיחות כנקראו, אז "חדשות" היה גדל לנצח
        val since = System.currentTimeMillis() - DAY_MILLIS
        val where = "${CallLog.Calls.TYPE} = ? AND ${CallLog.Calls.DATE} > ?"
        val args = arrayOf(CallLog.Calls.MISSED_TYPE.toString(), since.toString())
        val resolver = context.contentResolver
        val count = resolver.query(CallLog.Calls.CONTENT_URI, arrayOf(CallLog.Calls._ID), where, args, null)
            ?.use { it.count } ?: 0
        if (count == 0) return WidgetContent(value = "אין שיחות", subtitle = "ב-24 השעות האחרונות", title = title)
        resolver.query(
            CallLog.Calls.CONTENT_URI,
            arrayOf(CallLog.Calls.CACHED_NAME, CallLog.Calls.NUMBER, CallLog.Calls.DATE),
            where, args, "${CallLog.Calls.DATE} DESC",
        )?.use { c ->
            if (c.moveToFirst()) {
                val who = c.getString(0)?.takeIf { it.isNotBlank() }
                    ?: c.getString(1)?.takeIf { it.isNotBlank() }?.let { "⁦$it⁩" }
                    ?: "מספר חסוי"
                return WidgetContent(
                    value = "$count שלא נענו",
                    subtitle = "$who · ${ago(c.getLong(2))}",
                    title = title,
                )
            }
        }
        return WidgetContent(value = "$count שלא נענו", title = title)
    }

    private companion object {
        const val DAY_MILLIS = 24L * 60 * 60 * 1000
    }
}
