package com.future.files.ui
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.CreateNewFolder
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.DriveFileMove
import androidx.compose.material.icons.rounded.DriveFileRenameOutline
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Share
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
    // הנתיב של הפריט שנפתח לאחרונה מהתיקייה הזו - כשחוזרים "אחורה" (מקובץ
    // שנפתח, או מתת-תיקייה) הפוקוס צריך לשוב אליו בדיוק, לא תמיד לפריט הראשון.
    lastSelectedPath: String? = null,
) {
    val repository = remember { FileRepository() }
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
    // מקש Options הפיזי נחסם ברמת המערכת ולא מגיע כ-Key.Menu לאפליקציה -
    // זו הדרך האמיתית שהוא פותח את תפריט הפעולות של הפריט הממוקד.
    com.future.sharednav.nav.onOptionsKeyPress { if (focusedEntry != null) menuEntry = focusedEntry }
    // FocusRequester לפי נתיב - מתאפס בכל מעבר תיקייה, כדי שגם הפוקוס ההתחלתי
    // וגם השחזור אחרי חזרה "אחורה" יעבדו על אותה תבנית.
    val rowFocusRequesters = remember(currentDir) { mutableMapOf<String, FocusRequester>() }
    LaunchedEffect(currentDir, entries.map { it.file.absolutePath }) {
        val target = entries.firstOrNull { it.file.absolutePath == lastSelectedPath } ?: entries.firstOrNull()
        target?.let { rowFocusRequesters.getOrPut(it.file.absolutePath) { FocusRequester() }.requestFocus() }
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

    // Reset selection when changing directory
    LaunchedEffect(currentDir) {
        clearSelection()
    }

    fun displayNameOf(entry: FileEntry): String = entry.file.displayName(isRoot && entry.isDirectory)

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Box(modifier = Modifier.escapeTextFieldFocusTrap().fillMaxSize().background(theme.backgroundColor)) {
            Column(modifier = Modifier.fillMaxSize()) {
                if (selectedEntries.isEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (!isRoot) {
                            FocusableIconButton(Icons.AutoMirrored.Rounded.ArrowBack, "חזור", theme, onBack)
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(
                            if (isRoot) "קבצים" else currentDir.displayName(currentDirIsTopLevelFolder),
                            fontSize = FutureTypography.screenTitle,
                            fontWeight = FontWeight.Bold,
                            color = theme.textColor,
                            maxLines = 1,
                            modifier = Modifier.weight(1f)
                        )
                        if (hasAccess && hasClipboard) {
                            FocusableIconButton(Icons.Rounded.ContentPaste, "הדבק", theme, onPaste)
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        if (hasAccess && focusedEntry != null) {
                            FocusableIconButton(Icons.Rounded.MoreVert, "אפשרויות", theme, { menuEntry = focusedEntry })
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        if (hasAccess) {
                            FocusableIconButton(Icons.Rounded.CreateNewFolder, "תיקייה חדשה", theme, { showNewFolder = true })
                        }
                    }
                } else {
                    // סרגל בחירה מרובה
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(theme.accentColor.copy(alpha = 0.15f))
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FocusableIconButton(Icons.Rounded.Close, "בטל בחירה", theme, ::clearSelection)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            "${selectedEntries.size} נבחרו",
                            fontSize = FutureTypography.title,
                            fontWeight = FontWeight.Bold,
                            color = theme.textColor,
                            modifier = Modifier.weight(1f)
                        )
                        FocusableIconButton(Icons.Rounded.Share, "שתף נבחרים", theme, {
                            onShareMultiple(selectedEntries.toList())
                            clearSelection()
                        })
                        Spacer(modifier = Modifier.width(8.dp))
                        FocusableIconButton(Icons.Rounded.Delete, "מחק נבחרים", theme, {
                            deleteMultipleState = selectedEntries.toList()
                        })
                    }
                }

                if (!hasAccess) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("כדי לגשת לקבצים צריך לאשר הרשאת ניהול קבצים", color = theme.textColor.copy(alpha = 0.7f), fontSize = FutureTypography.bodyLarge)
                        Spacer(modifier = Modifier.height(16.dp))
                        FutureButton("אשר הרשאה", theme, onRequestAccess)
                    }
                } else if (entries.isEmpty()) {
                    EmptyState(
                        icon = Icons.Rounded.Folder,
                        title = "התיקייה ריקה",
                        textColor = theme.textColor,
                    )
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(entries, key = { it.file.absolutePath }) { entry ->
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
                                onFocusChanged = { isFocused -> if (isFocused) focusedEntry = entry },
                                focusRequester = rowFocusRequesters.getOrPut(entry.file.absolutePath) { FocusRequester() },
                            )
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
                    onDelete = { menuEntry = null; deleteEntryState = entry }
                )
            }

            detailsEntry?.let { entry ->
                FileDetailsDialog(entry = entry, displayName = displayNameOf(entry), repository = repository, theme = theme, onDismiss = { detailsEntry = null })
            }

            renameEntry?.let { entry ->
                NameInputDialog(
                    title = "שינוי שם",
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
                    message = "למחוק את \"${displayNameOf(entry)}\"?",
                    theme = theme,
                    onCancel = { deleteEntryState = null },
                    onConfirm = { deleteEntryState = null; onDelete(entry) }
                )
            }

            deleteMultipleState?.let { entriesToDelete ->
                ConfirmDialog(
                    message = "למחוק ${entriesToDelete.size} פריטים?",
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
                    title = "תיקייה חדשה",
                    initialValue = "",
                    theme = theme,
                    onDismiss = { showNewFolder = false },
                    onConfirm = { name ->
                        showNewFolder = false
                        if (name.isNotBlank()) onNewFolder(name)
                    }
                )
            }

            if (isBusy) {
                // המתנה בלי אורך ידוע: ההכהיה של הדיאלוג (60%) וספינר, על משטח של דיאלוג.
                Box(modifier = Modifier.fillMaxSize().background(theme.scrimColor), contentAlignment = Alignment.Center) {
                    Box(
                        modifier = Modifier
                            .clip(FutureShapes.dialog)
                            .background(theme.surfaceColor)
                            .padding(horizontal = FutureDimens.spacingXl, vertical = FutureDimens.spacingLg)
                    ) {
                        FutureSpinner(theme = theme, label = "מבצע פעולה")
                    }
                }
            }
        }
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
                entry.isDirectory -> FutureAvatar(theme = theme, icon = Icons.Rounded.Folder)
                else -> FilePreviewIcon(entry, theme)
            }
            Column(modifier = Modifier.weight(1f)) {
                MarqueeText(
                    text = displayName,
                    color = theme.textColor,
                    isFocused = focused,
                    style = androidx.compose.ui.text.TextStyle(fontSize = FutureTypography.title, fontWeight = FutureTypography.weightMedium),
                )
                if (!entry.isDirectory) {
                    Text(repository.formatSize(entry.sizeBytes), color = theme.mutedTextColor, fontSize = FutureTypography.summary)
                }
            }
        }
    }
}

