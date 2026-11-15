package com.future.systemui.notifications

import android.app.Notification
import android.service.notification.StatusBarNotification
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.future.sharednav.focus.dpadFocusBorder
import com.future.sharednav.i18n.tr
import com.future.sharednav.theme.FutureAppTheme
import com.future.sharednav.theme.FutureMotion
import com.future.sharednav.theme.FutureShapes
import com.future.sharednav.theme.FutureTypography
import com.future.sharednav.theme.LocalFutureTheme
import com.future.sharednav.theme.dividerColor
import com.future.sharednav.theme.mutedTextColor
import com.future.sharednav.theme.secondaryTextColor
import com.future.sharednav.theme.subtleTextColor
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import java.util.Date

/** Height of the status bar window (matches the FutureUI status bar), so the shade starts below it. */
private val StatusBarHeight = 28.dp

private fun StatusBarNotification.titleText(): String = try {
    notification.extras?.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
} catch (_: Throwable) { "" }

private fun StatusBarNotification.bodyText(): String = try {
    val e = notification.extras
    (e?.getCharSequence(Notification.EXTRA_BIG_TEXT) ?: e?.getCharSequence(Notification.EXTRA_TEXT))?.toString().orEmpty()
} catch (_: Throwable) { "" }

@Composable
private fun rememberAppLabel(pkg: String): String {
    val context = LocalContext.current
    return remember(pkg) {
        try {
            val pm = context.packageManager
            pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
        } catch (_: Throwable) {
            pkg.substringAfterLast('.')
        }
    }
}

@Composable
private fun AppIcon(pkg: String) {
    val context = LocalContext.current
    val bitmap = remember(pkg) {
        try {
            context.packageManager.getApplicationIcon(pkg).toBitmap(width = 48, height = 48).asImageBitmap()
        } catch (_: Throwable) { null }
    }
    if (bitmap != null) {
        Image(bitmap = bitmap, contentDescription = null, modifier = Modifier.size(22.dp))
    } else {
        Spacer(Modifier.size(22.dp))
    }
}

