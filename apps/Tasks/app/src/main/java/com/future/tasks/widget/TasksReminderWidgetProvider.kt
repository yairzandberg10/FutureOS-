package com.future.tasks.widget

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import com.future.sharednav.widget.FutureContentWidget
import com.future.sharednav.widget.WidgetContent
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** התזכורת הבאה של משימה פתוחה (רק תזכורות שעוד לא עברו). */
class TasksReminderWidgetProvider : FutureContentWidget() {
    override fun content(context: Context): WidgetContent {
        val file = context.getDatabasePath("task_database")
        if (!file.exists()) return WidgetContent(value = "אין תזכורות", subtitle = "משימה עם תזכורת תופיע כאן", title = "תזכורת הבאה")
        SQLiteDatabase.openDatabase(file.path, null, SQLiteDatabase.OPEN_READONLY).use { db ->
            val now = System.currentTimeMillis()
            db.rawQuery(
                "SELECT title, reminderAt FROM tasks WHERE isDone = 0 AND reminderAt IS NOT NULL AND reminderAt >= ? " +
                    "ORDER BY reminderAt ASC LIMIT 1",
                arrayOf(now.toString()),
            ).use { c ->
                if (c.moveToFirst()) {
                    val title = c.getString(0).orEmpty().ifBlank { "משימה" }
                    val at = SimpleDateFormat("dd.MM HH:mm", Locale.forLanguageTag("he")).format(Date(c.getLong(1)))
                    return WidgetContent(value = title, subtitle = "תזכורת ב-$at", title = "תזכורת הבאה")
                }
            }
        }
        return WidgetContent(value = "אין תזכורות", subtitle = "משימה עם תזכורת תופיע כאן", title = "תזכורת הבאה")
    }
}
