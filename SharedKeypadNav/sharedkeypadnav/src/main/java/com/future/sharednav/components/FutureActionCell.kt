package com.future.sharednav.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.future.sharednav.focus.FocusableItem
import com.future.sharednav.theme.FutureDimens
import com.future.sharednav.theme.FutureShapes
import com.future.sharednav.theme.FutureTheme
import com.future.sharednav.theme.LocalFutureAccent
import com.future.sharednav.theme.elevatedSurfaceColor
import com.future.sharednav.theme.mutedTextColor
import com.future.sharednav.theme.readableAccentColor
import com.future.sharednav.theme.rememberFutureType
import com.future.sharednav.theme.textAlpha

/**
 * תא אחד ב-ActionGrid (components/core/ActionGrid.jsx) - האריחים שנושאים
 * את הפעולות של מסך: פקדי שיחה בשתי עמודות, קיצורי דרך במסך בית, שורת
 * פעולות מתחת לתרגום. מילוי "זכוכית" (או 20% מההדגשה כשהפקד דלוק), רדיוס
 * כרטיס, מסגרת פוקוס של 2dp בהדגשה, ובמרכז אייקון ב-44% מגובה התא (בין
 * 20dp ל-40dp - [FutureDimens.iconActionCell]) מעל תווית 13sp ב-60%.
 *
 * האייקון ב-70% מהטקסט, בהדגשה כשהפקד דלוק, או ב-[iconColor] כשהפעולה
 * נושאת סטטוס (הקלטה - אדום).
 */
@Composable
fun FutureActionCell(
    icon: ImageVector,
    label: String?,
    theme: FutureTheme,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    active: Boolean = false,
    iconColor: Color? = null,
    height: Dp = ActionCellHeight,
    contentDescription: String? = label,
    focusRequester: FocusRequester? = null,
) {
    val type = rememberFutureType()
    val accent = LocalFutureAccent.current ?: theme.readableAccentColor
    val fill = if (active) accent.copy(alpha = 0.20f) else theme.elevatedSurfaceColor
    FocusableItem(
        onClick = onClick,
        accentColor = theme.accentColor,
        modifier = modifier.height(height),
        idleBackgroundColor = fill,
        focusedBackgroundColor = fill,
        borderWidth = FutureDimens.focusBorderControl,
        cornerRadius = FutureShapes.radiusLg,
        scaleOnFocus = false,
        contentPadding = 0.dp,
        focusRequester = focusRequester,
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(FutureDimens.spacingXxs, Alignment.CenterVertically),
        ) {
            Icon(
                icon,
                contentDescription = contentDescription,
                tint = iconColor ?: if (active) accent else theme.textAlpha(70),
                modifier = Modifier.size(FutureDimens.iconActionCell(height)),
            )
            if (label != null) {
                Text(
                    label,
                    color = theme.mutedTextColor,
                    fontSize = type.summary,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                )
            }
        }
    }
}

/** 66dp - גובה התא כברירת מחדל (132px ב-ActionGrid.jsx). */
val ActionCellHeight: Dp = 66.dp
