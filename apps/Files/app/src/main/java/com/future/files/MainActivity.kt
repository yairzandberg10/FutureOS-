package com.future.files

import com.future.sharednav.i18n.tr
import com.future.sharednav.systemui.StatusBarInset
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.padding

import android.content.Intent
import android.content.pm.PackageManager
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
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.core.content.FileProvider
import com.future.files.data.FileCategory
import com.future.files.data.FileEntry
import com.future.files.data.FileRepository
import com.future.files.data.categorize
import com.future.files.data.isInsideUserStorage
import com.future.sharednav.components.FutureSnackbarHost
import com.future.sharednav.theme.ThemeClient
import com.future.files.ui.AudioPlayerScreen
import com.future.files.ui.FilesScreen
import com.future.files.ui.FilesHomeScreen
import com.future.files.ui.ImageFileViewerScreen
import com.future.files.ui.PdfViewerScreen
import com.future.files.ui.TextViewerScreen
import com.future.sharednav.theme.FutureTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/** שמירת קובץ/תיקייה כנתיב מחרוזת - כדי ש-rememberSaveable ישרוד יצירה מחדש. */
private val FileSaver = Saver<File, String>(save = { it.absolutePath }, restore = { File(it) })
private val FileEntrySaver = Saver<FileEntry?, String>(
    save = { it?.file?.absolutePath },
    restore = { FileEntry(File(it), false, 0L) },
)

/** הלוח (העתק/העבר) - כמה פריטים. סוג הפריט נקרא מהדיסק בשחזור, כדי שתיקייה תישאר תיקייה. */
private val FileListSaver = Saver<List<FileEntry>, ArrayList<String>>(
    save = { list -> ArrayList(list.map { it.file.absolutePath }) },
    restore = { paths -> paths.map { path -> File(path).let { FileEntry(it, it.isDirectory, 0L) } } },
)

/**
 * בקשת בחירת קובץ מאפליקציה אחרת (GET_CONTENT / OPEN_DOCUMENT, #33). הקובץ שנבחר
 * מוחזר כ-content URI דרך FileProvider עם הרשאת קריאה, ולא כנתיב.
 */
private class PickRequest(private val mimeTypes: List<String>) {
    fun accepts(file: File): Boolean {
        if (mimeTypes.isEmpty() || mimeTypes.any { it == "*/*" || it == "*" }) return true
        val mime = android.webkit.MimeTypeMap.getSingleton().getMimeTypeFromExtension(file.extension.lowercase())
            ?: return false
        return mimeTypes.any { pattern ->
            pattern == mime || (pattern.endsWith("/*") && mime.startsWith(pattern.removeSuffix("*")))
        }
    }

