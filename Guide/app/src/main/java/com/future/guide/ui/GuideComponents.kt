package com.future.guide.ui

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
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
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
import androidx.compose.ui.unit.sp
import com.future.sharednav.theme.FutureTheme
import com.future.sharednav.components.ScreenTopBar as SharedScreenTopBar
import com.future.sharednav.components.TopBarIconButton as SharedTopBarIconButton
import com.future.sharednav.focus.FocusableItem as SharedFocusableItem

/** עטיפה דקה סביב הרכיב המשותף (מודול SharedKeypadNav) - חתימת הקריאה
 * נשארת זהה כדי שקריאות קיימות ב-Guide לא ישתנו. */
@Composable
fun GuideIconButton(icon: ImageVector, contentDescription: String, theme: FutureTheme, tint: Color = theme.accentColor, onClick: () -> Unit) {
    SharedTopBarIconButton(icon, contentDescription, tint, tint, onClick)
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
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onBack != null) {
                GuideIconButton(Icons.AutoMirrored.Rounded.ArrowBack, "חזור", theme = theme, onClick = onBack)
                Spacer(modifier = Modifier.width(10.dp))
            }
            Text(title, color = theme.textColor, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f, fill = true))
            trailing.invoke()
        }
    }
}

@Composable
fun GuideAppRow(icon: ImageVector, label: String, subtitle: String, theme: FutureTheme, onClick: () -> Unit, focusRequester: FocusRequester? = null) {
    SharedFocusableItem(
        onClick = onClick,
        accentColor = theme.accentColor,
        modifier = Modifier.fillMaxWidth(),
        idleBackgroundColor = theme.textColor.copy(alpha = 0.055f),
        focusedBackgroundColor = theme.textColor.copy(alpha = 0.14f),
        cornerRadius = 16.dp,
        focusRequester = focusRequester,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(theme.accentColor.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = theme.accentColor, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(label, color = theme.textColor, fontSize = 16.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(subtitle, color = theme.textColor.copy(alpha = 0.5f), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
fun GuideSectionTitle(text: String, theme: FutureTheme) {
    Text(
        text,
        color = theme.accentColor,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 18.dp, bottom = 6.dp)
    )
}

@Composable
fun GuideTip(text: String, theme: FutureTheme) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(theme.textColor.copy(alpha = 0.055f))
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Text(
            text,
            color = theme.textColor.copy(alpha = 0.85f),
            fontSize = 14.sp,
            lineHeight = 20.sp
        )
    }
}
