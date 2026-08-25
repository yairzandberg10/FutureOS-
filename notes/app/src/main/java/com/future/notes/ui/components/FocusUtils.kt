package com.future.notes.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

@Composable
fun Modifier.dpadFocusBorder(
    isFocused: Boolean,
    shape: Shape = RoundedCornerShape(8.dp)
): Modifier {
    return if (isFocused) {
        this.border(width = 2.dp, color = MaterialTheme.colorScheme.primary, shape = shape)
    } else {
        this
    }
}
