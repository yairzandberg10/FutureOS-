package com.future.notes.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notes")
data class Note(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isPinned: Boolean = false,
    /** פתק-רשימה: content מחזיק שורה לכל פריט, ר' [Checklist]. */
    val isChecklist: Boolean = false,
    /** הקלטה קולית מצורפת - קובץ בתיקיית audio של האפליקציה. */
    val audioPath: String? = null,
)
