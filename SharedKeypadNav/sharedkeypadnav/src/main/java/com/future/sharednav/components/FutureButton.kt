package com.future.sharednav.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.future.sharednav.focus.bringIntoViewOnFocus
import com.future.sharednav.theme.FutureDimens
import com.future.sharednav.theme.FutureMotion
import com.future.sharednav.theme.FutureShapes
import com.future.sharednav.theme.FutureTheme
import com.future.sharednav.theme.FutureTypography
import com.future.sharednav.theme.LocalFutureAccent
import com.future.sharednav.theme.onReadableAccentColor
import com.future.sharednav.theme.onStatusColor
import com.future.sharednav.theme.readableAccentColor
import com.future.sharednav.theme.textAlpha
import com.future.sharednav.theme.rememberFutureType

/**
 * ארבעת סוגי הכפתור של המערכת. לא היה כאן רכיב כפתור משותף - כל מסך
 * בנה אחד בעצמו, ולכן אותו תפקיד יצא בארבעה עיצובים שונים. הגיאומטריה
 * כאן היא של כפתור הדיאלוג, שהוא זה שהופיע הכי הרבה פעמים: רדיוס 20dp,
 * ריפוד 24/12dp, 16sp מודגש, ומסגרת פוקוס 2dp בצבע הטקסט.
 *
 * במנוחה הכפתור ב-70% אטימות ובפוקוס הוא מלא - כלומר הפוקוס נמסר גם
 * בעוצמת הצבע וגם במסגרת, ולא במסגרת בלבד.
 */
enum class FutureButtonVariant { Primary, Destructive, Secondary, Quiet }

@Composable
fun FutureButton(
    text: String,
    theme: FutureTheme,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: FutureButtonVariant = FutureButtonVariant.Primary,
    fillMaxWidth: Boolean = false,
    focusRequester: FocusRequester? = null,
) {
    val type = rememberFutureType()
    val accent = LocalFutureAccent.current ?: theme.readableAccentColor
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val quiet = variant == FutureButtonVariant.Quiet

    val fill = when (variant) {
        FutureButtonVariant.Primary -> accent
        FutureButtonVariant.Destructive -> theme.dangerColor
        FutureButtonVariant.Secondary -> theme.textColor
        FutureButtonVariant.Quiet -> theme.textAlpha(10)
    }
    val content = when (variant) {
        FutureButtonVariant.Primary -> theme.onReadableAccentColor
        FutureButtonVariant.Destructive -> theme.onStatusColor(theme.dangerColor)
        FutureButtonVariant.Secondary -> theme.onStatusColor(theme.textColor)
        FutureButtonVariant.Quiet -> theme.textColor
    }
    val baseAlpha = if (variant == FutureButtonVariant.Secondary) 0.7f else 1f

    val opacity by animateFloatAsState(
        if (quiet) 1f else if (isFocused) baseAlpha else baseAlpha * 0.7f,
        FutureMotion.fast(),
        label = "buttonOpacity",
    )
    val ring by animateColorAsState(
        if (isFocused && !quiet) theme.textColor else Color.Transparent,
        FutureMotion.focusColorSpec,
        label = "buttonRing",
    )
    val shape = if (quiet) FutureShapes.pill else FutureShapes.dialog

    Box(
        modifier = modifier
            .then(if (fillMaxWidth) Modifier.fillMaxWidth() else Modifier)
            .defaultMinSize(minHeight = if (quiet) 40.dp else FutureDimens.rowHeightDialogButton)
            .clip(shape)
            .alpha(opacity)
            .background(fill)
            .border(FutureDimens.focusBorderControl, ring, shape)
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .focusable(interactionSource = interactionSource)
            .bringIntoViewOnFocus()
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(
                horizontal = FutureDimens.spacingXl,
                vertical = if (quiet) 0.dp else FutureDimens.spacingMd,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            color = content,
            fontSize = if (quiet) type.body else type.bodyLarge,
            fontWeight = if (quiet) FutureTypography.weightMedium else FutureTypography.weightBold,
        )
    }
}
