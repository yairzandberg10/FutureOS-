package com.future.navigation

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import com.future.sharednav.widget.FutureContentWidget
import com.future.sharednav.widget.WidgetContent

/** מספר המקומות השמורים (טבלת saved_places ב-navigation.db), ומה כתובת הבית. */
class SavedPlacesWidgetProvider : FutureContentWidget() {
    override fun content(context: Context): WidgetContent {
        val file = context.getDatabasePath("navigation.db")
        if (!file.exists()) return WidgetContent(value = "אין מקומות שמורים", subtitle = "הוסיפו בית או עבודה")
        SQLiteDatabase.openDatabase(file.path, null, SQLiteDatabase.OPEN_READONLY).use { db ->
            val count = db.rawQuery("SELECT COUNT(*) FROM saved_places", null).use { if (it.moveToFirst()) it.getInt(0) else 0 }
            if (count == 0) return WidgetContent(value = "אין מקומות שמורים", subtitle = "הוסיפו בית או עבודה")
            val home = db.rawQuery("SELECT label FROM saved_places WHERE isHome = 1 LIMIT 1", null).use {
                if (it.moveToFirst()) it.getString(0) else null
            }
            return WidgetContent(
                value = if (count == 1) "מקום שמור אחד" else "$count מקומות שמורים",
                subtitle = if (home.isNullOrBlank()) "אין כתובת בית" else "בית: $home",
            )
        }
    }
}
