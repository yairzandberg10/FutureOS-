package com.future.fitness.ui.components

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.unit.dp
import com.future.fitness.ui.theme.FutureTheme
import com.future.sharednav.focus.FocusableItem as SharedFocusableItem

/** עטיפה דקה סביב הרכיב המשותף (מודול SharedKeypadNav) - אותו מראה בדיוק כמו
 * ב-Music: בלי אנימציית scale, פינות 16dp, בורדר 2dp בפוקוס, בלי ריפוד תוכן. */
@Composable
fun FocusableItem(
    onClick: () -> Unit,
    theme: FutureTheme,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null,
    content: @Composable BoxScope.(isFocused: Boolean) -> Unit,
) {
    SharedFocusableItem(
        onClick = onClick,
        accentColor = theme.accentColor,
        modifier = modifier,
        idleBackgroundColor = theme.textColor.copy(alpha = 0.05f),
        focusedBackgroundColor = theme.textColor.copy(alpha = 0.14f),
        borderColor = theme.accentColor,
        borderWidth = 2.dp,
        cornerRadius = 16.dp,
        scaleOnFocus = false,
        contentPadding = 0.dp,
        focusRequester = focusRequester,
        content = content,
    )
}
