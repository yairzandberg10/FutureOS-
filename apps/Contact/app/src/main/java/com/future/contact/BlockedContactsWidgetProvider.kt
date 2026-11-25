package com.future.contact

import android.content.Context
import android.content.pm.PackageManager
import com.future.contact.data.ContactsRepository
import com.future.sharednav.widget.FutureContentWidget
import com.future.sharednav.widget.WidgetContent

/** מספר אנשי הקשר החסומים, ושניים מהם בשורת המשנה. */
class BlockedContactsWidgetProvider : FutureContentWidget() {
    override fun content(context: Context): WidgetContent {
        if (context.checkSelfPermission(android.Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            return WidgetContent(value = "חסומים", subtitle = "פתח כדי לאשר גישה", title = "אנשי קשר חסומים")
        }
        val blocked = ContactsRepository(context).getAllContacts().filter { it.isBlocked }
        if (blocked.isEmpty()) return WidgetContent(value = "אין חסומים", title = "אנשי קשר חסומים")
        return WidgetContent(
            value = "${blocked.size} חסומים",
            subtitle = blocked.take(2).joinToString(", ") { it.name },
            title = "אנשי קשר חסומים",
        )
    }
}
