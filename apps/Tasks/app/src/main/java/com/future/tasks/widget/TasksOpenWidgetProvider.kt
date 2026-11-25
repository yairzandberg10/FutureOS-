package com.future.tasks.widget

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import com.future.sharednav.widget.FutureContentWidget
import com.future.sharednav.widget.WidgetContent

/** מספר המשימות הפתוחות, והחשובה ביותר מביניהן (עדיפות גבוהה קודם). */
class TasksOpenWidgetProvider : FutureContentWidget() {
    override fun content(context: Context): WidgetContent {
        val file = context.getDatabasePath("task_database")
        if (!file.exists()) return WidgetContent(value = "אין משימות", subtitle = "OK למשימה חדשה")
        SQLiteDatabase.openDatabase(file.path, null, SQLiteDatabase.OPEN_READONLY).use { db ->
            val count = db.rawQuery("SELECT COUNT(*) FROM tasks WHERE isDone = 0", null).use { if (it.moveToFirst()) it.getInt(0) else 0 }
            if (count == 0) return WidgetContent(value = "אין משימות פתוחות", subtitle = "הכול בוצע")
            val next = db.rawQuery(
                "SELECT title FROM tasks WHERE isDone = 0 ORDER BY priority DESC, timestamp DESC LIMIT 1",
                null,
            ).use { if (it.moveToFirst()) it.getString(0) else null }
            val value = if (count == 1) "משימה פתוחה אחת" else "$count משימות פתוחות"
            return WidgetContent(value = value, subtitle = next?.takeIf { it.isNotBlank() }, title = "משימות")
        }
    }
}
