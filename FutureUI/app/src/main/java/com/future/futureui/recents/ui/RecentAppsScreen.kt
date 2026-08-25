package com.future.futureui.recents.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.future.futureui.recents.logic.RecentAppInfo

@Composable
fun RecentAppsScreen(
    apps: List<RecentAppInfo>,
    onLaunch: (RecentAppInfo) -> Unit,
    onClose: (RecentAppInfo) -> Unit,
    onDismiss: () -> Unit,
    accentColor: Color = MaterialTheme.colorScheme.primary
) {
    var focusedIndex by remember { mutableIntStateOf(0) }
    val listState = rememberLazyListState()
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    LaunchedEffect(apps.size) {
        if (focusedIndex > apps.lastIndex) focusedIndex = apps.lastIndex.coerceAtLeast(0)
    }
    LaunchedEffect(focusedIndex) {
        if (apps.isNotEmpty()) listState.animateScrollToItem(focusedIndex)
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.94f))
                .focusRequester(focusRequester)
                .focusable()
                .onKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                    when (event.key) {
                        Key.DirectionDown -> {
                            if (apps.isNotEmpty()) focusedIndex = (focusedIndex + 1).coerceAtMost(apps.size - 1)
                            true
                        }
                        Key.DirectionUp -> {
                            if (apps.isNotEmpty()) focusedIndex = (focusedIndex - 1).coerceAtLeast(0)
                            true
                        }
                        Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> {
                            apps.getOrNull(focusedIndex)?.let(onLaunch)
                            true
                        }
                        Key.Back -> {
                            onDismiss()
                            true
                        }
                        Key.Menu, Key.Settings, Key.Delete, Key.Backspace -> {
                            apps.getOrNull(focusedIndex)?.let(onClose)
                            true
                        }
                        else -> false
                    }
                }
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.Apps, contentDescription = null, tint = accentColor, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("אפליקציות אחרונות", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }

                if (apps.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("אין אפליקציות אחרונות", color = Color.White.copy(alpha = 0.5f), fontSize = 14.sp)
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        itemsIndexed(apps, key = { _, app -> app.packageName }) { index, app ->
                            RecentAppRow(
                                app = app,
                                isFocused = index == focusedIndex,
                                accentColor = accentColor,
                                onClick = { focusedIndex = index; onLaunch(app) }
                            )
                        }
                    }
                }

                Text(
                    "אישור לפתיחה · אפשרויות לסגירה · חזרה ליציאה",
                    color = Color.White.copy(alpha = 0.35f),
                    fontSize = 11.sp,
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun RecentAppRow(app: RecentAppInfo, isFocused: Boolean, accentColor: Color, onClick: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    val bgColor by animateColorAsState(
        if (isFocused) Color.White.copy(alpha = 0.14f) else Color.White.copy(alpha = 0.05f),
        label = "recentRowBg"
    )
    val scale by animateFloatAsState(if (isFocused) 1.02f else 1f, label = "recentRowScale")
    val icon = remember(app.packageName) { app.icon.toBitmap().asImageBitmap() }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(bgColor)
            .then(if (isFocused) Modifier.border(width = 2.dp, color = accentColor, shape = shape) else Modifier)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            bitmap = icon,
            contentDescription = null,
            modifier = Modifier.size(if (isFocused) 44.dp else 40.dp).clip(RoundedCornerShape(percent = 28))
        )
        Spacer(modifier = Modifier.width(14.dp))
        Text(app.label, color = Color.White, fontSize = 15.sp, maxLines = 1, modifier = Modifier.weight(1f))
    }
}
