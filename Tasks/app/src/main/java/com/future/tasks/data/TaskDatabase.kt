package com.future.tasks.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * גרסה 3: עמודת reminderAt (תזכורת). גרסה 2 הייתה בבנייה שהותקנה במכשיר
 * ב-27.9 ולא נשמרה בגיט, והסכמה שלה לא ידועה - לכן המעבר ממנה בונה את הטבלה
 * מחדש ומעתיק רק את העמודות שקיימות, במקום להניח מבנה.
 */
@Database(entities = [Task::class], version = 3, exportSchema = false)
abstract class TaskDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao

    companion object {
        private const val CREATE_TASKS =
            "CREATE TABLE IF NOT EXISTS `%s` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`title` TEXT NOT NULL, `notes` TEXT NOT NULL, `priority` INTEGER NOT NULL, " +
                "`isDone` INTEGER NOT NULL, `timestamp` INTEGER NOT NULL, `reminderAt` INTEGER)"

        val MIGRATION_1_3 = object : Migration(1, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `tasks` ADD COLUMN `reminderAt` INTEGER")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                val columns = mutableSetOf<String>()
                db.query("PRAGMA table_info(`tasks`)").use { c ->
                    val nameIndex = c.getColumnIndex("name")
                    while (c.moveToNext()) columns += c.getString(nameIndex)
                }
                db.execSQL(CREATE_TASKS.format("tasks_new"))
                val base = listOf("id", "title", "notes", "priority", "isDone", "timestamp").filter { it in columns }
                // שם עמודת התזכורת בגרסה 2 לא ידוע - לוקחים את הראשונה שנראית כמו תזכורת.
                val reminder = columns.firstOrNull { it.contains("remind", ignoreCase = true) }
                val target = base + listOfNotNull(reminder?.let { "reminderAt" })
                val source = base + listOfNotNull(reminder)
                if (base.isNotEmpty()) {
                    db.execSQL(
                        "INSERT INTO `tasks_new` (${target.joinToString { "`$it`" }}) " +
                            "SELECT ${source.joinToString { "`$it`" }} FROM `tasks`",
                    )
                }
                db.execSQL("DROP TABLE `tasks`")
                db.execSQL("ALTER TABLE `tasks_new` RENAME TO `tasks`")
            }
        }
    }
}
