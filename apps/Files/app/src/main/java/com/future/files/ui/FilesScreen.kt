package com.future.files.ui

import com.future.sharednav.focus.focusGlideHost
import com.future.sharednav.i18n.tr
import com.future.sharednav.theme.idleChipColor
import com.future.sharednav.theme.readableAccentColor

import androidx.compose.material.icons.rounded.OpenInNew
import com.future.sharednav.icons.FutureIcons
import com.future.sharednav.components.FutureButton
import com.future.sharednav.components.EmptyState
import com.future.sharednav.components.FutureSpinner
import com.future.sharednav.components.FutureOptionsMenu
import com.future.sharednav.components.FutureMenuRow
import com.future.sharednav.components.FutureDialog
import com.future.sharednav.components.FutureDetailRow
import com.future.sharednav.components.InputDialog
import com.future.sharednav.components.FutureAvatar
import com.future.sharednav.components.AvatarListSize
import com.future.sharednav.components.FutureCheckbox
import com.future.sharednav.focus.FocusableItem
import com.future.sharednav.theme.FutureDimens
import com.future.sharednav.theme.scrimColor
import com.future.sharednav.theme.mutedTextColor
import androidx.compose.foundation.layout.heightIn
import com.future.sharednav.theme.FutureTypography
import com.future.sharednav.theme.FutureShapes
import com.future.sharednav.components.MarqueeText
import com.future.sharednav.focus.bringIntoViewOnFocus

