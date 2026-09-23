package com.future.music.ui.screens
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.automirrored.rounded.QueueMusic

import com.future.sharednav.icons.FutureIcons
import com.future.sharednav.components.FutureProgressBar
import com.future.sharednav.components.FutureDialog
import com.future.sharednav.components.FutureButton
import com.future.sharednav.components.FutureListItem
import com.future.sharednav.components.FutureCheckbox
import com.future.sharednav.theme.FutureDimens
import com.future.sharednav.theme.FutureMotion
import com.future.sharednav.theme.idleFieldColor
import com.future.sharednav.theme.readableAccentColor
import com.future.sharednav.theme.onReadableAccentColor
import com.future.sharednav.theme.mutedTextColor

import com.future.sharednav.theme.FutureTypography
import com.future.sharednav.theme.FutureShapes
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import com.future.sharednav.focus.bringIntoViewOnFocus
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import com.future.music.data.Playlist
import com.future.music.playback.PlayerUiState
import com.future.music.ui.components.ScreenTopBar
import com.future.music.ui.components.formatDuration
import com.future.music.ui.components.rememberAlbumArt

import com.future.sharednav.nav.digitForKey
import com.future.sharednav.theme.FutureTheme
import kotlinx.coroutines.delay

@Composable
fun NowPlayingScreen(
    theme: FutureTheme,
    playerState: PlayerUiState,
    isFavorite: Boolean,
    playlists: List<Playlist>,
    onBack: (() -> Unit)?,
    onTick: () -> Unit,
    onTogglePlay: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeekRelative: (Long) -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onToggleFavorite: () -> Unit,
    onTogglePlaylistMembership: (Long) -> Unit,
    onOpenQueue: () -> Unit,
    onOpenSound: () -> Unit,
    onOpenMenu: () -> Unit,
) {
    val song = playerState.currentSong
    var showPlaylistDialog by remember { mutableStateOf(false) }

    LaunchedEffect(playerState.isPlaying) {
        while (playerState.isPlaying) {
            delay(500)
            onTick()
        }
    }

    val focusRequester = remember { FocusRequester() }
    val playButtonFocusRequester = remember { FocusRequester() }
    // כמה כפתורי ניגון ממוקדים כרגע (0 או 1, אבל מונה ולא בוליאני כדי לא
    // להיתפס במרוץ בין "איבוד פוקוס בכפתור הישן" ל"קבלת פוקוס בכפתור החדש").
    // כשאחד מהם ממוקד, שמאל/ימין אמורים לזוז בין הכפתורים הצמודים - לא
    // "לחטוף" תמיד את הלחיצה ל-seek, אחרת אין דרך לזוז ביניהם ב-D-pad בכלל.
    var focusedControlCount by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) { playButtonFocusRequester.requestFocus() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .focusRequester(focusRequester)
            .focusable()
            .onKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                if (song != null) {
                    when (digitForKey(event.key)) {
                        "1" -> { onToggleFavorite(); return@onKeyEvent true }
                        "2" -> { showPlaylistDialog = true; return@onKeyEvent true }
                        "3" -> { onOpenSound(); return@onKeyEvent true }
                        "4" -> { onPrevious(); return@onKeyEvent true }
                        "5" -> { onTogglePlay(); return@onKeyEvent true }
                        "6" -> { onNext(); return@onKeyEvent true }
                        "7" -> { onToggleShuffle(); return@onKeyEvent true }
                        "8" -> { onCycleRepeat(); return@onKeyEvent true }
                        "9" -> { onOpenQueue(); return@onKeyEvent true }
                    }
                    if (focusedControlCount <= 0) {
                        when (event.key) {
                            Key.DirectionLeft -> { onSeekRelative(-10_000); return@onKeyEvent true }
                            Key.DirectionRight -> { onSeekRelative(10_000); return@onKeyEvent true }
                        }
                    }
                }
                false
            }
    ) {
        ScreenTopBar(
            title = "מתנגן כעת",
            theme = theme,
            onBack = onBack,
        )

        val artBitmap = rememberAlbumArt(song?.uri)

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.5f)
                    .aspectRatio(1f)
                    .clip(FutureShapes.xl)
                    .background(theme.accentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                if (artBitmap != null) {
                    Image(bitmap = artBitmap, contentDescription = null, modifier = Modifier.fillMaxSize())
                } else {
                    Icon(FutureIcons.MusicNote, contentDescription = null, tint = theme.accentColor, modifier = Modifier.size(48.dp))
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                song?.title ?: "נגן",
                color = theme.textColor,
                fontSize = FutureTypography.bodyLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                song?.artist ?: "לחצו על מקש Options לבחירת שיר",
                color = theme.textColor.copy(alpha = 0.6f),
                fontSize = FutureTypography.label,
                maxLines = 1,
                textAlign = TextAlign.Center,
            )

            if (song != null) {
                Spacer(modifier = Modifier.height(10.dp))
                val progress = if (playerState.durationMs > 0) (playerState.positionMs.toFloat() / playerState.durationMs.toFloat()).coerceIn(0f, 1f) else 0f
                FutureProgressBar(progress = progress, theme = theme)
                Row(modifier = Modifier.fillMaxWidth().padding(top = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(formatDuration(playerState.positionMs), color = theme.textColor.copy(alpha = 0.5f), fontSize = FutureTypography.caption)
                    Text(formatDuration(playerState.durationMs), color = theme.textColor.copy(alpha = 0.5f), fontSize = FutureTypography.caption)
                }

                val trackControlFocus: (Boolean) -> Unit = { focused -> focusedControlCount += if (focused) 1 else -1 }

                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    RoundIconButton(Icons.Rounded.SkipPrevious, "קודם (4)", theme, size = 42.dp, onClick = onPrevious, onFocusChanged = trackControlFocus)
                    Spacer(modifier = Modifier.width(14.dp))
                    RoundIconButton(
                        if (playerState.isPlaying) FutureIcons.Pause else FutureIcons.PlayArrow,
                        if (playerState.isPlaying) "השהה (5)" else "נגן (5)",
                        theme, size = 54.dp, filled = true, onClick = onTogglePlay,
                        focusRequester = playButtonFocusRequester, onFocusChanged = trackControlFocus,
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    RoundIconButton(Icons.Rounded.SkipNext, "הבא (6)", theme, size = 42.dp, onClick = onNext, onFocusChanged = trackControlFocus)
                }

                Spacer(modifier = Modifier.height(10.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    RoundIconButton(
                        if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                        "מועדפים (1)", theme, size = 32.dp,
                        active = isFavorite,
                        onClick = onToggleFavorite,
                        onFocusChanged = trackControlFocus,
                    )
                    RoundIconButton(Icons.AutoMirrored.Rounded.PlaylistAdd, "הוסף לפלייליסט (2)", theme, size = 32.dp, onClick = { showPlaylistDialog = true }, onFocusChanged = trackControlFocus)
                    RoundIconButton(Icons.Rounded.Equalizer, "סאונד (3)", theme, size = 32.dp, onClick = onOpenSound, onFocusChanged = trackControlFocus)
                    RoundIconButton(
                        Icons.Rounded.Shuffle, "ערבוב (7)", theme, size = 32.dp,
                        active = playerState.shuffleEnabled,
                        onClick = onToggleShuffle,
                        onFocusChanged = trackControlFocus,
                    )
                    RoundIconButton(
                        if (playerState.repeatMode == Player.REPEAT_MODE_ONE) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
                        "חזרה (8)", theme, size = 32.dp,
                        active = playerState.repeatMode != Player.REPEAT_MODE_OFF,
                        onClick = onCycleRepeat,
                        onFocusChanged = trackControlFocus,
                    )
                    RoundIconButton(Icons.AutoMirrored.Rounded.QueueMusic, "תור (9)", theme, size = 32.dp, onClick = onOpenQueue, onFocusChanged = trackControlFocus)
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }
    }

    if (showPlaylistDialog) {
        AddToPlaylistDialog(
            playlists = playlists,
            currentSongId = song?.id,
            theme = theme,
            onToggle = onTogglePlaylistMembership,
            onDismiss = { showPlaylistDialog = false },
        )
    }
}

@Composable
private fun RoundIconButton(
    icon: ImageVector,
    contentDescription: String,
    theme: FutureTheme,
    size: androidx.compose.ui.unit.Dp,
    filled: Boolean = false,
    active: Boolean = false,
    onClick: () -> Unit,
    focusRequester: FocusRequester? = null,
    onFocusChanged: (Boolean) -> Unit = {},
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    // LaunchedEffect(isFocused) יורה גם בקומפוזיציה הראשונה (עם הערך ההתחלתי
    // false, לפני שהכפתור קיבל בכלל פוקוס אמיתי) - בלי השמירה הזו, כל כפתור
    // לא-ממוקד "מדווח" false פעם אחת סתם, ומוריד את focusedControlCount
    // שבמסך ההורה מתחת ל-0 (במקום להישאר על 0), כך שהתנאי focusedControlCount
    // <= 0 תמיד מתקיים - ואז ימין/שמאל תמיד עושים seek, גם כשכפתור כן ממוקד.
    var hasEmittedFocusChange by remember { mutableStateOf(false) }
    LaunchedEffect(isFocused) {
        if (hasEmittedFocusChange || isFocused) onFocusChanged(isFocused)
        hasEmittedFocusChange = true
    }
    // כפתור אייקון של הדיזיין סיסטם (IconButton.jsx): 8% במנוחה, 30% הדגשה
    // בפוקוס. "מלא" (נגן/השהה) הוא הכפתור הראשי - מילוי בהדגשה המתוקנת ודיו
    // שמתאים לה, ובפוקוס מסגרת 2dp בצבע הטקסט. "פעיל" (ערבוב/חזרה) = נבחר,
    // ולכן האייקון בהדגשה.
    val accent = theme.readableAccentColor
    val bg by animateColorAsState(
        when {
            filled -> accent
            isFocused -> accent.copy(alpha = 0.30f)
            else -> theme.idleFieldColor
        },
        FutureMotion.focusColorSpec,
        label = "roundIconBtnBg",
    )
    val tint = if (filled) theme.onReadableAccentColor else if (active) accent else theme.textColor
    val focusBorderColor by animateColorAsState(
        if (isFocused && filled) theme.textColor else androidx.compose.ui.graphics.Color.Transparent,
        FutureMotion.focusColorSpec,
        label = "roundIconBtnFocusBorder",
    )
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(bg)
            .border(width = FutureDimens.focusBorderControl, color = focusBorderColor, shape = CircleShape)
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .focusable(interactionSource = interactionSource).bringIntoViewOnFocus(),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = contentDescription, tint = tint, modifier = Modifier.size(size * 0.45f))
    }
}

/** הוספה לפלייליסטים - דיאלוג של הדיזיין סיסטם; כל פלייליסט הוא שורה עם תיבת סימון בסופה. */
@Composable
private fun AddToPlaylistDialog(
    playlists: List<Playlist>,
    currentSongId: Long?,
    theme: FutureTheme,
    onToggle: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    FutureDialog(
        theme = theme,
        onDismissRequest = onDismiss,
        title = "הוספה לפלייליסט",
        buttons = { FutureButton("סגור", theme, onDismiss) },
    ) {
        if (playlists.isEmpty()) {
            Text(
                "אין פלייליסטים",
                color = theme.mutedTextColor,
                fontSize = FutureTypography.body,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(FutureDimens.spacingXs)) {
                playlists.forEach { playlist ->
                    val checked = currentSongId != null && currentSongId in playlist.songIds
                    FutureListItem(
                        title = playlist.name,
                        theme = theme,
                        onClick = { onToggle(playlist.id) },
                        trailing = { FutureCheckbox(checked, theme) },
                    )
                }
            }
        }
    }
}