@Composable
private fun NotificationHeader(sbn: StatusBarNotification) {
    val theme = LocalFutureTheme.current
    val context = LocalContext.current
    val label = rememberAppLabel(sbn.packageName)
    val time = remember(sbn.postTime) {
        try { android.text.format.DateFormat.getTimeFormat(context).format(Date(sbn.postTime)) } catch (_: Throwable) { "" }
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        AppIcon(sbn.packageName)
        Spacer(Modifier.width(8.dp))
        Text(
            text = label,
            color = theme.mutedTextColor,
            fontSize = FutureTypography.label,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Text(text = time, color = theme.subtleTextColor, fontSize = FutureTypography.label)
    }
}

/** One notification row: app label + time, title, text. OK opens it, DEL clears it (if clearable). */
@Composable
private fun NotificationRow(
    sbn: StatusBarNotification,
    requester: FocusRequester?,
    onOpen: (StatusBarNotification) -> Unit,
    onClear: (StatusBarNotification) -> Unit,
) {
    val theme = LocalFutureTheme.current
    var focused by remember { mutableStateOf(false) }
    val title = remember(sbn.key, sbn.postTime) { sbn.titleText() }
    val body = remember(sbn.key, sbn.postTime) { sbn.bodyText() }
    val shape = FutureShapes.row
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(theme.surfaceColor)
            .dpadFocusBorder(focused, shape)
            .then(if (requester != null) Modifier.focusRequester(requester) else Modifier)
            .onKeyEvent { ev ->
                if (!focused) return@onKeyEvent false
                when (ev.key) {
                    Key.DirectionCenter, Key.Enter -> {
                        if (ev.type == KeyEventType.KeyUp) onOpen(sbn)
                        true
                    }
                    Key.Backspace, Key.Delete -> {
                        if (ev.type == KeyEventType.KeyUp && sbn.isClearable) onClear(sbn)
                        true
                    }
                    else -> false
                }
            }
            .onFocusChanged { focused = it.isFocused }
            .focusable()
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        NotificationHeader(sbn)
        if (title.isNotEmpty()) {
            Text(
                text = title,
                color = theme.textColor,
                fontSize = FutureTypography.title,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (body.isNotEmpty()) {
            Text(
                text = body,
                color = theme.secondaryTextColor,
                fontSize = FutureTypography.bodyLarge,
                maxLines = if (focused) 4 else 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun ClearAllButton(onClearAll: () -> Unit) {
    val theme = LocalFutureTheme.current
    var focused by remember { mutableStateOf(false) }
    val shape = FutureShapes.row
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(theme.surfaceColor)
            .dpadFocusBorder(focused, shape)
            .onKeyEvent { ev ->
                if (!focused) return@onKeyEvent false
                if (ev.key == Key.DirectionCenter || ev.key == Key.Enter) {
                    if (ev.type == KeyEventType.KeyUp) onClearAll()
                    true
                } else false
            }
            .onFocusChanged { focused = it.isFocused }
            .focusable()
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = tr("נקה הכל"),
            color = theme.textColor,
            fontSize = FutureTypography.bodyLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
    }
}

/** The full-screen shade. [visibleState] drives the enter/exit motion; the host removes the window after the exit. */
@Composable
fun ShadeScreen(
    visibleState: MutableTransitionState<Boolean>,
    notifications: StateFlow<List<StatusBarNotification>>,
    onOpen: (StatusBarNotification) -> Unit,
    onClear: (StatusBarNotification) -> Unit,
    onClearAll: () -> Unit,
) {
    FutureAppTheme {
        val theme = LocalFutureTheme.current
        val list by notifications.collectAsState()
        val firstRequester = remember { FocusRequester() }
        val clearRequester = remember { FocusRequester() }
        var hasFocus by remember { mutableStateOf(false) }

        // Initial focus, and re-focus when the focused row disappears (cleared / opened).
        LaunchedEffect(list.size) {
            delay(120)
            if (!hasFocus) {
                try {
                    if (list.isNotEmpty()) firstRequester.requestFocus() else clearRequester.requestFocus()
                } catch (_: Throwable) {
                }
            }
        }

        AnimatedVisibility(
            visibleState = visibleState,
            enter = fadeIn(FutureMotion.enter()) + slideInVertically(FutureMotion.enter()) { -it / 3 },
            exit = fadeOut(FutureMotion.exit()) + slideOutVertically(FutureMotion.exit()) { -it / 3 },
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(theme.backgroundColor.copy(alpha = 0.97f))
                    .onFocusChanged { hasFocus = it.hasFocus }
                    .padding(top = StatusBarHeight + 8.dp, start = 16.dp, end = 16.dp, bottom = 12.dp),
            ) {
                Text(
                    text = tr("התראות"),
                    color = theme.textColor,
                    fontSize = FutureTypography.title,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
                Box(Modifier.fillMaxWidth().height(1.dp).background(theme.dividerColor))
                Spacer(Modifier.height(8.dp))
                if (list.isEmpty()) {
                    Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text(
                            text = tr("אין התראות חדשות"),
                            color = theme.secondaryTextColor,
                            fontSize = FutureTypography.bodyLarge,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.focusRequester(clearRequester).focusable(),
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(vertical = 4.dp),
                    ) {
                        items(list, key = { it.key }) { sbn ->
                            NotificationRow(
                                sbn = sbn,
                                requester = if (sbn.key == list.first().key) firstRequester else null,
                                onOpen = onOpen,
                                onClear = onClear,
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    ClearAllButton(onClearAll)
                }
            }
        }
    }
}

/** Small top card for a heads-up notification. Never focusable (the window is FLAG_NOT_FOCUSABLE). */
@Composable
fun HeadsUpCard(visibleState: MutableTransitionState<Boolean>, sbn: StatusBarNotification?) {
    FutureAppTheme {
        val theme = LocalFutureTheme.current
        AnimatedVisibility(
            visibleState = visibleState,
            enter = fadeIn(FutureMotion.enter()) + slideInVertically(FutureMotion.enter()) { -it },
            exit = fadeOut(FutureMotion.exit()) + slideOutVertically(FutureMotion.exit()) { -it },
        ) {
            if (sbn != null) {
                val title = remember(sbn.key, sbn.postTime) { sbn.titleText() }
                val body = remember(sbn.key, sbn.postTime) { sbn.bodyText() }
                Column(
                    modifier = Modifier
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .fillMaxWidth()
                        .clip(FutureShapes.xxl)
                        .background(theme.surfaceColor)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    NotificationHeader(sbn)
                    if (title.isNotEmpty()) {
                        Text(
                            text = title,
                            color = theme.textColor,
                            fontSize = FutureTypography.title,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    if (body.isNotEmpty()) {
                        Text(
                            text = body,
                            color = theme.secondaryTextColor,
                            fontSize = FutureTypography.bodyLarge,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}