import android.graphics.Bitmap
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import com.future.sharednav.components.FutureSectionHeader
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.future.sharednav.components.AppDialog
import com.future.files.data.FileCategory
import com.future.files.data.FileEntry
import com.future.files.data.FileRepository
import com.future.files.data.ThumbnailCache
import com.future.files.data.categorize
import com.future.files.data.displayName
import com.future.sharednav.focus.escapeTextFieldFocusTrap
import com.future.sharednav.theme.FutureTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun FilesScreen(
    currentDir: File,
    entries: List<FileEntry>,
    /** קריאת התיקייה נכשלה - מוצג מצב שגיאה במקום "התיקייה ריקה". */
    loadFailed: Boolean = false,
    hasAccess: Boolean,
    isRoot: Boolean,
    currentDirIsTopLevelFolder: Boolean = false,
    theme: FutureTheme,
    hasClipboard: Boolean = false,
    isBusy: Boolean = false,
    onRequestAccess: () -> Unit,
    onOpenFile: (FileEntry) -> Unit,
    onBack: () -> Unit,
    onNewFolder: (String) -> Unit = {},
    onRename: (FileEntry, String) -> Unit = { _, _ -> },
    onDelete: (FileEntry) -> Unit = {},
    onShare: (FileEntry) -> Unit = {},
    onOpenExternally: (FileEntry) -> Unit = {},
    onCopy: (FileEntry) -> Unit = {},
    onMove: (FileEntry) -> Unit = {},
    onPaste: () -> Unit = {},
    onDeleteMultiple: (List<FileEntry>) -> Unit = {},
    onShareMultiple: (List<FileEntry>) -> Unit = {},
    onCopyMultiple: (List<FileEntry>) -> Unit = {},
    onMoveMultiple: (List<FileEntry>) -> Unit = {},
    /** תצוגת רשת (ריבועים) או רשימה - מתחלפת מתפריט האפשרויות. */
    gridView: Boolean = false,
    onToggleGridView: () -> Unit = {},
    /** סדר המיון הנוכחי - מוצג ומוחלף בכפתור המיון שבשורה העליונה. */
    sortOrder: com.future.files.data.SortOrder = com.future.files.data.SortOrder.NAME,
    onCycleSort: () -> Unit = {},
    /** התקדמות העתקה/העברה (0..1), או null כשאין. */
    busyProgress: Float? = null,
    onCancelBusy: (() -> Unit)? = null,
    onOpenInGallery: (FileEntry) -> Unit = {},
    // הנתיב של הפריט שנפתח לאחרונה מהתיקייה הזו - כשחוזרים "אחורה" (מקובץ
    // שנפתח, או מתת-תיקייה) הפוקוס צריך לשוב אליו בדיוק, לא תמיד לפריט הראשון.
    lastSelectedPath: String? = null,
    /** FL-N03: הפתיחה הגיעה מבקשת בחירת קובץ מאפליקציה אחרת - מסומן בשורה העליונה. */
    pickMode: Boolean = false,
) {
    val repository = remember { FileRepository() }
    // בזמן העתקה או מחיקה נחסמים כל המקשים, חוץ מ"ביטול" (ר' ההודעה בשכבת הפעולה).
    var overlayFocused by remember { mutableStateOf(false) }
    LaunchedEffect(isBusy) { overlayFocused = false }
    var selectedEntries by remember { mutableStateOf(setOf<FileEntry>()) }
    var menuEntry by remember { mutableStateOf<FileEntry?>(null) }
    var renameEntry by remember { mutableStateOf<FileEntry?>(null) }
    var deleteEntryState by remember { mutableStateOf<FileEntry?>(null) }
    var deleteMultipleState by remember { mutableStateOf<List<FileEntry>?>(null) }
    var detailsEntry by remember { mutableStateOf<FileEntry?>(null) }
    var showNewFolder by remember { mutableStateOf(false) }
    // עוקב אחר הפריט הממוקד כרגע ברשימה כדי לאפשר פתיחת התפריט גם דרך כפתור
    // ה-⋮ הממוקד בסרגל העליון (לא רק דרך מקש Menu/Settings בחומרה).
    var focusedEntry by remember { mutableStateOf<FileEntry?>(null) }
    // תפריט Options נפתח תמיד: פעולות על הפריט הממוקד (אם יש), ופעולות על
    // התיקייה - תיקייה חדשה, הדבקה, רשת/רשימה. אין להן כפתורים על המסך.
    var generalMenu by remember { mutableStateOf(false) }
    // במצב בחירה Options פותח את הפעולות על כל הנבחרים (העתק, העבר, שתף, מחק),
    // ולא את התפריט של הפריט הממוקד.
    var selectionMenu by remember { mutableStateOf(false) }
    com.future.sharednav.nav.onOptionsKeyPress {
        if (!hasAccess) return@onOptionsKeyPress
        if (selectedEntries.isNotEmpty()) { selectionMenu = true; return@onOptionsKeyPress }
        val focused = focusedEntry?.takeIf { f -> entries.any { it.file == f.file } }
        if (focused != null) menuEntry = focused else generalMenu = true
    }
    // FocusRequester לפי נתיב - מתאפס בכל מעבר תיקייה, כדי שגם הפוקוס ההתחלתי
    // וגם השחזור אחרי חזרה "אחורה" יעבדו על אותה תבנית.
    val rowFocusRequesters = remember(currentDir) { mutableMapOf<String, FocusRequester>() }
    // הפוקוס הראשוני בתיקייה: הפריט שממנו יצאנו (או הראשון). אחרי זה, כששינוי ברשימה
    // (מחיקה, שינוי שם, יצירה) מחליף אותה, הפוקוס חוזר לפריט הממוקד אם הוא עדיין קיים,
    // ואחרת לפריט הראשון - כדי שהפוקוס לא ייעלם מהמקשים.
    var initialFocusDone by remember(currentDir) { mutableStateOf(false) }
    // תיקייה שנוצרה עכשיו מקבלת את הפוקוס אחרי הטעינה מחדש (ולא הפריט הראשון).
    var pendingFocusPath by remember(currentDir) { mutableStateOf<String?>(null) }
    // מצב הגלילה לכל תיקייה בנפרד - כדי לגלול לשורה לפני בקשת הפוקוס.
    val listState = remember(currentDir) { androidx.compose.foundation.lazy.LazyListState() }
    val gridState = remember(currentDir) { androidx.compose.foundation.lazy.grid.LazyGridState() }
    // One UI: לפני הפריטים יש פריטי כותרת שנגללים עם הרשימה - הכותרת הגדולה, ומתחתיה
    // בשורש כרטיס האחסון וכותרת הקטע, ובתיקייה פנימית שורת הנתיב. הגלילה לפריט
    // הממוקד מוסיפה את מספרם לאינדקס של הפריט.
    val headerCount = if (isRoot) 3 else 2
    // הכותרת הקטנה בסרגל העליון מופיעה רק כשהכותרת הגדולה נגללה החוצה.
    val titleCollapsed by remember(currentDir, gridView) {
        derivedStateOf {
            if (gridView) gridState.firstVisibleItemIndex > 0 || gridState.firstVisibleItemScrollOffset > 120
            else listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 120
        }
    }
    LaunchedEffect(currentDir, entries) {
        // רשימה ישנה מהתיקייה הקודמת (הטעינה אסינכרונית) - מחכים לרשימה של התיקייה הזו.
        if (entries.isEmpty() || entries.first().file.parentFile?.absolutePath != currentDir.absolutePath) return@LaunchedEffect
        val wanted = pendingFocusPath ?: if (initialFocusDone) focusedEntry?.file?.absolutePath else lastSelectedPath
        val targetIndex = entries.indexOfFirst { it.file.absolutePath == wanted }.coerceAtLeast(0)
        val targetPath = entries[targetIndex].file.absolutePath
        // FL-D03 / FL-D08: שורה מחוץ למסך לא מורכבת ב-LazyColumn, ולכן אין לה FocusRequester
        // מחובר והפוקוס נשאר על "+". גוללים אליה קודם (בלי אנימציה), ורק אז מבקשים פוקוס.
        // הצלחה נמדדת לפי הפוקוס בפועל (onFocused של השורה), לא לפי היעדר חריגה.
        // אחרי הצלחה בודקים שוב שהפוקוס לא נלקח (סגירת דיאלוג מחזירה פוקוס לכפתור שפתח אותו).
        var settledChecks = 0
        repeat(20) {
            // הפריט הראשון - גוללים לראש הרשימה, כדי שהכותרת הגדולה תיראה מעליו.
            val scrollIndex = if (targetIndex == 0) 0 else targetIndex + headerCount
            runCatching { if (gridView) gridState.scrollToItem(scrollIndex) else listState.scrollToItem(scrollIndex) }
            val requested = runCatching { rowFocusRequesters.getOrPut(targetPath) { FocusRequester() }.requestFocus() }.isSuccess
            repeat(3) { androidx.compose.runtime.withFrameNanos { } }
            if (requested && focusedEntry?.file?.absolutePath == targetPath) {
                if (++settledChecks >= 2) {
                    initialFocusDone = true
                    pendingFocusPath = null
                    return@LaunchedEffect
                }
                kotlinx.coroutines.delay(120)
            } else {
                kotlinx.coroutines.delay(30)
            }
        }
        pendingFocusPath = null
    }

    fun toggleSelection(entry: FileEntry) {
        selectedEntries = if (selectedEntries.contains(entry)) {
            selectedEntries - entry
        } else {
            selectedEntries + entry
        }
    }

    fun clearSelection() {
        selectedEntries = emptySet()
    }

    fun selectAll() {
        selectedEntries = entries.toSet()
    }

    // BACK במצב בחירה מבטל את הבחירה קודם, ולא יוצא מהתיקייה.
    androidx.activity.compose.BackHandler(enabled = selectedEntries.isNotEmpty()) { clearSelection() }
    // בזמן פעולה ברקע BACK לא משנה תיקייה: בהעתקה הוא מבטל אותה, ובמחיקה הוא לא עושה דבר.
    androidx.activity.compose.BackHandler(enabled = isBusy) { onCancelBusy?.invoke() }

    // # נצרך ברמת המערכת ומגיע רק כשידור (ר' onPoundKeyPress) - ה-Key.Pound בשורות
    // נשאר רק למקלדת בלי FutureUI. בוחר או מבטל את הפריט הממוקד.
    com.future.sharednav.nav.onPoundKeyPress {
        if (!hasAccess || menuEntry != null || generalMenu || selectionMenu || renameEntry != null || detailsEntry != null || showNewFolder) return@onPoundKeyPress
        focusedEntry?.let { f -> entries.firstOrNull { it.file == f.file } }?.let { toggleSelection(it) }
    }

    // Reset selection when changing directory
    LaunchedEffect(currentDir) {
        clearSelection()
    }

    fun displayNameOf(entry: FileEntry): String = entry.file.displayName(isRoot && entry.isDirectory)

    val screenTitle = if (isRoot) tr("קבצים") else currentDir.displayName(currentDirIsTopLevelFolder)
    // שורת המשנה מתחת לכותרת הגדולה: מספר הפריטים וסדר המיון (ובחירה לאפליקציה אחרת).
    val heroSubtitle = buildString {
        if (pickMode) append(tr("בחירת קובץ")).append(" · ")
        append(if (entries.size == 1) tr("פריט אחד") else tr("{0} פריטים", entries.size))
        append(" · ").append(tr("ממוין לפי {0}", sortOrder.label))
    }
    // הנתיב מהשורש: "אחסון פנימי › DCIM › Camera" (הראשונה בשם המתורגם של תיקיית השורש).
    val pathSegments = remember(currentDir) {
        val root = repository.rootDirectory()
        val parts = mutableListOf<String>()
        var dir: File? = currentDir
        while (dir != null && dir.absolutePath != root.absolutePath) {
            parts.add(0, dir.displayName(dir.parentFile?.absolutePath == root.absolutePath))
            dir = dir.parentFile
        }
        listOf(tr("אחסון פנימי")) + parts
    }
    // נפח האחסון (בשורש בלבד), נקרא מחוץ ל-UI thread.
    val storage by produceState<StorageInfo?>(null, isRoot, hasAccess) {
        if (!isRoot) return@produceState
        value = withContext(Dispatchers.IO) {
            runCatching {
                val stat = android.os.StatFs(repository.rootDirectory().absolutePath)
                StorageInfo(totalBytes = stat.totalBytes, freeBytes = stat.availableBytes)
            }.getOrNull()
        }
    }

    // סמן הפוקוס המשותף (Elastic Focus): המילוי של השורה הממוקדת גולש לשורה הבאה
    // במקום להיעלם ולהופיע - כמו בשאר הרשימות במערכת. המסך הזה לא בנוי על
    // ScreenScaffold, ולכן מארח את הסמן בעצמו.
    val glide = com.future.sharednav.focus.rememberFocusGlideState()

    CompositionLocalProvider(
        LocalLayoutDirection provides com.future.sharednav.i18n.Tr.layoutDirection,
        com.future.sharednav.focus.LocalFocusGlide provides glide,
    ) {
        Box(
            modifier = Modifier
                .escapeTextFieldFocusTrap()
                .fillMaxSize()
                .background(theme.backgroundColor)
                .focusGlideHost(glide)
                .onPreviewKeyEvent { event ->
                    val passThrough = !isBusy || overlayFocused || event.key == Key.Back
                    !passThrough
                },
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                if (selectedEntries.isEmpty()) {
                    // הכותרת, ובסופה שלושה כפתורי סרגל עליון: מיון, תצוגה (רשימה/רשת)
                    // ותיקייה חדשה. חזרה במקש BACK, ושאר הפעולות במקש Options.
                    FilesTopBar(
                        title = screenTitle,
                        showTitle = titleCollapsed && hasAccess && entries.isNotEmpty(),
                        theme = theme,
                        gridView = gridView,
                        enabled = hasAccess,
                        onSort = onCycleSort,
                        onToggleView = onToggleGridView,
                        onNewFolder = { showNewFolder = true },
                    )
                } else {
                    // סרגל בחירה מרובה
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(theme.accentColor.copy(alpha = 0.15f))
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FocusableIconButton(FutureIcons.Close, tr("בטל בחירה"), theme, ::clearSelection)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            tr("{0} נבחרו", selectedEntries.size),
                            fontSize = FutureTypography.title,
                            fontWeight = FontWeight.Bold,
                            color = theme.textColor,
                            modifier = Modifier.weight(1f)
                        )
                        FocusableIconButton(FutureIcons.Share, tr("שתף נבחרים"), theme, {
                            onShareMultiple(selectedEntries.toList())
                            clearSelection()
                        })
                        Spacer(modifier = Modifier.width(8.dp))
                        FocusableIconButton(FutureIcons.Delete, tr("מחק נבחרים"), theme, {
                            deleteMultipleState = selectedEntries.toList()
                        })
                    }
                }

                if (!hasAccess) {
                    FilesHero(screenTitle, null, theme)
                    Column(
                        modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(tr("כדי לגשת לקבצים צריך לאשר הרשאת ניהול קבצים"), color = theme.textColor.copy(alpha = 0.7f), fontSize = FutureTypography.bodyLarge)
                        Spacer(modifier = Modifier.height(16.dp))
                        FutureButton(tr("אשר הרשאה"), theme, onRequestAccess)
                    }
                } else if (entries.isEmpty()) {
                    FilesHero(screenTitle, heroSubtitle, theme)
                    if (!isRoot) PathBar(pathSegments, theme, FutureDimens.spacingXl)
                    Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                        EmptyState(
                            icon = FutureIcons.Folder,
                            title = if (loadFailed) "לא ניתן לקרוא את התיקייה" else tr("התיקייה ריקה"),
                            textColor = theme.textColor,
                            subtitle = if (loadFailed) "לחזרה לתיקייה הקודמת לחץ BACK" else null,
                        )
                    }
                } else if (gridView) {
                    androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
                        columns = androidx.compose.foundation.lazy.grid.GridCells.Fixed(3),
                        state = gridState,
                        contentPadding = PaddingValues(start = FutureDimens.spacingLg, end = FutureDimens.spacingLg, bottom = FutureDimens.spacingLg),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        // פריטי הכותרת תופסים שורה שלמה. מספרם = headerCount.
                        val fullLine: (androidx.compose.foundation.lazy.grid.LazyGridItemSpanScope.() -> androidx.compose.foundation.lazy.grid.GridItemSpan) =
                            { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }
                        item(key = "hero", span = fullLine) { FilesHero(screenTitle, heroSubtitle, theme) }
                        if (isRoot) {
                            item(key = "storage", span = fullLine) { StorageCard(storage, repository, theme, horizontalInset = 0.dp) }
                            item(key = "section", span = fullLine) { FutureSectionHeader(tr("תיקיות"), theme, inset = false, modifier = Modifier.padding(horizontal = FutureDimens.spacingSm)) }
                        } else {
                            item(key = "path", span = fullLine) { PathBar(pathSegments, theme, FutureDimens.spacingSm) }
                        }
                        items(entries.size, key = { entries[it].file.absolutePath }) { index ->
                            val entry = entries[index]
                            FileTile(
                                entry = entry,
                                displayName = displayNameOf(entry),
                                theme = theme,
                                isSelected = selectedEntries.contains(entry),
                                onClick = {
                                    if (selectedEntries.isNotEmpty()) toggleSelection(entry)
                                    else onOpenFile(entry)
                                },
                                onToggleSelection = { toggleSelection(entry) },
                                onFocused = { focused -> if (focused) focusedEntry = entry else if (focusedEntry == entry) focusedEntry = null },
                                focusRequester = rowFocusRequesters.getOrPut(entry.file.absolutePath) { FocusRequester() },
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(bottom = FutureDimens.spacingLg),
                    ) {
                        // פריטי הכותרת - מספרם = headerCount.
                        item(key = "hero") { FilesHero(screenTitle, heroSubtitle, theme) }
                        if (isRoot) {
                            item(key = "storage") { StorageCard(storage, repository, theme, horizontalInset = FutureDimens.spacingLg) }
                            item(key = "section") { FutureSectionHeader(tr("תיקיות"), theme) }
                        } else {
                            item(key = "path") { PathBar(pathSegments, theme, FutureDimens.spacingXl) }
                        }
                        // הפריטים בכרטיס אחד (One UI): כל פריט הוא פרוסה של הכרטיס - הראשון
                        // עם הפינות העליונות, האחרון עם התחתונות, וקו שיער ביניהם.
                        itemsIndexed(entries, key = { _, it -> it.file.absolutePath }) { index, entry ->
                          CardSlice(theme, first = index == 0, last = index == entries.lastIndex) {
                            FileRow(
                                entry,
                                displayNameOf(entry),
                                repository,
                                theme,
                                isSelected = selectedEntries.contains(entry),
                                onClick = {
                                    if (selectedEntries.isNotEmpty()) toggleSelection(entry)
                                    else onOpenFile(entry)
                                },
                                onMenu = { menuEntry = entry },
                                onToggleSelection = { toggleSelection(entry) },
                                onFocusChanged = { isFocused -> if (isFocused) focusedEntry = entry else if (focusedEntry == entry) focusedEntry = null },
                                focusRequester = rowFocusRequesters.getOrPut(entry.file.absolutePath) { FocusRequester() },
                            )
                          }
                        }
                    }
                }
            }

            menuEntry?.let { entry ->
                FileOptionsMenu(
                    entryName = displayNameOf(entry),
                    isDirectory = entry.isDirectory,
                    theme = theme,
                    onDismiss = { menuEntry = null },
                    onRename = { menuEntry = null; renameEntry = entry },
                    onShare = { menuEntry = null; onShare(entry) },
                    onOpenExternally = { menuEntry = null; onOpenExternally(entry) },
                    onCopy = { menuEntry = null; onCopy(entry) },
                    onMove = { menuEntry = null; onMove(entry) },
                    onDetails = { menuEntry = null; detailsEntry = entry },
                    isImage = categorize(entry.file) == FileCategory.IMAGE,
                    onOpenInGallery = { menuEntry = null; onOpenInGallery(entry) },
                    onDelete = { menuEntry = null; deleteEntryState = entry },
                    onSelect = { menuEntry = null; toggleSelection(entry) },
                    folderRows = {
                        FolderMenuRows(theme, hasClipboard, gridView, onDone = { menuEntry = null }, onNewFolder = { showNewFolder = true }, onPaste = onPaste, onToggleGridView = onToggleGridView)
                    },
                )
            }

            if (generalMenu) {
                FutureOptionsMenu(theme = theme, onDismissRequest = { generalMenu = false }, header = if (isRoot) tr("קבצים") else currentDir.name) {
                    if (entries.isNotEmpty()) {
                        FutureMenuRow(tr("בחר הכול"), FutureIcons.Checklist, theme, { generalMenu = false; selectAll() })
                    }
                    FolderMenuRows(theme, hasClipboard, gridView, onDone = { generalMenu = false }, onNewFolder = { showNewFolder = true }, onPaste = onPaste, onToggleGridView = onToggleGridView)
                }
            }

            if (selectionMenu) {
                val selected = selectedEntries.toList()
                fun pick(action: () -> Unit): () -> Unit = { selectionMenu = false; action() }
                FutureOptionsMenu(theme = theme, onDismissRequest = { selectionMenu = false }, header = tr("{0} נבחרו", selected.size)) {
                    if (selected.size < entries.size) {
                        FutureMenuRow(tr("בחר הכול"), FutureIcons.Checklist, theme, pick { selectAll() })
                    }
                    FutureMenuRow(tr("העתק"), FutureIcons.ContentCopy, theme, pick { onCopyMultiple(selected); clearSelection() })
                    FutureMenuRow(tr("העבר"), FutureIcons.DriveFileMove, theme, pick { onMoveMultiple(selected); clearSelection() })
                    if (selected.any { !it.isDirectory }) {
                        FutureMenuRow(tr("שתף"), FutureIcons.Share, theme, pick { onShareMultiple(selected); clearSelection() })
                    }
                    FutureMenuRow(tr("בטל בחירה"), FutureIcons.Close, theme, pick { clearSelection() })
                    FutureMenuRow(tr("מחק"), FutureIcons.Delete, theme, pick { deleteMultipleState = selected }, destructive = true)
                }
            }

            detailsEntry?.let { entry ->
                FileDetailsDialog(entry = entry, displayName = displayNameOf(entry), repository = repository, theme = theme, onDismiss = { detailsEntry = null })
            }

            renameEntry?.let { entry ->
                NameInputDialog(
                    title = tr("שינוי שם"),
                    initialValue = entry.file.name,
                    theme = theme,
                    onDismiss = { renameEntry = null },
                    onConfirm = { newName ->
                        renameEntry = null
                        if (newName.isNotBlank()) onRename(entry, newName)
                    }
                )
            }

            deleteEntryState?.let { entry ->
                ConfirmDialog(
                    message = tr("למחוק את \"{0}\"?", displayNameOf(entry)),
                    theme = theme,
                    onCancel = { deleteEntryState = null },
                    onConfirm = { deleteEntryState = null; onDelete(entry) }
                )
            }

            deleteMultipleState?.let { entriesToDelete ->
                ConfirmDialog(
                    message = tr("למחוק {0} פריטים?", entriesToDelete.size),
                    theme = theme,
                    onCancel = { deleteMultipleState = null },
                    onConfirm = {
                        deleteMultipleState = null
                        onDeleteMultiple(entriesToDelete)
                        clearSelection()
                    }
                )
            }

            if (showNewFolder) {
                NameInputDialog(
                    title = tr("תיקייה חדשה"),
                    initialValue = "",
                    theme = theme,
                    onDismiss = { showNewFolder = false },
                    onConfirm = { name ->
                        showNewFolder = false
                        if (name.isNotBlank()) {
                            pendingFocusPath = java.io.File(currentDir, name).absolutePath
                            onNewFolder(name)
                        }
                    }
                )
            }

            if (isBusy) {
                // ההכהיה של הדיאלוג (60%) ומשטח של דיאלוג: בהעתקה - פס התקדמות
                // וכפתור ביטול; בפעולה בלי אורך ידוע (מחיקה) - ספינר.
                Box(modifier = Modifier.fillMaxSize().background(theme.scrimColor), contentAlignment = Alignment.Center) {
                    Column(
                        modifier = Modifier
                            .onFocusChanged { overlayFocused = it.hasFocus }
                            .fillMaxWidth(0.85f)
                            .clip(FutureShapes.dialog)
                            .background(theme.surfaceColor)
                            .padding(horizontal = FutureDimens.spacingXl, vertical = FutureDimens.spacingLg),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(FutureDimens.spacingMd),
                    ) {
                        if (busyProgress != null) {
                            Text("מעתיק · ${(busyProgress * 100).toInt()}%", color = theme.textColor, fontSize = FutureTypography.body)
                            com.future.sharednav.components.FutureProgressBar(progress = busyProgress, theme = theme)
                            if (onCancelBusy != null) {
                                val cancelFocus = remember { FocusRequester() }
                                LaunchedEffect(Unit) { runCatching { cancelFocus.requestFocus() } }
                                FutureButton("ביטול", theme, onCancelBusy, variant = com.future.sharednav.components.FutureButtonVariant.Secondary, focusRequester = cancelFocus)
                            }
                        } else {
                            FutureSpinner(theme = theme, label = tr("מבצע פעולה"))
                        }
                    }
                }
            }
        }
    }
}

