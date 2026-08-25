package com.future.sharednav.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * שורת כותרת משותפת למסכי רשימה - כפתור חזור (RTL: חץ ימינה) + כותרת +
 * כפתור פעולה אופציונלי בצד ההפוך. היה קיים זהה כמעט לחלוטין (רק סוג
 * ה-theme היה שונה) גם ב-Music וגם ב-Sfarim - כאן הגרסה המשותפת, עם
 * textColor/accentColor כפרמטרים פרימיטיביים במקום FutureTheme.
 */
@Composable
fun ScreenTopBar(
    title: String,
    textColor: Color,
    accentColor: Color,
    onBack: (() -> Unit)? = null,
    trailingIcon: ImageVector? = null,
    trailingContentDescription: String? = null,
    onTrailingClick: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            TopBarIconButton(Icons.AutoMirrored.Rounded.ArrowBack, "חזור", textColor, accentColor, onBack)
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
            title,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = textColor,
            maxLines = 1,
            modifier = Modifier.weight(1f),
        )
        if (trailingIcon != null && onTrailingClick != null) {
            TopBarIconButton(trailingIcon, trailingContentDescription ?: "", textColor, accentColor, onTrailingClick)
        }
    }
}

@Composable
fun TopBarIconButton(
    icon: ImageVector,
    contentDescription: String,
    textColor: Color,
    accentColor: Color,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val bgColor by animateColorAsState(
        if (isFocused) accentColor.copy(alpha = 0.3f) else textColor.copy(alpha = 0.08f),
        label = "topBarIconBtnBg",
    )
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(bgColor)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = contentDescription, tint = textColor, modifier = Modifier.size(18.dp))
    }
}
