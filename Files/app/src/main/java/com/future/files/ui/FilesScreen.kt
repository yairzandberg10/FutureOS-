package com.future.files.ui
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
import androidx.compose.ui.window.Dialog
import com.future.files.data.FileCategory
import com.future.files.data.FileEntry
import com.future.files.data.FileRepository
import com.future.files.data.ThumbnailCache
import com.future.files.data.categorize
import com.future.files.data.displayName
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
        Box(modifier = Modifier.fillMaxSize().background(theme.backgroundColor)) {
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
                            fontSize = 20.sp,
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
                            fontSize = 18.sp,
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
                        Text("כדי לגשת לקבצים צריך לאשר הרשאת ניהול קבצים", color = theme.textColor.copy(alpha = 0.7f), fontSize = 15.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        val interactionSource = remember { MutableInteractionSource() }
                        val isFocused by interactionSource.collectIsFocusedAsState()
                        val bgColor by animateColorAsState(if (isFocused) theme.accentColor else theme.accentColor.copy(alpha = 0.7f), label = "btnBg")
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(bgColor)
                                .clickable(interactionSource = interactionSource, indication = null, onClick = onRequestAccess)
                                .focusable(interactionSource = interactionSource).bringIntoViewOnFocus()
                                .padding(horizontal = 24.dp, vertical = 12.dp)
                        ) {
                            Text("אשר הרשאה", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                } else if (entries.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("התיקייה ריקה", color = theme.textColor.copy(alpha = 0.5f), fontSize = 15.sp)
                    }
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
                Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)), contentAlignment = Alignment.Center) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(theme.surfaceColor)
                            .padding(horizontal = 24.dp, vertical = 16.dp)
                    ) {
                        Text("מבצע פעולה...", color = theme.textColor, fontSize = 14.sp)
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
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    LaunchedEffect(isFocused) { onFocusChanged(isFocused) }
    val shape = RoundedCornerShape(16.dp)
    val bgColor by animateColorAsState(
        if (isSelected) theme.accentColor.copy(alpha = 0.35f)
        else if (isFocused) theme.accentColor.copy(alpha = 0.22f)
        else theme.textColor.copy(alpha = 0.06f),
        label = "rowBg"
    )
    val scale by animateFloatAsState(if (isFocused) 1.02f else 1f, label = "rowScale")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(shape)
            .background(bgColor)
            .then(if (isFocused) Modifier.border(2.dp, theme.accentColor, shape) else Modifier)
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .focusable(interactionSource = interactionSource).bringIntoViewOnFocus()
            .onKeyEvent { event ->
                if (isFocused && event.type == KeyEventType.KeyUp) {
                    when (event.key) {
                        Key.Menu, Key.Settings -> { onMenu(); true }
                        Key.Pound -> { onToggleSelection(); true }
                        else -> false
                    }
                } else false
            }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isSelected) {
            Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = theme.accentColor, modifier = Modifier.size(24.dp))
        } else if (entry.isDirectory) {
            Icon(Icons.Rounded.Folder, contentDescription = null, tint = theme.accentColor, modifier = Modifier.size(24.dp))
        } else {
            FilePreviewIcon(entry, theme)
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(displayName, color = theme.textColor, fontSize = 14.sp, maxLines = 1)
            if (!entry.isDirectory) {
                Text(repository.formatSize(entry.sizeBytes), color = theme.textColor.copy(alpha = 0.5f), fontSize = 11.sp)
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
                modifier = Modifier.size(36.dp).clip(RoundedCornerShape(8.dp))
            )
        } else {
            Icon(Icons.Rounded.Image, contentDescription = null, tint = theme.textColor.copy(alpha = 0.7f), modifier = Modifier.size(24.dp))
        }
    } else {
        val icon = when (category) {
            FileCategory.AUDIO -> Icons.Rounded.MusicNote
            FileCategory.VIDEO -> Icons.Rounded.Movie
            FileCategory.PDF -> Icons.Rounded.PictureAsPdf
            FileCategory.APK -> Icons.Rounded.Android
            else -> Icons.Rounded.Description
        }
        Icon(icon, contentDescription = null, tint = theme.textColor.copy(alpha = 0.7f), modifier = Modifier.size(24.dp))
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
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(theme.surfaceColor)
                .padding(vertical = 8.dp)
        ) {
            Text(entryName, color = theme.textColor.copy(alpha = 0.5f), fontSize = 12.sp, maxLines = 1, modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp))
            if (!isDirectory) {
                MenuRow("שתף", Icons.Rounded.Share, theme, onShare)
                MenuRow("פתח באפליקציה חיצונית", Icons.Rounded.OpenInNew, theme, onOpenExternally)
            }
            MenuRow("שנה שם", Icons.Rounded.DriveFileRenameOutline, theme, onRename)
            MenuRow("העתק", Icons.Rounded.ContentCopy, theme, onCopy)
            MenuRow("העבר", Icons.Rounded.DriveFileMove, theme, onMove)
            MenuRow("פרטים", Icons.Rounded.Info, theme, onDetails)
            MenuRow("מחק", Icons.Rounded.Delete, theme, onDelete, isDestructive = true)
        }
    }
}

