package com.future.camera.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** כפתור עגול צף מעל תצוגת המצלמה - אותה שפת עיצוב כמו ToolsIconButton
 * (טבעת מיקוד בצבע ההדגשה, רקע כהה למראה שקוף מעל התצוגה החיה). */
@Composable
fun CameraIconButton(
    icon: ImageVector,
    contentDescription: String,
    tint: Color = Color.White,
    size: Dp = 52.dp,
    iconSize: Dp = 24.dp,
    accentColor: Color,
    focusRequester: FocusRequester? = null,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val bgColor by animateColorAsState(
        if (isFocused) accentColor.copy(alpha = 0.85f) else Color.Black.copy(alpha = 0.4f),
        label = "cameraIconBtnBg"
    )
    Box(
        modifier = Modifier
            .size(size)
            .background(bgColor, CircleShape)
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .focusable(interactionSource = interactionSource),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = contentDescription, tint = if (isFocused) Color.Black else tint, modifier = Modifier.size(iconSize))
    }
}