/** נפח האחסון הפנימי - לכרטיס האחסון שבשורש. */
private class StorageInfo(val totalBytes: Long, val freeBytes: Long) {
    val usedBytes: Long get() = (totalBytes - freeBytes).coerceAtLeast(0L)
    val fraction: Float get() = if (totalBytes > 0) usedBytes.toFloat() / totalBytes else 0f
}

/**
 * הסרגל העליון בסגנון One UI: שלושת כפתורי הסרגל (מיון, תצוגה, תיקייה חדשה)
 * תמיד במקום, והכותרת הקטנה מופיעה לידם רק כשהכותרת הגדולה נגללה החוצה.
 */
@Composable
private fun FilesTopBar(
    title: String,
    showTitle: Boolean,
    theme: FutureTheme,
    gridView: Boolean,
    enabled: Boolean,
    onSort: () -> Unit,
    onToggleView: () -> Unit,
    onNewFolder: () -> Unit,
) {
    val type = com.future.sharednav.theme.rememberFutureType()
    val titleAlpha by animateFloatAsState(if (showTitle) 1f else 0f, label = "collapsedTitle")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = FutureDimens.rowHeightTopBarButton + FutureDimens.spacingLg)
            .padding(horizontal = FutureDimens.spacingLg, vertical = FutureDimens.spacingSm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(FutureDimens.spacingSm),
    ) {
        Text(
            title,
            color = theme.textColor,
            fontSize = type.screenTitle,
            fontWeight = FutureTypography.weightBold,
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).graphicsLayer { alpha = titleAlpha },
        )
        if (enabled) {
            FocusableIconButton(FutureIcons.AutoMirrored.Sort, "מיון", theme, onSort)
            FocusableIconButton(if (gridView) FutureIcons.AutoMirrored.ViewList else FutureIcons.GridView, if (gridView) tr("תצוגת רשימה") else tr("תצוגת רשת"), theme, onToggleView)
            FocusableIconButton(FutureIcons.CreateNewFolder, tr("תיקייה חדשה"), theme, onNewFolder)
        }
    }
}

