package com.future.notes.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.core.content.ContextCompat
import com.future.notes.R
import com.future.notes.data.Checklist
import com.future.notes.data.ChecklistItem
import com.future.notes.data.Note
import com.future.notes.data.NoteShare
import com.future.notes.ui.VoiceNote
import com.future.sharednav.components.ConfirmDialog
import com.future.sharednav.components.FutureCheckbox
import com.future.sharednav.components.FutureFocusCard
import com.future.sharednav.components.FutureListItem
import com.future.sharednav.components.FutureMenuRow
import com.future.sharednav.components.FutureOptionsMenu
import com.future.sharednav.components.FutureTextField
import com.future.sharednav.components.InputDialog
import com.future.sharednav.components.ScreenScaffold
import com.future.sharednav.focus.escapeTextFieldFocusTrap
import com.future.sharednav.icons.FutureIcons
import com.future.sharednav.nav.onOptionsKeyPress
import com.future.sharednav.share.FutureShare
import com.future.sharednav.theme.FutureDimens
import com.future.sharednav.theme.FutureTheme
import com.future.sharednav.theme.FutureTypography
import com.future.sharednav.theme.subtleTextColor
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * עורך פתק. נשמר אוטומטית תוך כדי הקלדה (אין כפתור שמירה) וגם ביציאה.
 * פתק יכול להיות טקסט או רשימה עם סימון V, ולשאת הקלטה קולית. כל
 * הפעולות - נעיצה, רשימה/טקסט, הקלטה, שיתוף, מחיקה - בתפריט האפשרויות.
 */
