package com.future.music.ui.components
import com.future.sharednav.components.InputDialog
import com.future.sharednav.components.FutureDialog
import com.future.sharednav.components.FutureButton
import com.future.sharednav.components.FutureButtonVariant
import com.future.sharednav.theme.FutureTypography
import com.future.sharednav.theme.mutedTextColor
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.text.style.TextAlign

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import com.future.sharednav.focus.escapeTextFieldFocusTrap
import com.future.sharednav.theme.FutureTheme

/** עטיפה סביב InputDialog המשותף - חתימת הקריאה של Music נשמרת. */
@Composable
fun NameInputDialog(
    title: String,
    theme: FutureTheme,
    initialValue: String = "",
    confirmLabel: String = "אישור",
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    InputDialog(
        title = title,
        theme = theme,
        initialValue = initialValue,
        confirmLabel = confirmLabel,
        onDismiss = onDismiss,
        onConfirm = onConfirm,
    )
}

/**
 * אישור מחיקה: המשטח והכפתורים של הדיזיין סיסטם (היה AlertDialog עם
 * TextButton-ים), עם שורת הסבר מתחת לכותרת.
 */
@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    theme: FutureTheme,
    confirmLabel: String = "מחק",
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val confirmFocusRequester = remember { FocusRequester() }
    FutureDialog(
        theme = theme,
        onDismissRequest = onDismiss,
        title = title,
        buttons = {
            FutureButton("ביטול", theme, onDismiss, variant = FutureButtonVariant.Secondary)
            FutureButton(confirmLabel, theme, onConfirm, variant = FutureButtonVariant.Destructive, focusRequester = confirmFocusRequester)
        },
    ) {
        Text(
            message,
            color = theme.mutedTextColor,
            fontSize = FutureTypography.body,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
    LaunchedEffect(Unit) { runCatching { confirmFocusRequester.requestFocus() } }
}
