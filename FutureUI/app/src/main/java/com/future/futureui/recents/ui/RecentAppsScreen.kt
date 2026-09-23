package com.future.futureui.recents.ui

import com.future.sharednav.icons.FutureIcons
import com.future.sharednav.theme.FutureMotion
import com.future.sharednav.theme.readableAccentColor
import com.future.sharednav.theme.idleChipColor
import com.future.sharednav.theme.FutureDimens
import androidx.compose.runtime.ReadOnlyComposable

import com.future.sharednav.theme.FutureTypography
import com.future.sharednav.theme.FutureShapes
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
                .background(shellTheme.backgroundColor)
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
                    Icon(FutureIcons.Apps, contentDescription = null, tint = shellTheme.textColor, modifier = Modifier.size(FutureDimens.iconSettingRow))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("אפליקציות אחרונות", color = shellTheme.textColor, fontSize = FutureTypography.screenTitle, fontWeight = FontWeight.Bold)
                }

                if (apps.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("אין אפליקציות אחרונות", color = shellTheme.textColor.copy(alpha = 0.6f), fontSize = FutureTypography.body)
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
                    color = shellTheme.textColor.copy(alpha = 0.4f),
                    fontSize = FutureTypography.caption,
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun RecentAppRow(app: RecentAppInfo, isFocused: Boolean, accentColor: Color, onClick: () -> Unit) {
    val shape = FutureShapes.row
    val bgColor by animateColorAsState(
        if (isFocused) shellTheme.readableAccentColor.copy(alpha = 0.14f) else shellTheme.idleChipColor,
        FutureMotion.focusColorSpec,
        label = "recentRowBg"
    )
    val scale by animateFloatAsState(if (isFocused) 1.02f else 1f, label = "recentRowScale")
    val icon = remember(app.packageName) { app.icon.toBitmap().asImageBitmap() }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(bgColor)
            .then(if (isFocused) Modifier.border(width = FutureDimens.focusBorderItem, color = shellTheme.readableAccentColor, shape = shape) else Modifier)
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
        Text(app.label, color = shellTheme.textColor, fontSize = FutureTypography.bodyLarge, maxLines = 1, modifier = Modifier.weight(1f))
    }
}

/** הערכה הפעילה - מסכי המעטפת עוקבים אחרי מצב כהה/בהיר וצבע ההדגשה, כמו כל אפליקציה. */
private val shellTheme: com.future.sharednav.theme.FutureTheme
    @Composable @ReadOnlyComposable get() = com.future.sharednav.theme.LocalFutureTheme.current
