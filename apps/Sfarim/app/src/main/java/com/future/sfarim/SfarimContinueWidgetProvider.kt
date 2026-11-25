package com.future.sfarim

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import com.future.sfarim.data.LibraryDatabase
import com.future.sharednav.widget.FutureContentWidget
import com.future.sharednav.widget.WidgetContent

/** ההמשך מהקריאה האחרונה: הספר, המקטע, ומתי נקרא. */
class SfarimContinueWidgetProvider : FutureContentWidget() {
    override fun content(context: Context): WidgetContent {
        val file = LibraryDatabase.expectedPath(context)
        if (!file.exists()) return WidgetContent(value = "הספרייה לא הותקנה", subtitle = "פתחו את ספרים", title = "המשך קריאה")
        val last = runCatching {
            SQLiteDatabase.openDatabase(file.path, null, SQLiteDatabase.OPEN_READONLY).use { db ->
                db.rawQuery(
                    "SELECT COALESCE(b.title_he, b.title_en), s.ref_display, rp.updated_at " +
                        "FROM reading_progress rp " +
                        "JOIN books b ON b.id = rp.book_id " +
                        "JOIN segments s ON s.id = rp.segment_id " +
                        "ORDER BY rp.updated_at DESC LIMIT 1",
                    null,
                ).use { c ->
                    if (c.moveToFirst()) Triple(c.getString(0).orEmpty(), c.getString(1).orEmpty(), c.getLong(2)) else null
                }
            }
        }.getOrNull()
        if (last == null) return WidgetContent(value = "אין קריאה אחרונה", subtitle = "פתחו ספר כדי להתחיל", title = "המשך קריאה")
        val subtitle = listOf(last.second, FutureContentWidget.ago(last.third)).filter { it.isNotBlank() }.joinToString(" · ")
        return WidgetContent(value = last.first.ifBlank { "ספר" }, subtitle = subtitle, title = "המשך קריאה")
    }
}
