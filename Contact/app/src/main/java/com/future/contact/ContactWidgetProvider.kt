package com.future.contact

import android.content.pm.PackageManager
import android.provider.ContactsContract
import android.content.Context
import com.future.sharednav.widget.FutureContentWidget
import com.future.sharednav.widget.WidgetContent

/** המועדפים (עד שלושה שמות) ומספר אנשי הקשר. */
class ContactWidgetProvider : FutureContentWidget() {
    override fun content(context: Context): WidgetContent {
        if (context.checkSelfPermission(android.Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            return WidgetContent(value = "אנשי קשר", subtitle = "פתח כדי לאשר גישה")
        }
        val resolver = context.contentResolver
        val total = resolver.query(ContactsContract.Contacts.CONTENT_URI, arrayOf(ContactsContract.Contacts._ID), null, null, null)?.use { it.count } ?: 0
        val favorites = mutableListOf<String>()
        resolver.query(
            ContactsContract.Contacts.CONTENT_URI,
            arrayOf(ContactsContract.Contacts.DISPLAY_NAME_PRIMARY),
            "${ContactsContract.Contacts.STARRED} = 1", null,
            "${ContactsContract.Contacts.DISPLAY_NAME_PRIMARY} COLLATE LOCALIZED ASC",
        )?.use { c -> while (c.moveToNext() && favorites.size < 3) c.getString(0)?.let(favorites::add) }
        return if (favorites.isEmpty()) WidgetContent(value = "$total אנשי קשר", subtitle = "אין מועדפים")
        else WidgetContent(value = favorites.joinToString(", "), subtitle = "מועדפים · $total אנשי קשר")
    }
}
