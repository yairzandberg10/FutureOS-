package com.future.futureui.controlcenter.ui.components
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.ui.draw.clip
import com.future.sharednav.focus.focusMotion
import com.future.futureui.ui.theme.ShellGlass
import com.future.futureui.ui.theme.shellFocusRing
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

/** כפתור עגול בכותרת - אריח זכוכית, פוקוס במסגרת בלבד (ShellGlass). */
@Composable
fun HeaderActionButton(icon: ImageVector, color: Color, onClick: () -> Unit, isPower: Boolean = false) {
    val theme = LocalFutureTheme.current
    val interactionSource = androidx.compose.runtime.remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    androidx.compose.foundation.layout.Box(
        modifier = Modifier
            .size(FutureDimens.rowHeightTopBarButton)
            .focusMotion(interactionSource, focusedScale = 1.08f, pressedScale = 0.92f)
            .clip(androidx.compose.foundation.shape.CircleShape)
            .background(com.future.futureui.ui.theme.shellTile(isFocused))
            .shellFocusRing(isFocused, androidx.compose.foundation.shape.CircleShape)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .focusable(interactionSource = interactionSource),
        contentAlignment = androidx.compose.ui.Alignment.Center,
    ) {
        androidx.compose.material3.Icon(icon, contentDescription = null, tint = if (isPower) theme.dangerColor else color, modifier = Modifier.size(FutureDimens.iconTopBar))
    }
}

/**
 * טבעת הפוקוס של מעטפת המערכת - מסגרת 2dp בהדגשה המתוקנת, כמו כל פקד
 * (guidelines/focus-spec.html). הייתה אפור בהיר קבוע (Color.LightGray), כלומר
 * פוקוס שלא מגיב לצבע ההדגשה שהמשתמש בחר ונעלם על משטח בהיר.
 */
fun Modifier.focusEffect(isFocused: Boolean, shape: androidx.compose.ui.graphics.Shape = FutureShapes.lg): Modifier = composed {
    // מסגרת בצבע הטקסט ולא בהדגשה - המעטפת לא צובעת פוקוס (ShellGlass).
    this
        .zIndex(if (isFocused) 1f else 0f)
        .shellFocusRing(isFocused, shape)
}
