package com.future.tasks.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    // משימות שבוצעו יורדות לתחתית הרשימה; בתוך כל קבוצה, עדיפות גבוהה קודם,
    // ואז החדשות ביותר.
    @Query("SELECT * FROM tasks ORDER BY isDone ASC, priority DESC, timestamp DESC")
    fun getAllTasks(): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE title LIKE '%' || :query || '%' OR notes LIKE '%' || :query || '%' ORDER BY isDone ASC, priority DESC, timestamp DESC")
    fun searchTasks(query: String): Flow<List<Task>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: Task): Long

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getTask(id: Int): Task?

    /** משימות פתוחות עם תזכורת עתידית - לתזמון מחדש אחרי אתחול/שינוי שעה. */
    @Query("SELECT * FROM tasks WHERE isDone = 0 AND reminderAt IS NOT NULL AND reminderAt > :now")
    suspend fun upcomingReminders(now: Long): List<Task>

    @Delete
    suspend fun deleteTask(task: Task)

    @Update
    suspend fun updateTask(task: Task)
}
