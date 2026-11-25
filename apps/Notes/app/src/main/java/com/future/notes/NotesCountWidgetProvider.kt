package com.future.notes

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import com.future.sharednav.widget.FutureContentWidget
import com.future.sharednav.widget.WidgetContent

/** מספר הפתקים הכולל, ומתוכם כמה נעוצים. */
class NotesCountWidgetProvider : FutureContentWidget() {
    override fun content(context: Context): WidgetContent {
        val file = context.getDatabasePath("note_database")
        if (!file.exists()) return WidgetContent(value = "אין פתקים", subtitle = "OK לפתק חדש")
        SQLiteDatabase.openDatabase(file.path, null, SQLiteDatabase.OPEN_READONLY).use { db ->
            val total = db.rawQuery("SELECT COUNT(*) FROM notes", null).use { if (it.moveToFirst()) it.getInt(0) else 0 }
            if (total == 0) return WidgetContent(value = "אין פתקים", subtitle = "OK לפתק חדש")
            val pinned = db.rawQuery("SELECT COUNT(*) FROM notes WHERE isPinned = 1", null).use { if (it.moveToFirst()) it.getInt(0) else 0 }
            return WidgetContent(
                value = if (total == 1) "פתק אחד" else "$total פתקים",
                subtitle = if (pinned == 0) "אין נעוצים" else "$pinned נעוצים",
            )
        }
    }
}
