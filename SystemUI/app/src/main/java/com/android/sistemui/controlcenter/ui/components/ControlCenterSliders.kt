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

/**
 * הסליידרים של מרכז הבקרה (בהירות, עוצמה) - FutureSlider של הדיזיין סיסטם,
 * כמו בהגדרות. על הזכוכית המטושטשת הם בצבעי הערכה הכהה, עם ההדגשה של המשתמש.
 */
@Composable
fun SliderBar(
    icon: ImageVector,
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    isDarkBackground: Boolean = false
) {
    val system = com.future.sharednav.theme.rememberFutureTheme()
    val theme = remember(system.accentColor, isDarkBackground) {
        com.future.sharednav.theme.FutureTheme(isDarkMode = isDarkBackground || system.isDarkMode, accentColor = system.accentColor)
    }
    com.future.sharednav.components.FutureSlider(
        label = label,
        value = value,
        onValueChange = onValueChange,
        theme = theme,
        icon = icon,
    )
}
