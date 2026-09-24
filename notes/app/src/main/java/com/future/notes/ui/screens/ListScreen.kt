package com.future.notes.ui.screens
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.automirrored.rounded.Notes

import com.future.sharednav.icons.FutureIcons

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.ViewAgenda
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.future.notes.data.Checklist
import com.future.sharednav.components.ConfirmDialog
import com.future.sharednav.components.FutureFocusCard
import com.future.sharednav.theme.FutureTypography
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.stringResource
import com.future.notes.R
import com.future.notes.data.Note
import com.future.sharednav.components.EmptyState
import com.future.sharednav.components.FutureListItem
import com.future.sharednav.components.FutureMenuRow
import com.future.sharednav.components.FutureOptionsMenu
import com.future.sharednav.components.FutureTextField
import com.future.sharednav.components.ScreenScaffold
import com.future.sharednav.focus.escapeTextFieldFocusTrap
import com.future.sharednav.nav.onOptionsKeyPress
import com.future.sharednav.theme.FutureDimens
import com.future.sharednav.theme.FutureTheme
import com.future.sharednav.theme.mutedTextColor
import com.future.sharednav.theme.subtleTextColor

/**
 * רשימת הפתקים. שלד המסך, שדה החיפוש והשורות הם הרכיבים של הדיזיין
 * סיסטם (ScreenScaffold / FutureTextField / FutureListItem) - קודם זה היה
 * Scaffold של Material עם כותרת 24sp בצבע ההדגשה, OutlinedTextField, כרטיס
 * לכל פתק וכפתור צף, כלומר ארבעה רכיבים שאין להם מקבילה בעיצוב.
 *
 * נעיצה עברה מכפתור שני בתוך כל שורה אל תפריט האפשרויות: "כל פעולה של
 * מסך יושבת בתפריט" (components/navigation/OptionsMenu.prompt.md). פתק
 * נעוץ מסומן בסיכה בסוף השורה.
 */
@Composable
fun ListScreen(
    notes: List<Note>,
    searchQuery: String,
    theme: FutureTheme,
    onSearchChanged: (String) -> Unit,
    onNoteClick: (Note) -> Unit,
    onAddNote: () -> Unit,
    onTogglePin: (Note) -> Unit,
    onAddChecklist: () -> Unit,
    onDeleteNote: (Note) -> Unit,
    gridView: Boolean,
    onToggleGrid: () -> Unit,
    // הפתק שנפתח לאחרונה - כשחוזרים "אחורה" מהעורך, הפוקוס צריך לשוב אליו
    // בדיוק, לא תמיד לפתק הראשון ברשימה.
    lastSelectedNoteId: Int? = null,
) {
    // בלי פוקוס D-pad התחלתי, המסך הראשי עלול להישאר לגמרי בלתי נגיש
    // בהפעלה. ברשימה ריקה הפוקוס עובר לכפתור ההוספה שבשורה העליונה.
    val rowFocusRequesters = remember { mutableMapOf<Int, FocusRequester>() }
    val addFocusRequester = remember { FocusRequester() }
    LaunchedEffect(notes.map { it.id }) {
        runCatching {
            if (notes.isEmpty()) {
                addFocusRequester.requestFocus()
            } else {
                val target = notes.firstOrNull { it.id == lastSelectedNoteId } ?: notes.first()
                rowFocusRequesters.getOrPut(target.id) { FocusRequester() }.requestFocus()
            }
        }
    }

    var focusedNote by remember { mutableStateOf<Note?>(null) }
    var showMenu by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<Note?>(null) }
    onOptionsKeyPress { showMenu = true }

    ScreenScaffold(
        modifier = Modifier.escapeTextFieldFocusTrap(),
        backgroundColor = theme.backgroundColor,
        title = stringResource(R.string.my_notes),
        textColor = theme.textColor,
        accentColor = theme.accentColor,
        trailingIcon = FutureIcons.Add,
        trailingContentDescription = stringResource(R.string.add_note),
        onTrailingClick = onAddNote,
        trailingFocusRequester = addFocusRequester,
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            FutureTextField(
                value = searchQuery,
                onValueChange = onSearchChanged,
                theme = theme,
                placeholder = stringResource(R.string.search_notes),
                leading = {
                    Icon(
                        FutureIcons.Search,
                        contentDescription = null,
                        tint = theme.mutedTextColor,
                        modifier = Modifier.size(FutureDimens.iconTopBar),
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = FutureDimens.screenPadding, vertical = FutureDimens.spacingSm),
            )

            if (notes.isEmpty()) {
                EmptyState(
                    icon = FutureIcons.Description,
                    title = stringResource(if (searchQuery.isBlank()) R.string.no_notes else R.string.no_notes_found),
                    subtitle = if (searchQuery.isBlank()) stringResource(R.string.no_notes_hint) else null,
                    textColor = theme.textColor,
                    modifier = Modifier.weight(1f),
                )
            } else {
                if (gridView) {
                    NotesGrid(
                        notes = notes,
                        theme = theme,
                        onNoteClick = onNoteClick,
                        focusRequesterFor = { rowFocusRequesters.getOrPut(it) { FocusRequester() } },
                        onFocused = { focusedNote = it },
                        modifier = Modifier.weight(1f),
                    )
                } else LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = FutureDimens.screenPadding, vertical = FutureDimens.spacingSm),
                    verticalArrangement = Arrangement.spacedBy(FutureDimens.itemSpacing),
                ) {
                    itemsIndexed(notes, key = { _, note -> note.id }) { _, note ->
                        FutureListItem(
                            title = note.title.ifEmpty { stringResource(R.string.untitled) },
                            summary = notePreview(note).ifEmpty { stringResource(R.string.no_content) },
                            summaryMaxLines = 2,
                            theme = theme,
                            onClick = { onNoteClick(note) },
                            focusRequester = rowFocusRequesters.getOrPut(note.id) { FocusRequester() },
                            modifier = Modifier.onFocusChanged { if (it.hasFocus) focusedNote = note },
                            trailing = if (note.isPinned || note.audioPath != null) {
                                { NoteBadges(note, theme) }
                            } else null,
                        )
                    }
                }
            }
        }
    }

    if (showMenu) {
        val note = focusedNote?.takeIf { focused -> notes.any { it.id == focused.id } }
        FutureOptionsMenu(
            theme = theme,
            onDismissRequest = { showMenu = false },
            header = note?.title?.ifEmpty { stringResource(R.string.untitled) },
        ) {
            FutureMenuRow(stringResource(R.string.new_note), FutureIcons.Add, theme, {
                showMenu = false
                onAddNote()
            })
            FutureMenuRow("רשימה חדשה", Icons.Rounded.Checklist, theme, {
                showMenu = false
                onAddChecklist()
            })
            if (note != null) {
                FutureMenuRow(
                    stringResource(if (note.isPinned) R.string.unpin else R.string.pin),
                    Icons.Rounded.PushPin,
                    theme,
                    {
                        showMenu = false
                        onTogglePin(note)
                    },
                )
            }
            FutureMenuRow(if (gridView) "תצוגת רשימה" else "תצוגת רשת", if (gridView) Icons.Rounded.ViewAgenda else Icons.Rounded.GridView, theme, {
                showMenu = false
                onToggleGrid()
            })
            if (note != null) {
                FutureMenuRow(stringResource(R.string.delete), FutureIcons.Delete, theme, {
                    showMenu = false
                    pendingDelete = note
                }, destructive = true)
            }
        }
    }

    pendingDelete?.let { note ->
        ConfirmDialog(
            message = stringResource(R.string.delete_note_confirm_message),
            theme = theme,
            onCancel = { pendingDelete = null },
            onConfirm = {
                pendingDelete = null
                onDeleteNote(note)
            },
            confirmLabel = stringResource(R.string.delete_confirm),
            cancelLabel = stringResource(R.string.cancel),
        )
    }
}

