package com.future.tasks

import android.app.Application
import androidx.room.Room
import com.future.tasks.data.TaskDatabase
import com.future.tasks.data.TaskRepository

class TasksApp : Application() {
    val database by lazy {
        Room.databaseBuilder(this, TaskDatabase::class.java, "task_database").build()
    }
    val repository by lazy { TaskRepository(database.taskDao()) }
}
