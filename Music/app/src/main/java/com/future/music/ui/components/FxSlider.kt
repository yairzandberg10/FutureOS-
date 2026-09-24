package com.future.music.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import com.future.sharednav.focus.bringIntoViewOnFocus
import com.future.sharednav.theme.FutureDimens
import com.future.sharednav.theme.FutureMotion
import com.future.sharednav.theme.FutureShapes
import com.future.sharednav.theme.FutureTheme
import com.future.sharednav.theme.FutureTypography
import com.future.sharednav.theme.readableAccentColor

/**
 * מחוון ערך בשורה משלו - components/forms/Slider של הדיזיין סיסטם: כרטיס
 * 16dp, תווית 60%, מסלול 12dp ב-15% והמילוי בהדגשה גדל מימין (RTL - שמאלה
 * זה "קדימה"). שמאל/ימין זזים ב-[step]. בפוקוס: רקע 18% ומסגרת 2dp.
 * [valueText] - הערך בצד השני של התווית (dB, אחוזים).
 */
@Composable
fun FxSlider(
    label: String,
    value: Float,
    theme: FutureTheme,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueText: String? = null,
    step: Float = 0.05f,
    focusRequester: FocusRequester? = null,
) {
    var isFocused by remember { mutableStateOf(false) }
    val accent = theme.readableAccentColor
    val shape = FutureShapes.lg
    val bg by animateColorAsState(
        if (isFocused) theme.textColor.copy(alpha = 0.18f) else theme.textColor.copy(alpha = 0.06f),
        FutureMotion.focusColorSpec,
        label = "fxSliderBg",
    )
    val fill by animateFloatAsState(value.coerceIn(0f, 1f), FutureMotion.fast(), label = "fxSliderFill")

    Column(
        modifier = modifier
            .fillMaxWidth()
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .onFocusChanged { isFocused = it.isFocused }
            .onKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                when (event.key) {
                    Key.DirectionLeft -> { onValueChange((value + step).coerceIn(0f, 1f)); true }
                    Key.DirectionRight -> { onValueChange((value - step).coerceIn(0f, 1f)); true }
                    else -> false
                }
            }
            .focusable()
            .bringIntoViewOnFocus()
            .clip(shape)
            .background(bg)
            .then(if (isFocused) Modifier.border(FutureDimens.focusBorderControl, accent, shape) else Modifier)
            .padding(16.dp),
    ) {
        Row {
            Text(label, fontSize = FutureTypography.body, color = theme.textColor.copy(alpha = 0.6f), modifier = Modifier.weight(1f))
            if (valueText != null) {
                Text(valueText, fontSize = FutureTypography.body, color = theme.textColor.copy(alpha = 0.6f))
            }
        }
        Spacer(Modifier.height(10.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(theme.textColor.copy(alpha = 0.15f)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fill)
                    .clip(RoundedCornerShape(6.dp))
                    .background(accent),
            )
        }
    }
}
