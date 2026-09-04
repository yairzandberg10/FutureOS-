package com.future.settings.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.future.settings.ui.theme.ThemeConfig

@Composable
fun SettingItem(
    title: String,
    summary: String? = null,
    icon: ImageVector? = null,
    theme: ThemeConfig,
    showChevron: Boolean = true,
    onClick: (() -> Unit)? = null
) {
    var isFocused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(16.dp)
    // שורות מידע-בלבד (onClick == null) לא מקבלות אף אחד מהאפקטים של פוקוס/לחיצה
    // למטה - בלעדי זה שורה שלחיצה עליה היא no-op הייתה מקבלת בדיוק אותה הדגשת
    // פוקוס מלאה כמו פריט לחיץ אמיתי, ומטעה את המשתמש לחשוב שיש לה פעולה.
    val isInteractive = onClick != null
    val bgColor by animateColorAsState(
        if (isInteractive && isFocused) theme.primaryColor.copy(alpha = 0.18f) else theme.textColor.copy(alpha = 0.06f),
        label = "settingItemBg"
    )
    val scale by animateFloatAsState(if (isInteractive && isFocused) 1.02f else 1f, label = "settingItemScale")

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .then(if (isInteractive) Modifier.onFocusChanged { isFocused = it.isFocused } else Modifier)
            .then(
                if (isInteractive) Modifier.onKeyEvent {
                    if (it.type == KeyEventType.KeyDown && (it.key == Key.DirectionCenter || it.key == Key.Enter || it.key == Key.NumPadEnter)) {
                        onClick!!()
                        true
                    } else false
                } else Modifier
            )
            .then(if (isInteractive) Modifier.focusable() else Modifier)
            .padding(horizontal = 4.dp, vertical = 2.dp)
            .then(if (isInteractive && isFocused) Modifier.border(width = 2.dp, color = theme.primaryColor, shape = shape) else Modifier)
            .then(if (isInteractive) Modifier.clickable { onClick!!() } else Modifier),
        color = bgColor,
        shape = shape
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 14.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = theme.primaryColor,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = theme.titleFontSize,
                    color = theme.textColor,
                    fontWeight = FontWeight.SemiBold
                )
                if (summary != null) {
                    Text(
                        text = summary,
                        fontSize = theme.summaryFontSize,
                        color = theme.textColor.copy(alpha = 0.6f)
                    )
                }
            }
            if (showChevron) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                    contentDescription = null,
                    tint = theme.textColor.copy(alpha = 0.3f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun SettingSwitch(
    title: String,
    summary: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    theme: ThemeConfig
) {
    var isFocused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(16.dp)
    val bgColor by animateColorAsState(
        if (isFocused) theme.primaryColor.copy(alpha = 0.18f) else theme.textColor.copy(alpha = 0.06f),
        label = "settingSwitchBg"
    )
    val scale by animateFloatAsState(if (isFocused) 1.02f else 1f, label = "settingSwitchScale")

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .onFocusChanged { isFocused = it.isFocused }
            .onKeyEvent {
                if (it.type == KeyEventType.KeyDown && (it.key == Key.DirectionCenter || it.key == Key.Enter || it.key == Key.NumPadEnter)) {
                    onCheckedChange(!checked)
                    true
                } else false
            }
            .focusable()
            .padding(horizontal = 4.dp, vertical = 2.dp)
            .then(if (isFocused) Modifier.border(width = 2.dp, color = theme.primaryColor, shape = shape) else Modifier)
            .clickable { onCheckedChange(!checked) },
        color = bgColor,
        shape = shape
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = theme.titleFontSize,
                    color = theme.textColor,
                    fontWeight = FontWeight.SemiBold
                )
                if (summary != null) {
                    Text(
                        text = summary,
                        fontSize = theme.summaryFontSize,
                        color = theme.textColor.copy(alpha = 0.6f)
                    )
                }
            }
            Switch(
                checked = checked,
                onCheckedChange = null,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = theme.primaryColor,
                    uncheckedThumbColor = Color.White,
                    uncheckedTrackColor = Color.LightGray
                )
            )
        }
    }
}

@Composable
fun SettingsCard(theme: ThemeConfig, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(theme.borderRadius),
        color = theme.surfaceColor,
        tonalElevation = 0.dp,
        shadowElevation = if (theme.isDarkMode) 4.dp else 1.dp
    ) {
        Column(modifier = Modifier.padding(vertical = 4.dp)) {
            content()
        }
    }
}

@Composable
fun SettingDivider(theme: ThemeConfig? = null) {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 16.dp),
        thickness = 0.8.dp,
        color = theme?.dividerColor ?: Color.LightGray.copy(alpha = 0.4f)
    )
}

@Composable
fun LargeHeader(title: String, theme: ThemeConfig) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 40.dp)
    ) {
        Text(
            text = title,
            fontSize = theme.headerFontSize,
            fontWeight = FontWeight.Bold,
            color = theme.textColor
        )
    }
}

@Composable
fun SettingHeader(title: String, theme: ThemeConfig) {
    Text(
        text = title,
        modifier = Modifier
            .padding(start = 24.dp, top = 20.dp, bottom = 8.dp)
            .fillMaxWidth(),
        color = theme.primaryColor,
        fontWeight = FontWeight.Bold,
        fontSize = 13.sp,
        letterSpacing = 1.sp
    )
}
