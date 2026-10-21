package com.future.dialer

import android.content.pm.PackageManager
import android.provider.CallLog
import android.content.Context
import com.future.sharednav.widget.FutureContentWidget
import com.future.sharednav.widget.WidgetContent

/** השיחה האחרונה: מי, איזה סוג, ומתי. */
class DialerWidgetProvider : FutureContentWidget() {
    override fun content(context: Context): WidgetContent {
        if (context.checkSelfPermission(android.Manifest.permission.READ_CALL_LOG) != PackageManager.PERMISSION_GRANTED) {
            return WidgetContent(value = "טלפון", subtitle = "פתח כדי לאשר גישה")
        }
        context.contentResolver.query(
            CallLog.Calls.CONTENT_URI,
            arrayOf(CallLog.Calls.CACHED_NAME, CallLog.Calls.NUMBER, CallLog.Calls.TYPE, CallLog.Calls.DATE),
            null, null, "${CallLog.Calls.DATE} DESC",
        )?.use { c ->
            if (c.moveToFirst()) {
                val who = c.getString(0)?.takeIf { it.isNotBlank() } ?: c.getString(1)?.let { "\u2066$it\u2069" } ?: "מספר חסוי"
                val type = when (c.getInt(2)) {
                    CallLog.Calls.INCOMING_TYPE -> "נכנסת"
                    CallLog.Calls.OUTGOING_TYPE -> "יוצאת"
                    CallLog.Calls.MISSED_TYPE -> "שלא נענתה"
                    CallLog.Calls.REJECTED_TYPE -> "שנדחתה"
                    CallLog.Calls.BLOCKED_TYPE -> "שנחסמה"
                    else -> ""
                }
                // בלי סוג מוכר אין רווח כפול ("שיחה  ·")
                val label = if (type.isEmpty()) "שיחה" else "שיחה $type"
                return WidgetContent(value = who, subtitle = "$label · ${ago(c.getLong(3))}", title = "שיחה אחרונה")
            }
        }
        return WidgetContent(value = "אין שיחות", subtitle = null)
    }
}