/**
 * הכותרת הגדולה של One UI: שם התיקייה בגודל display במרכז אזור פתוח, ומתחתיו
 * שורת משנה מעומעמת. נגללת עם הרשימה, והסרגל העליון מקבל אז את הכותרת הקטנה.
 */
@Composable
private fun FilesHero(title: String, subtitle: String?, theme: FutureTheme) {
    val type = com.future.sharednav.theme.rememberFutureType()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(HeroHeight)
            .padding(horizontal = FutureDimens.spacingXl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            title,
            color = theme.textColor,
            fontSize = type.display,
            fontWeight = FutureTypography.weightBold,
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        if (subtitle != null) {
            Spacer(modifier = Modifier.height(FutureDimens.spacingXs))
            Text(
                subtitle,
                color = theme.mutedTextColor,
                fontSize = type.summary,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
    }
}

/** 112dp - גובה אזור הכותרת הגדולה (כרבע מגובה המסך). */
private val HeroHeight = 112.dp

/**
 * שורת הנתיב (breadcrumb) של One UI: "אחסון פנימי › DCIM › Camera". התיקייה
 * הנוכחית בצבע הטקסט ובמשקל בינוני, הקודמות מעומעמות. כל שם מבודד בכיוון
 * שלו (FSI…PDI), כדי ששמות לטיניים לא יערבבו את סדר הנתיב.
 */
@Composable
private fun PathBar(segments: List<String>, theme: FutureTheme, horizontalInset: androidx.compose.ui.unit.Dp) {
    val type = com.future.sharednav.theme.rememberFutureType()
    val text = androidx.compose.ui.text.buildAnnotatedString {
        segments.forEachIndexed { i, name ->
            if (i > 0) {
                pushStyle(androidx.compose.ui.text.SpanStyle(color = theme.mutedTextColor))
                append("  ›  ")
                pop()
            }
            val current = i == segments.lastIndex
            pushStyle(
                androidx.compose.ui.text.SpanStyle(
                    color = if (current) theme.textColor else theme.mutedTextColor,
                    fontWeight = if (current) FutureTypography.weightMedium else null,
                )
            )
            append("⁨$name⁩")
            pop()
        }
    }
    Text(
        text,
        fontSize = type.summary,
        maxLines = 1,
        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = horizontalInset, end = horizontalInset, bottom = FutureDimens.spacingMd),
    )
}

/**
 * כרטיס האחסון שבשורש (One UI "אחסון פנימי"): כמה תפוס מתוך הנפח, ופס
 * התקדמות. לא ממוקד - מידע בלבד.
 */
@Composable
private fun StorageCard(storage: StorageInfo?, repository: FileRepository, theme: FutureTheme, horizontalInset: androidx.compose.ui.unit.Dp) {
    val type = com.future.sharednav.theme.rememberFutureType()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalInset)
            .clip(FutureShapes.xl)
            .background(theme.surfaceColor)
            .padding(horizontal = FutureDimens.spacingLg, vertical = FutureDimens.spacingMd),
        verticalArrangement = Arrangement.spacedBy(FutureDimens.spacingSm),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(FutureDimens.spacingMd)) {
            Icon(FutureIcons.Folder, contentDescription = null, tint = theme.readableAccentColor, modifier = Modifier.size(FutureDimens.iconSettingRow))
            Text(tr("אחסון פנימי"), color = theme.textColor, fontSize = type.title, fontWeight = FutureTypography.weightMedium, modifier = Modifier.weight(1f))
            if (storage != null) {
                Text("${(storage.fraction * 100).toInt()}%", color = theme.mutedTextColor, fontSize = type.summary)
            }
        }
        if (storage != null) {
            com.future.sharednav.components.FutureProgressBar(progress = storage.fraction, theme = theme)
            Text(
                tr("{0} בשימוש מתוך {1}", "⁦${repository.formatSize(storage.usedBytes)}⁩", "⁦${repository.formatSize(storage.totalBytes)}⁩"),
                color = theme.mutedTextColor,
                fontSize = type.summary,
            )
        }
    }
}

