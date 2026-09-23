package com.future.futureui.recents.ui

import android.text.format.DateUtils
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.future.futureui.recents.logic.RecentAppInfo
import com.future.futureui.ui.theme.ShellGlass
import com.future.futureui.ui.theme.shellFocusRing
import com.future.sharednav.icons.FutureIcons
import com.future.sharednav.theme.FutureDimens
import com.future.sharednav.theme.FutureMotion
import com.future.sharednav.theme.FutureShapes
import com.future.sharednav.theme.FutureTypography
import com.future.sharednav.theme.LocalFutureTheme
import com.future.futureui.statusbar.logic.StatusBarLayoutManager

/**
 * אפליקציות אחרונות: כל אפליקציה היא "חלון" - שורת כותרת עם האייקון, השם
 * ו-X בצד שמאל, וגוף החלון עם האייקון הגדול ומתי היא הייתה בשימוש.
 *
 * חצים למעלה/למטה בין החלונות, OK פותח, Options (או X) סוגר את החלון
 * הממוקד, "סגור הכל" בסוף הרשימה (או 0) סוגר את כולם, BACK יוצא.
 * [accentColor] נשאר בחתימה לתאימות; המעטפת לא צובעת פוקוס (ShellGlass).
 */
@Composable
fun RecentAppsScreen(
    apps: List<RecentAppInfo>,
    onLaunch: (RecentAppInfo) -> Unit,
    onClose: (RecentAppInfo) -> Unit,
    onDismiss: () -> Unit,
    accentColor: Color = Color.White,
) {
    val theme = LocalFutureTheme.current
    // אינדקס apps.size = שורת "סגור הכל".
    var focusedIndex by remember { mutableIntStateOf(0) }
    val listState = rememberLazyListState()
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focusRequester.requestFocus() } }
    LaunchedEffect(apps.size) { focusedIndex = focusedIndex.coerceIn(0, apps.size) }
    LaunchedEffect(focusedIndex) { if (apps.isNotEmpty()) listState.animateScrollToItem(focusedIndex) }

    fun closeAll() {
        apps.toList().forEach(onClose)
        onDismiss()
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(ShellGlass.scrim(LocalFutureTheme.current))
                .focusRequester(focusRequester)
                .focusable()
                .onKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                    when (event.key) {
                        Key.DirectionDown -> { if (apps.isNotEmpty()) focusedIndex = (focusedIndex + 1).coerceAtMost(apps.size); true }
                        Key.DirectionUp -> { focusedIndex = (focusedIndex - 1).coerceAtLeast(0); true }
                        Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> {
                            if (focusedIndex == apps.size) closeAll() else apps.getOrNull(focusedIndex)?.let(onLaunch)
                            true
                        }
                        Key.Back -> { onDismiss(); true }
                        Key.Menu, Key.Settings, Key.Delete, Key.Backspace -> {
                            if (focusedIndex == apps.size) closeAll() else apps.getOrNull(focusedIndex)?.let(onClose)
                            true
                        }
                        Key.Zero, Key.NumPad0 -> { closeAll(); true }
                        else -> false
                    }
                }
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(top = StatusBarLayoutManager.HEIGHT_DP.dp)) {
                Text(
                    "אפליקציות אחרונות",
                    color = ShellGlass.ink(theme),
                    fontSize = FutureTypography.screenTitle,
                    fontWeight = FutureTypography.weightBold,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                )
                if (apps.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("אין אפליקציות אחרונות", color = ShellGlass.inkMuted(theme), fontSize = FutureTypography.body)
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        itemsIndexed(apps, key = { _, app -> app.packageName }) { index, app ->
                            AppWindow(app = app, isFocused = index == focusedIndex, modifier = Modifier.animateItem())
                        }
                        item(key = "close-all") {
                            val focused = focusedIndex == apps.size
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(FutureDimens.rowHeightDialogButton)
                                    .clip(FutureShapes.pill)
                                    .background(if (focused) ShellGlass.tileFocused(theme) else ShellGlass.tile(theme))
                                    .shellFocusRing(focused, FutureShapes.pill),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text("סגור הכל", color = ShellGlass.ink(theme), fontSize = FutureTypography.bodyLarge, fontWeight = FutureTypography.weightMedium)
                            }
                        }
                    }
                }
                Text(
                    "OK פתיחה · Options סגירה · 0 סגור הכל",
                    color = ShellGlass.inkMuted(theme),
                    fontSize = FutureTypography.caption,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                )
            }
        }
    }
}

/** חלון של אפליקציה: שורת כותרת (אייקון, שם, X משמאל) וגוף עם האייקון הגדול. */
@Composable
private fun AppWindow(app: RecentAppInfo, isFocused: Boolean, modifier: Modifier = Modifier) {
    val theme = LocalFutureTheme.current
    val icon = remember(app.packageName) { app.icon.toBitmap(128, 128).asImageBitmap() }
    val shape = FutureShapes.xl
    val scale = androidx.compose.animation.core.animateFloatAsState(if (isFocused) 1f else 0.96f, FutureMotion.focusScaleSpec, label = "windowScale")
    Column(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer { scaleX = scale.value; scaleY = scale.value; alpha = if (isFocused) 1f else 0.82f }
            .clip(shape)
            .background(ShellGlass.panel(theme))
            .shellFocusRing(isFocused, shape)
            .animateContentSize(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().background(ShellGlass.tile(theme)).padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(icon, contentDescription = null, modifier = Modifier.size(22.dp).clip(RoundedCornerShape(percent = 28)))
            Spacer(modifier = Modifier.width(8.dp))
            Text(app.label, color = ShellGlass.ink(theme), fontSize = FutureTypography.body, fontWeight = FutureTypography.weightMedium, maxLines = 1, modifier = Modifier.weight(1f))
            // ה-X בצד שמאל (סוף השורה ב-RTL) - מקש Options סוגר את החלון.
            Box(
                modifier = Modifier.size(26.dp).clip(CircleShape).background(if (isFocused) ShellGlass.on(theme) else ShellGlass.tile(theme)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(FutureIcons.Close, contentDescription = "סגור", tint = if (isFocused) ShellGlass.onInk(theme) else ShellGlass.ink(theme), modifier = Modifier.size(14.dp))
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().height(if (isFocused) 96.dp else 64.dp).padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(icon, contentDescription = null, modifier = Modifier.size(if (isFocused) 56.dp else 40.dp).clip(RoundedCornerShape(percent = 28)))
            Spacer(modifier = Modifier.width(14.dp))
            Text(
                DateUtils.getRelativeTimeSpanString(app.lastTimeUsed, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS).toString(),
                color = ShellGlass.inkMuted(theme),
                fontSize = FutureTypography.summary,
            )
        }
    }
}
