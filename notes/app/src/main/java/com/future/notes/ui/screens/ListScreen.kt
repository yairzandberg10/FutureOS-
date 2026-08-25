package com.future.notes.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.future.notes.R
import com.future.notes.data.Note
import com.future.notes.ui.components.dpadFocusBorder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListScreen(
    notes: List<Note>,
    searchQuery: String,
    onSearchChanged: (String) -> Unit,
    onNoteClick: (Note) -> Unit,
    onAddNote: () -> Unit,
    onTogglePin: (Note) -> Unit
) {
    Scaffold(
        topBar = {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    stringResource(R.string.my_notes),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                var isFocused by remember { mutableStateOf(false) }
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchChanged,
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { isFocused = it.isFocused }
                        .dpadFocusBorder(isFocused, RoundedCornerShape(12.dp)),
                    placeholder = { Text(stringResource(R.string.search_notes)) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )
            }
        },
        floatingActionButton = {
            var isFocused by remember { mutableStateOf(false) }
            FloatingActionButton(
                onClick = onAddNote,
                modifier = Modifier
                    .onFocusChanged { isFocused = it.isFocused }
                    .dpadFocusBorder(isFocused, FloatingActionButtonDefaults.shape),
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_note))
            }
        }
    ) { padding ->
        if (notes.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.no_notes_found), style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(notes, key = { it.id }) { note ->
                    NoteItem(
                        note = note,
                        onClick = { onNoteClick(note) },
                        onTogglePin = { onTogglePin(note) }
                    )
                }
            }
        }
    }
}

@Composable
fun NoteItem(note: Note, onClick: () -> Unit, onTogglePin: () -> Unit = {}) {
    var isFocused by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged { isFocused = it.isFocused }
            .dpadFocusBorder(isFocused, RoundedCornerShape(16.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (note.isPinned)
                MaterialTheme.colorScheme.secondaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    note.title.ifEmpty { stringResource(R.string.untitled) },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    note.content.ifEmpty { stringResource(R.string.no_content) },
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            var pinFocused by remember { mutableStateOf(false) }
            IconButton(
                onClick = onTogglePin,
                modifier = Modifier
                    .onFocusChanged { pinFocused = it.isFocused }
                    .dpadFocusBorder(pinFocused, RoundedCornerShape(50))
            ) {
                Icon(
                    Icons.Default.PushPin,
                    contentDescription = stringResource(R.string.pin),
                    tint = if (note.isPinned) MaterialTheme.colorScheme.primary else LocalContentColor.current
                )
            }
        }
    }
}