    companion object {
        fun from(intent: Intent?): PickRequest? {
            if (intent == null) return null
            val action = intent.action
            if (action != Intent.ACTION_GET_CONTENT && action != Intent.ACTION_OPEN_DOCUMENT) return null
            val types = intent.getStringArrayExtra(Intent.EXTRA_MIME_TYPES)?.toList() ?: listOfNotNull(intent.type)
            return PickRequest(types)
        }
    }
}

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
            // מצב בחירה (לאפליקציה אחרת): לא null כשהפתיחה הגיעה מ-GET_CONTENT / OPEN_DOCUMENT.
            val pickRequest = remember { PickRequest.from(intent) }
            var hasAccess by remember { mutableStateOf(repository.hasAllFilesAccess()) }
            val root = remember { repository.rootDirectory() }
            var currentDir by rememberSaveable(stateSaver = FileSaver) { mutableStateOf(root) }
            // מסך הבית (אחרונים, קטגוריות, אחסון). ממנו נכנסים לאחסון הפנימי או לקטגוריה.
            var atHome by rememberSaveable { mutableStateOf(true) }
            var categoryName by rememberSaveable { mutableStateOf<String?>(null) }
            val category = categoryName?.let { n -> runCatching { com.future.files.data.HomeCategory.valueOf(n) }.getOrNull() }
            // סריקת כל הקבצים (לקטגוריות ולאחרונים), ונפח האחסון.
            var scanned by remember { mutableStateOf<List<com.future.files.data.ScannedFile>?>(null) }
            var storage by remember { mutableStateOf<Pair<Long, Long>?>(null) }
            var homeFocusKey by rememberSaveable { mutableStateOf<String?>(null) }
            var entries by remember { mutableStateOf(listOf<FileEntry>()) }
            // true כשקריאת התיקייה נכשלה - מוצג "לא ניתן לקרוא" במקום "התיקייה ריקה".
            var loadFailed by remember { mutableStateOf(false) }
            var viewingFile by rememberSaveable(stateSaver = FileEntrySaver) { mutableStateOf<FileEntry?>(null) }
            // הנתיב שנפתח לאחרונה מכל תיקייה (לפי נתיב התיקייה עצמה) - כך
            // שכשחוזרים "אחורה" מקובץ שנפתח או מתת-תיקייה, הפוקוס חוזר בדיוק
            // לפריט שממנו יצאנו, לא תמיד לפריט הראשון ברשימה.
            var lastSelectedPathByDir by rememberSaveable { mutableStateOf<Map<String, String>>(emptyMap()) }
            var clipboardEntries by rememberSaveable(stateSaver = FileListSaver) { mutableStateOf<List<FileEntry>>(emptyList()) }
            var clipboardIsMove by rememberSaveable { mutableStateOf(false) }
            // מציין שפעולת קבצים כבדה (מחיקה/העתקה/העברה) רצה כרגע ברקע כדי
            // שהתור העליון של ה-UI thread לא ייחסם בתיקיות גדולות.
            var isBusy by remember { mutableStateOf(false) }
            val snackbar = com.future.sharednav.components.rememberFutureSnackbarState()
            val uiPrefs = remember { getSharedPreferences("files_ui", MODE_PRIVATE) }
            var gridView by remember { mutableStateOf(uiPrefs.getBoolean("grid", false)) }
            var sortOrder by remember {
                mutableStateOf(runCatching { com.future.files.data.SortOrder.valueOf(uiPrefs.getString("sort", "NAME")!!) }
                    .getOrDefault(com.future.files.data.SortOrder.NAME))
            }
            // ההעתקה הרצה כרגע - מקור ההתקדמות וה"ביטול" במסך ההמתנה
            var copyProgress by remember { mutableStateOf<com.future.files.data.CopyProgress?>(null) }
            var progressValue by remember { mutableStateOf(0f) }
            LaunchedEffect(copyProgress) {
                val p = copyProgress ?: return@LaunchedEffect
                while (true) { progressValue = p.fraction; kotlinx.coroutines.delay(200) }
            }
            // מצב מפתח: רק בו מוצגים קבצים ותיקיות טכניים (ר' isDeveloperMode).
            var developer by remember { mutableStateOf(com.future.files.data.isDeveloperMode(this@MainActivity)) }
            // עולה בכל חזרה לאפליקציה - מרענן את הרשימה (קבצים שנוצרו/השתנו מחוץ לאפליקציה).
            var resumeTick by remember { mutableIntStateOf(0) }
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
                        resumeTick++
                        hasAccess = repository.hasAllFilesAccess()
                        developer = com.future.files.data.isDeveloperMode(this@MainActivity)
                        val shared = ThemeClient.getTheme(this@MainActivity)
                        theme = FutureTheme(isDarkMode = shared.isDarkMode, accentColor = Color(shared.primaryColor))
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
            }

            val isRootDir = currentDir.absolutePath == root.absolutePath
            val isCurrentDirTopLevel = currentDir.parentFile?.absolutePath == root.absolutePath

            // קריאת התיקייה מחוץ ל-UI thread. תיקייה שלא נקראת מציגה שגיאה ולא רשימה ריקה.
            // rescan = false - קטגוריה מסוננת מהסריקה השמורה (מעבר אליה, שינוי מיון);
            // אחרי מחיקה / שינוי שם / הדבקה סורקים מחדש.
            suspend fun reloadEntries(rescan: Boolean = true) {
                val cat = category
                if (cat != null) {
                    val cached = scanned
                    val all = if (!rescan && cached != null) cached else withContext(Dispatchers.IO) { repository.scanUserFiles(developer) }
                    scanned = all
                    loadFailed = false
                    entries = withContext(Dispatchers.Default) { repository.categoryEntries(all, cat, sortOrder) }
                    return
                }
                val loaded = withContext(Dispatchers.IO) { repository.listDirectory(currentDir, isRootDir, sortOrder, developer) }
                loadFailed = loaded == null
                entries = loaded ?: emptyList()
            }

            LaunchedEffect(hasAccess, currentDir, sortOrder, developer, resumeTick, categoryName, atHome) {
                if (hasAccess && !atHome) reloadEntries(rescan = false)
            }
            // מסך הבית: סריקה ונפח, בכל חזרה אליו.
            LaunchedEffect(hasAccess, atHome, developer, resumeTick) {
                if (!hasAccess || !atHome) return@LaunchedEffect
                storage = withContext(Dispatchers.IO) { repository.storageUsage() }
                scanned = withContext(Dispatchers.IO) { repository.scanUserFiles(developer) }
            }

            fun goUp() {
                val parent = currentDir.parentFile
                if (parent != null && currentDir.absolutePath != root.absolutePath) {
                    currentDir = if (!isInsideUserStorage(parent, root)) root else parent
                }
            }

            fun listKey(): String = category?.let { "category:${it.name}" } ?: currentDir.absolutePath
            fun openEntry(entry: FileEntry) {
                        if (!atHome) lastSelectedPathByDir = lastSelectedPathByDir + (listKey() to entry.file.absolutePath)
                        if (entry.isDirectory) {
                            // תיקיות מערכת ונתוני אפליקציות לא נפתחים, גם דרך קישור.
                            if (isInsideUserStorage(entry.file, root)) currentDir = entry.file
                            else message(tr("אין גישה לתיקייה הזו"))
                        } else if (pickRequest != null) {
                            // במצב בחירה קובץ שנבחר מוחזר לאפליקציה הקוראת, בלי לפתוח אותו.
                            if (pickRequest.accepts(entry.file)) returnPickedFile(entry.file)
                            else message("הקובץ הזה לא מתאים לבחירה")
                        } else {
                            when (categorize(entry.file)) {
                                // הקבצים לא מתקינים אפליקציות. וידאו נפתח באפליקציה חיצונית שמתאימה לו.
                                FileCategory.APK -> message("התקנת אפליקציות לא נתמכת")
                                FileCategory.VIDEO -> if (!openFile(entry.file)) message(tr("אין אפליקציה שפותחת את הקובץ"))
                                // שירים וקבצי שמע נפתחים במוזיקה; בלעדיה - בנגן הפנימי.
                                FileCategory.AUDIO -> if (!openInMusic(entry.file)) viewingFile = entry
                                FileCategory.OTHER -> if (!openFile(entry.file)) message(tr("אין אפליקציה שפותחת את הקובץ"))
                                else -> viewingFile = entry
                            }
                        }
                    }

            // BACK: מציג קובץ ← רשימה; קטגוריה ← בית; תיקייה ← התיקייה שמעליה; שורש האחסון ← בית.
            BackHandler(enabled = viewingFile != null || (hasAccess && !atHome)) {
                when {
                    viewingFile != null -> viewingFile = null
                    category != null -> { entries = emptyList(); categoryName = null; atHome = true }
                    currentDir.absolutePath != root.absolutePath -> goUp()
                    else -> { entries = emptyList(); atHome = true }
                }
            }
            // בחירה לאפליקציה אחרת: BACK במסך הבית מבטל את הבחירה.
            BackHandler(enabled = pickRequest != null && viewingFile == null && (atHome || !hasAccess)) {
                setResult(RESULT_CANCELED)
                finish()
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
                        FileCategory.TEXT -> TextViewerScreen(viewing.file, theme, onBack = { viewingFile = null }, onMessage = ::message)
                        FileCategory.IMAGE -> ImageFileViewerScreen(viewing.file, theme, onBack = { viewingFile = null }, onOpenInGallery = {
                            if (!openFile(viewing.file, GALLERY_PACKAGE)) message("הגלריה לא מותקנת")
                        })
                        FileCategory.AUDIO -> AudioPlayerScreen(viewing.file, theme, onBack = { viewingFile = null })
                        FileCategory.PDF -> PdfViewerScreen(viewing.file, theme, onBack = { viewingFile = null })
                        else -> {}
                    }
                } else if (hasAccess && atHome) FilesHomeScreen(
                    theme = theme,
                    repository = repository,
                    recents = remember(scanned) { scanned?.let { repository.recentFiles(it) } },
                    storage = storage,
                    pickMode = pickRequest != null,
                    lastFocusKey = homeFocusKey,
                    onOpenRecent = { item -> homeFocusKey = item.entry.file.absolutePath; openEntry(item.entry) },
                    onOpenCategory = { cat ->
                        homeFocusKey = "cat:${cat.name}"
                        // מהסריקה השמורה מיד - בלי מסך "ריק" עד שהרשימה נטענת.
                        entries = scanned?.let { repository.categoryEntries(it, cat, sortOrder) } ?: emptyList()
                        categoryName = cat.name
                        atHome = false
                    },
                    onOpenStorage = {
                        homeFocusKey = "storage"
                        entries = emptyList()
                        currentDir = root
                        atHome = false
                    },
                ) else FilesScreen(
                    currentDir = if (category != null) File(root, ".category-${category.name}") else currentDir,
                    titleOverride = category?.label,
                    categoryMode = category != null,
                    entries = entries,
                    loadFailed = loadFailed,
                    hasAccess = hasAccess,
                    isRoot = currentDir.absolutePath == root.absolutePath,
                    currentDirIsTopLevelFolder = isCurrentDirTopLevel,
                    theme = theme,
                    hasClipboard = clipboardEntries.isNotEmpty(),
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
                    onOpenFile = { entry -> openEntry(entry) },
                    gridView = gridView,
                    sortOrder = sortOrder,
                    onCycleSort = {
                        sortOrder = com.future.files.data.SortOrder.entries[(sortOrder.ordinal + 1) % com.future.files.data.SortOrder.entries.size]
                        uiPrefs.edit().putString("sort", sortOrder.name).apply()
                    },
                    busyProgress = copyProgress?.let { progressValue },
                    onCancelBusy = copyProgress?.let { p -> { p.cancel() } },
                    onOpenInGallery = { entry -> if (!openFile(entry.file, GALLERY_PACKAGE)) message("הגלריה לא מותקנת") },
                    onToggleGridView = {
                        gridView = !gridView
                        uiPrefs.edit().putBoolean("grid", gridView).apply()
                    },
                    lastSelectedPath = lastSelectedPathByDir[listKey()],
                    pickMode = pickRequest != null,
                    onBack = { goUp() },
                    onNewFolder = { name ->
                        coroutineScope.launch {
                            if (withContext(Dispatchers.IO) { repository.createFolder(currentDir, name) }) {
                                reloadEntries()
                            } else {
                                message(tr("לא ניתן ליצור את התיקייה"))
                            }
                        }
                    },
                    onRename = { entry, newName ->
                        coroutineScope.launch {
                            if (withContext(Dispatchers.IO) { repository.renameEntry(entry.file, newName) }) {
                                reloadEntries()
                            } else {
                                message(tr("לא ניתן לשנות את השם"))
                            }
                        }
                    },
                    isBusy = isBusy,
                    onDelete = { entry ->
                        if (!isBusy) {
                            isBusy = true
                            coroutineScope.launch {
                                val ok = withContext(Dispatchers.IO) { repository.deleteEntry(entry.file) }
                                if (ok) {
                                    reloadEntries()
                                } else {
                                    message(tr("לא ניתן למחוק"))
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
                            com.future.sharednav.share.FutureShare.open(this@MainActivity, shareIntent, tr("שיתוף קובץ"))
                        } catch (e: Exception) {
                            message(tr("לא ניתן לשתף"))
                        }
                    },
                    onOpenExternally = { entry -> if (!openFile(entry.file)) message(tr("אין אפליקציה שפותחת את הקובץ")) },
                    onCopy = { entry -> clipboardEntries = listOf(entry); clipboardIsMove = false },
                    onMove = { entry -> clipboardEntries = listOf(entry); clipboardIsMove = true },
                    // בחירה מרובה: כל הפריטים שנבחרו נכנסים ללוח, וההדבקה מעבירה את כולם.
                    onCopyMultiple = { selected ->
                        clipboardEntries = selected
                        clipboardIsMove = false
                        message(tr("{0} פריטים מוכנים להדבקה - נווטו לתיקייה ובחרו \"הדבק כאן\"", selected.size))
                    },
                    onMoveMultiple = { selected ->
                        clipboardEntries = selected
                        clipboardIsMove = true
                        message(tr("{0} פריטים מוכנים להעברה - נווטו לתיקייה ובחרו \"הדבק כאן\"", selected.size))
                    },
                    onPaste = {
                        // העברה לתיקייה שהפריט כבר בה לא עושה כלום - מדלגים עליו.
                        val toPaste = clipboardEntries.filterNot {
                            clipboardIsMove && it.file.parentFile?.absolutePath == currentDir.absolutePath
                        }
                        if (clipboardEntries.isNotEmpty() && toPaste.isEmpty()) {
                            message(if (clipboardEntries.size == 1) "הפריט כבר נמצא כאן" else tr("הפריטים כבר נמצאים כאן"))
                        } else if (toPaste.isNotEmpty() && !isBusy) {
                            isBusy = true
                            val progress = com.future.files.data.CopyProgress()
                            progressValue = 0f
                            copyProgress = progress
                            // התיקייה נקבעת לפני ההעתקה - חזרה "אחורה" באמצע לא משנה את היעד.
                            val destDir = currentDir
                            val moving = clipboardIsMove
                            coroutineScope.launch {
                                val failed = ArrayList<FileEntry>()
                                for (item in toPaste) {
                                    if (progress.cancelled) { failed += item; continue }
                                    // שם תפוס לא עוצר את ההדבקה: הפריט נשמר כ"שם (2)".
                                    val targetName = withContext(Dispatchers.IO) { repository.availableName(destDir, item.file.name, item.isDirectory) }
                                    val ok = withContext(Dispatchers.IO) {
                                        if (moving) repository.moveEntry(item.file, destDir, progress, targetName)
                                        else repository.copyEntry(item.file, destDir, progress, targetName)
                                    }
                                    if (!ok) failed += item
                                }
                                reloadEntries()
                                when {
                                    progress.cancelled -> message(tr("הפעולה בוטלה"))
                                    failed.size == toPaste.size -> message(tr("לא ניתן להדביק כאן"))
                                    failed.isNotEmpty() -> message(tr("לא הודבקו {0} מתוך {1} פריטים", failed.size, toPaste.size))
                                }
                                // ביטול או כשלון משאירים בלוח רק את מה שלא הודבק - אפשר לנסות שוב.
                                clipboardEntries = failed
                                copyProgress = null
                                isBusy = false
                            }
                        }
                    },
                    onDeleteMultiple = { selected ->
                        if (!isBusy && selected.isNotEmpty()) {
                            isBusy = true
                            coroutineScope.launch {
                                // ספירה של כל הפריטים (לא עוצרים בכשל הראשון), כדי שההודעה תהיה מדויקת.
                                val failed = withContext(Dispatchers.IO) {
                                    selected.count { !repository.deleteEntry(it.file) }
                                }
                                reloadEntries()
                                if (failed > 0) {
                                    message("לא נמחקו $failed מתוך ${selected.size} פריטים")
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
                                message(tr("אין קבצים לשתף"))
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
                            com.future.sharednav.share.FutureShare.open(this@MainActivity, shareIntent, tr("שיתוף קבצים"))
                        } catch (e: Exception) {
                            message(tr("לא ניתן לשתף"))
                        }
                    }
                )
                }
                FutureSnackbarHost(snackbar, theme)
              }
            }
        }
    }

    /** מחזיר את הקובץ שנבחר לאפליקציה שביקשה אותו, ומסיים את המסך (#33). */
    private fun returnPickedFile(file: File) {
        val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
        val result = Intent().setData(uri)
        result.clipData = android.content.ClipData.newRawUri("", uri)
        result.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        setResult(RESULT_OK, result)
        finish()
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

    private companion object {
        const val MUSIC_PACKAGE = "com.future.music"
        const val GALLERY_PACKAGE = "com.future.gallery"
    }
}
