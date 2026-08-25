package com.future.notes.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.future.notes.R
import com.future.notes.data.Note
import com.future.notes.ui.components.dpadFocusBorder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    note: Note?,
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (note == null) stringResource(R.string.new_note) else stringResource(R.string.edit_note), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    var isFocused by remember { mutableStateOf(false) }
                    IconButton(
                        onClick = { saveIfNeeded() },
                        modifier = Modifier
                            .onFocusChanged { isFocused = it.isFocused }
                            .dpadFocusBorder(isFocused, RoundedCornerShape(50))
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    var pinFocused by remember { mutableStateOf(false) }
                    IconButton(
                        onClick = { isPinned = !isPinned },
                        modifier = Modifier
                            .onFocusChanged { pinFocused = it.isFocused }
                            .dpadFocusBorder(pinFocused, RoundedCornerShape(50))
                    ) {
                        Icon(
                            Icons.Default.PushPin,
                            contentDescription = stringResource(R.string.pin),
                            tint = if (isPinned) MaterialTheme.colorScheme.primary else LocalContentColor.current
                        )
                    }
                    if (note != null) {
                        var deleteFocused by remember { mutableStateOf(false) }
                        IconButton(
                            onClick = { showDeleteConfirm = true },
                            modifier = Modifier
                                .onFocusChanged { deleteFocused = it.isFocused }
                                .dpadFocusBorder(deleteFocused, RoundedCornerShape(50))
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete))
                        }
                    }
                    var saveFocused by remember { mutableStateOf(false) }
                    IconButton(
                        onClick = { onSave(title, content, isPinned) },
                        modifier = Modifier
                            .onFocusChanged { saveFocused = it.isFocused }
                            .dpadFocusBorder(saveFocused, RoundedCornerShape(50))
                    ) {
                        Icon(Icons.Default.Save, contentDescription = stringResource(R.string.save))
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            var titleFocused by remember { mutableStateOf(false) }
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text(stringResource(R.string.title)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { titleFocused = it.isFocused }
                    .dpadFocusBorder(titleFocused, RoundedCornerShape(12.dp)),
                shape = RoundedCornerShape(12.dp),
                textStyle = MaterialTheme.typography.titleLarge,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent
                )
            )

            var contentFocused by remember { mutableStateOf(false) }
            OutlinedTextField(
                value = content,
                onValueChange = { content = it },
                label = { Text(stringResource(R.string.content)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .onFocusChanged { contentFocused = it.isFocused }
                    .dpadFocusBorder(contentFocused, RoundedCornerShape(12.dp)),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent
                )
            )
        }
    }

    if (showDeleteConfirm) {
        val cancelFocusRequester = remember { FocusRequester() }
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(stringResource(R.string.delete_note_confirm_title)) },
            text = { Text(stringResource(R.string.delete_note_confirm_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    onDelete()
                }) {
                    Text(stringResource(R.string.delete_confirm))
                }
            },
            dismissButton = {
                // ברירת מחדל בטוחה: הפוקוס הראשוני על "ביטול" ולא על "מחק", כדי
                // שלחיצה מהירה/בטעות על מרכז ה-D-pad לא תמחק את הפתק בטעות.
                TextButton(
                    onClick = { showDeleteConfirm = false },
                    modifier = Modifier.focusRequester(cancelFocusRequester)
                ) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
        LaunchedEffect(Unit) {
            cancelFocusRequester.requestFocus()
        }
    }
}
