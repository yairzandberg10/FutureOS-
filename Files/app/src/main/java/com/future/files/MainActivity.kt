package com.future.files

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.content.FileProvider
import com.future.files.data.FileCategory
import com.future.files.data.FileEntry
import com.future.files.data.FileRepository
import com.future.files.data.categorize
import com.future.files.theme.ThemeClient
import com.future.files.ui.AudioPlayerScreen
import com.future.files.ui.FilesScreen
import com.future.files.ui.ImageFileViewerScreen
import com.future.files.ui.PdfViewerScreen
import com.future.files.ui.TextViewerScreen
import com.future.files.ui.theme.FutureTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class MainActivity : ComponentActivity() {
    // המכשיר האמיתי הוא מקלדת T9 בלבד בלי מסך מגע - מבטלים קלט מגע לגמרי כדי
    // שההתנהגות תישאר תואמת לחומרה האמיתית. לא פוגע בניווט/הפעלה במקשים -
    // dispatchKeyEvent הוא נתיב נפרד לגמרי מ-dispatchTouchEvent.
    override fun dispatchTouchEvent(ev: android.view.MotionEvent): Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val repository = remember { FileRepository() }
            var hasAccess by remember { mutableStateOf(repository.hasAllFilesAccess()) }
            val root = remember { repository.rootDirectory() }
            var currentDir by remember { mutableStateOf(root) }
            var entries by remember { mutableStateOf(listOf<FileEntry>()) }
            var viewingFile by remember { mutableStateOf<FileEntry?>(null) }
            var clipboardEntry by remember { mutableStateOf<FileEntry?>(null) }
            var clipboardIsMove by remember { mutableStateOf(false) }
            // מציין שפעולת קבצים כבדה (מחיקה/העתקה/העברה) רצה כרגע ברקע כדי
            // שהתור העליון של ה-UI thread לא ייחסם בתיקיות גדולות.
            var isBusy by remember { mutableStateOf(false) }
            val coroutineScope = rememberCoroutineScope()
            var theme by remember {
                mutableStateOf(
                    ThemeClient.getTheme(this@MainActivity).let {
                        FutureTheme(isDarkMode = it.isDarkMode, accentColor = Color(it.primaryColor))
                    }
                )
            }

            val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
            DisposableEffect(lifecycleOwner) {
                val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
                    if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                        hasAccess = repository.hasAllFilesAccess()
                        val shared = ThemeClient.getTheme(this@MainActivity)
                        theme = FutureTheme(isDarkMode = shared.isDarkMode, accentColor = Color(shared.primaryColor))
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
            }

            val isRootDir = currentDir.absolutePath == root.absolutePath
            val isCurrentDirTopLevel = currentDir.parentFile?.absolutePath == root.absolutePath

            LaunchedEffect(hasAccess, currentDir) {
                if (hasAccess) entries = repository.listDirectory(currentDir, isRootDir)
            }

            fun goUp() {
                val parent = currentDir.parentFile
                if (parent != null && currentDir.absolutePath != root.absolutePath) {
                    currentDir = if (parent.absolutePath.length < root.absolutePath.length) root else parent
                }
            }

            BackHandler(enabled = viewingFile != null || currentDir.absolutePath != root.absolutePath) {
                if (viewingFile != null) viewingFile = null else goUp()
            }

            Surface(modifier = Modifier.fillMaxSize(), color = theme.backgroundColor) {
                val viewing = viewingFile
                if (viewing != null) {
                    when (categorize(viewing.file)) {
                        FileCategory.TEXT -> TextViewerScreen(viewing.file, theme, onBack = { viewingFile = null })
                        FileCategory.IMAGE -> ImageFileViewerScreen(viewing.file, theme, onBack = { viewingFile = null })
                        FileCategory.AUDIO -> AudioPlayerScreen(viewing.file, theme, onBack = { viewingFile = null })
                        FileCategory.PDF -> PdfViewerScreen(viewing.file, theme, onBack = { viewingFile = null })
                        else -> {}
                    }
                } else FilesScreen(
                    currentDir = currentDir,
                    entries = entries,
                    hasAccess = hasAccess,
                    isRoot = currentDir.absolutePath == root.absolutePath,
                    currentDirIsTopLevelFolder = isCurrentDirTopLevel,
                    theme = theme,
                    hasClipboard = clipboardEntry != null,
                    onRequestAccess = {
                        if (Build.VERSION.SDK_INT >= 30) {
                            try {
                                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                                intent.data = Uri.parse("package:$packageName")
                                startActivity(intent)
                            } catch (e: Exception) {
                                startActivity(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION))
                            }
                        }
                    },
                    onOpenFile = { entry ->
                        if (entry.isDirectory) {
                            currentDir = entry.file
                        } else {
                            when (categorize(entry.file)) {
                                FileCategory.APK -> android.widget.Toast.makeText(this@MainActivity, "התקנת אפליקציות אינה נתמכת במכשיר הזה", android.widget.Toast.LENGTH_SHORT).show()
                                FileCategory.VIDEO, FileCategory.OTHER -> openFile(entry.file)
                                else -> viewingFile = entry
                            }
                        }
                    },
                    onBack = { goUp() },
                    onNewFolder = { name ->
                        if (repository.createFolder(currentDir, name)) {
                            entries = repository.listDirectory(currentDir, isRootDir)
                        } else {
                            android.widget.Toast.makeText(this@MainActivity, "לא ניתן ליצור את התיקייה", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    },
                    onRename = { entry, newName ->
                        if (repository.renameEntry(entry.file, newName)) {
                            entries = repository.listDirectory(currentDir, isRootDir)
                        } else {
                            android.widget.Toast.makeText(this@MainActivity, "לא ניתן לשנות את השם", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    },
                    isBusy = isBusy,
                    onDelete = { entry ->
                        if (!isBusy) {
                            isBusy = true
                            coroutineScope.launch {
                                val ok = withContext(Dispatchers.IO) { repository.deleteEntry(entry.file) }
                                if (ok) {
                                    entries = repository.listDirectory(currentDir, isRootDir)
                                } else {
                                    android.widget.Toast.makeText(this@MainActivity, "לא ניתן למחוק", android.widget.Toast.LENGTH_SHORT).show()
                                }
                                isBusy = false
                            }
                        }
                    },
                    onShare = { entry ->
                        try {
                            val uri = FileProvider.getUriForFile(this@MainActivity, "$packageName.fileprovider", entry.file)
                            val mime = contentResolver.getType(uri) ?: "*/*"
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = mime
                                putExtra(Intent.EXTRA_STREAM, uri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            startActivity(Intent.createChooser(shareIntent, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                        } catch (e: Exception) {
                            android.widget.Toast.makeText(this@MainActivity, "לא ניתן לשתף", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    },
                    onOpenExternally = { entry -> openFile(entry.file) },
                    onCopy = { entry -> clipboardEntry = entry; clipboardIsMove = false },
                    onMove = { entry -> clipboardEntry = entry; clipboardIsMove = true },
                    onPaste = {
                        val toPaste = clipboardEntry
                        if (toPaste != null && !isBusy) {
                            isBusy = true
                            coroutineScope.launch {
                                val ok = withContext(Dispatchers.IO) {
                                    if (clipboardIsMove) repository.moveEntry(toPaste.file, currentDir)
                                    else repository.copyEntry(toPaste.file, currentDir)
                                }
                                if (ok) {
                                    entries = repository.listDirectory(currentDir, isRootDir)
                                } else {
                                    android.widget.Toast.makeText(this@MainActivity, "לא ניתן להדביק כאן", android.widget.Toast.LENGTH_SHORT).show()
                                }
                                clipboardEntry = null
                                isBusy = false
                            }
                        }
                    }
                )
            }
        }
    }

    private fun openFile(file: File) {
        try {
            val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
            val mime = contentResolver.getType(uri) ?: "*/*"
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mime)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(intent)
        } catch (e: Exception) {
            android.widget.Toast.makeText(this, "לא ניתן לפתוח את הקובץ", android.widget.Toast.LENGTH_SHORT).show()
        }
    }
}
