package com.future.fitness.ui.components

import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import com.future.fitness.ui.theme.FutureTheme

/** צבעי שדה טקסט אחידים לכל מסכי האפליקציה (Settings, WorkoutBuilder) - היה
 * מוגדר בנפרד וזהה בשני המסכים. */
@Composable
fun fitnessTextFieldColors(theme: FutureTheme): TextFieldColors = OutlinedTextFieldDefaults.colors(
    focusedTextColor = theme.textColor,
    unfocusedTextColor = theme.textColor,
    focusedBorderColor = theme.accentColor,
    unfocusedBorderColor = theme.textColor.copy(alpha = 0.25f),
    focusedLabelColor = theme.accentColor,
    unfocusedLabelColor = theme.textColor.copy(alpha = 0.6f),
    cursorColor = theme.accentColor,
)
