package com.future.tasks

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.future.sharednav.components.AnimatedScreenHost
import com.future.sharednav.theme.FutureMaterialTheme
import com.future.sharednav.theme.rememberFutureTheme
import com.future.tasks.ui.TaskViewModel
import com.future.tasks.ui.screens.TaskEditorScreen
import com.future.tasks.ui.screens.TaskListScreen

class MainActivity : ComponentActivity() {
    // המכשיר האמיתי הוא מקלדת T9 בלבד בלי מסך מגע - מבטלים קלט מגע לגמרי כדי
    // שההתנהגות תישאר תואמת לחומרה האמיתית. לא פוגע בניווט/הפעלה במקשים -
    // dispatchKeyEvent הוא נתיב נפרד לגמרי מ-dispatchTouchEvent.
    override fun dispatchTouchEvent(ev: android.view.MotionEvent): Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            // מתעדכן בזמן אמת כשמצב כהה/בהיר או צבע ההדגשה משתנים (ר' rememberFutureTheme).
            val theme = rememberFutureTheme()

            val app = application as TasksApp
            val viewModel: TaskViewModel = viewModel(
                factory = object : ViewModelProvider.Factory {
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        @Suppress("UNCHECKED_CAST")
                        return TaskViewModel(app.repository) as T
                    }
                }
            )

            val tasks by viewModel.tasks.collectAsState()
            val query by viewModel.searchQuery.collectAsState()
            // null = מסך הרשימה, 0 = משימה חדשה, אחרת = עריכת משימה קיימת לפי id.
            var editingTaskId by remember { mutableStateOf<Int?>(null) }
            // נשמר גם אחרי שחוזרים לרשימה - כדי שהפוקוס יחזור למשימה שממנה
            // נכנסנו לעורך, לא תמיד למשימה הראשונה ברשימה.
            var lastSelectedTaskId by remember { mutableStateOf<Int?>(null) }

            BackHandler(enabled = editingTaskId != null) { editingTaskId = null }

            FutureMaterialTheme(theme) {
                // ריווח משורת המצב של FutureUI - במשימות לבקשת המשתמש (2026-09-25),
                // אף ש-StatusBarInset כבוי בשאר האפליקציות.
                androidx.compose.foundation.layout.Box(
                    modifier = androidx.compose.ui.Modifier
                        .fillMaxSize()
                        .background(theme.backgroundColor)
                        .padding(top = com.future.sharednav.systemui.StatusBarInset.HEIGHT_DP.dp),
                ) {
                // ה-id נמסר כמצב: העורך שיוצא באנימציה ממשיך לצייר את המשימה שלו
                // גם אחרי ש-editingTaskId כבר חזר ל-null.
                AnimatedScreenHost(
                    targetState = editingTaskId,
                    depthOf = { if (it == null) 0 else 1 },
                ) { editingId ->
                    if (editingId != null) {
                        val task = if (editingId != 0) tasks.find { it.id == editingId } else null
                        TaskEditorScreen(
                            task = task,
                            theme = theme,
                            onSave = { title, notes, priority, isDone ->
                                viewModel.addOrUpdateTask(editingId, title, notes, priority, isDone)
                                editingTaskId = null
                            },
                            onDelete = {
                                task?.let { viewModel.deleteTask(it) }
                                editingTaskId = null
                            },
                            onBack = { editingTaskId = null },
                        )
                    } else {
                        TaskListScreen(
                            tasks = tasks,
                            searchQuery = query,
                            theme = theme,
                            onSearchChanged = { viewModel.onSearchQueryChanged(it) },
                            onTaskClick = { task -> lastSelectedTaskId = task.id; editingTaskId = task.id },
                            onAddTask = { editingTaskId = 0 },
                            lastSelectedTaskId = lastSelectedTaskId,
                        )
                    }
                }
                }
            }
        }
    }
}