/** תצוגה מקדימה (preview) של קובץ ברשימה: תמונה ממוזערת אמיתית לתמונות, ואייקון
 * ייעודי לפי סוג הקובץ (מוזיקה, וידאו, PDF, APK...) לכל שאר הקבצים. */
@Composable
private fun FilePreviewIcon(entry: FileEntry, theme: FutureTheme) {
    val category = remember(entry.file.absolutePath) { categorize(entry.file) }
    if (category == FileCategory.IMAGE) {
        val thumb by produceState<Bitmap?>(ThumbnailCache.get(entry.file), entry.file.absolutePath, entry.file.lastModified()) {
            value = withContext(Dispatchers.IO) { ThumbnailCache.decode(entry.file, 96) }
        }
        val bitmap = thumb
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(AvatarListSize).clip(FutureShapes.sm)
            )
        } else {
            FutureAvatar(theme = theme, icon = Icons.Rounded.Image)
        }
    } else {
        val icon = when (category) {
            FileCategory.AUDIO -> Icons.Rounded.MusicNote
            FileCategory.VIDEO -> Icons.Rounded.Movie
            FileCategory.PDF -> Icons.Rounded.PictureAsPdf
            FileCategory.APK -> Icons.Rounded.Android
            else -> Icons.Rounded.Description
        }
        FutureAvatar(theme = theme, icon = icon)
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
    onDelete: () -> Unit
) {
    FutureOptionsMenu(theme = theme, onDismissRequest = onDismiss, header = entryName) {
        if (!isDirectory) {
            FutureMenuRow("שתף", Icons.Rounded.Share, theme, onShare)
            FutureMenuRow("פתח באפליקציה חיצונית", Icons.Rounded.OpenInNew, theme, onOpenExternally)
        }
        FutureMenuRow("שנה שם", Icons.Rounded.DriveFileRenameOutline, theme, onRename)
        FutureMenuRow("העתק", Icons.Rounded.ContentCopy, theme, onCopy)
        FutureMenuRow("העבר", Icons.Rounded.DriveFileMove, theme, onMove)
        FutureMenuRow("פרטים", Icons.Rounded.Info, theme, onDetails)
        FutureMenuRow("מחק", Icons.Rounded.Delete, theme, onDelete, destructive = true)
    }
}

@Composable
private fun FileDetailsDialog(entry: FileEntry, displayName: String, repository: FileRepository, theme: FutureTheme, onDismiss: () -> Unit) {
    FutureDialog(
        theme = theme,
        onDismissRequest = onDismiss,
        title = displayName,
        buttons = { FutureButton("סגור", theme, onDismiss) },
    ) {
        FutureDetailRow("סוג", if (entry.isDirectory) "תיקייה" else "קובץ", theme)
        if (!entry.isDirectory) {
            FutureDetailRow("גודל", repository.formatSize(entry.sizeBytes), theme)
        }
        FutureDetailRow("נתיב", entry.file.absolutePath, theme)
        FutureDetailRow(
            "שונה לאחרונה",
            android.text.format.DateFormat.format("dd/MM/yyyy HH:mm", entry.file.lastModified()).toString(),
            theme
        )
    }
}





@Composable
private fun NameInputDialog(title: String, initialValue: String, theme: FutureTheme, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    InputDialog(title = title, theme = theme, initialValue = initialValue, onDismiss = onDismiss, onConfirm = onConfirm)
}

@Composable
private fun ConfirmDialog(message: String, theme: FutureTheme, onCancel: () -> Unit, onConfirm: () -> Unit) {
    com.future.sharednav.components.ConfirmDialog(message = message, theme = theme, onCancel = onCancel, onConfirm = onConfirm, destructive = true)
}


