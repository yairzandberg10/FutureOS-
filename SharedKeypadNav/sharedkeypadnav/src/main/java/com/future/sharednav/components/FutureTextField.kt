package com.future.sharednav.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.foundation.layout.Arrangement
import com.future.sharednav.focus.bringIntoViewOnFocus
import com.future.sharednav.theme.FutureDimens
import com.future.sharednav.theme.FutureMotion
import com.future.sharednav.theme.FutureShapes
import com.future.sharednav.theme.FutureTheme
import com.future.sharednav.theme.LocalFutureAccent
import com.future.sharednav.theme.idleFieldColor
import com.future.sharednav.theme.readableAccentColor
import com.future.sharednav.theme.rememberFutureType
import com.future.sharednav.theme.subtleTextColor

/**
 * שדה הקלט של המערכת (components/forms/TextField.jsx): רקע של 8% מצבע
 * הטקסט, רדיוס 10dp, ריפוד 12dp, טקסט 15sp, מסגרת פוקוס 2dp בצבע ההדגשה
 * וסמן בצבע ההדגשה. אין תווית מעליו ואין טקסט עזר מתחתיו - הכותרת של
 * הדיאלוג או של השורה עושה את זה. אין גרסה עם קו תחתון: השדה תמיד מלא.
 *
 * מחליף את OutlinedTextField/TextField של Material, שהופיעו ב-26 מקומות עם
 * מסגרת מתאר, תווית צפה ורדיוס 4dp - שלושה דברים שאין בעיצוב בכלל.
 *
 * [autoFocus] מבקש פוקוס ברגע שהשדה נמדד בפועל. בתוך Dialog זה ההבדל בין
 * שדה שאפשר להקליד בו מיד לבין שדה שבקשת הפוקוס אליו נבלעה בשקט, כי
 * החלון של הדיאלוג עוד לא היה מחובר.
 */
@Composable
fun FutureTextField(
    value: String,
    onValueChange: (String) -> Unit,
    theme: FutureTheme,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    singleLine: Boolean = true,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    minLines: Int = 1,
    enabled: Boolean = true,
    autoFocus: Boolean = false,
    focusRequester: FocusRequester? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    leading: (@Composable RowScope.() -> Unit)? = null,
    trailing: (@Composable RowScope.() -> Unit)? = null,
) {
    FieldFrame(theme, modifier, focusRequester, autoFocus) { fieldModifier, interactionSource, style, cursor ->
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = fieldModifier,
            enabled = enabled,
            textStyle = style,
            cursorBrush = cursor,
            singleLine = singleLine,
            maxLines = maxLines,
            minLines = minLines,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            visualTransformation = visualTransformation,
            interactionSource = interactionSource,
            decorationBox = { inner ->
                FieldDecoration(value.isEmpty(), placeholder, singleLine, theme, style, leading, trailing, inner)
            },
        )
    }
}

/** אותו שדה, עם [TextFieldValue] - למי שצריך לשלוט במיקום הסמן או בבחירה. */
@Composable
fun FutureTextField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    theme: FutureTheme,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    singleLine: Boolean = true,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    minLines: Int = 1,
    enabled: Boolean = true,
    autoFocus: Boolean = false,
    focusRequester: FocusRequester? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    leading: (@Composable RowScope.() -> Unit)? = null,
    trailing: (@Composable RowScope.() -> Unit)? = null,
) {
    FieldFrame(theme, modifier, focusRequester, autoFocus) { fieldModifier, interactionSource, style, cursor ->
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = fieldModifier,
            enabled = enabled,
            textStyle = style,
            cursorBrush = cursor,
            singleLine = singleLine,
            maxLines = maxLines,
            minLines = minLines,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            visualTransformation = visualTransformation,
            interactionSource = interactionSource,
            decorationBox = { inner ->
                FieldDecoration(value.text.isEmpty(), placeholder, singleLine, theme, style, leading, trailing, inner)
            },
        )
    }
}

@Composable
private fun FieldFrame(
    theme: FutureTheme,
    modifier: Modifier,
    focusRequester: FocusRequester?,
    autoFocus: Boolean,
    field: @Composable (Modifier, MutableInteractionSource, TextStyle, SolidColor) -> Unit,
) {
    val type = rememberFutureType()
    val accent = LocalFutureAccent.current ?: theme.readableAccentColor
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val ring by animateColorAsState(
        if (isFocused) accent else Color.Transparent,
        FutureMotion.focusColorSpec,
        label = "textFieldRing",
    )
    val ownRequester = remember { FocusRequester() }
    val requester = focusRequester ?: if (autoFocus) ownRequester else null
    var hasAutoFocused by remember { mutableStateOf(false) }

    val fieldModifier = modifier
        .clip(FutureShapes.textField)
        .background(theme.idleFieldColor)
        .border(FutureDimens.focusBorderControl, ring, FutureShapes.textField)
        .then(if (requester != null) Modifier.focusRequester(requester) else Modifier)
        .then(
            if (autoFocus && requester != null) Modifier.onGloballyPositioned {
                if (!hasAutoFocused) {
                    hasAutoFocused = true
                    runCatching { requester.requestFocus() }
                }
            } else Modifier
        )
        .bringIntoViewOnFocus()
        .padding(FutureDimens.spacingMd)

    field(
        fieldModifier,
        interactionSource,
        TextStyle(color = theme.textColor, fontSize = type.dialog),
        SolidColor(accent),
    )
}

@Composable
private fun FieldDecoration(
    isEmpty: Boolean,
    placeholder: String?,
    singleLine: Boolean,
    theme: FutureTheme,
    style: TextStyle,
    leading: (@Composable RowScope.() -> Unit)?,
    trailing: (@Composable RowScope.() -> Unit)?,
    inner: @Composable () -> Unit,
) {
    // שדה רב-שורות (גוף של פתק) מתחיל מלמעלה; שדה בשורה אחת ממורכז.
    Row(
        verticalAlignment = if (singleLine) Alignment.CenterVertically else Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(FutureDimens.spacingSm),
    ) {
        leading?.invoke(this)
        Box(modifier = Modifier.weight(1f)) {
            if (isEmpty && placeholder != null) {
                Text(placeholder, color = theme.subtleTextColor, fontSize = style.fontSize, maxLines = 1)
            }
            inner()
        }
        trailing?.invoke(this)
    }
}
