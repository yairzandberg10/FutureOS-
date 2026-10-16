package com.future.contact

import android.content.pm.PackageManager
import android.content.Context
import com.future.contact.data.ContactSort
import com.future.contact.data.ContactsRepository
import com.future.sharednav.widget.FutureContentWidget
import com.future.sharednav.widget.WidgetContent

/** המועדפים (עד שלושה שמות) ומספר אנשי הקשר - אותו מסנן ואותו מיון כמו באפליקציה. */
class ContactWidgetProvider : FutureContentWidget() {
    override fun content(context: Context): WidgetContent {
        if (context.checkSelfPermission(android.Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            return WidgetContent(value = "אנשי קשר", subtitle = "פתח כדי לאשר גישה")
        }
        val sort = context.getSharedPreferences("contacts_ui", Context.MODE_PRIVATE)
            .getString("sort", null)?.let { name -> ContactSort.entries.firstOrNull { it.name == name } } ?: ContactSort.FIRST_NAME
        val all = ContactsRepository(context).getAllContacts(sort)
        val favorites = all.filter { it.isFavorite && !it.isBlocked }.take(3).map { it.name }
        val total = all.size
        return if (favorites.isEmpty()) WidgetContent(value = "$total אנשי קשר", subtitle = "אין מועדפים")
        else WidgetContent(value = favorites.joinToString(", "), subtitle = "מועדפים · $total אנשי קשר")
    }
}
