package com.future.messages

import android.content.pm.PackageManager
import android.provider.Telephony
import com.future.sharednav.contacts.ContactLookup
import android.content.Context
import com.future.sharednav.widget.FutureContentWidget
import com.future.sharednav.widget.WidgetContent

/** הודעות שלא נקראו, והאחרונה שהתקבלה - ממי ומה. */
class MessagesWidgetProvider : FutureContentWidget() {
    override fun content(context: Context): WidgetContent {
        if (context.checkSelfPermission(android.Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) {
            return WidgetContent(value = "הודעות", subtitle = "פתח כדי לאשר גישה")
        }
        val resolver = context.contentResolver
        val unread = resolver.query(Telephony.Sms.Inbox.CONTENT_URI, arrayOf(Telephony.Sms._ID), "${Telephony.Sms.READ} = 0", null, null)?.use { it.count } ?: 0
        val last = resolver.query(
            Telephony.Sms.Inbox.CONTENT_URI,
            arrayOf(Telephony.Sms.ADDRESS, Telephony.Sms.BODY),
            null, null, "${Telephony.Sms.DATE} DESC",
        )?.use { c ->
            if (c.moveToFirst()) {
                val address = c.getString(0).orEmpty()
                val who = runCatching { ContactLookup.nameForNumber(context, address) }.getOrNull() ?: "\u2066$address\u2069"
                "$who: ${c.getString(1).orEmpty().replace('\n', ' ')}"
            } else null
        }
        val value = when (unread) {
            0 -> "אין הודעות חדשות"
            1 -> "הודעה חדשה אחת"
            else -> "$unread הודעות חדשות"
        }
        return WidgetContent(value = value, subtitle = last)
    }
}