@Composable
fun EditorScreen(
    noteId: Int,
    theme: FutureTheme,
    startAsChecklist: Boolean = false,
    loadNote: suspend (Int) -> Note?,
    persist: suspend (Note) -> Int,
    onDelete: (Note) -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var loaded by remember { mutableStateOf(noteId == 0) }
    var id by remember { mutableIntStateOf(noteId) }
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var isPinned by remember { mutableStateOf(false) }
    var isChecklist by remember { mutableStateOf(startAsChecklist) }
    var audioPath by remember { mutableStateOf<String?>(null) }
    // מה שנשמר לאחרונה - כדי לא לכתוב ל-DB כשלא השתנה כלום.
    var savedSnapshot by remember { mutableStateOf<Note?>(null) }

    LaunchedEffect(noteId) {
        if (noteId != 0) {
            loadNote(noteId)?.let {
                title = it.title; content = it.content; isPinned = it.isPinned
                isChecklist = it.isChecklist; audioPath = it.audioPath
                savedSnapshot = it.copy(timestamp = 0)
            }
            loaded = true
        }
    }

    fun current() = Note(id, title, content, 0, isPinned, isChecklist, audioPath)
    fun isEmpty() = title.isBlank() && content.isBlank() && audioPath == null

    suspend fun saveNow() {
        if (!loaded) return
        val note = current()
        if (note == savedSnapshot) return
        if (id == 0 && isEmpty()) return
        id = persist(note)
        savedSnapshot = note.copy(id = id)
    }

    // שמירה אוטומטית - חצי שנייה אחרי שמפסיקים להקליד.
    LaunchedEffect(title, content, isPinned, isChecklist, audioPath, loaded) {
        delay(600)
        saveNow()
    }

    val voice = remember { VoiceNote(context) }
    var recording by remember { mutableStateOf(false) }
    var playing by remember { mutableStateOf(false) }
    var recordStart by remember { mutableLongStateOf(0L) }
    var elapsed by remember { mutableLongStateOf(0L) }
    DisposableEffect(Unit) { onDispose { voice.release() } }
    LaunchedEffect(recording) {
        while (recording) {
            elapsed = System.currentTimeMillis() - recordStart
            delay(250)
        }
    }

    fun startRecording() {
        if (voice.startRecording()) {
            recordStart = System.currentTimeMillis(); elapsed = 0; recording = true
        } else {
            Toast.makeText(context, "ההקלטה נכשלה", Toast.LENGTH_SHORT).show()
        }
    }

    fun stopRecording() {
        recording = false
        val path = voice.stopRecording() ?: return
        audioPath?.let { runCatching { java.io.File(it).delete() } }
        audioPath = path
    }

    val micPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) startRecording() else Toast.makeText(context, "צריך הרשאת מיקרופון להקלטה", Toast.LENGTH_SHORT).show()
    }

    fun requestRecording() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            startRecording()
        } else {
            micPermission.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    fun exit() {
        if (recording) stopRecording()
        scope.launch {
            saveNow()
            onBack()
        }
    }

    BackHandler { exit() }

    var items by remember(isChecklist, content) { mutableStateOf(Checklist.parse(content)) }
    fun setItems(newItems: List<ChecklistItem>) {
        items = newItems
        content = Checklist.serialize(newItems)
    }
    var focusedItem by remember { mutableStateOf<Int?>(null) }
    var editingItem by remember { mutableStateOf<Int?>(null) }
    var newItemText by remember { mutableStateOf("") }

    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    onOptionsKeyPress { showMenu = true }

    ScreenScaffold(
        modifier = Modifier.escapeTextFieldFocusTrap(),
        backgroundColor = theme.backgroundColor,
        title = stringResource(if (noteId == 0) R.string.new_note else R.string.edit_note),
        textColor = theme.textColor,
        accentColor = theme.accentColor,
        onBack = { exit() },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = FutureDimens.screenPadding, vertical = FutureDimens.spacingSm),
            verticalArrangement = Arrangement.spacedBy(FutureDimens.itemSpacing),
        ) {
            FutureTextField(
                value = title,
                onValueChange = { title = it },
                theme = theme,
                placeholder = stringResource(R.string.title),
                autoFocus = noteId == 0,
                modifier = Modifier.fillMaxWidth(),
            )

            if (recording || audioPath != null) {
                AudioRow(
                    theme = theme,
                    recording = recording,
                    playing = playing,
                    label = when {
                        recording -> "מקליט… ${VoiceNote.format(elapsed)} · OK לעצירה"
                        playing -> "מתנגן · OK לעצירה"
                        else -> "הקלטה קולית · ${VoiceNote.format(remember(audioPath) { audioPath?.let(VoiceNote::durationMs) ?: 0L })}"
                    },
                    onClick = {
                        when {
                            recording -> stopRecording()
                            playing -> { voice.stopPlayback(); playing = false }
                            else -> audioPath?.let { playing = voice.play(it) { playing = false } }
                        }
                    },
                )
            }

            if (isChecklist) {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentPadding = PaddingValues(vertical = FutureDimens.spacingXs),
                    verticalArrangement = Arrangement.spacedBy(FutureDimens.spacingXs),
                ) {
                    itemsIndexed(items) { index, item ->
                        FutureListItem(
                            title = item.text,
                            theme = theme,
                            titleColor = if (item.checked) theme.subtleTextColor else theme.textColor,
                            titleDecoration = if (item.checked) TextDecoration.LineThrough else null,
                            onClick = {
                                setItems(items.mapIndexed { i, it -> if (i == index) it.copy(checked = !it.checked) else it })
                            },
                            leading = { FutureCheckbox(checked = item.checked, theme = theme) },
                            modifier = Modifier.onFocusChanged {
                                if (it.isFocused) focusedItem = index
                                else if (focusedItem == index) focusedItem = null
                            },
                        )
                    }
                    item {
                        FutureTextField(
                            value = newItemText,
                            onValueChange = { newItemText = it },
                            theme = theme,
                            placeholder = "פריט חדש",
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = {
                                if (newItemText.isNotBlank()) {
                                    setItems(items + ChecklistItem(newItemText.trim(), false))
                                    newItemText = ""
                                }
                            }),
                            leading = {
                                Icon(FutureIcons.Add, null, tint = theme.subtleTextColor, modifier = Modifier.size(FutureDimens.iconTopBar))
                            },
                            modifier = Modifier.fillMaxWidth().padding(top = FutureDimens.spacingXs),
                        )
                    }
                }
            } else {
                FutureTextField(
                    value = content,
                    onValueChange = { content = it },
                    theme = theme,
                    placeholder = stringResource(R.string.content),
                    singleLine = false,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                )
            }
        }
    }

    if (showMenu) {
        val itemIndex = focusedItem?.takeIf { isChecklist && it in items.indices }
        FutureOptionsMenu(
            theme = theme,
            onDismissRequest = { showMenu = false },
            header = title.ifBlank { stringResource(R.string.untitled) },
        ) {
            if (itemIndex != null) {
                FutureMenuRow("עריכת פריט", FutureIcons.Edit, theme, {
                    showMenu = false
                    editingItem = itemIndex
                })
                FutureMenuRow("מחיקת פריט", FutureIcons.Close, theme, {
                    showMenu = false
                    setItems(items.filterIndexed { i, _ -> i != itemIndex })
                })
            }
            FutureMenuRow(stringResource(if (isPinned) R.string.unpin else R.string.pin), Icons.Rounded.PushPin, theme, {
                showMenu = false
                isPinned = !isPinned
            })
            FutureMenuRow(if (isChecklist) "הפוך לפתק טקסט" else "הפוך לרשימה", Icons.Rounded.Checklist, theme, {
                showMenu = false
                if (isChecklist) {
                    content = Checklist.parse(content).joinToString("\n") { it.text }
                    isChecklist = false
                } else {
                    content = Checklist.serialize(content.lines().filter { it.isNotBlank() }.map { ChecklistItem(it.trim(), false) })
                    isChecklist = true
                }
            })
            if (recording) {
                FutureMenuRow("עצירת הקלטה", FutureIcons.MicOff, theme, {
                    showMenu = false
                    stopRecording()
                })
            } else {
                FutureMenuRow(if (audioPath == null) "הקלטה קולית" else "הקלטה חדשה", FutureIcons.Mic, theme, {
                    showMenu = false
                    voice.stopPlayback(); playing = false
                    requestRecording()
                })
            }
            if (audioPath != null && !recording) {
                FutureMenuRow("מחיקת ההקלטה", FutureIcons.VolumeOff, theme, {
                    showMenu = false
                    voice.stopPlayback(); playing = false
                    audioPath?.let { runCatching { java.io.File(it).delete() } }
                    audioPath = null
                })
            }
            if (!isEmpty()) {
                FutureMenuRow("שיתוף ל-FuturePhone", FutureIcons.Share, theme, {
                    showMenu = false
                    FutureShare.text(context, NoteShare.format(current()), "שיתוף פתק")
                })
            }
            if (id != 0) {
                FutureMenuRow(stringResource(R.string.delete), FutureIcons.Delete, theme, {
                    showMenu = false
                    showDeleteConfirm = true
                }, destructive = true)
            }
        }
    }

    editingItem?.let { index ->
        InputDialog(
            title = "עריכת פריט",
            theme = theme,
            initialValue = items.getOrNull(index)?.text.orEmpty(),
            onDismiss = { editingItem = null },
            onConfirm = { text ->
                editingItem = null
                setItems(items.mapIndexed { i, it -> if (i == index) it.copy(text = text.trim()) else it })
            },
        )
    }

    if (showDeleteConfirm) {
        ConfirmDialog(
            message = stringResource(R.string.delete_note_confirm_message),
            theme = theme,
            onCancel = { showDeleteConfirm = false },
            onConfirm = {
                showDeleteConfirm = false
                if (recording) { recording = false; voice.stopRecording() }
                onDelete(current())
            },
            confirmLabel = stringResource(R.string.delete_confirm),
            cancelLabel = stringResource(R.string.cancel),
        )
    }
}

@Composable
private fun AudioRow(theme: FutureTheme, recording: Boolean, playing: Boolean, label: String, onClick: () -> Unit) {
    FutureFocusCard(theme = theme, onClick = onClick, modifier = Modifier.fillMaxWidth()) { _ ->
        Icon(
            when {
                recording -> FutureIcons.Mic
                playing -> FutureIcons.Pause
                else -> FutureIcons.PlayArrow
            },
            contentDescription = null,
            tint = if (recording) theme.dangerColor else theme.textColor,
            modifier = Modifier.size(FutureDimens.iconTopBar),
        )
        Text(label, color = theme.textColor, fontSize = FutureTypography.body)
    }
}
