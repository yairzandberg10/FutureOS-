package com.future.music.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
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
import com.future.music.ui.digitForKey
import com.future.music.ui.theme.FutureTheme
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
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

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
                    when (event.key) {
                        Key.DirectionLeft -> { onSeekRelative(-10_000); return@onKeyEvent true }
                        Key.DirectionRight -> { onSeekRelative(10_000); return@onKeyEvent true }
                    }
                }
                false
            }
    ) {
        ScreenTopBar(
            title = "מתנגן כעת",
            theme = theme,
            onBack = onBack,
            trailingIcon = Icons.Rounded.Menu,
            trailingContentDescription = "תפריט ראשי",
            onTrailingClick = onOpenMenu,
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
                    .clip(RoundedCornerShape(20.dp))
                    .background(theme.accentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                if (artBitmap != null) {
                    Image(bitmap = artBitmap, contentDescription = null, modifier = Modifier.fillMaxSize())
                } else {
                    Icon(Icons.Rounded.MusicNote, contentDescription = null, tint = theme.accentColor, modifier = Modifier.size(48.dp))
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                song?.title ?: "נגן",
                color = theme.textColor,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                song?.artist ?: "לחצו על מקש Options לבחירת שיר",
                color = theme.textColor.copy(alpha = 0.6f),
                fontSize = 12.sp,
                maxLines = 1,
                textAlign = TextAlign.Center,
            )

            if (song != null) {
                Spacer(modifier = Modifier.height(10.dp))
                val progress = if (playerState.durationMs > 0) (playerState.positionMs.toFloat() / playerState.durationMs.toFloat()).coerceIn(0f, 1f) else 0f
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                    color = theme.accentColor,
                    trackColor = theme.textColor.copy(alpha = 0.12f),
                )
                Row(modifier = Modifier.fillMaxWidth().padding(top = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(formatDuration(playerState.positionMs), color = theme.textColor.copy(alpha = 0.5f), fontSize = 10.sp)
                    Text(formatDuration(playerState.durationMs), color = theme.textColor.copy(alpha = 0.5f), fontSize = 10.sp)
                }

                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    RoundIconButton(Icons.Rounded.SkipPrevious, "קודם (4)", theme, size = 42.dp, onClick = onPrevious)
                    Spacer(modifier = Modifier.width(14.dp))
                    RoundIconButton(
                        if (playerState.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        if (playerState.isPlaying) "השהה (5)" else "נגן (5)",
                        theme, size = 54.dp, filled = true, onClick = onTogglePlay,
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    RoundIconButton(Icons.Rounded.SkipNext, "הבא (6)", theme, size = 42.dp, onClick = onNext)
                }

                Spacer(modifier = Modifier.height(10.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    RoundIconButton(
                        if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                        "מועדפים (1)", theme, size = 32.dp,
                        active = isFavorite,
                        onClick = onToggleFavorite,
                    )
                    RoundIconButton(Icons.AutoMirrored.Rounded.PlaylistAdd, "הוסף לפלייליסט (2)", theme, size = 32.dp, onClick = { showPlaylistDialog = true })
                    RoundIconButton(Icons.Rounded.Equalizer, "סאונד (3)", theme, size = 32.dp, onClick = onOpenSound)
                    RoundIconButton(
                        Icons.Rounded.Shuffle, "ערבוב (7)", theme, size = 32.dp,
                        active = playerState.shuffleEnabled,
                        onClick = onToggleShuffle,
                    )
                    RoundIconButton(
                        if (playerState.repeatMode == Player.REPEAT_MODE_ONE) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
                        "חזרה (8)", theme, size = 32.dp,
                        active = playerState.repeatMode != Player.REPEAT_MODE_OFF,
                        onClick = onCycleRepeat,
                    )
                    RoundIconButton(Icons.AutoMirrored.Rounded.QueueMusic, "תור (9)", theme, size = 32.dp, onClick = onOpenQueue)
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
) {
    val bg = when {
        filled -> theme.accentColor
        active -> theme.accentColor.copy(alpha = 0.3f)
        else -> theme.textColor.copy(alpha = 0.08f)
    }
    val tint = if (filled) theme.backgroundColor else if (active) theme.accentColor else theme.textColor
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(bg)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = contentDescription, tint = tint, modifier = Modifier.size(size * 0.45f))
    }
}

@Composable
private fun AddToPlaylistDialog(
    playlists: List<Playlist>,
    currentSongId: Long?,
    theme: FutureTheme,
    onToggle: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = theme.surfaceColor,
        title = { Text("הוספה לפלייליסט", color = theme.textColor) },
        text = {
            if (playlists.isEmpty()) {
                Text("אין עדיין פלייליסטים - צרו אחד ממסך הפלייליסטים", color = theme.textColor.copy(alpha = 0.6f), fontSize = 13.sp)
            } else {
                Column {
                    playlists.forEach { playlist ->
                        val checked = currentSongId != null && currentSongId in playlist.songIds
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onToggle(playlist.id) }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(
                                checked = checked,
                                onCheckedChange = { onToggle(playlist.id) },
                                colors = CheckboxDefaults.colors(checkedColor = theme.accentColor),
                            )
                            Text(playlist.name, color = theme.textColor, fontSize = 14.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) { Text("סגור", color = theme.accentColor) }
        },
    )
}
