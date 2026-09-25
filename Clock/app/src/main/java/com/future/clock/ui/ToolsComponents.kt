package com.future.clock.ui

import com.future.sharednav.icons.FutureIcons

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import com.future.sharednav.components.FutureAvatar
import com.future.sharednav.components.FutureButton
import com.future.sharednav.components.FutureButtonVariant
import com.future.sharednav.components.FutureListItem
import com.future.sharednav.components.ScreenTopBar as SharedScreenTopBar
import com.future.sharednav.components.TopBarIconButton as SharedTopBarIconButton
import com.future.sharednav.focus.bringIntoViewOnFocus
import com.future.sharednav.theme.FutureContrast
import com.future.sharednav.theme.FutureDimens
import com.future.sharednav.theme.FutureMotion
import com.future.sharednav.theme.FutureTheme
import com.future.sharednav.theme.FutureTypography
import com.future.sharednav.theme.rememberFutureType

/**
 * כפתור אייקון - TopBarIconButton המשותף. האייקון בצבע הטקסט
 * (components/core/IconButton.jsx); קודם הוא היה בצבע ההדגשה, בפינות 16dp
 * ולא בעיגול.
 */
@Composable
fun ToolsIconButton(icon: ImageVector, contentDescription: String, theme: FutureTheme, tint: Color = theme.textColor, onClick: () -> Unit) {
    SharedTopBarIconButton(icon, contentDescription, tint, theme.accentColor, onClick)
}

/**
 * השורה העליונה - ScreenTopBar, או אותה שורה בדיוק (16/12dp, כותרת 20sp)
 * כשיש בצד השני תוכן חופשי. קודם הכותרת כאן הייתה 17sp.
 */
@Composable
fun ToolsHeader(title: String, theme: FutureTheme, onBack: (() -> Unit)? = null, trailing: (@Composable () -> Unit)? = null) {
    if (trailing == null) {
        SharedScreenTopBar(title = title, textColor = theme.textColor, accentColor = theme.accentColor, onBack = onBack)
        return
    }
    val type = rememberFutureType()
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = FutureDimens.spacingLg, vertical = FutureDimens.spacingMd),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // אין כפתור חזור על המסך - מקש BACK הפיזי עושה את זה
        if (onBack != null) androidx.activity.compose.BackHandler(onBack = onBack)
        Text(
            title,
            color = theme.textColor,
            fontSize = type.screenTitle,
            fontWeight = FutureTypography.weightBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = true),
        )
        trailing.invoke()
    }
}

/** שורת רשימה של הדיזיין סיסטם עם אווטאר-אייקון (קודם: 5.5% במנוחה, פוקוס 14% מהטקסט, עיגול בהדגשה). */
@Composable
fun ToolRow(
    icon: ImageVector,
    label: String,
    subtitle: String,
    theme: FutureTheme,
    onClick: () -> Unit,
    trailing: (@Composable () -> Unit)? = null,
    focusRequester: FocusRequester? = null
) {
    FutureListItem(
        title = label,
        summary = subtitle.ifEmpty { null },
        theme = theme,
        onClick = onClick,
        focusRequester = focusRequester,
        leading = { FutureAvatar(theme = theme, icon = icon) },
        trailing = trailing?.let { content -> { content() } },
    )
}

/**
 * שורת הפעולות של שעון העצר והטיימר: שני כפתורי מערכת ([FutureButton],
 * components/core/Button.jsx) ברוחב שווה - הראשי מימין, המשני לצידו -
 * עם אותו מרווח כמו כפתורי בורר השעה. הריפוד מהצדדים משאיר מקום לטבעת
 * הפוקוס שנמתחת מחוץ לגלולה.
 */
@Composable
fun ClockActionRow(
    primaryLabel: String,
    primaryVariant: FutureButtonVariant,
    onPrimary: () -> Unit,
    secondaryLabel: String,
    onSecondary: () -> Unit,
    theme: FutureTheme,
    primaryFocus: FocusRequester? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = FutureDimens.spacingXl),
        horizontalArrangement = Arrangement.spacedBy(FutureDimens.spacingLg),
    ) {
        FutureButton(primaryLabel, theme, onPrimary, modifier = Modifier.weight(1f), variant = primaryVariant, focusRequester = primaryFocus)
        FutureButton(secondaryLabel, theme, onSecondary, modifier = Modifier.weight(1f), variant = FutureButtonVariant.Secondary)
    }
}
