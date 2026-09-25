package com.android.sistemui.controlcenter.ui.components

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
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val shape = FutureShapes.xxl

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(47.dp)
            .focusEffect(isFocused, shape)
            .clip(shape)
            // ניגודיות: קודם מסילה אפורה-בהירה 50% ומילוי אפור 40% - על הזכוכית
            // של מרכז הבקרה כמעט לא היה אפשר לראות איפה נגמר המילוי. עכשיו
            // מסילה כהה שקופה-למחצה ומילוי לבן כמעט אטום.
            .background(SliderTrack)
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
                .clip(shape)
                .background(SliderFill)
        )

        // האייקון יושב בתחילת המסילה: כהה כשהמילוי הלבן כבר מתחתיו, לבן כשלא.
        val iconOnFill = value > 0.14f
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (iconOnFill) SliderInkOnFill else Color.White,
            modifier = Modifier
                .padding(start = 14.dp)
                .size(22.dp)
        )

        // האחוז בקצה השני - תמיד על המסילה הכהה, חוץ מקרוב ל-100%.
        val percentOnFill = value > 0.86f
        androidx.compose.material3.Text(
            "${(value * 100).toInt()}%",
            color = if (percentOnFill) SliderInkOnFill else Color.White,
            fontSize = com.future.sharednav.theme.FutureTypography.summary,
            fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 16.dp)
        )
    }
}

private val SliderTrack = Color(0x66000000)
private val SliderFill = Color(0xF2FFFFFF)
private val SliderInkOnFill = Color(0xFF1C1C1E)
