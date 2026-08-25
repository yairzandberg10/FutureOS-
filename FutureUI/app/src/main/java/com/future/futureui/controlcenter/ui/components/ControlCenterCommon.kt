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
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val shape = CircleShape

    Box(
        modifier = Modifier
            .size(36.dp)
            .graphicsLayer {
                scaleX = if (isFocused) 1.25f else 1f
                scaleY = if (isFocused) 1.25f else 1f
            }
            .then(if (isFocused) Modifier.border(2.dp, Color.LightGray, shape) else Modifier)
            .clip(shape)
            .background(if (isFocused) Color.White.copy(alpha = 0.2f) else Color.Transparent)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .focusable(interactionSource = interactionSource),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isPower) Color.Red else color,
            modifier = Modifier.size(22.dp)
        )
    }
}

fun Modifier.focusEffect(isFocused: Boolean, shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(16.dp)): Modifier = this
    .zIndex(if (isFocused) 1f else 0f)
    .then(
        if (isFocused) {
            Modifier.border(2.dp, Color.LightGray, shape)
        } else Modifier
    )
