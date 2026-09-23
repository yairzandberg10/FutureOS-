package com.future.fitness.ui.components
import com.future.sharednav.components.FutureAvatar
import com.future.sharednav.components.FutureListItem
import com.future.sharednav.theme.FutureDimens
import com.future.sharednav.theme.mutedTextColor
import com.future.sharednav.theme.subtleTextColor
import com.future.sharednav.theme.chevronColor
import com.future.sharednav.theme.idleChipColor
import com.future.sharednav.theme.idleFieldColor
import androidx.compose.material.icons.rounded.KeyboardArrowLeft
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Arrangement

import com.future.sharednav.theme.FutureTypography
import com.future.sharednav.theme.FutureShapes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.future.sharednav.theme.FutureTheme

/** אריח סטטיסטיקה קטן - קלוריות/דקות/רצף בבית, ומספרי סיכום בהתקדמות. כרטיס (16dp). */
@Composable
fun StatTile(icon: ImageVector, value: String, label: String, theme: FutureTheme, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(theme.surfaceColor, FutureShapes.lg)
            .padding(FutureDimens.spacingMd),
    ) {
        Icon(icon, contentDescription = null, tint = theme.mutedTextColor, modifier = Modifier.size(FutureDimens.iconTopBar))
        Text(value, color = theme.textColor, fontSize = FutureTypography.screenTitle, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = FutureDimens.spacingSm))
        Text(label, color = theme.mutedTextColor, fontSize = FutureTypography.caption)
    }
}

/**
 * שורת רשימה עם אייקון בעיגול, כותרת/סיכום, וספרת קיצור-דרך או חץ -
 * הפריט החוזר בתפריט הבית, ברשימת אימונים ובהיסטוריה. זו שורת הרשימה של
 * הדיזיין סיסטם (FutureListItem); החץ הוא חץ הכניסה של המערכת - מצביע
 * שמאלה, לא מתהפך, ב-30%.
 */
@Composable
fun IconListRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    theme: FutureTheme,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    digit: String? = null,
    showChevron: Boolean = false,
    focusRequester: FocusRequester? = null,
) {
    val leading: @Composable () -> Unit = { FutureAvatar(theme = theme, icon = icon) }
    val trailing: @Composable RowScope.() -> Unit = {
        if (digit != null) {
            Text(digit, color = theme.subtleTextColor, fontSize = FutureTypography.body, fontWeight = FontWeight.Bold)
        }
        if (showChevron) {
            Icon(
                Icons.Rounded.KeyboardArrowLeft,
                contentDescription = null,
                tint = theme.chevronColor,
                modifier = Modifier.size(FutureDimens.iconTopBar),
            )
        }
    }

    if (onClick != null) {
        FutureListItem(
            title = title,
            summary = subtitle,
            theme = theme,
            onClick = onClick,
            modifier = modifier,
            idleBackgroundColor = theme.idleChipColor,
            focusRequester = focusRequester,
            leading = leading,
            trailing = trailing,
        )
    } else {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .background(theme.idleChipColor, FutureShapes.row)
                .heightIn(min = FutureDimens.rowHeightList)
                .padding(horizontal = FutureDimens.spacingMd, vertical = FutureDimens.spacingSm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(FutureDimens.spacingMd),
        ) {
            leading()
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = theme.textColor, fontSize = FutureTypography.title, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(subtitle, color = theme.mutedTextColor, fontSize = FutureTypography.summary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            trailing()
        }
    }
}
