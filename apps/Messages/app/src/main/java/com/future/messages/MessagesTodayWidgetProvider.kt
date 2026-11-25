package com.future.messages

import android.content.Context
import android.content.pm.PackageManager
import android.provider.Telephony
import com.future.sharednav.widget.FutureContentWidget
import com.future.sharednav.widget.WidgetContent
import java.util.Calendar

/** מספר ההודעות שנכנסו והודעות שנשלחו מאז חצות. */
class MessagesTodayWidgetProvider : FutureContentWidget() {
    override fun content(context: Context): WidgetContent {
        if (context.checkSelfPermission(android.Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) {
            return WidgetContent(value = "הודעות היום", subtitle = "פתח כדי לאשר גישה", title = "היום")
        }
        val midnight = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val resolver = context.contentResolver
        val selection = "${Telephony.Sms.DATE} >= ?"
        val args = arrayOf(midnight.toString())
        val received = resolver.query(Telephony.Sms.Inbox.CONTENT_URI, arrayOf(Telephony.Sms._ID), selection, args, null)?.use { it.count } ?: 0
        val sent = resolver.query(Telephony.Sms.Sent.CONTENT_URI, arrayOf(Telephony.Sms._ID), selection, args, null)?.use { it.count } ?: 0
        val value = when (received) {
            0 -> "אין הודעות היום"
            1 -> "הודעה אחת היום"
            else -> "$received הודעות היום"
        }
        return WidgetContent(value = value, subtitle = if (sent == 0) "לא נשלחו הודעות" else "נשלחו $sent", title = "היום")
    }
}
