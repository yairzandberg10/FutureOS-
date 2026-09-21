package com.future.sharednav.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowLeft
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
import androidx.compose.ui.unit.dp
import com.future.sharednav.focus.bringIntoViewOnFocus
import com.future.sharednav.theme.FutureDimens
import com.future.sharednav.theme.FutureMotion
import com.future.sharednav.theme.FutureShapes
import com.future.sharednav.theme.FutureTheme
import com.future.sharednav.theme.FutureTypography
import com.future.sharednav.theme.LocalFutureAccent
import com.future.sharednav.theme.chevronColor
import com.future.sharednav.theme.focusFillSettingColor
import com.future.sharednav.theme.mutedTextColor
import com.future.sharednav.theme.readableAccentColor
import com.future.sharednav.theme.rememberFutureType

/**
 * שורת הגדרה בתוך [FutureCard] - אייקון בצבע ההדגשה, כותרת וערך נוכחי
 * מתחתיה, ובסוף חץ כניסה או פקד (מתג, מחוון).
 *
 * שים לב שסימון הפוקוס שלה *אינו* זה של שורת רשימה: רקע ב-6% מצבע הטקסט
 * (ולא בהדגשה), מסגרת 2dp (ולא 1.5dp), ובלי הגדלה. הפירוט מפריד ביניהן
 * במפורש (guidelines/focus-spec.html).
 *
 * חץ הכניסה מצביע **שמאלה** ואינו מתהפך: הממשק כולו RTL, ושמאלה זה קדימה.
 */
@Composable
fun FutureSettingItem(
    title: String,
    theme: FutureTheme,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    summary: String? = null,
    icon: ImageVector? = null,
    showChevron: Boolean = true,
    focusRequester: FocusRequester? = null,
    trailing: (@Composable RowScope.() -> Unit)? = null,
) {
    val type = rememberFutureType()
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val accent = LocalFutureAccent.current ?: theme.readableAccentColor

    val background by animateColorAsState(
        if (isFocused) theme.focusFillSettingColor else Color.Transparent,
        FutureMotion.focusColorSpec,
        label = "settingItemBg",
    )
    val border by animateColorAsState(
        if (isFocused) accent else Color.Transparent,
        FutureMotion.focusColorSpec,
        label = "settingItemBorder",
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(FutureDimens.spacingXs)
            .height(FutureDimens.rowHeightSetting)
            .clip(FutureShapes.lg)
            .background(background)
            .border(FutureDimens.focusBorderControl, border, FutureShapes.lg)
            .padding(horizontal = FutureDimens.spacingMd)
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .bringIntoViewOnFocus()
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(FutureDimens.spacingLg),
    ) {
        if (icon != null) {
            Icon(
                icon,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(FutureDimens.iconSettingRow),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                color = theme.textColor,
                fontSize = type.title,
                fontWeight = FutureTypography.weightSemibold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (summary != null) {
                Text(
                    summary,
                    color = theme.mutedTextColor,
                    fontSize = type.summary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
        when {
            trailing != null -> trailing()
            showChevron -> Icon(
                Icons.Rounded.KeyboardArrowLeft,
                contentDescription = null,
                tint = theme.chevronColor,
                modifier = Modifier.size(FutureDimens.iconTopBar),
            )
        }
    }
}
