package com.future.recorder.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.text.format.DateFormat
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.future.recorder.data.RecorderService
import com.future.recorder.data.RecorderStatus
import com.future.recorder.data.Recording
import com.future.recorder.data.Recordings
import com.future.recorder.data.formatDuration
import com.future.sharednav.components.AvatarListSize
import com.future.sharednav.components.ConfirmDialog
import com.future.sharednav.components.EmptyState
import com.future.sharednav.components.FutureAvatar
import com.future.sharednav.components.FutureButton
import com.future.sharednav.components.FutureButtonVariant
import com.future.sharednav.components.FutureListItem
import com.future.sharednav.components.FutureMenuRow
import com.future.sharednav.components.FutureOptionsMenu
import com.future.sharednav.components.InputDialog
import com.future.sharednav.components.ScreenTopBar
import com.future.sharednav.icons.FutureIcons
import com.future.sharednav.nav.onOptionsKeyPress
import com.future.sharednav.share.FutureShare
import com.future.sharednav.theme.FutureDimens
import com.future.sharednav.theme.FutureTheme
import com.future.sharednav.theme.FutureTypography
import com.future.sharednav.theme.mutedTextColor
import com.future.sharednav.theme.readableAccentColor
import com.future.sharednav.theme.subtleTextColor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun RecorderHomeScreen(theme: FutureTheme, onPlay: (File) -> Unit) {
    val context = LocalContext.current
    val state by RecorderService.state.collectAsState()
    var recordings by remember { mutableStateOf<List<Recording>?>(null) }
    var reloadKey by remember { mutableStateOf(0) }
    LaunchedEffect(state.savedCount, reloadKey) {
        recordings = withContext(Dispatchers.IO) { Recordings.list(context) }
    }
    LaunchedEffect(state.error) {
        state.error?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            RecorderService.clearError()
        }
    }

    // השעון רץ לפי הפריים רק בזמן הקלטה - השירות לא משדר כל עשירית שנייה
    var now by remember { mutableLongStateOf(android.os.SystemClock.uptimeMillis()) }
    LaunchedEffect(state.status) {
        while (state.status == RecorderStatus.Recording) {
            withFrameMillis { now = android.os.SystemClock.uptimeMillis() }
        }
    }

    var pendingStart by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted && pendingStart) RecorderService.send(context, RecorderService.ACTION_START)
        else if (!granted) Toast.makeText(context, "נדרשת הרשאת מיקרופון כדי להקליט", Toast.LENGTH_LONG).show()
        pendingStart = false
    }
    fun startRecording() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            RecorderService.send(context, RecorderService.ACTION_START)
        } else {
            pendingStart = true
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    val mainButton = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { mainButton.requestFocus() } }

    var focused by remember { mutableStateOf<Recording?>(null) }
    var menuFor by remember { mutableStateOf<Recording?>(null) }
    var renaming by remember { mutableStateOf<Recording?>(null) }
    var deleting by remember { mutableStateOf<Recording?>(null) }
    onOptionsKeyPress { menuFor = if (menuFor == null) focused else null }

    val listState = rememberLazyListState()

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Column(modifier = Modifier.fillMaxSize()) {
            ScreenTopBar(title = "רשמקול", textColor = theme.textColor, accentColor = theme.accentColor)
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = FutureDimens.screenPadding, vertical = FutureDimens.spacingXs),
                verticalArrangement = Arrangement.spacedBy(FutureDimens.spacingSm),
            ) {
                item(key = "recorder") {
                    RecorderPanel(
                        theme = theme,
                        status = state.status,
                        elapsedMs = state.elapsedMs(now),
                        levels = state.levels,
                        mainButton = mainButton,
                        onStart = ::startRecording,
                        onPause = { RecorderService.send(context, RecorderService.ACTION_PAUSE) },
                        onResume = { RecorderService.send(context, RecorderService.ACTION_RESUME) },
                        onStop = { RecorderService.send(context, RecorderService.ACTION_STOP) },
                        modifier = Modifier.onFocusChanged { if (it.hasFocus) focused = null },
                    )
                }
                // הקובץ שמוקלט עכשיו כבר קיים בתיקייה - בלי הסינון הוא הופיע ברשימה
                // אחרי רענון (שינוי שם/מחיקה תוך כדי הקלטה) ואפשר היה להשמיע או למחוק אותו.
                val list = recordings?.filter { it.file != state.file }
                if (list != null && list.isEmpty() && state.status == RecorderStatus.Idle) {
                    item(key = "empty") {
                        EmptyState(
                            icon = FutureIcons.Mic,
                            title = "אין הקלטות עדיין",
                            subtitle = "OK על \"הקלטה\" כדי להתחיל",
                            textColor = theme.textColor,
                            modifier = Modifier.padding(top = FutureDimens.spacingLg),
                        )
                    }
                }
                if (!list.isNullOrEmpty()) {
                    item(key = "header") {
                        Text(
                            "הקלטות (${list.size})",
                            color = theme.mutedTextColor,
                            fontSize = FutureTypography.label,
                            fontWeight = FutureTypography.weightMedium,
                            modifier = Modifier.padding(top = FutureDimens.spacingMd, start = FutureDimens.spacingXs),
                        )
                    }
                    items(list, key = { it.file.path }) { rec ->
                        FutureListItem(
                            title = rec.name,
                            summary = "${formatDate(context, rec.modified)} · ${formatDuration(rec.durationMs)}",
                            theme = theme,
                            onClick = { onPlay(rec.file) },
                            leading = { FutureAvatar(theme = theme, icon = FutureIcons.GraphicEq, size = AvatarListSize) },
                            modifier = Modifier.onFocusChanged { if (it.isFocused) focused = rec },
                        )
                    }
                    item(key = "hint") {
                        Text(
                            "OK השמעה · אפשרויות: שינוי שם, שיתוף, מחיקה",
                            color = theme.subtleTextColor,
                            fontSize = FutureTypography.caption,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(vertical = FutureDimens.spacingMd),
                        )
                    }
                }
            }
        }
    }

    menuFor?.let { rec ->
        FutureOptionsMenu(theme = theme, onDismissRequest = { menuFor = null }, header = rec.name) {
            FutureMenuRow("השמע", FutureIcons.PlayArrow, theme, { menuFor = null; onPlay(rec.file) })
            FutureMenuRow("שנה שם", FutureIcons.DriveFileRenameOutline, theme, { menuFor = null; renaming = rec })
            FutureMenuRow("שתף", FutureIcons.Share, theme, { menuFor = null; shareRecording(context, rec.file) })
            FutureMenuRow("מחק", FutureIcons.Delete, theme, { menuFor = null; deleting = rec }, destructive = true)
        }
    }
    renaming?.let { rec ->
        InputDialog(
            title = "שינוי שם",
            theme = theme,
            initialValue = rec.name,
            onDismiss = { renaming = null },
            onConfirm = { name ->
                renaming = null
                if (Recordings.rename(rec.file, name) == null) {
                    Toast.makeText(context, "השם לא חוקי או כבר קיים", Toast.LENGTH_SHORT).show()
                }
                reloadKey++
            },
        )
    }
    deleting?.let { rec ->
        ConfirmDialog(
            message = "למחוק את \"${rec.name}\"?",
            theme = theme,
            onCancel = { deleting = null },
            onConfirm = {
                deleting = null
                Recordings.delete(rec.file)
                reloadKey++
            },
        )
    }
}

