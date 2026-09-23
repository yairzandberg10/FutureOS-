package com.future.notes.ui.screens
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Save

import com.future.sharednav.icons.FutureIcons

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.future.notes.R
import com.future.notes.data.Note
import com.future.sharednav.components.ConfirmDialog
import com.future.sharednav.components.FutureMenuRow
import com.future.sharednav.components.FutureOptionsMenu
import com.future.sharednav.components.FutureTextField
import com.future.sharednav.components.ScreenScaffold
import com.future.sharednav.focus.escapeTextFieldFocusTrap
import com.future.sharednav.nav.onOptionsKeyPress
import com.future.sharednav.theme.FutureDimens
import com.future.sharednav.theme.FutureTheme

/**
 * עורך פתק. השורה העליונה היא ScreenTopBar (חזרה שומרת, ושמירה מפורשת
 * כפעולה בצד השני); נעיצה ומחיקה יושבות בתפריט האפשרויות, כמו שהמדריך
 * כבר מתאר ("מחיקת פתק מתבצעת מתוך תפריט האפשרויות של הפתק הפתוח").
 * קודם הן היו ארבעה IconButton של Material בתוך TopAppBar.
 */
@Composable
fun EditorScreen(
    note: Note?,
    theme: FutureTheme,
    onSave: (String, String, Boolean) -> Unit,
    onDelete: () -> Unit,
    onBack: () -> Unit
) {
    // ה-remember חייב להיות ממופתח (keyed) ב-id של ההערה: כשהמסך נפתח לפני
    // שה-Flow מה-DB סיפק את ההערה האמיתית, note==null וה-state מאותחל לריק.
    // בלי key, ה-remember הזה לא רץ מחדש כשnote מתעדכן מ-null לערך האמיתי,
    // וכל שמירה אחר כך דורסת את ההערה הקיימת בתוכן ריק (אובדן מידע).
    val noteKey = note?.id ?: -1
    var title by remember(noteKey) { mutableStateOf(note?.title ?: "") }
    var content by remember(noteKey) { mutableStateOf(note?.content ?: "") }
    var isPinned by remember(noteKey) { mutableStateOf(note?.isPinned ?: false) }

    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    onOptionsKeyPress { showMenu = true }

    fun saveIfNeeded() {
        if (title.isNotBlank() || content.isNotBlank()) {
            onSave(title, content, isPinned)
        } else {
            onBack()
        }
    }

    // מקש Back פיזי לא אמור למחוק שינויים בשקט - שומרים (אלא אם שני
    // השדות ריקים, כדי לא ליצור הערות ריקות) לפני חזרה למסך הקודם.
    BackHandler {
        saveIfNeeded()
    }

    ScreenScaffold(
        modifier = Modifier.escapeTextFieldFocusTrap(),
        backgroundColor = theme.backgroundColor,
        title = stringResource(if (note == null) R.string.new_note else R.string.edit_note),
        textColor = theme.textColor,
        accentColor = theme.accentColor,
        onBack = { saveIfNeeded() },
        trailingIcon = Icons.Rounded.Save,
        trailingContentDescription = stringResource(R.string.save),
        onTrailingClick = { onSave(title, content, isPinned) },
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
                autoFocus = note == null,
                modifier = Modifier.fillMaxWidth(),
            )
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

    if (showMenu) {
        FutureOptionsMenu(
            theme = theme,
            onDismissRequest = { showMenu = false },
            header = title.ifBlank { stringResource(R.string.untitled) },
        ) {
            FutureMenuRow(stringResource(if (isPinned) R.string.unpin else R.string.pin), Icons.Rounded.PushPin, theme, {
                showMenu = false
                isPinned = !isPinned
            })
            if (note != null) {
                FutureMenuRow(stringResource(R.string.delete), FutureIcons.Delete, theme, {
                    showMenu = false
                    showDeleteConfirm = true
                }, destructive = true)
            }
        }
    }

    if (showDeleteConfirm) {
        ConfirmDialog(
            message = stringResource(R.string.delete_note_confirm_message),
            theme = theme,
            onCancel = { showDeleteConfirm = false },
            onConfirm = {
                showDeleteConfirm = false
                onDelete()
            },
            confirmLabel = stringResource(R.string.delete_confirm),
            cancelLabel = stringResource(R.string.cancel),
        )
    }
}
