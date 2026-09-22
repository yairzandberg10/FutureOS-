package com.future.notes

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.future.notes.ui.NoteViewModel
import com.future.notes.ui.screens.EditorScreen
import com.future.notes.ui.screens.ListScreen
import com.future.sharednav.theme.FutureAppTheme
import com.future.sharednav.theme.FutureTransitions
import com.future.sharednav.theme.rememberFutureTheme

class MainActivity : ComponentActivity() {
    // המכשיר האמיתי הוא מקלדת T9 בלבד בלי מסך מגע - מבטלים קלט מגע לגמרי כדי
    // שההתנהגות תישאר תואמת לחומרה האמיתית. לא פוגע בניווט/הפעלה במקשים -
    // dispatchKeyEvent הוא נתיב נפרד לגמרי מ-dispatchTouchEvent.
    override fun dispatchTouchEvent(ev: android.view.MotionEvent): Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            // מתעדכן בזמן אמת כשהעיצוב משתנה מ-FutureUI (קונטרול סנטר/הגדרות),
            // גם כשהשינוי נעשה מעל האפליקציה בלי לצאת ממנה.
            val theme = rememberFutureTheme()

            FutureAppTheme(theme) {
                val app = application as NotesApp
                val repository = app.repository
                val navController = rememberNavController()
                // נשמר גם אחרי שחוזרים ל-list - כדי שהפוקוס יחזור לפתק
                // שממנו נכנסנו לעורך, לא תמיד לפתק הראשון ברשימה.
                var lastSelectedNoteId by remember { mutableStateOf<Int?>(null) }
                val viewModel: NoteViewModel = viewModel(
                    factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                            @Suppress("UNCHECKED_CAST")
                            return NoteViewModel(repository) as T
                        }
                    }
                )

                NavHost(
                    navController = navController,
                    startDestination = "list",
                    enterTransition = { FutureTransitions.navEnter },
                    exitTransition = { FutureTransitions.navExit },
                    popEnterTransition = { FutureTransitions.navPopEnter },
                    popExitTransition = { FutureTransitions.navPopExit },
                ) {
                    composable("list") {
                        val notes by viewModel.notes.collectAsState()
                        val query by viewModel.searchQuery.collectAsState()
                        ListScreen(
                            notes = notes,
                            searchQuery = query,
                            theme = theme,
                            onSearchChanged = { viewModel.onSearchQueryChanged(it) },
                            onNoteClick = { note -> lastSelectedNoteId = note.id; navController.navigate("editor/${note.id}") },
                            onAddNote = { navController.navigate("editor/0") },
                            onTogglePin = { viewModel.togglePin(it) },
                            lastSelectedNoteId = lastSelectedNoteId,
                        )
                    }
                    composable(
                        "editor/{noteId}",
                        arguments = listOf(navArgument("noteId") { type = NavType.IntType })
                    ) { backStackEntry ->
                        val noteId = backStackEntry.arguments?.getInt("noteId") ?: 0
                        val notes by viewModel.notes.collectAsState()
                        val note = if (noteId != 0) notes.find { it.id == noteId } else null

                        EditorScreen(
                            note = note,
                            theme = theme,
                            onSave = { title, content, isPinned ->
                                viewModel.addOrUpdateNote(noteId, title, content, isPinned)
                                navController.popBackStack()
                            },
                            onDelete = {
                                note?.let { viewModel.deleteNote(it) }
                                navController.popBackStack()
                            },
                            onBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}
