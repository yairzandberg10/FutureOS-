package com.future.tools.ui
import com.future.sharednav.focus.bringIntoViewOnFocus

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
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
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.future.sharednav.theme.FutureTheme
import com.future.sharednav.components.ScreenTopBar as SharedScreenTopBar
import com.future.sharednav.components.TopBarIconButton as SharedTopBarIconButton
import com.future.sharednav.focus.FocusableItem as SharedFocusableItem

/** עטיפה דקה סביב הרכיב המשותף (מודול SharedKeypadNav) - חתימת הקריאה
 * נשארת זהה כדי שקריאות קיימות ב-Tools לא ישתנו. */
@Composable
fun ToolsIconButton(icon: ImageVector, contentDescription: String, theme: FutureTheme, tint: Color = theme.accentColor, onClick: () -> Unit) {
    SharedTopBarIconButton(icon, contentDescription, tint, tint, onClick)
}

@Composable
fun ToolsHeader(title: String, theme: FutureTheme, onBack: (() -> Unit)? = null, trailing: (@Composable () -> Unit)? = null) {
    if (trailing == null) {
        SharedScreenTopBar(title = title, textColor = theme.textColor, accentColor = theme.accentColor, onBack = onBack)
    } else {
        // ScreenTopBar המשותף תומך רק בכפתור trailing יחיד לפי אייקון - Tools
        // צריך תוכן טריילינג חופשי (@Composable), אז השורה עצמה נשארת מקומית,
        // אבל כפתור החזרה בתוכה כן משתמש ברכיב המשותף.
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onBack != null) {
                ToolsIconButton(Icons.AutoMirrored.Rounded.ArrowBack, "חזור", theme = theme, onClick = onBack)
                Spacer(modifier = Modifier.width(10.dp))
            }
            Text(title, color = theme.textColor, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f, fill = true))
            trailing.invoke()
        }
    }
}

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
            Column(modifier = Modifier.weight(1f, fill = true)) {
                Text(label, color = theme.textColor, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                Text(subtitle, color = theme.textColor.copy(alpha = 0.5f), fontSize = 12.sp)
            }
            trailing?.invoke()
        }
    }
}

@Composable
fun ToolsStepperButton(label: String, theme: FutureTheme, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val bgColor by animateColorAsState(
        if (isFocused) theme.accentColor else theme.textColor.copy(alpha = 0.1f),
        label = "stepperBg"
    )
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(bgColor)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .focusable(interactionSource = interactionSource).bringIntoViewOnFocus(),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = if (isFocused) Color.Black else theme.textColor, fontSize = 20.sp, fontWeight = FontWeight.Bold)
    }
}
