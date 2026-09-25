package com.future.camera.ui
import com.future.sharednav.systemui.StatusBarInset

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.future.camera.data.AspectOption
import com.future.camera.data.CameraMedia
import com.future.camera.data.CameraOptions
import com.future.camera.data.PhotoQualityOption
import com.future.camera.data.SceneOption
import com.future.camera.data.VideoQualityOption
import com.future.sharednav.components.FutureCard
import com.future.sharednav.components.FutureDivider
import com.future.sharednav.components.FutureSectionHeader
import com.future.sharednav.components.FutureSettingItem
import com.future.sharednav.components.FutureSwitch
import com.future.sharednav.components.ScreenScaffold
import com.future.sharednav.icons.FutureIcons
import com.future.sharednav.theme.FutureTheme
import com.future.sharednav.theme.FutureTransitions
import com.future.sharednav.theme.FutureTypography
import com.future.sharednav.theme.mutedTextColor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private fun <T> List<T>.after(current: T): T = this[(indexOf(current) + 1).mod(size)]

/**
 * הגדרות המצלמה: כל שורה מחליפה את הערך שלה ב-OK (אין מה לפתוח - שתיים עד
 * ארבע אפשרויות לכל הגדרה). מצב הצילום מציג רק מצבים שהחומרה תומכת בהם.
 */
@Composable
fun CameraSettingsScreen(
    theme: FutureTheme,
    options: CameraOptions,
    availableScenes: List<SceneOption>,
    onChange: (CameraOptions) -> Unit,
) {
    val first = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { first.requestFocus() } }
    Box(modifier = Modifier.fillMaxSize().background(theme.backgroundColor)) {
        ScreenScaffold(
            modifier = Modifier.padding(top = StatusBarInset.TITLE_GAP_DP.dp),
            backgroundColor = theme.backgroundColor,
            title = "הגדרות מצלמה",
            textColor = theme.textColor,
            accentColor = theme.accentColor,
        ) {
            Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 24.dp)) {
                FutureSectionHeader("תמונה", theme)
                FutureCard(theme = theme) {
                    FutureSettingItem(
                        title = "יחס תמונה",
                        summary = options.aspect.label,
                        icon = FutureIcons.AspectRatio,
                        theme = theme,
                        showChevron = false,
                        focusRequester = first,
                        onClick = { onChange(options.copy(aspect = AspectOption.entries.after(options.aspect))) },
                    )
                    FutureDivider(theme = theme)
                    FutureSettingItem(
                        title = "איכות צילום",
                        summary = options.photoQuality.label,
                        icon = FutureIcons.HighQuality,
                        theme = theme,
                        showChevron = false,
                        onClick = { onChange(options.copy(photoQuality = PhotoQualityOption.entries.after(options.photoQuality))) },
                    )
                    // מצבים מיוחדים (לילה, HDR, פורטרט) - רק אם החומרה תומכת.
                    if (availableScenes.size > 1) {
                        FutureDivider(theme = theme)
                        FutureSettingItem(
                            title = "מצב צילום",
                            summary = options.scene.label,
                            icon = FutureIcons.AutoAwesome,
                            theme = theme,
                            showChevron = false,
                            onClick = { onChange(options.copy(scene = availableScenes.after(options.scene))) },
                        )
                    }
                    FutureDivider(theme = theme)
                    FutureSettingItem(
                        title = "טיימר עצמי",
                        summary = if (options.timerSeconds == 0) "כבוי" else "${options.timerSeconds} שניות",
                        icon = FutureIcons.Timer,
                        theme = theme,
                        showChevron = false,
                        onClick = { onChange(options.copy(timerSeconds = listOf(0, 3, 10).after(options.timerSeconds))) },
                    )
                    FutureDivider(theme = theme)
                    FutureSettingItem(
                        title = "קווי רשת",
                        summary = if (options.showGrid) "מוצגים" else "כבויים",
                        icon = FutureIcons.GridOn,
                        theme = theme,
                        showChevron = false,
                        onClick = { onChange(options.copy(showGrid = !options.showGrid)) },
                        trailing = { FutureSwitch(checked = options.showGrid, theme = theme) },
                    )
                }
                FutureSectionHeader("וידאו", theme)
                FutureCard(theme = theme) {
                    FutureSettingItem(
                        title = "איכות וידאו",
                        summary = options.videoQuality.label,
                        icon = FutureIcons.Videocam,
                        theme = theme,
                        showChevron = false,
                        onClick = { onChange(options.copy(videoQuality = VideoQualityOption.entries.after(options.videoQuality))) },
                    )
                }
                FutureSectionHeader("מקשים", theme)
                FutureCard(theme = theme) {
                    KeyHintRow("Options", "החלפת מצלמה", theme)
                    FutureDivider(theme = theme)
                    KeyHintRow("↑ / ↓ מוחזק", "זום", theme)
                    FutureDivider(theme = theme)
                    KeyHintRow("1-9", "נקודת מיקוד · 0 אוטומטי", theme)
                    FutureDivider(theme = theme)
                    KeyHintRow("* / #", "תמונה/וידאו · הבזק", theme)
                }
            }
        }
    }
}

/** שורת מידע (בלי פוקוס) - מקש ומה הוא עושה במצלמה. */
@Composable
private fun KeyHintRow(key: String, action: String, theme: FutureTheme) {
    FutureSettingItem(title = action, summary = key, theme = theme, onClick = null, showChevron = false)
}

/**
 * מציג התמונות האחרונות של המצלמה, בתוך האפליקציה: ימינה/שמאלה בין
 * התמונות (RTL - שמאלה היא הבאה, הישנה יותר), OK פותח בגלריה, BACK חוזר
 * למצלמה (CameraScreen).
 */
@Composable
fun PhotoViewer(theme: FutureTheme, startUri: Uri?) {
    val context = LocalContext.current
    var photos by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var index by remember { mutableIntStateOf(0) }
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    val focus = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        photos = withContext(Dispatchers.IO) { CameraMedia.recentPhotos(context) }
        index = photos.indexOf(startUri).coerceAtLeast(0)
        runCatching { focus.requestFocus() }
    }
    val current = photos.getOrNull(index)
    LaunchedEffect(current) {
        bitmap = current?.let { uri ->
            withContext(Dispatchers.IO) { CameraMedia.screenImage(context, uri, 1280) }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusRequester(focus)
            .focusable()
            .onKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                when (event.key) {
                    Key.DirectionLeft -> { if (index < photos.lastIndex) index++; true }
                    Key.DirectionRight -> { if (index > 0) index--; true }
                    Key.DirectionCenter, Key.Enter -> {
                        current?.let { uri ->
                            runCatching {
                                context.startActivity(
                                    android.content.Intent(android.content.Intent.ACTION_VIEW, uri)
                                        .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                        .addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                )
                            }
                        }
                        true
                    }
                    else -> false
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        AnimatedContent(targetState = bitmap, transitionSpec = { FutureTransitions.appear() }, label = "viewerImage") { shown ->
            if (shown != null) {
                Image(shown.asImageBitmap(), contentDescription = null, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize())
            } else {
                Box(modifier = Modifier.fillMaxSize())
            }
        }
        if (photos.isEmpty()) {
            Text("אין תמונות", color = theme.mutedTextColor, fontSize = FutureTypography.body)
        } else {
            Text(
                "${index + 1}/${photos.size}",
                color = Color.White,
                fontSize = FutureTypography.summary,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 12.dp)
                    .background(ScrimOverPreview, com.future.sharednav.theme.FutureShapes.pill)
                    .padding(horizontal = 12.dp, vertical = 4.dp),
            )
        }
    }
}