/**
 * פרוסה של כרטיס One UI בתוך LazyColumn: FutureCard הוא Column לא עצלה, ולכן
 * כל פריט מצייר את חלקו בכרטיס - הראשון עם הפינות העליונות, האחרון עם
 * התחתונות, וקו שיער מתחת לכל פריט חוץ מהאחרון. השורה בפנים מציירת את
 * הפוקוס שלה בעצמה (בלי הסמן המשותף, שהמילוי שלו היה מוסתר מאחורי הכרטיס).
 */
@Composable
private fun CardSlice(theme: FutureTheme, first: Boolean, last: Boolean, content: @Composable () -> Unit) {
    val r = FutureShapes.radiusXl
    val shape = RoundedCornerShape(
        topStart = if (first) r else 0.dp,
        topEnd = if (first) r else 0.dp,
        bottomStart = if (last) r else 0.dp,
        bottomEnd = if (last) r else 0.dp,
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = FutureDimens.spacingLg)
            .clip(shape)
            .background(theme.surfaceColor)
            .padding(
                start = FutureDimens.spacingXs,
                end = FutureDimens.spacingXs,
                top = if (first) FutureDimens.spacingXs else 0.dp,
                bottom = if (last) FutureDimens.spacingXs else 0.dp,
            ),
    ) {
        CompositionLocalProvider(com.future.sharednav.focus.LocalFocusGlide provides null) { content() }
        if (!last) com.future.sharednav.components.FutureDivider(theme, modifier = Modifier.padding(horizontal = FutureDimens.spacingMd), inset = false)
    }
}

