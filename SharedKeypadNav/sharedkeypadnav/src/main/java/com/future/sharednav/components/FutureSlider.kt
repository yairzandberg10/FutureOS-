package com.future.sharednav.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.future.sharednav.theme.LocalFutureAccent
import com.future.sharednav.theme.readableAccentColor
import com.future.sharednav.theme.rememberFutureType
import com.future.sharednav.theme.textAlpha

/**
 * הסליידר של הדיזיין סיסטם (components/forms/Slider.jsx, VolumeSlider): שורה
 * ברוחב מלא בפינות כרטיס (16dp), ריפוד 16dp, תווית 14sp ב-60% (ואייקון אם
 * יש), מסילה 6dp ב-15% מהטקסט, ומילוי בהדגשה שגדל *מימין* - הממשק RTL.
 * השורה היא יעד הפוקוס ולא המסילה: 6% במנוחה, 18% וטבעת 2dp בהדגשה בפוקוס.
 * שמאלה מעלה וימינה מוריד (שמאלה = קדימה), בצעדים של 5%. אין ידית.
 *
 * עד עכשיו הרכיב הזה היה רק בעיצוב: מרכז הבקרה בנה סליידר משלו (גלולה בגובה
 * 47dp, מילוי לבן, מסגרת אפורה, אחוז בקצה) שלא דמה לשום פקד אחר במערכת.
 */
@Composable
fun FutureSlider(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    theme: FutureTheme,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    step: Float = 0.05f,
    focusRequester: FocusRequester? = null,
) {
    val type = rememberFutureType()
    val accent = LocalFutureAccent.current ?: theme.readableAccentColor
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val fill by animateColorAsState(if (isFocused) theme.textAlpha(18) else theme.textAlpha(6), FutureMotion.focusColorSpec, label = "sliderRow")
    val ring by animateColorAsState(if (isFocused) accent else Color.Transparent, FutureMotion.focusColorSpec, label = "sliderRing")
    val shown by animateFloatAsState(value.coerceIn(0f, 1f), FutureMotion.fast(), label = "sliderValue")
    val shape = FutureShapes.lg

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(fill)
            .border(FutureDimens.focusBorderControl, ring, shape)
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .onKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                when (event.key) {
                    Key.DirectionLeft -> { onValueChange((value + step).coerceIn(0f, 1f)); true }
                    Key.DirectionRight -> { onValueChange((value - step).coerceIn(0f, 1f)); true }
                    else -> false
                }
            }
            .focusable(interactionSource = interactionSource)
            .bringIntoViewOnFocus()
            .padding(FutureDimens.spacingLg),
        verticalArrangement = Arrangement.spacedBy(FutureDimens.spacingMd),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(FutureDimens.spacingSm)) {
            if (icon != null) Icon(icon, contentDescription = null, tint = theme.textAlpha(60), modifier = Modifier.size(FutureDimens.iconMenuRow))
            Text(label, color = theme.textAlpha(60), fontSize = type.body, maxLines = 1)
        }
        // המסילה: Box ב-RTL מתחיל מימין, כך שהמילוי "גדל מימין" בלי חשבון נוסף.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(TrackHeight)
                .clip(FutureShapes.pill)
                .background(theme.textAlpha(15)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(shown)
                    .clip(FutureShapes.pill)
                    .background(accent),
            )
        }
    }
}

/** 6dp - עובי המסילה (12px ב-Slider.jsx). */
private val TrackHeight = 6.dp
