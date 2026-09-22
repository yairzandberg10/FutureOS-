package com.future.futureui.controlcenter.ui.components
import com.future.sharednav.theme.LocalFutureTheme
import com.future.sharednav.theme.elevatedSurfaceColor
import com.future.sharednav.theme.raisedSurfaceColor
import com.future.sharednav.theme.readableAccentColor
import com.future.sharednav.theme.onReadableAccentColor
import com.future.sharednav.theme.FutureDimens
import com.future.sharednav.theme.textAlpha

import com.future.sharednav.theme.FutureShapes
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
    val theme = LocalFutureTheme.current
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val shape = FutureShapes.xxl

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(47.dp)
            .focusEffect(isFocused, shape)
            .clip(shape)
            .background(theme.elevatedSurfaceColor)
            .then(
                if (isFocused) Modifier.border(FutureDimens.focusBorderControl, theme.readableAccentColor, shape) else Modifier
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
                // המילוי הוא הערך - בהדגשה, כמו במחוון של הדיזיין סיסטם (Slider.jsx).
                .background(theme.readableAccentColor)
        )

        Icon(
            imageVector = icon,
            contentDescription = null,
            // האייקון יושב בתחילת המילוי; ברגע שהמילוי מכסה אותו הוא עובר לדיו של ההדגשה.
            tint = if (value >= 0.12f) theme.onReadableAccentColor else theme.textColor,
            modifier = Modifier
                .padding(start = 14.dp)
                .size(22.dp)
        )
    }
}
