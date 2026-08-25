package com.future.notes

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.future.notes.theme.ThemeClient
import com.future.notes.ui.NoteViewModel
import com.future.notes.ui.screens.EditorScreen
import com.future.notes.ui.screens.ListScreen
import com.future.notes.ui.theme.NotesTheme

class MainActivity : ComponentActivity() {
    // המכשיר האמיתי הוא מקלדת T9 בלבד בלי מסך מגע - מבטלים קלט מגע לגמרי כדי
    // שההתנהגות תישאר תואמת לחומרה האמיתית. לא פוגע בניווט/הפעלה במקשים -
    // dispatchKeyEvent הוא נתיב נפרד לגמרי מ-dispatchTouchEvent.
    override fun dispatchTouchEvent(ev: android.view.MotionEvent): Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            // נקרא פעם אחת ואז מתעדכן בכל ON_RESUME - כך שינוי עיצוב שנעשה
            // מ-FutureUI (קונטרול סנטר/הגדרות) בזמן שהאפליקציה ברקע מתעדכן כאן
            // ברגע שחוזרים אליה, בדיוק כמו בכל שאר אפליקציות FutureOS.
            var sharedTheme by remember { mutableStateOf(ThemeClient.getTheme(this)) }
            val lifecycleOwner = LocalLifecycleOwner.current
            DisposableEffect(lifecycleOwner) {
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_RESUME) {
                        sharedTheme = ThemeClient.getTheme(this@MainActivity)
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
            }

            NotesTheme(isDarkMode = sharedTheme.isDarkMode, accentColor = Color(sharedTheme.primaryColor)) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val app = application as NotesApp
                    val repository = app.repository
                    val navController = rememberNavController()
                    val viewModel: NoteViewModel = viewModel(
                        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                                return NoteViewModel(repository) as T
                            }
                        }
                    )

                    NavHost(navController = navController, startDestination = "list") {
                        composable("list") {
                            val notes by viewModel.notes.collectAsState()
                            val query by viewModel.searchQuery.collectAsState()
                            ListScreen(
                                notes = notes,
                                searchQuery = query,
                                onSearchChanged = { viewModel.onSearchQueryChanged(it) },
                                onNoteClick = { note -> navController.navigate("editor/${note.id}") },
                                onAddNote = { navController.navigate("editor/0") },
                                onTogglePin = { viewModel.togglePin(it) }
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
}
