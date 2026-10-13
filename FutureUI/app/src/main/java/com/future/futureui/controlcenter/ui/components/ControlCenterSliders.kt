package com.future.futureui.controlcenter.ui.components

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
import com.future.futureui.ui.LocalOneUiPalette
import com.future.futureui.ui.oneUiFocus
import com.future.sharednav.theme.FutureContrast

@Composable
fun SliderBar(
    icon: ImageVector,
    value: Float,
    onValueChange: (Float) -> Unit,
    isDarkBackground: Boolean = false
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val shape = FutureShapes.xxl
    val palette = LocalOneUiPalette.current
    // המילוי הוא צבע הטקסט של הפלטה; האייקון והאחוז הופכים לדיו הפוך מעליו.
    val fill = palette.text
    val inkOnFill = FutureContrast.onColor(fill)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(47.dp)
            .oneUiFocus(isFocused, shape)
            .clip(shape)
            // ניגודיות: קודם מסילה אפורה-בהירה 50% ומילוי אפור 40% - על הזכוכית
            // של מרכז הבקרה כמעט לא היה אפשר לראות איפה נגמר המילוי. עכשיו
            // מסילה כהה שקופה-למחצה ומילוי לבן כמעט אטום.
            .background(palette.sliderTrack)
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
                .clip(shape)
                .background(fill)
        )

        // האייקון יושב בתחילת המסילה: כהה כשהמילוי הלבן כבר מתחתיו, לבן כשלא.
        val iconOnFill = value > 0.14f
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (iconOnFill) inkOnFill else palette.text,
            modifier = Modifier
                .padding(start = 14.dp)
                .size(22.dp)
        )

        // האחוז בקצה השני - תמיד על המסילה הכהה, חוץ מקרוב ל-100%.
        val percentOnFill = value > 0.86f
        androidx.compose.material3.Text(
            "${(value * 100).toInt()}%",
            color = if (percentOnFill) inkOnFill else palette.text,
            fontSize = com.future.sharednav.theme.FutureTypography.summary,
            fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 16.dp)
        )
    }
}
