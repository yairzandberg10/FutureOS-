package com.future.fitness.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import com.future.sharednav.components.FutureTextField
import com.future.sharednav.theme.FutureDimens
import com.future.sharednav.theme.FutureTheme
import com.future.sharednav.theme.mutedTextColor
import com.future.sharednav.theme.rememberFutureType

/**
 * שדה בטופס (פרופיל, אימון חדש): שדה הקלט של הדיזיין סיסטם, ומעליו שם
 * השדה בשורת תווית. קודם זה היה OutlinedTextField של Material - מסגרת
 * מתאר ותווית צפה, שני דברים שאין בעיצוב.
 */
@Composable
fun FitnessTextField(
    theme: FutureTheme,
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    singleLine: Boolean = true,
    focusRequester: FocusRequester? = null,
) {
    val type = rememberFutureType()
    Column(modifier = modifier) {
        Text(label, color = theme.mutedTextColor, fontSize = type.label, modifier = Modifier.padding(bottom = FutureDimens.spacingXs))
        FutureTextField(
            value = value,
            onValueChange = onValueChange,
            theme = theme,
            singleLine = singleLine,
            keyboardOptions = keyboardOptions,
            focusRequester = focusRequester,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
