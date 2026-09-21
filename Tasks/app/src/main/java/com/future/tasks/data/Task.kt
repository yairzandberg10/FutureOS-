package com.future.tasks.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/** נשמר כ-Int פשוט (לא enum) כדי לא להזדקק ל-TypeConverter של Room -
 * בדיוק כמו כל שדה אחר בישויות הקיימות בסוויטה (ר' notes/data/Note.kt). */
object TaskPriority {
    const val LOW = 0
    const val NORMAL = 1
    const val HIGH = 2
}

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val notes: String = "",
    val priority: Int = TaskPriority.NORMAL,
    val isDone: Boolean = false,
    val timestamp: Long = System.currentTimeMillis(),
)
