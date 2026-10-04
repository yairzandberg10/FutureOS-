package com.future.notes

import android.database.sqlite.SQLiteDatabase
import android.content.Context
import com.future.sharednav.widget.FutureContentWidget
import com.future.sharednav.widget.WidgetContent

/** הפתק העליון (נעוץ, או האחרון שנערך) ומספר הפתקים. */
class NotesWidgetProvider : FutureContentWidget() {
    override fun content(context: Context): WidgetContent {
        val file = context.getDatabasePath("note_database")
        if (!file.exists()) return WidgetContent(value = "אין פתקים", subtitle = "OK לפתק חדש")
        SQLiteDatabase.openDatabase(file.path, null, SQLiteDatabase.OPEN_READONLY).use { db ->
            val count = db.rawQuery("SELECT COUNT(*) FROM notes", null).use { if (it.moveToFirst()) it.getInt(0) else 0 }
            if (count == 0) return WidgetContent(value = "אין פתקים", subtitle = "OK לפתק חדש")
            db.rawQuery("SELECT title, content FROM notes ORDER BY isPinned DESC, timestamp DESC LIMIT 1", null).use { c ->
                if (c.moveToFirst()) {
                    val title = c.getString(0).orEmpty().ifBlank { c.getString(1).orEmpty().lineSequence().firstOrNull { it.isNotBlank() }.orEmpty() }
                    return WidgetContent(value = title.ifBlank { "פתק" }, subtitle = if (count == 1) "פתק אחד" else "$count פתקים")
                }
            }
        }
        return WidgetContent(value = "פתקים")
    }
}
