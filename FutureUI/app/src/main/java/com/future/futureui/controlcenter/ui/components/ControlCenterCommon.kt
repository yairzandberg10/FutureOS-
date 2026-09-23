package com.future.futureui.controlcenter.ui.components
import com.future.sharednav.theme.LocalFutureTheme
import com.future.sharednav.theme.readableAccentColor
import com.future.sharednav.theme.FutureDimens
import androidx.compose.ui.composed

import com.future.sharednav.theme.FutureShapes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex

@Composable
fun HeaderActionButton(icon: ImageVector, color: Color, onClick: () -> Unit, isPower: Boolean = false) {
    val theme = LocalFutureTheme.current
    com.future.sharednav.components.TopBarIconButton(
        icon = icon,
        contentDescription = "",
        textColor = if (isPower) theme.dangerColor else color,
        accentColor = theme.accentColor,
        onClick = onClick,
    )
}

/**
 * טבעת הפוקוס של מעטפת המערכת - מסגרת 2dp בהדגשה המתוקנת, כמו כל פקד
 * (guidelines/focus-spec.html). הייתה אפור בהיר קבוע (Color.LightGray), כלומר
 * פוקוס שלא מגיב לצבע ההדגשה שהמשתמש בחר ונעלם על משטח בהיר.
 */
fun Modifier.focusEffect(isFocused: Boolean, shape: androidx.compose.ui.graphics.Shape = FutureShapes.lg): Modifier = composed {
    val ring = LocalFutureTheme.current.readableAccentColor
    this
        .zIndex(if (isFocused) 1f else 0f)
        .then(if (isFocused) Modifier.border(FutureDimens.focusBorderControl, ring, shape) else Modifier)
}
