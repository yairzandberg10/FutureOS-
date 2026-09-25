package com.future.files
import com.future.sharednav.systemui.StatusBarInset
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.padding

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
import com.future.files.data.isInsideUserStorage
import com.future.files.ui.VideoPlayerScreen
import com.future.sharednav.components.FutureSnackbarHost
import com.future.sharednav.theme.ThemeClient
import com.future.files.ui.AudioPlayerScreen
import com.future.files.ui.FilesScreen
import com.future.files.ui.ImageFileViewerScreen
import com.future.files.ui.PdfViewerScreen
import com.future.files.ui.TextViewerScreen
import com.future.sharednav.theme.FutureTheme
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
            // הנתיב שנפתח לאחרונה מכל תיקייה (לפי נתיב התיקייה עצמה) - כך
            // שכשחוזרים "אחורה" מקובץ שנפתח או מתת-תיקייה, הפוקוס חוזר בדיוק
            // לפריט שממנו יצאנו, לא תמיד לפריט הראשון ברשימה.
            var lastSelectedPathByDir by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
            var clipboardEntry by remember { mutableStateOf<FileEntry?>(null) }
            var clipboardIsMove by remember { mutableStateOf(false) }
            // מציין שפעולת קבצים כבדה (מחיקה/העתקה/העברה) רצה כרגע ברקע כדי
            // שהתור העליון של ה-UI thread לא ייחסם בתיקיות גדולות.
            var isBusy by remember { mutableStateOf(false) }
            val snackbar = com.future.sharednav.components.rememberFutureSnackbarState()
            val uiPrefs = remember { getSharedPreferences("files_ui", MODE_PRIVATE) }
            var gridView by remember { mutableStateOf(uiPrefs.getBoolean("grid", false)) }
            // התקנת APK מחכה לאישור מפורש של המשתמש.
            var pendingApk by remember { mutableStateOf<FileEntry?>(null) }
            fun message(text: String) = snackbar.show(text)
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
                    currentDir = if (!isInsideUserStorage(parent, root)) root else parent
                }
            }

            BackHandler(enabled = viewingFile != null || currentDir.absolutePath != root.absolutePath) {
                if (viewingFile != null) viewingFile = null else goUp()
            }

            Surface(modifier = Modifier.fillMaxSize(), color = theme.backgroundColor) {
              // ריווח קטן - הכותרת (שם התיקייה) ישבה מתחת לשורת המצב.
              androidx.compose.foundation.layout.Box(modifier = Modifier.fillMaxSize().padding(top = StatusBarInset.TITLE_GAP_DP.dp)) {
                // המציג והרשימה מחליקים זה מול זה. מעבר בין תיקיות נשאר בתוך
                // אותו מסך בכוונה (FilesScreen שומר מצב בחירה/חיפוש פנימי), והשם
                // בכותרת מתחלף ב-crossfade של ScreenTopBar.
                com.future.sharednav.components.AnimatedScreenHost(
                    targetState = viewingFile,
                    depthOf = { if (it == null) 0 else 1 },
                    contentKey = { it?.file?.absolutePath },
                ) { viewing ->
                if (viewing != null) {
                    when (categorize(viewing.file)) {
                        FileCategory.TEXT -> TextViewerScreen(viewing.file, theme, onBack = { viewingFile = null })
                        FileCategory.IMAGE -> ImageFileViewerScreen(viewing.file, theme, onBack = { viewingFile = null })
                        FileCategory.AUDIO -> AudioPlayerScreen(viewing.file, theme, onBack = { viewingFile = null })
                        FileCategory.PDF -> PdfViewerScreen(viewing.file, theme, onBack = { viewingFile = null })
                        FileCategory.VIDEO -> VideoPlayerScreen(viewing.file, theme, onBack = { viewingFile = null })
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
                        lastSelectedPathByDir = lastSelectedPathByDir + (currentDir.absolutePath to entry.file.absolutePath)
                        if (entry.isDirectory) {
                            // תיקיות מערכת ונתוני אפליקציות לא נפתחים, גם דרך קישור.
                            if (isInsideUserStorage(entry.file, root)) currentDir = entry.file
                            else message("אין גישה לתיקייה הזו")
                        } else {
                            when (categorize(entry.file)) {
                                FileCategory.APK -> pendingApk = entry
                                // שירים וקבצי שמע נפתחים במוזיקה; בלעדיה - בנגן הפנימי.
                                FileCategory.AUDIO -> if (!openInMusic(entry.file)) viewingFile = entry
                                FileCategory.OTHER -> if (!openFile(entry.file)) message("אין אפליקציה שפותחת את הקובץ")
                                else -> viewingFile = entry
                            }
                        }
                    },
                    gridView = gridView,
                    onToggleGridView = {
                        gridView = !gridView
                        uiPrefs.edit().putBoolean("grid", gridView).apply()
                    },
                    lastSelectedPath = lastSelectedPathByDir[currentDir.absolutePath],
                    onBack = { goUp() },
                    onNewFolder = { name ->
                        if (repository.createFolder(currentDir, name)) {
                            entries = repository.listDirectory(currentDir, isRootDir)
                        } else {
                            message("לא ניתן ליצור את התיקייה")
                        }
                    },
                    onRename = { entry, newName ->
                        if (repository.renameEntry(entry.file, newName)) {
                            entries = repository.listDirectory(currentDir, isRootDir)
                        } else {
                            message("לא ניתן לשנות את השם")
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
                                    message("לא ניתן למחוק")
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
                                clipData = android.content.ClipData.newRawUri(entry.file.name, uri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            com.future.sharednav.share.FutureShare.open(this@MainActivity, shareIntent, "שיתוף קובץ")
                        } catch (e: Exception) {
                            message("לא ניתן לשתף")
                        }
                    },
                    onOpenExternally = { entry -> if (!openFile(entry.file)) message("אין אפליקציה שפותחת את הקובץ") },
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
                                    message("לא ניתן להדביק כאן")
                                }
                                clipboardEntry = null
                                isBusy = false
                            }
                        }
                    },
                    onDeleteMultiple = { selected ->
                        if (!isBusy && selected.isNotEmpty()) {
                            isBusy = true
                            coroutineScope.launch {
                                val allOk = withContext(Dispatchers.IO) {
                                    selected.all { repository.deleteEntry(it.file) }
                                }
                                entries = repository.listDirectory(currentDir, isRootDir)
                                if (!allOk) {
                                    message("חלק מהפריטים לא נמחקו")
                                }
                                isBusy = false
                            }
                        }
                    },
                    onShareMultiple = { selected ->
                        try {
                            val uris = ArrayList<Uri>()
                            selected.forEach { entry ->
                                if (!entry.isDirectory) {
                                    uris.add(FileProvider.getUriForFile(this@MainActivity, "$packageName.fileprovider", entry.file))
                                }
                            }
                            if (uris.isEmpty()) {
                                message("אין קבצים לשתף")
                                return@FilesScreen
                            }
                            val shareIntent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                                type = "*/*"
                                putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                                clipData = android.content.ClipData.newRawUri("", uris.first()).also { clip ->
                                    uris.drop(1).forEach { clip.addItem(android.content.ClipData.Item(it)) }
                                }
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            com.future.sharednav.share.FutureShare.open(this@MainActivity, shareIntent, "שיתוף קבצים")
                        } catch (e: Exception) {
                            message("לא ניתן לשתף")
                        }
                    }
                )
                }
                FutureSnackbarHost(snackbar, theme)
              }
            }

            pendingApk?.let { apk ->
                com.future.sharednav.components.ConfirmDialog(
                    message = "להתקין את ${apk.file.name}?",
                    theme = theme,
                    confirmLabel = "התקן",
                    destructive = false,
                    onCancel = { pendingApk = null },
                    onConfirm = {
                        pendingApk = null
                        if (!installApk(apk.file)) message("לא ניתן להתקין את הקובץ")
                    },
                )
            }
        }
    }

    private fun openFile(file: File, targetPackage: String? = null): Boolean = try {
        val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
        val mime = contentResolver.getType(uri) ?: "*/*"
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mime)
            targetPackage?.let { setPackage(it) }
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(intent)
        true
    } catch (e: Exception) {
        false
    }

    /** שירים וקבצי שמע - באפליקציית המוזיקה של המערכת. */
    private fun openInMusic(file: File): Boolean = openFile(file, MUSIC_PACKAGE)

    /**
     * התקנת APK - אחרי שהמשתמש אישר בדיאלוג. בפעם הראשונה אנדרואיד מבקש
     * לאשר ל"קבצים" להתקין אפליקציות (מקורות לא ידועים) - מסך המערכת נפתח.
     */
    private fun installApk(file: File): Boolean {
        if (!packageManager.canRequestPackageInstalls()) {
            return try {
                startActivity(
                    Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:$packageName"))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
                true
            } catch (e: Exception) {
                false
            }
        }
        return try {
            val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
            startActivity(
                Intent(Intent.ACTION_VIEW)
                    .setDataAndType(uri, "application/vnd.android.package-archive")
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
            true
        } catch (e: Exception) {
            false
        }
    }

    private companion object {
        const val MUSIC_PACKAGE = "com.future.music"
    }
}