@Composable
private fun RecorderPanel(
    theme: FutureTheme,
    status: RecorderStatus,
    elapsedMs: Long,
    levels: List<Float>,
    mainButton: FocusRequester,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            when (status) {
                RecorderStatus.Idle -> "מוכן להקלטה"
                RecorderStatus.Recording -> "מקליט"
                RecorderStatus.Paused -> "מושהה"
            },
            color = if (status == RecorderStatus.Recording) theme.dangerColor else theme.mutedTextColor,
            fontSize = FutureTypography.label,
            fontWeight = FutureTypography.weightMedium,
            modifier = Modifier.padding(top = FutureDimens.spacingSm),
        )
        Text(
            formatDuration(elapsedMs),
            color = theme.textColor,
            fontSize = FutureTypography.hero,
            fontWeight = FutureTypography.weightLight,
            fontFamily = FutureTypography.monoFamily,
        )
        LevelBars(levels = levels, active = status == RecorderStatus.Recording, theme = theme)
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = FutureDimens.spacingMd, bottom = FutureDimens.spacingSm),
            horizontalArrangement = Arrangement.spacedBy(FutureDimens.spacingSm),
        ) {
            when (status) {
                RecorderStatus.Idle -> FutureButton("הקלטה", theme, onStart, fillMaxWidth = true, focusRequester = mainButton)
                RecorderStatus.Recording, RecorderStatus.Paused -> {
                    FutureButton(
                        "עצור ושמור", theme, onStop,
                        modifier = Modifier.weight(1f), fillMaxWidth = true, focusRequester = mainButton,
                    )
                    FutureButton(
                        if (status == RecorderStatus.Recording) "השהה" else "המשך", theme,
                        if (status == RecorderStatus.Recording) onPause else onResume,
                        modifier = Modifier.weight(1f), variant = FutureButtonVariant.Secondary, fillMaxWidth = true,
                    )
                }
            }
        }
    }
    // כשהמצב מתחלף הכפתור הממוקד נעלם ואחר בא במקומו - הפוקוס עובר לכפתור הראשי
    LaunchedEffect(status) { runCatching { mainButton.requestFocus() } }
}

/** גלי קול: עמודה לכל דגימה, החדשה ביותר מימין (תחילת השורה ב-RTL). */
@Composable
private fun LevelBars(levels: List<Float>, active: Boolean, theme: FutureTheme) {
    val color = if (active) theme.readableAccentColor else theme.textColor.copy(alpha = 0.25f)
    val idle = theme.textColor.copy(alpha = 0.12f)
    Canvas(modifier = Modifier.fillMaxWidth().height(56.dp).padding(horizontal = FutureDimens.spacingSm)) {
        val bars = 48
        val step = size.width / bars
        val stroke = step * 0.55f
        val mid = size.height / 2f
        for (i in 0 until bars) {
            // i=0 בקצה הימני = הדגימה האחרונה
            val level = levels.getOrNull(levels.size - 1 - i)
            val x = size.width - step * (i + 0.5f)
            val h = ((level ?: 0f) * size.height * 0.9f).coerceAtLeast(stroke)
            drawLine(
                if (level == null) idle else color,
                Offset(x, mid - h / 2f), Offset(x, mid + h / 2f),
                strokeWidth = stroke, cap = StrokeCap.Round,
            )
        }
    }
}

private fun formatDate(context: Context, millis: Long): String {
    val today = DateFormat.format("yyyyMMdd", System.currentTimeMillis())
    val day = DateFormat.format("yyyyMMdd", millis)
    val time = DateFormat.getTimeFormat(context).format(java.util.Date(millis))
    return if (today == day) "היום $time" else "${DateFormat.format("dd.MM.yy", millis)} $time"
}

internal fun shareRecording(context: Context, file: File) {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
    val send = Intent(Intent.ACTION_SEND)
        .setType("audio/mp4")
        .putExtra(Intent.EXTRA_STREAM, uri)
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    send.clipData = android.content.ClipData.newRawUri(file.nameWithoutExtension, uri)
    FutureShare.open(context, send, "שיתוף הקלטה")
}