@Composable
private fun FocusableIconButton(icon: ImageVector, contentDescription: String, theme: FutureTheme, onClick: () -> Unit) {
    // עטיפה דקה סביב TopBarIconButton המשותף (מודול SharedKeypadNav) - חתימת
    // הקריאה נשארת זהה כדי שקריאות קיימות ב-Files לא ישתנו.
    com.future.sharednav.components.TopBarIconButton(icon, contentDescription, theme.textColor, theme.accentColor, onClick)
}

/**
 * שורת קובץ - שורת הרשימה של הדיזיין סיסטם (FocusableItem: 14% הדגשה, מסגרת
 * 1.5dp, 1.02). בחירה מרובה מסומנת בתיבת סימון בתחילת השורה (Checkbox.jsx -
 * "bulk-delete"), ולא ב-35% הדגשה על כל השורה. אייקון תיקייה/סוג בצבע
 * הטקסט, לא בהדגשה.
 */
@Composable
private fun FileRow(
    entry: FileEntry,
    displayName: String,
    repository: FileRepository,
    theme: FutureTheme,
    isSelected: Boolean,
    onClick: () -> Unit,
    onMenu: () -> Unit,
    onToggleSelection: () -> Unit,
    onFocusChanged: (Boolean) -> Unit = {},
    focusRequester: FocusRequester? = null,
) {
    var isFocused by remember { mutableStateOf(false) }
    LaunchedEffect(isFocused) { onFocusChanged(isFocused) }
    FocusableItem(
        onClick = onClick,
        accentColor = theme.accentColor,
        contentPadding = 0.dp,
        // שורה בתוך כרטיס - כמו שורת הגדרה, בלי הגדלה בפוקוס (היא הייתה נחתכת בשפת הכרטיס).
        scaleOnFocus = false,
        focusRequester = focusRequester,
        modifier = Modifier
            .fillMaxWidth()
            .onKeyEvent { event ->
                if (isFocused && event.type == KeyEventType.KeyUp) {
                    when (event.key) {
                        Key.Menu, Key.Settings -> { onMenu(); true }
                        Key.Pound -> { onToggleSelection(); true }
                        else -> false
                    }
                } else false
            },
    ) { focused ->
        LaunchedEffect(focused) { isFocused = focused }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = FutureDimens.rowHeightList)
                .padding(horizontal = FutureDimens.spacingMd, vertical = FutureDimens.spacingSm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(FutureDimens.spacingMd),
        ) {
            when {
                isSelected -> Box(modifier = Modifier.size(AvatarListSize), contentAlignment = Alignment.Center) {
                    FutureCheckbox(checked = true, theme = theme)
                }
                else -> FileGlyph(entry, theme, size = AvatarListSize)
            }
            Column(modifier = Modifier.weight(1f)) {
                MarqueeText(
                    text = displayName,
                    color = theme.textColor,
                    isFocused = focused,
                    style = androidx.compose.ui.text.TextStyle(fontSize = FutureTypography.title, fontWeight = FutureTypography.weightMedium),
                )
                // One UI: תאריך השינוי, ולקובץ גם הגודל. lastModified נקרא פעם אחת לפריט.
                val subtitle = remember(entry) {
                    val date = "⁦" + android.text.format.DateFormat.format("dd/MM/yy HH:mm", entry.file.lastModified()) + "⁩"
                    if (entry.isDirectory) date else "$date  ·  ⁦${repository.formatSize(entry.sizeBytes)}⁩"
                }
                Text(subtitle, color = theme.mutedTextColor, fontSize = FutureTypography.summary, maxLines = 1)
            }
        }
    }
}

