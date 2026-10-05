package com.future.tasks.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.future.tasks.data.Task
import com.future.tasks.data.TaskRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * [scheduleReminder]/[cancelReminder] - ל-AlarmManager (ר' TaskReminders); מוזרקים
 * מה-Activity כדי שה-ViewModel לא יחזיק Context.
 */
class TaskViewModel(
    private val repository: TaskRepository,
    private val scheduleReminder: (Task) -> Unit = {},
    private val cancelReminder: (Int) -> Unit = {},
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    @OptIn(ExperimentalCoroutinesApi::class)
    val tasks: StateFlow<List<Task>> = _searchQuery
        .map { it.trim() }
        .flatMapLatest { query ->
            if (query.isEmpty()) repository.allTasks else repository.searchTasks(query)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun addOrUpdateTask(id: Int = 0, title: String, notes: String, priority: Int, isDone: Boolean = false, reminderAt: Long? = null) {
        viewModelScope.launch {
            val task = Task(id = id, title = title, notes = notes, priority = priority, isDone = isDone, timestamp = System.currentTimeMillis(), reminderAt = reminderAt)
            val savedId = if (id == 0) repository.insert(task) else { repository.update(task); id }
            scheduleReminder(task.copy(id = savedId))
        }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch {
            repository.delete(task)
            cancelReminder(task.id)
        }
    }

    fun toggleDone(task: Task) {
        viewModelScope.launch {
            val updated = task.copy(isDone = !task.isDone)
            repository.update(updated)
            // משימה שבוצעה לא מזכירה; משימה שנפתחה מחדש - התזכורת חוזרת (אם עוד בעתיד).
            scheduleReminder(updated)
        }
    }
}
