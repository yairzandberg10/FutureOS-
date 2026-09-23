package com.future.music.ui.screens
import com.future.sharednav.components.FutureAvatar
import com.future.sharednav.components.FutureButton

import com.future.sharednav.theme.FutureTypography
import com.future.sharednav.theme.FutureShapes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.future.sharednav.theme.FutureTheme

@Composable
fun PermissionScreen(theme: FutureTheme, onRequestPermission: () -> Unit) {
    val buttonFocusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { buttonFocusRequester.requestFocus() }

    Box(modifier = Modifier.fillMaxSize().background(theme.backgroundColor), contentAlignment = Alignment.Center) {
        Column(modifier = Modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            FutureAvatar(theme = theme, icon = Icons.Rounded.LibraryMusic, size = 88.dp)
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(top = 20.dp))
            Text(
                "גישה לספריית המוזיקה",
                color = theme.textColor,
                fontSize = FutureTypography.screenTitle,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(top = 8.dp))
            Text(
                "כדי להציג ולנגן את השירים שכבר נמצאים בטלפון, האפליקציה צריכה הרשאת גישה למוזיקה.",
                color = theme.textColor.copy(alpha = 0.6f),
                fontSize = FutureTypography.body,
                textAlign = TextAlign.Center,
            )
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(top = 24.dp))
            FutureButton("אפשר גישה", theme, onRequestPermission, focusRequester = buttonFocusRequester)
        }
    }
}