/**
 * הסמל של פריט: תיקייה - אייקון בצבע ההדגשה (צבע הפוקוס), בלי עיגול סביבו;
 * תמונה - תמונה ממוזערת; קובץ אחר - אייקון הסוג שלו.
 */
@Composable
private fun FileGlyph(entry: FileEntry, theme: FutureTheme, size: androidx.compose.ui.unit.Dp) {
    if (entry.isDirectory) {
        Box(modifier = Modifier.size(size), contentAlignment = Alignment.Center) {
            Icon(FutureIcons.Folder, contentDescription = null, tint = theme.readableAccentColor, modifier = Modifier.size(size * 0.75f))
        }
        return
    }
    FilePreviewIcon(entry, theme, size)
}

/** תצוגה מקדימה (preview) של קובץ ברשימה: תמונה ממוזערת אמיתית לתמונות, ואייקון
 * ייעודי לפי סוג הקובץ (מוזיקה, וידאו, PDF, APK...) לכל שאר הקבצים. */
@Composable
private fun FilePreviewIcon(entry: FileEntry, theme: FutureTheme, size: androidx.compose.ui.unit.Dp) {
    val category = remember(entry.file.absolutePath) { categorize(entry.file) }
    if (category == FileCategory.IMAGE) {
        // F18: lastModified נקרא פעם אחת לפריט ולא בכל הרכבה מחדש של השורה.
        val modified = remember(entry) { entry.file.lastModified() }
        val thumb by produceState<Bitmap?>(ThumbnailCache.get(entry.file), entry.file.absolutePath, modified) {
            value = withContext(Dispatchers.IO) { ThumbnailCache.decode(entry.file, 96) }
        }
        val bitmap = thumb
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(size).clip(FutureShapes.sm)
            )
        } else {
            Box(modifier = Modifier.size(size), contentAlignment = Alignment.Center) {
                Icon(FutureIcons.Image, contentDescription = null, tint = theme.mutedTextColor, modifier = Modifier.size(size * 0.7f))
            }
        }
    } else {
        val icon = when (category) {
            FileCategory.AUDIO -> FutureIcons.MusicNote
            FileCategory.VIDEO -> FutureIcons.Movie
            FileCategory.PDF -> FutureIcons.PictureAsPdf
            FileCategory.APK -> FutureIcons.Android
            else -> if (com.future.files.data.isScript(entry.file)) FutureIcons.Code else FutureIcons.Description
        }
        Box(modifier = Modifier.size(size), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = theme.mutedTextColor, modifier = Modifier.size(size * 0.7f))
        }
    }
}

@Composable
private fun FileOptionsMenu(
    entryName: String,
    isDirectory: Boolean,
    theme: FutureTheme,
    onDismiss: () -> Unit,
    onRename: () -> Unit,
    onShare: () -> Unit,
    onOpenExternally: () -> Unit,
    onCopy: () -> Unit,
    onMove: () -> Unit,
    onDetails: () -> Unit,
    onDelete: () -> Unit,
    isImage: Boolean = false,
    onOpenInGallery: () -> Unit = {},
    onSelect: () -> Unit = {},
    folderRows: @Composable () -> Unit = {},
) {
    FutureOptionsMenu(theme = theme, onDismissRequest = onDismiss, header = entryName) {
        if (isImage) FutureMenuRow("פתח בגלריה", FutureIcons.Image, theme, onOpenInGallery)
        if (!isDirectory) {
            FutureMenuRow(tr("שתף"), FutureIcons.Share, theme, onShare)
            FutureMenuRow(tr("פתח באפליקציה חיצונית"), Icons.Rounded.OpenInNew, theme, onOpenExternally)
        }
        FutureMenuRow(tr("שנה שם"), FutureIcons.DriveFileRenameOutline, theme, onRename)
        FutureMenuRow(tr("העתק"), FutureIcons.ContentCopy, theme, onCopy)
        FutureMenuRow(tr("העבר"), FutureIcons.DriveFileMove, theme, onMove)
        FutureMenuRow(tr("פרטים"), FutureIcons.Info, theme, onDetails)
        // בחירה מרובה: מכאן (או במקש #) מתחילים לסמן, ואז OK מסמן עוד פריטים
        // ו-Options פותח את הפעולות על כל הנבחרים.
        FutureMenuRow(tr("בחר"), FutureIcons.Checklist, theme, onSelect)
        // FL-D14: "מחק" האדום תמיד אחרון - פעולות התיקייה (הדבק כאן...) לפניו.
        folderRows()
        FutureMenuRow(tr("מחק"), FutureIcons.Delete, theme, onDelete, destructive = true)
    }
}

