package com.future.futureui.controlcenter.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex

@Composable
fun HeaderActionButton(icon: ImageVector, color: Color, onClick: () -> Unit, isPower: Boolean = false) {
    // עטיפה דקה סביב TopBarIconButton המשותף (מודול SharedKeypadNav) - חתימת
    // הקריאה נשארת זהה כדי שקריאות קיימות ב-FutureUI לא ישתנו.
    com.future.sharednav.components.TopBarIconButton(
        icon = icon,
        contentDescription = "",
        textColor = if (isPower) Color.Red else color,
        accentColor = Color.White,
        onClick = onClick,
    )
}

fun Modifier.focusEffect(isFocused: Boolean, shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(16.dp)): Modifier = this
    .zIndex(if (isFocused) 1f else 0f)
    .then(
        if (isFocused) {
            Modifier.border(2.dp, Color.LightGray, shape)
        } else Modifier
    )
