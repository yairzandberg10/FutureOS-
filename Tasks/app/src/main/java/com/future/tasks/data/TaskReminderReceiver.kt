package com.future.tasks.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.future.tasks.TasksApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * רשום במניפסט כבר מהבנייה של 27.9, אבל המחלקה עצמה לא נשמרה בגיט - אתחול
 * של המכשיר היה מפיל את התהליך. מקבל את התזכורת עצמה (ACTION_REMIND) ואת
 * אירועי המערכת שאחריהם צריך לתזמן הכל מחדש.
 */
class TaskReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext as TasksApp
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val dao = app.database.taskDao()
                when (intent.action) {
                    TaskReminders.ACTION_REMIND -> {
                        val id = intent.getIntExtra(TaskReminders.EXTRA_TASK_ID, -1)
                        val task = dao.getTask(id)
                        if (task != null && !task.isDone && task.reminderAt != null) {
                            TaskReminders.notify(context, task)
                            // תזכורת חד-פעמית: אחרי שהוצגה היא נמחקת מהמשימה.
                            dao.updateTask(task.copy(reminderAt = null))
                        }
                    }
                    else -> dao.upcomingReminders(System.currentTimeMillis()).forEach { TaskReminders.schedule(context, it) }
                }
            } finally {
                pending.finish()
            }
        }
    }
}
