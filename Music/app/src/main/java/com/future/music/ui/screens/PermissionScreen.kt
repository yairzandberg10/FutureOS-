package com.future.music.ui.screens

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
            Box(
                modifier = Modifier.size(84.dp).clip(CircleShape).background(theme.accentColor.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.LibraryMusic, contentDescription = null, tint = theme.accentColor, modifier = Modifier.size(40.dp))
            }
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(top = 20.dp))
            Text(
                "גישה לספריית המוזיקה",
                color = theme.textColor,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(top = 8.dp))
            Text(
                "כדי להציג ולנגן את השירים שכבר נמצאים בטלפון, האפליקציה צריכה הרשאת גישה למוזיקה.",
                color = theme.textColor.copy(alpha = 0.6f),
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
            )
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(top = 24.dp))
            val interactionSource = remember { MutableInteractionSource() }
            val isFocused by interactionSource.collectIsFocusedAsState()
            val shape = RoundedCornerShape(16.dp)
            Box(
                modifier = Modifier
                    .clip(shape)
                    .background(theme.accentColor)
                    .then(if (isFocused) Modifier.border(2.dp, theme.textColor, shape) else Modifier)
                    .focusRequester(buttonFocusRequester)
                    .clickable(interactionSource = interactionSource, indication = null, onClick = onRequestPermission)
                    .focusable(interactionSource = interactionSource)
                    .padding(horizontal = 28.dp, vertical = 14.dp),
            ) {
                Text("אפשר גישה", color = theme.backgroundColor, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