/** פעולות על התיקייה הנוכחית - בסוף כל תפריט Options של הקבצים. */
@Composable
private fun FolderMenuRows(
    theme: FutureTheme,
    hasClipboard: Boolean,
    gridView: Boolean,
    onDone: () -> Unit,
    onNewFolder: () -> Unit,
    onPaste: () -> Unit,
    onToggleGridView: () -> Unit,
) {
    fun pick(action: () -> Unit): () -> Unit = { onDone(); action() }
    if (hasClipboard) FutureMenuRow(tr("הדבק כאן"), FutureIcons.ContentPaste, theme, pick(onPaste))
    FutureMenuRow(tr("תיקייה חדשה"), FutureIcons.CreateNewFolder, theme, pick(onNewFolder))
    FutureMenuRow(
        if (gridView) tr("תצוגת רשימה") else tr("תצוגת רשת"),
        if (gridView) FutureIcons.AutoMirrored.ViewList else FutureIcons.GridView,
        theme,
        pick(onToggleGridView),
    )
}

/**
 * פריט בתצוגת רשת: אייקון גדול (או תמונה ממוזערת) ושם בשתי שורות. תיקייה
 * בצבע ההדגשה - בלי עיגול סביבה.
 */
@Composable
private fun FileTile(
    entry: FileEntry,
    displayName: String,
    theme: FutureTheme,
    isSelected: Boolean,
    onClick: () -> Unit,
    onToggleSelection: () -> Unit,
    onFocused: (Boolean) -> Unit,
    focusRequester: FocusRequester,
) {
    var isFocused by remember { mutableStateOf(false) }
    LaunchedEffect(isFocused) { onFocused(isFocused) }
    FocusableItem(
        onClick = onClick,
        accentColor = theme.accentColor,
        cornerRadius = FutureShapes.radiusXl,
        // אריח ברשת הוא כרטיס קטן (One UI) - משטח הכרטיס, לא צבע צ'יפ.
        idleBackgroundColor = theme.surfaceColor,
        borderWidth = FutureDimens.focusBorderControl,
        contentPadding = 0.dp,
        focusRequester = focusRequester,
        modifier = Modifier
            .fillMaxWidth()
            .onKeyEvent { event ->
                if (isFocused && event.type == KeyEventType.KeyUp && event.key == Key.Pound) {
                    onToggleSelection(); true
                } else false
            },
    ) { focused ->
        LaunchedEffect(focused) { isFocused = focused }
        Column(
            modifier = Modifier.fillMaxWidth().height(TileHeight).padding(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
        ) {
            if (isSelected) {
                FutureCheckbox(checked = true, theme = theme)
            } else {
                FileGlyph(entry, theme, size = 40.dp)
            }
            Text(
                displayName,
                color = theme.textColor,
                fontSize = FutureTypography.summary,
                maxLines = 2,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
    }
}

/** 104dp - גובה פריט ברשת (שלוש עמודות, ארבע שורות על המסך). */
private val TileHeight = 104.dp

@Composable
private fun FileDetailsDialog(entry: FileEntry, displayName: String, repository: FileRepository, theme: FutureTheme, onDismiss: () -> Unit) {
    FutureDialog(
        theme = theme,
        onDismissRequest = onDismiss,
        title = displayName,
        buttons = { FutureButton(tr("סגור"), theme, onDismiss) },
    ) {
        FutureDetailRow(tr("סוג"), if (entry.isDirectory) tr("תיקייה") else tr("קובץ"), theme)
        if (!entry.isDirectory) {
            FutureDetailRow(tr("גודל"), repository.formatSize(entry.sizeBytes), theme)
        }
        FutureDetailRow(tr("נתיב"), "\u2066${entry.file.absolutePath}\u2069", theme) // LRI…PDI: הנתיב משמאל לימין, בלי "/" בסוף
        FutureDetailRow(
            tr("שונה לאחרונה"),
            android.text.format.DateFormat.format("dd/MM/yyyy HH:mm", entry.file.lastModified()).toString(),
            theme
        )
    }
}





@Composable
private fun NameInputDialog(title: String, initialValue: String, theme: FutureTheme, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    // FL-N01: ב-InputDialog המשותף OK בשדה לא שומר (רק פעולת Done של מקלדת מסך). דיאלוג מקומי
    // שבו OK/Enter בשדה שומר, בלי לשנות את SharedKeypadNav.
    var text by remember { mutableStateOf(initialValue) }
    val submit = { if (text.isNotBlank()) onConfirm(text.trim()) }
    FutureDialog(
        theme = theme,
        onDismissRequest = onDismiss,
        title = title,
        buttons = {
            FutureButton("ביטול", theme, onDismiss, variant = com.future.sharednav.components.FutureButtonVariant.Secondary)
            FutureButton("שמור", theme, { submit() })
        },
    ) {
        com.future.sharednav.components.FutureTextField(
            value = text,
            onValueChange = { text = it },
            theme = theme,
            autoFocus = true,
            keyboardActions = androidx.compose.foundation.text.KeyboardActions(onDone = { submit() }),
            modifier = Modifier
                .fillMaxWidth()
                .onPreviewKeyEvent { event ->
                    if (event.type == KeyEventType.KeyUp && (event.key == Key.DirectionCenter || event.key == Key.Enter || event.key == Key.NumPadEnter)) {
                        submit(); true
                    } else event.type == KeyEventType.KeyDown && (event.key == Key.DirectionCenter || event.key == Key.Enter || event.key == Key.NumPadEnter)
                },
        )
    }
}

@Composable
private fun ConfirmDialog(message: String, theme: FutureTheme, onCancel: () -> Unit, onConfirm: () -> Unit) {
    com.future.sharednav.components.ConfirmDialog(message = message, theme = theme, onCancel = onCancel, onConfirm = onConfirm, destructive = true)
}


