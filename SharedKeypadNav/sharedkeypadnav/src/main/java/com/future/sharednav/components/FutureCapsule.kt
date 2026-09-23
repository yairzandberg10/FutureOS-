package com.future.sharednav.components

import com.future.sharednav.focus.animatedFocusSurface
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.future.sharednav.focus.bringIntoViewOnFocus
import com.future.sharednav.theme.FutureDimens
import com.future.sharednav.theme.FutureMotion
import com.future.sharednav.theme.FutureShapes
import com.future.sharednav.theme.FutureTheme
import com.future.sharednav.theme.FutureTypography
import com.future.sharednav.theme.LocalFutureAccent
import com.future.sharednav.theme.mutedTextColor
import com.future.sharednav.theme.readableAccentColor
import com.future.sharednav.theme.rememberFutureType
import com.future.sharednav.theme.sectionHeaderColor
import com.future.sharednav.theme.textAlpha

/**
 * פקד בצורת גלולה שנושא תווית קטנה מעל הערך שלו (components/forms/Capsule.jsx)
 * - בוחרי השפות בתרגום. מילוי ב-8% מהטקסט (20% מההדגשה כשהוא [active]),
 * ופוקוס של מסגרת 2dp בהדגשה - לא הטבעת הלבנה של הכפתור, ובלי הגדלה.
 *
 * התווית ב-12sp ב-55% עם ריווח של כותרת קטע, והערך ב-17sp בינוני.
 */
@Composable
fun FutureCapsule(
    value: String,
    theme: FutureTheme,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    icon: ImageVector? = null,
    active: Boolean = false,
    focusRequester: FocusRequester? = null,
) {
    val type = rememberFutureType()
    val accent = LocalFutureAccent.current ?: theme.readableAccentColor
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val ring = animateColorAsState(
        if (isFocused) accent else Color.Transparent,
        FutureMotion.focusColorSpec,
        label = "capsuleRing",
    )
    val shape = FutureShapes.pill

    Column(
        modifier = modifier
            .clip(shape)
            .animatedFocusSurface(
                shape,
                FutureDimens.focusBorderControl,
                fill = { if (active) accent.copy(alpha = 0.20f) else theme.textAlpha(8) },
                ring = { ring.value },
            )
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .bringIntoViewOnFocus()
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(horizontal = CapsulePaddingH, vertical = FutureDimens.spacingSm),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(1.dp, Alignment.CenterVertically),
    ) {
        if (label != null) {
            Text(
                label,
                color = theme.sectionHeaderColor,
                fontSize = type.label,
                letterSpacing = FutureTypography.trackingSection,
                maxLines = 1,
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (icon != null) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = if (active) accent else theme.mutedTextColor,
                    modifier = Modifier.size(FutureDimens.iconTopBar),
                )
            }
            Text(
                value,
                color = if (active) accent else theme.textColor,
                fontSize = type.title,
                fontWeight = FutureTypography.weightMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/**
 * הגרסה העגולה של [FutureCapsule] (Capsule.jsx, `round`) - כפתור המיקרופון
 * הגדול במצב שיחה. אייקון ב-48% מהקוטר, ב-60% מהטקסט במנוחה ובהדגשה כשהוא
 * פעיל (מקשיב).
 */
@Composable
fun FutureRoundCapsule(
    icon: ImageVector,
    theme: FutureTheme,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    active: Boolean = false,
    size: Dp = RoundCapsuleSize,
    contentDescription: String? = null,
    focusRequester: FocusRequester? = null,
) {
    val accent = LocalFutureAccent.current ?: theme.readableAccentColor
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val ring = animateColorAsState(
        if (isFocused) accent else Color.Transparent,
        FutureMotion.focusColorSpec,
        label = "roundCapsuleRing",
    )
    val fill = animateColorAsState(
        if (active) accent.copy(alpha = 0.20f) else theme.textAlpha(8),
        FutureMotion.focusColorSpec,
        label = "roundCapsuleFill",
    )
    val shape = FutureShapes.pill
    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .animatedFocusSurface(shape, FutureDimens.focusBorderControl, fill = { fill.value }, ring = { ring.value })
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .bringIntoViewOnFocus()
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon,
            contentDescription = contentDescription,
            tint = if (active) accent else theme.mutedTextColor,
            modifier = Modifier.size(size * 0.48f),
        )
    }
}

/** 14dp - הריפוד האופקי של הגלולה (--fos-space-6). */
private val CapsulePaddingH = 14.dp

/** 84dp - קוטר ברירת המחדל של הגרסה העגולה (168px ב-Capsule.jsx). */
val RoundCapsuleSize: Dp = 84.dp
