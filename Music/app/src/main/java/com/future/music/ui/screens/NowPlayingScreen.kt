package com.future.music.ui.screens

import com.future.sharednav.icons.FutureIcons
import com.future.sharednav.components.FutureProgressBar
import com.future.sharednav.components.FutureDialog
import com.future.sharednav.components.FutureButton
import com.future.sharednav.components.FutureListItem
import com.future.sharednav.components.FutureCheckbox
import com.future.sharednav.components.FutureActionCell
import androidx.compose.ui.focus.onFocusChanged
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
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.ui.layout.ContentScale
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
    onOpenDevices: () -> Unit,
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
    var focusedControl by remember { mutableStateOf<String?>(null) }
    // פוקוס אוטומטי על נגן/השהה - וגם כשהשיר הראשון נטען אחרי שהמסך כבר
    // פתוח. בלי שיר אין כפתורים, והפוקוס נשאר על המסך עצמו (מקשי הספרות).
    LaunchedEffect(song != null) {
        runCatching { if (song != null) playButtonFocusRequester.requestFocus() else focusRequester.requestFocus() }
    }

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
                        "9" -> { onOpenDevices(); return@onKeyEvent true }
                    }
                    if (focusedControl == null) {
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

        // בלי גלילה: כל הפקדים בגובה קבוע, ותמונת האלבום לוקחת את כל הגובה
        // שנשאר (ריבוע, עד רוחב המסך).
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .aspectRatio(1f, matchHeightConstraintsFirst = true)
                        .clip(FutureShapes.xl)
                        .background(theme.accentColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center,
                ) {
                    if (artBitmap != null) {
                        Image(bitmap = artBitmap, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                    } else {
                        Icon(FutureIcons.MusicNote, contentDescription = null, tint = theme.accentColor, modifier = Modifier.size(56.dp))
                    }
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
                fun track(key: String): (Boolean) -> Unit = { focused ->
                    if (focused) focusedControl = key else if (focusedControl == key) focusedControl = null
                }

                // פקדי ניגון לא מתהפכים ב-RTL (כמו בכל נגן): הקודם משמאל, הבא
                // מימין, כמו מקשי 4 ו-6, וההתקדמות זורמת משמאל לימין.
                androidx.compose.runtime.CompositionLocalProvider(
                    androidx.compose.ui.platform.LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Ltr,
                ) {
                Column {
                // זמנים משני צדי הפס, בשורה אחת - חוסך גובה לתמונה.
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(formatDuration(playerState.positionMs), color = theme.textColor.copy(alpha = 0.5f), fontSize = FutureTypography.caption)
                    FutureProgressBar(progress = progress, theme = theme, modifier = Modifier.weight(1f).padding(horizontal = 8.dp))
                    Text(formatDuration(playerState.durationMs), color = theme.textColor.copy(alpha = 0.5f), fontSize = FutureTypography.caption)
                }

                Spacer(modifier = Modifier.height(10.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    RoundIconButton(FutureIcons.SkipPrevious, "קודם (4)", theme, size = 50.dp, onClick = onPrevious, onFocusChanged = track("prev"))
                    Spacer(modifier = Modifier.width(24.dp))
                    RoundIconButton(
                        if (playerState.isPlaying) FutureIcons.Pause else FutureIcons.PlayArrow,
                        if (playerState.isPlaying) "השהה (5)" else "נגן (5)",
                        theme, size = 58.dp, filled = true, onClick = onTogglePlay,
                        focusRequester = playButtonFocusRequester, onFocusChanged = track("play"),
                    )
                    Spacer(modifier = Modifier.width(24.dp))
                    RoundIconButton(FutureIcons.SkipNext, "הבא (6)", theme, size = 50.dp, onClick = onNext, onFocusChanged = track("next"))
                }
                }
                }

                // שאר הפעולות - שורה אחת של כפתורים עגולים מתחת לנגן והדילוג.
                Spacer(modifier = Modifier.height(12.dp))
                val actions = listOf(
                    NowPlayingAction("fav", if (isFavorite) FutureIcons.Favorite else FutureIcons.FavoriteBorder, "מועדף (1)", isFavorite, onToggleFavorite),
                    NowPlayingAction("playlist", FutureIcons.AutoMirrored.PlaylistAdd, "פלייליסט (2)", false) { showPlaylistDialog = true },
                    NowPlayingAction("sound", FutureIcons.Equalizer, "סאונד (3)", false, onOpenSound),
                    NowPlayingAction("shuffle", FutureIcons.Shuffle, "ערבוב (7)", playerState.shuffleEnabled, onToggleShuffle),
                    NowPlayingAction(
                        "repeat",
                        if (playerState.repeatMode == Player.REPEAT_MODE_ONE) FutureIcons.RepeatOne else FutureIcons.Repeat,
                        "חזרה (8)", playerState.repeatMode != Player.REPEAT_MODE_OFF, onCycleRepeat,
                    ),
                    NowPlayingAction("bt", FutureIcons.Bluetooth, "Bluetooth (9)", false, onOpenDevices),
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    actions.forEach { action ->
                        RoundIconButton(
                            action.icon, action.label, theme, size = 40.dp,
                            active = action.active, onClick = action.onClick, onFocusChanged = track(action.key),
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
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

private data class NowPlayingAction(
    val key: String,
    val icon: ImageVector,
    val label: String,
    val active: Boolean,
    val onClick: () -> Unit,
)

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
    // פוקוס כמו בשאר המערכת (פקדי השיחה): מסגרת 2dp בהדגשה, בלי שינוי מילוי.
    val bg by animateColorAsState(
        when {
            filled -> accent
            active -> accent.copy(alpha = 0.20f)
            else -> theme.idleFieldColor
        },
        FutureMotion.focusColorSpec,
        label = "roundIconBtnBg",
    )
    val tint = if (filled) theme.onReadableAccentColor else if (active) accent else theme.textColor
    // פוקוס = מסגרת בהדגשה. בכפתור המלא (נגן/השהה) זו טבעת עם רווח מסביב
    // לעיגול, כדי שתיראה גם כשההדגשה לבנה והמילוי לבן.
    val focusBorderColor by animateColorAsState(
        if (isFocused) accent else androidx.compose.ui.graphics.Color.Transparent,
        FutureMotion.focusColorSpec,
        label = "roundIconBtnFocusBorder",
    )
    val ringGap = if (filled) 4.dp else 0.dp
    Box(
        modifier = Modifier
            .size(size + ringGap * 2)
            .border(width = FutureDimens.focusBorderControl, color = focusBorderColor, shape = CircleShape)
            .padding(ringGap)
            .clip(CircleShape)
            .background(bg)
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
