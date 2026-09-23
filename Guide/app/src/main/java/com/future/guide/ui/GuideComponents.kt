package com.future.guide.ui

import com.future.sharednav.icons.FutureIcons
import com.future.sharednav.components.FutureListItem
import com.future.sharednav.components.FutureAvatar
import com.future.sharednav.components.FutureSectionHeader
import com.future.sharednav.theme.FutureDimens
import com.future.sharednav.theme.idleChipColor

import com.future.sharednav.theme.FutureTypography
import com.future.sharednav.theme.FutureShapes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.future.sharednav.theme.FutureTheme
import com.future.sharednav.components.ScreenTopBar as SharedScreenTopBar
import com.future.sharednav.components.TopBarIconButton as SharedTopBarIconButton
import com.future.sharednav.focus.FocusableItem as SharedFocusableItem

/** עטיפה דקה סביב הרכיב המשותף (מודול SharedKeypadNav) - חתימת הקריאה
 * נשארת זהה כדי שקריאות קיימות ב-Guide לא ישתנו. */
@Composable
fun GuideIconButton(icon: ImageVector, contentDescription: String, theme: FutureTheme, tint: Color = theme.textColor, onClick: () -> Unit) {
    SharedTopBarIconButton(icon, contentDescription, tint, theme.accentColor, onClick)
}

@Composable
fun GuideHeader(title: String, theme: FutureTheme, onBack: (() -> Unit)? = null, trailing: (@Composable () -> Unit)? = null) {
    if (trailing == null) {
        SharedScreenTopBar(title = title, textColor = theme.textColor, accentColor = theme.accentColor, onBack = onBack)
    } else {
        // ScreenTopBar המשותף תומך רק בכפתור trailing יחיד לפי אייקון - Guide
        // צריך תוכן טריילינג חופשי (@Composable), אז השורה עצמה נשארת מקומית,
        // אבל כפתור החזרה בתוכה כן משתמש ברכיב המשותף.
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = FutureDimens.spacingLg, vertical = FutureDimens.spacingMd),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // אין כפתור חזור על המסך - מקש BACK הפיזי עושה את זה
            if (onBack != null) androidx.activity.compose.BackHandler(onBack = onBack)
            Text(title, color = theme.textColor, fontSize = FutureTypography.screenTitle, fontWeight = FontWeight.Bold, maxLines = 1, modifier = Modifier.weight(1f, fill = true))
            trailing.invoke()
        }
    }
}

@Composable
fun GuideAppRow(icon: ImageVector, label: String, subtitle: String, theme: FutureTheme, onClick: () -> Unit, focusRequester: FocusRequester? = null) {
    FutureListItem(
        title = label,
        summary = subtitle,
        theme = theme,
        onClick = onClick,
        focusRequester = focusRequester,
        leading = { FutureAvatar(theme = theme, icon = icon) },
    )
}

/** כותרת קטע - FutureSectionHeader (13sp, 55%, ריווח 1sp); הייתה בצבע ההדגשה, שאינו צבע לכותרות. */
@Composable
fun GuideSectionTitle(text: String, theme: FutureTheme) {
    FutureSectionHeader(text, theme, inset = false, modifier = Modifier.padding(top = FutureDimens.spacingSm))
}

@Composable
fun GuideTip(text: String, theme: FutureTheme) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(FutureShapes.lg)
            .background(theme.idleChipColor)
            .padding(horizontal = FutureDimens.spacingMd, vertical = FutureDimens.spacingMd)
    ) {
        Text(
            text,
            color = theme.textColor,
            fontSize = FutureTypography.body,
            lineHeight = FutureTypography.body * FutureTypography.lineHeightRatio
        )
    }
}
