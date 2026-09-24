package com.future.notes

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.LaunchedEffect
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
import com.future.notes.data.NoteShare
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

    // טקסט משותף (ACTION_SEND) שעוד לא נשמר כפתק.
    private var pendingShare by mutableStateOf<Intent?>(null)

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.action == Intent.ACTION_SEND) pendingShare = intent
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null && intent?.action == Intent.ACTION_SEND) pendingShare = intent

        val prefs = getSharedPreferences("notes_ui", MODE_PRIVATE)

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
                var gridView by remember { mutableStateOf(prefs.getBoolean("grid", false)) }
                val viewModel: NoteViewModel = viewModel(
                    factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                            @Suppress("UNCHECKED_CAST")
                            return NoteViewModel(repository) as T
                        }
                    }
                )

                LaunchedEffect(pendingShare) {
                    val share = pendingShare ?: return@LaunchedEffect
                    pendingShare = null
                    val text = share.getStringExtra(Intent.EXTRA_TEXT).orEmpty()
                    if (text.isNotBlank()) {
                        val parsed = NoteShare.parse(text, share.getStringExtra(Intent.EXTRA_SUBJECT))
                        viewModel.importShared(parsed.title, parsed.content, parsed.isChecklist)
                        Toast.makeText(this@MainActivity, "הפתק נשמר", Toast.LENGTH_SHORT).show()
                    }
                }

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
                            onAddChecklist = { navController.navigate("editor/0?checklist=true") },
                            onDeleteNote = { viewModel.deleteNote(it) },
                            gridView = gridView,
                            onToggleGrid = {
                                gridView = !gridView
                                prefs.edit().putBoolean("grid", gridView).apply()
                            },
                            lastSelectedNoteId = lastSelectedNoteId,
                        )
                    }
                    composable(
                        "editor/{noteId}?checklist={checklist}",
                        arguments = listOf(
                            navArgument("noteId") { type = NavType.IntType },
                            navArgument("checklist") { type = NavType.BoolType; defaultValue = false },
                        )
                    ) { backStackEntry ->
                        val noteId = backStackEntry.arguments?.getInt("noteId") ?: 0
                        val checklist = backStackEntry.arguments?.getBoolean("checklist") ?: false

                        EditorScreen(
                            noteId = noteId,
                            theme = theme,
                            startAsChecklist = checklist,
                            loadNote = { viewModel.getNote(it) },
                            persist = { viewModel.save(it) },
                            onDelete = { note ->
                                viewModel.deleteNote(note)
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
