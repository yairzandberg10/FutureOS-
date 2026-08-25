package com.future.fitness.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import com.future.fitness.ui.theme.FutureTheme
import com.future.sharednav.components.ScreenTopBar as SharedScreenTopBar
import com.future.sharednav.components.TopBarIconButton as SharedTopBarIconButton

/** עטיפה דקה סביב הרכיב המשותף (מודול SharedKeypadNav), כמו בשאר אפליקציות
 * הסוויטה - שומרת על חתימת קריאה עם FutureTheme במקום פרמטרים פרימיטיביים. */
@Composable
fun ScreenTopBar(
    title: String,
    theme: FutureTheme,
    onBack: (() -> Unit)? = null,
    trailingIcon: ImageVector? = null,
    trailingContentDescription: String? = null,
    onTrailingClick: (() -> Unit)? = null,
) {
    SharedScreenTopBar(
        title = title,
        textColor = theme.textColor,
        accentColor = theme.accentColor,
        onBack = onBack,
        trailingIcon = trailingIcon,
        trailingContentDescription = trailingContentDescription,
        onTrailingClick = onTrailingClick,
    )
}

@Composable
fun TopBarIconButton(icon: ImageVector, contentDescription: String, theme: FutureTheme, onClick: () -> Unit) {
    SharedTopBarIconButton(icon, contentDescription, theme.textColor, theme.accentColor, onClick)
}
