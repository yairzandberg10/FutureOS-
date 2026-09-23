package com.future.sharednav.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import com.future.sharednav.focus.animatedFocusSurface
import com.future.sharednav.focus.bringIntoViewOnFocus
import com.future.sharednav.focus.focusMotion
import com.future.sharednav.theme.FutureDimens
import com.future.sharednav.theme.FutureElevation
import com.future.sharednav.theme.FutureMotion
import com.future.sharednav.theme.FutureShapes
import com.future.sharednav.theme.FutureTheme
import com.future.sharednav.theme.LocalFutureAccent
import com.future.sharednav.theme.readableAccentColor

/**
 * כרטיס עצמאי שהוא בעצמו יעד פוקוס - מכשיר בלוטות', פריט שהוא "דבר" ולא
 * שורה ברשימה. בנוי מאותם טוקנים של [FutureCard] (משטח, צל הכרטיס, רדיוס
 * 22dp) ושל פקד (מסגרת 2dp בהדגשה, [FutureDimens.focusBorderControl]).
 *
 * ההבדל מול שורה בתוך כרטיס: הפוקוס של שורה ממלא את רוחב הכרטיס שמסביבה,
 * וכאן הכרטיס כולו הוא מה שמסומן - הטבעת מקיפה אותו, הוא מתרומם מעט
 * ([FutureDimens.focusScale]) ומקבל גוון קל של ההדגשה, והמשטח עצמו נשאר.
 * כך רשימה של כרטיסים נקראת כקבוצת עצמים נפרדים, לא כטבלה.
 */
@Composable
fun FutureFocusCard(
    theme: FutureTheme,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null,
    minHeight: Dp = FutureDimens.rowHeightList,
    contentPadding: Dp = FutureDimens.spacingMd,
    content: @Composable RowScope.(isFocused: Boolean) -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val accent = LocalFutureAccent.current ?: theme.readableAccentColor
    val shape = FutureShapes.xl

    val fill = animateColorAsState(
        if (isFocused) accent.copy(alpha = 0.10f) else Color.Transparent,
        FutureMotion.focusColorSpec,
        label = "focusCardFill",
    )
    val ring = animateColorAsState(
        if (isFocused) accent else accent.copy(alpha = 0f),
        FutureMotion.focusColorSpec,
        label = "focusCardRing",
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .bringIntoViewOnFocus()
            .focusMotion(interactionSource, focusedScale = FutureDimens.focusScale)
            .shadow(FutureElevation.card(theme.isDarkMode), shape)
            .clip(shape)
            .background(theme.surfaceColor)
            .animatedFocusSurface(shape, FutureDimens.focusBorderControl, fill = { fill.value }, ring = { ring.value })
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .heightIn(min = minHeight)
            .padding(horizontal = FutureDimens.spacingLg, vertical = contentPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(FutureDimens.spacingMd),
    ) {
        content(isFocused)
    }
}
