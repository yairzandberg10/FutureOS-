package com.future.sharednav.focus

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/**
 * בורדר + רקע + scale בפוקוס - כתוסף Modifier (בניגוד ל-FocusableItem
 * שבאותו מודול, שעוטף content ב-Box משלו). מיועד לצרכן כמו notes שמשתמש
 * ב-Material3 ColorScheme אמיתי (לא ב-FutureTheme הידני) ומצרף פוקוס
 * לרכיבי Material3 קיימים (OutlinedTextField, Card, FloatingActionButton,
 * IconButton) דרך onFocusChanged + Modifier.then, במקום לעטוף אותם מחדש.
 *
 * זהה בהתנהגות לגרסה שהייתה כפולה בקוד המקומי של notes (ui/components/
 * FocusUtils.kt, שמונעת עכשיו) - הועבר לכאן כמקור אמת יחיד.
 */
@Composable
fun Modifier.dpadFocusBorder(
    isFocused: Boolean,
    shape: Shape = RoundedCornerShape(8.dp),
): Modifier {
    val scale by animateFloatAsState(if (isFocused) 1.02f else 1f, label = "dpadFocusScale")
    val bgColor by animateColorAsState(
        if (isFocused) MaterialTheme.colorScheme.primary.copy(alpha = 0.14f) else Color.Transparent,
        label = "dpadFocusBg",
    )
    return this
        .bringIntoViewOnFocus()
        .scale(scale)
        .background(bgColor, shape)
        .then(
            if (isFocused) Modifier.border(width = 2.dp, color = MaterialTheme.colorScheme.primary, shape = shape)
            else Modifier
        )
}
