package com.future.futureui.controlcenter.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.*
import androidx.compose.ui.unit.dp

@Composable
fun SliderBar(
    icon: ImageVector,
    value: Float,
    onValueChange: (Float) -> Unit,
    isDarkBackground: Boolean = false
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val shape = RoundedCornerShape(30.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(47.dp)
            .focusEffect(isFocused, shape)
            .clip(shape)
            .background(Color(0x80E0E0E0))
            .then(
                if (isFocused) Modifier.border(2.dp, Color.LightGray, shape) else Modifier
            )
            .onKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown) {
                    when (event.key) {
                        Key.DirectionLeft -> {
                            onValueChange((value + 0.1f).coerceIn(0f, 1f))
                            true
                        }
                        Key.DirectionRight -> {
                            onValueChange((value - 0.1f).coerceIn(0f, 1f))
                            true
                        }
                        else -> false
                    }
                } else false
            }
            .focusable(interactionSource = interactionSource),
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(value.coerceAtLeast(0.01f))
                .background(Color(0x66BDBDBD))
        )

        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isDarkBackground) Color.White else Color(0xFF616161),
            modifier = Modifier
                .padding(start = 14.dp)
                .size(22.dp)
        )
    }
}
