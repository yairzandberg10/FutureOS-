package com.future.fitness.ui.components

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
import com.future.fitness.ui.theme.FutureTheme

/** אריח סטטיסטיקה קטן - קלוריות/דקות/רצף בבית, ומספרי סיכום בהתקדמות. */
@Composable
fun StatTile(icon: ImageVector, value: String, label: String, theme: FutureTheme, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(theme.surfaceColor, RoundedCornerShape(16.dp))
            .padding(14.dp),
    ) {
        Icon(icon, contentDescription = null, tint = theme.accentColor, modifier = Modifier.size(18.dp))
        Text(value, color = theme.textColor, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
        Text(label, color = theme.textColor.copy(alpha = 0.6f), fontSize = 11.sp)
    }
}

/** שורת רשימה עם באדג' אייקון עגול, כותרת/כותרת-משנה, וספרת קיצור-דרך
 * אופציונלית או חץ - הפריט החוזר בתפריט הבית, ברשימת אימונים, ובהיסטוריה. */
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
    val rowContent: @Composable () -> Unit = {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(44.dp).background(theme.accentColor.copy(alpha = 0.18f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = theme.accentColor, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = theme.textColor, fontSize = 17.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(subtitle, color = theme.textColor.copy(alpha = 0.55f), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (digit != null) {
                Text(digit, color = theme.textColor.copy(alpha = 0.35f), fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
            if (showChevron) {
                Icon(
                    Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                    contentDescription = null,
                    tint = theme.textColor.copy(alpha = 0.35f),
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }

    if (onClick != null) {
        FocusableItem(onClick = onClick, theme = theme, modifier = modifier.fillMaxWidth(), focusRequester = focusRequester) {
            rowContent()
        }
    } else {
        Box(modifier = modifier.fillMaxWidth().background(theme.textColor.copy(alpha = 0.05f), RoundedCornerShape(16.dp))) {
            rowContent()
        }
    }
}