@Composable
private fun FileDetailsDialog(entry: FileEntry, displayName: String, repository: FileRepository, theme: FutureTheme, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(theme.surfaceColor)
                .padding(20.dp)
        ) {
            Text(displayName, color = theme.textColor, fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 2)
            Spacer(modifier = Modifier.height(14.dp))
            DetailRow("סוג", if (entry.isDirectory) "תיקייה" else "קובץ", theme)
            if (!entry.isDirectory) {
                DetailRow("גודל", repository.formatSize(entry.sizeBytes), theme)
            }
            DetailRow("נתיב", entry.file.absolutePath, theme)
            DetailRow(
                "שונה לאחרונה",
                android.text.format.DateFormat.format("dd/MM/yyyy HH:mm", entry.file.lastModified()).toString(),
                theme
            )
            Spacer(modifier = Modifier.height(16.dp))
            DialogButton("סגור", theme.accentColor, onDismiss)
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String, theme: FutureTheme) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Text(label, color = theme.textColor.copy(alpha = 0.5f), fontSize = 11.sp)
        Text(value, color = theme.textColor, fontSize = 13.sp)
    }
}

@Composable
private fun MenuRow(label: String, icon: ImageVector, theme: FutureTheme, onClick: () -> Unit, isDestructive: Boolean = false) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val bgColor by animateColorAsState(if (isFocused) theme.textColor.copy(alpha = 0.12f) else Color.Transparent, label = "menuRowBg")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .focusable(interactionSource = interactionSource).bringIntoViewOnFocus()
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = if (isDestructive) theme.dangerColor else theme.textColor, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(14.dp))
        Text(label, color = if (isDestructive) theme.dangerColor else theme.textColor, fontSize = 15.sp)
    }
}

@Composable
private fun NameInputDialog(title: String, initialValue: String, theme: FutureTheme, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var text by remember { mutableStateOf(initialValue) }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(theme.surfaceColor)
                .padding(20.dp)
        ) {
            Text(title, color = theme.textColor, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Spacer(modifier = Modifier.height(12.dp))
            BasicTextField(
                value = text,
                onValueChange = { text = it },
                textStyle = androidx.compose.ui.text.TextStyle(color = theme.textColor, fontSize = 15.sp),
                cursorBrush = androidx.compose.ui.graphics.SolidColor(theme.accentColor),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
                    .background(theme.textColor.copy(alpha = 0.08f), RoundedCornerShape(10.dp))
                    .padding(12.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                DialogButton("ביטול", theme.textColor.copy(alpha = 0.7f), onDismiss)
                DialogButton("אישור", theme.accentColor, { onConfirm(text) })
            }
        }
    }
}

@Composable
private fun ConfirmDialog(message: String, theme: FutureTheme, onCancel: () -> Unit, onConfirm: () -> Unit) {
    Dialog(onDismissRequest = onCancel) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(theme.surfaceColor)
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(message, color = theme.textColor, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Spacer(modifier = Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                DialogButton("ביטול", theme.textColor.copy(alpha = 0.7f), onCancel)
                DialogButton("מחק", theme.dangerColor, onConfirm)
            }
        }
    }
}

@Composable
private fun DialogButton(text: String, color: Color, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val bgColor by animateColorAsState(if (isFocused) color else color.copy(alpha = 0.7f), label = "dialogBtnBg")
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bgColor)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .focusable(interactionSource = interactionSource).bringIntoViewOnFocus()
            .padding(horizontal = 24.dp, vertical = 12.dp)
    ) {
        Text(text, color = Color.Black, fontWeight = FontWeight.Bold)
    }
}
