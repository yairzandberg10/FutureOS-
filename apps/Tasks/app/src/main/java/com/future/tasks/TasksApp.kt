package com.future.tasks

import android.app.Application
import androidx.room.Room
import com.future.tasks.data.TaskDatabase
import com.future.tasks.data.TaskRepository

class TasksApp : Application() {
    val database by lazy {
        Room.databaseBuilder(this, TaskDatabase::class.java, "task_database")
            .addMigrations(TaskDatabase.MIGRATION_1_3, TaskDatabase.MIGRATION_2_3)
            // גרסה גבוהה יותר במכשיר (בנייה עתידית) - לא קורסים בפתיחה.
            .fallbackToDestructiveMigrationOnDowngrade(dropAllTables = true)
            .build()
    }
    val repository by lazy { TaskRepository(database.taskDao()) }
}
