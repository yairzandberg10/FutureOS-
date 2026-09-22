package com.future.sharednav.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.future.sharednav.theme.FutureContrast
import com.future.sharednav.theme.FutureShapes
import com.future.sharednav.theme.FutureTheme
import com.future.sharednav.theme.FutureTypography
import com.future.sharednav.theme.LocalFutureAccent
import com.future.sharednav.theme.elevatedSurfaceColor
import com.future.sharednav.theme.mutedTextColor
import com.future.sharednav.theme.readableAccentColor
import com.future.sharednav.theme.rememberFutureType
import com.future.sharednav.theme.secondaryTextColor

/**
 * העיגול שחוזר בכל משטח של אדם או מכשיר (components/core/Avatar.jsx):
 * מילוי "זכוכית" (elevatedSurfaceColor), עיגול מלא, ובתוכו אייקון ב-44%
 * מהקוטר ב-60% מהטקסט, או ראשי התיבות של השם ב-30% מהקוטר ב-70%.
 * 44dp בשורת רשימה, 88dp כגיבור מסך.
 *
 * לפני הרכיב הזה כל אפליקציה ציירה את העיגול הזה אחרת - לרוב ב-18-20%
 * מצבע ההדגשה, כלומר ההדגשה כקישוט, שמתחלפת בכל פעם שהמשתמש משנה אותה.
 */
@Composable
fun FutureAvatar(
    theme: FutureTheme,
    modifier: Modifier = Modifier,
    name: String? = null,
    icon: ImageVector? = null,
    size: Dp = AvatarListSize,
    contentColor: Color? = null,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(FutureShapes.pill)
            .background(theme.elevatedSurfaceColor),
        contentAlignment = Alignment.Center,
    ) {
        if (icon != null) {
            Icon(
                icon,
                contentDescription = null,
                tint = contentColor ?: theme.mutedTextColor,
                modifier = Modifier.size(size * 0.44f),
            )
        } else if (name != null) {
            Text(
                initials(name),
                color = contentColor ?: theme.secondaryTextColor,
                fontSize = (size.value * 0.3f).sp,
                fontWeight = FutureTypography.weightMedium,
                maxLines = 1,
            )
        }
    }
}

/** 44dp - גודל האווטאר בשורת רשימה (88px ב-Avatar.jsx). */
val AvatarListSize: Dp = 44.dp

/** עד שתי אותיות ראשונות של השם - "מיכל לוי" -> "מל". */
fun initials(name: String): String =
    name.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }.take(2).joinToString("") { it.take(1) }

/**
 * מונה הודעות שלא נקראו (components/feedback/Badge.jsx): עיגול 18dp בצבע
 * ההדגשה, ספרות 10sp מודגשות בדיו שמתאים לו. לספירה בלבד - לא לסטטוס.
 */
@Composable
fun FutureBadge(
    count: Int,
    theme: FutureTheme,
    modifier: Modifier = Modifier,
) {
    val type = rememberFutureType()
    val accent = LocalFutureAccent.current ?: theme.readableAccentColor
    Box(
        modifier = modifier
            .defaultMinSize(minWidth = BadgeSize, minHeight = BadgeSize)
            .clip(FutureShapes.pill)
            .background(accent)
            .padding(horizontal = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            count.toString(),
            color = FutureContrast.onColor(accent),
            fontSize = type.badge,
            fontWeight = FutureTypography.weightBold,
            maxLines = 1,
        )
    }
}

/** 18dp - הקוטר של התג (36px ב-Badge.jsx). */
private val BadgeSize = 18.dp
