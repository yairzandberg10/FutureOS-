package com.future.sharednav.focus

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import com.future.sharednav.theme.FutureDimens
import com.future.sharednav.theme.FutureMotion
import com.future.sharednav.theme.FutureShapes

/**
 * בורדר + רקע + scale בפוקוס - כתוסף Modifier (בניגוד ל-FocusableItem
 * שבאותו מודול, שעוטף content ב-Box משלו). מיועד לצרכן שמשתמש ב-Material3
 * ColorScheme ומצרף פוקוס לרכיבי Material3 קיימים (OutlinedTextField, Card,
 * FloatingActionButton, IconButton) דרך onFocusChanged + Modifier.then,
 * במקום לעטוף אותם מחדש.
 *
 * אותה תנועה בדיוק כמו FocusableItem: אותם צבעים, אותו עובי מסגרת, אותו
 * קפיץ. קודם שני הרכיבים האלה סימנו פוקוס אחרת (1.5dp מול 2dp, מסגרת
 * שקופצת מול רקע מונפש).
 */
@Composable
fun Modifier.dpadFocusBorder(
    isFocused: Boolean,
    shape: Shape = FutureShapes.sm,
): Modifier {
    val accent = MaterialTheme.colorScheme.primary
    val scale = animateFloatAsState(
        if (isFocused) 1.02f else 1f,
        FutureMotion.focusScaleSpec,
        label = "dpadFocusScale",
    )
    val bgColor by animateColorAsState(
        if (isFocused) accent.copy(alpha = 0.14f) else Color.Transparent,
        FutureMotion.focusColorSpec,
        label = "dpadFocusBg",
    )
    val ringColor by animateColorAsState(
        if (isFocused) accent else accent.copy(alpha = 0f),
        FutureMotion.focusColorSpec,
        label = "dpadFocusRing",
    )
    return this
        .bringIntoViewOnFocus()
        .graphicsLayer {
            scaleX = scale.value
            scaleY = scale.value
        }
        .background(bgColor, shape)
        .border(width = FutureDimens.focusBorderWidth, color = ringColor, shape = shape)
}