/** תקציר לשורה/כרטיס - ברשימה עם ☐/☑, כך שרואים כמה סומן. */
private fun notePreview(note: Note): String =
    if (note.isChecklist) Checklist.display(note.content) else note.content

@Composable
private fun NoteBadges(note: Note, theme: FutureTheme) {
    if (note.audioPath != null) {
        Icon(FutureIcons.Mic, contentDescription = "הקלטה", tint = theme.subtleTextColor, modifier = Modifier.size(FutureDimens.iconTopBar))
    }
    if (note.isPinned) {
        Icon(Icons.Rounded.PushPin, contentDescription = stringResource(R.string.pinned), tint = theme.subtleTextColor, modifier = Modifier.size(FutureDimens.iconTopBar))
    }
}

/** תצוגת רשת - שני טורים של כרטיסים, ניווט בחיצים בכל ארבעת הכיוונים. */
@Composable
private fun NotesGrid(
    notes: List<Note>,
    theme: FutureTheme,
    onNoteClick: (Note) -> Unit,
    focusRequesterFor: (Int) -> FocusRequester,
    onFocused: (Note) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = FutureDimens.screenPadding, vertical = FutureDimens.spacingSm),
        horizontalArrangement = Arrangement.spacedBy(FutureDimens.itemSpacing),
        verticalArrangement = Arrangement.spacedBy(FutureDimens.itemSpacing),
    ) {
        items(notes, key = { it.id }) { note ->
            FutureFocusCard(
                theme = theme,
                onClick = { onNoteClick(note) },
                focusRequester = focusRequesterFor(note.id),
                minHeight = 140.dp,
                modifier = Modifier.onFocusChanged { if (it.hasFocus) onFocused(note) },
            ) { _ ->
                Column(modifier = Modifier.fillMaxWidth().heightIn(min = 116.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            note.title.ifEmpty { stringResource(R.string.untitled) },
                            color = theme.textColor,
                            fontSize = FutureTypography.body,
                            fontWeight = FutureTypography.weightMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        NoteBadges(note, theme)
                    }
                    Text(
                        notePreview(note).ifEmpty { stringResource(R.string.no_content) },
                        color = theme.subtleTextColor,
                        fontSize = FutureTypography.label,
                        maxLines = 5,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = FutureDimens.spacingXs),
                    )
                }
            }
        }
    }
}
