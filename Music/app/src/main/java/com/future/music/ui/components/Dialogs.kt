package com.future.music.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.future.music.ui.theme.FutureTheme

@Composable
fun NameInputDialog(
    title: String,
    theme: FutureTheme,
    initialValue: String = "",
    confirmLabel: String = "אישור",
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var text by remember { mutableStateOf(initialValue) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = theme.surfaceColor,
        title = { Text(title, color = theme.textColor) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = theme.textColor,
                    unfocusedTextColor = theme.textColor,
                    focusedBorderColor = theme.accentColor,
                    cursorColor = theme.accentColor,
                ),
            )
        },
        confirmButton = {
            TextButton(onClick = { if (text.isNotBlank()) onConfirm(text.trim()) }) {
                Text(confirmLabel, color = theme.accentColor)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("ביטול", color = theme.textColor.copy(alpha = 0.6f)) } },
    )
}

@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    theme: FutureTheme,
    confirmLabel: String = "מחק",
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = theme.surfaceColor,
        title = { Text(title, color = theme.textColor) },
        text = { Text(message, color = theme.textColor.copy(alpha = 0.7f)) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(confirmLabel, color = androidx.compose.ui.graphics.Color(0xFFFF6B6B)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("ביטול", color = theme.textColor.copy(alpha = 0.6f)) } },
    )
}
