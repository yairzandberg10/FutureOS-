package com.future.music.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import com.future.music.data.AlbumGroup
import com.future.music.data.ArtistGroup
import com.future.music.data.PlaylistStore
import com.future.music.data.SongRepository
import com.future.music.playback.PlayerController
import com.future.music.ui.components.ConfirmDialog
import com.future.music.ui.screens.AlbumsScreen
import com.future.music.ui.screens.ArtistsScreen
import com.future.music.ui.screens.HomeScreen
import com.future.music.ui.screens.NowPlayingScreen
import com.future.music.ui.screens.PlaylistsScreen
import com.future.music.ui.screens.QueueScreen
import com.future.music.ui.screens.SearchScreen
import com.future.music.ui.screens.SongListScreen
import com.future.music.ui.screens.SoundScreen
import com.future.sharednav.theme.FutureTheme
import kotlinx.coroutines.delay

@Composable
fun MusicNavHost(
    repository: SongRepository,
    playlistStore: PlaylistStore,
    playerController: PlayerController,
    theme: FutureTheme,
) {
    val backStack = remember { mutableStateListOf<Route>(Route.NowPlaying) }
    val current = backStack.last()
    var dataVersion by remember { mutableIntStateOf(0) }
    var restoredLastQueue by remember { mutableStateOf(false) }
    // הפריט (לפי digit) שנפתח לאחרונה מתפריט הבית - כשחוזרים "אחורה", הפוקוס
    // צריך לשוב אליו בדיוק, לא תמיד לפריט הראשון בתפריט.
    var lastOpenedHomeItemId by remember { mutableStateOf<String?>(null) }

    BackHandler(enabled = backStack.size > 1) { backStack.removeAt(backStack.lastIndex) }
    fun push(route: Route) = backStack.add(route)
    fun pop() { if (backStack.size > 1) backStack.removeAt(backStack.lastIndex) }

    val allSongs = rememberIoState(Unit) { repository.getAllSongs() } ?: emptyList()
    val playerState = playerController.state

    val artists = remember(allSongs) {
        allSongs.groupBy { it.artist }
            .map { (name, songs) -> ArtistGroup(name, songs.size) }
            .sortedBy { it.name }
    }
    val albums = remember(allSongs) {
        allSongs.groupBy { it.albumId }
            .map { (id, songs) -> AlbumGroup(id, songs.first().album, songs.first().artist, songs.size) }
            .sortedBy { it.name }
    }
    val favoriteIds = remember(dataVersion) { playlistStore.getFavoriteIds() }
    val favorites = remember(allSongs, favoriteIds) { allSongs.filter { it.id in favoriteIds } }
    val playlists = remember(dataVersion) { playlistStore.getPlaylists() }

    // שחזור התור מהפעם הקודמת - רק פעם אחת, ורק אם עדיין אין ניגון פעיל
    // (למשל אם ה-service כבר רץ מריצה קודמת של התהליך).
    LaunchedEffect(allSongs, playerState.isConnected) {
        if (!restoredLastQueue && allSongs.isNotEmpty() && playerState.isConnected && playerState.currentSong == null) {
            restoredLastQueue = true
            val last = playlistStore.getLastQueue()
            if (last != null) {
                val songs = last.songIds.mapNotNull { id -> allSongs.find { it.id == id } }
                if (songs.isNotEmpty()) {
                    playerController.restoreQueue(songs, last.index.coerceIn(0, songs.lastIndex), last.positionMs)
                }
            }
        }
    }

    // שמירת מיקום/תור תקופתית כדי לתמוך בהמשך-מהיכן-שהפסקת בפתיחה הבאה.
    LaunchedEffect(playerState.currentSong?.id, playerState.queue.size) {
        val s = playerController.state
        if (s.queue.isNotEmpty()) playlistStore.saveLastQueue(s.queue.map { it.id }, s.currentIndex, s.positionMs)
    }
    LaunchedEffect(playerState.isPlaying) {
        while (playerController.state.isPlaying) {
            delay(5000)
            playerController.tick()
            val s = playerController.state
            if (s.queue.isNotEmpty()) playlistStore.saveLastQueue(s.queue.map { it.id }, s.currentIndex, s.positionMs)
        }
    }

    fun toggleFavorite(songId: Long) {
        playlistStore.toggleFavorite(songId)
        dataVersion++
    }

    fun togglePlaylistMembership(playlistId: Long, songId: Long) {
        val playlist = playlistStore.getPlaylists().find { it.id == playlistId }
        if (playlist != null && songId in playlist.songIds) {
            playlistStore.removeFromPlaylist(playlistId, songId)
        } else {
            playlistStore.addToPlaylist(playlistId, songId)
        }
        dataVersion++
    }

    // מקש Options פותח מכל מקום באפליקציה את התפריט הראשי (כל השירים/אמנים/
    // אלבומים/פלייליסטים/מועדפים/חיפוש) - עולה למעלה כי המסכים עצמם לא
    // מטפלים ב-Key.Menu, אז האירוע מבעבע לכאן מהילד הממוקד בפועל.
    fun openMainMenu() {
        if (current !is Route.Home) push(Route.Home)
    }

    // מקש Options הפיזי תמיד נחסם ברמת המערכת (FutureUI's StatusBarAccessibilityService
    // צורך אותו ללחיצה ארוכה ל"אפליקציות אחרונות") - שום Key.Menu לא באמת מגיע
    // לאפליקציה. לחיצה קצרה על אותו מקש משודרת בשידור גלובלי, ואנחנו מאזינים
    // לו כאן כדי לפתוח את התפריט הראשי - זו הדרך האמיתית שהמקש הפיזי עובד.
    val context = LocalContext.current
    DisposableEffect(Unit) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                openMainMenu()
            }
        }
        val filter = IntentFilter("com.future.futureui.ACTION_OPTIONS_SHORT_PRESS")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            context.registerReceiver(receiver, filter)
        }
        onDispose { context.unregisterReceiver(receiver) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown && event.key == Key.Menu) {
                    openMainMenu()
                    true
                } else {
                    false
                }
            }
    ) {
    when (val route = current) {
        is Route.Home -> HomeScreen(
            theme = theme,
            playerState = playerState,
            onOpenAllSongs = { lastOpenedHomeItemId = "1"; push(Route.AllSongs) },
            onOpenArtists = { lastOpenedHomeItemId = "2"; push(Route.Artists) },
            onOpenAlbums = { lastOpenedHomeItemId = "3"; push(Route.Albums) },
            onOpenPlaylists = { lastOpenedHomeItemId = "4"; push(Route.Playlists) },
            onOpenFavorites = { lastOpenedHomeItemId = "5"; push(Route.Favorites) },
            onOpenSearch = { lastOpenedHomeItemId = "6"; push(Route.Search) },
            onOpenNowPlaying = { push(Route.NowPlaying) },
            onTogglePlay = playerController::togglePlayPause,
            lastOpenedItemId = lastOpenedHomeItemId,
        )

        is Route.AllSongs -> SongListScreen(
            title = "כל השירים",
            songs = allSongs,
            theme = theme,
            playerState = playerState,
            onBack = ::pop,
            onPlaySong = { index -> playerController.playQueue(allSongs, index) },
            onOpenNowPlaying = { push(Route.NowPlaying) },
            onTogglePlay = playerController::togglePlayPause,
            emptyMessage = "לא נמצאה מוזיקה בטלפון",
        )

        is Route.Artists -> ArtistsScreen(
            artists = artists,
            theme = theme,
            onBack = ::pop,
            onOpenArtist = { name -> push(Route.ArtistSongs(name)) },
        )

        is Route.ArtistSongs -> {
            val songs = remember(allSongs, route.artist) { allSongs.filter { it.artist == route.artist } }
            SongListScreen(
                title = route.artist,
                songs = songs,
                theme = theme,
                playerState = playerState,
                onBack = ::pop,
                onPlaySong = { index -> playerController.playQueue(songs, index) },
                onOpenNowPlaying = { push(Route.NowPlaying) },
                onTogglePlay = playerController::togglePlayPause,
            )
        }

        is Route.Albums -> AlbumsScreen(
            albums = albums,
            theme = theme,
            onBack = ::pop,
            onOpenAlbum = { id, name -> push(Route.AlbumSongs(id, name)) },
        )

        is Route.AlbumSongs -> {
            val songs = remember(allSongs, route.albumId) { allSongs.filter { it.albumId == route.albumId } }
            SongListScreen(
                title = route.name,
                songs = songs,
                theme = theme,
                playerState = playerState,
                onBack = ::pop,
                onPlaySong = { index -> playerController.playQueue(songs, index) },
                onOpenNowPlaying = { push(Route.NowPlaying) },
                onTogglePlay = playerController::togglePlayPause,
            )
        }

        is Route.Playlists -> PlaylistsScreen(
            playlists = playlists,
            theme = theme,
            onBack = ::pop,
            onOpenPlaylist = { push(Route.PlaylistSongs(it.id, it.name)) },
            onCreatePlaylist = { name ->
                playlistStore.createPlaylist(name)
                dataVersion++
            },
        )

        is Route.PlaylistSongs -> {
            var showDeleteConfirm by remember(route.playlistId) { mutableStateOf(false) }
            val playlist = playlists.find { it.id == route.playlistId }
            val songs = remember(allSongs, playlist) {
                playlist?.songIds?.mapNotNull { id -> allSongs.find { it.id == id } } ?: emptyList()
            }
            SongListScreen(
                title = route.name,
                songs = songs,
                theme = theme,
                playerState = playerState,
                onBack = ::pop,
                onPlaySong = { index -> playerController.playQueue(songs, index) },
                onOpenNowPlaying = { push(Route.NowPlaying) },
                onTogglePlay = playerController::togglePlayPause,
                emptyMessage = "אין שירים בפלייליסט הזה עדיין - הוסיפו ממסך הניגון",
                topBarTrailingIcon = Icons.Rounded.DeleteOutline,
                topBarTrailingDescription = "מחק פלייליסט",
                onTopBarTrailingClick = { showDeleteConfirm = true },
            )
            if (showDeleteConfirm) {
                ConfirmDialog(
                    title = "מחיקת פלייליסט",
                    message = "למחוק את \"${route.name}\"? הפעולה לא ניתנת לביטול.",
                    theme = theme,
                    onDismiss = { showDeleteConfirm = false },
                    onConfirm = {
                        playlistStore.deletePlaylist(route.playlistId)
                        dataVersion++
                        showDeleteConfirm = false
                        pop()
                    },
                )
            }
        }

        is Route.Favorites -> SongListScreen(
            title = "מועדפים",
            songs = favorites,
            theme = theme,
            playerState = playerState,
            onBack = ::pop,
            onPlaySong = { index -> playerController.playQueue(favorites, index) },
            onOpenNowPlaying = { push(Route.NowPlaying) },
            onTogglePlay = playerController::togglePlayPause,
            emptyMessage = "אין עדיין שירים מועדפים",
        )

        is Route.Search -> SearchScreen(
            allSongs = allSongs,
            theme = theme,
            playerState = playerState,
            onBack = ::pop,
            onPlayResults = { songs, index -> playerController.playQueue(songs, index) },
            onOpenNowPlaying = { push(Route.NowPlaying) },
            onTogglePlay = playerController::togglePlayPause,
        )

        is Route.NowPlaying -> {
            val currentSongId = playerState.currentSong?.id
            NowPlayingScreen(
                theme = theme,
                playerState = playerState,
                isFavorite = currentSongId != null && currentSongId in favoriteIds,
                playlists = playlists,
                onBack = if (backStack.size > 1) ::pop else null,
                onTick = playerController::tick,
                onTogglePlay = playerController::togglePlayPause,
                onNext = playerController::skipToNext,
                onPrevious = playerController::skipToPrevious,
                onSeekRelative = playerController::seekRelative,
                onToggleShuffle = playerController::toggleShuffle,
                onCycleRepeat = playerController::cycleRepeatMode,
                onToggleFavorite = { currentSongId?.let(::toggleFavorite) },
                onTogglePlaylistMembership = { playlistId -> currentSongId?.let { togglePlaylistMembership(playlistId, it) } },
                onOpenQueue = { push(Route.Queue) },
                onOpenSound = { push(Route.Sound) },
                onOpenMenu = ::openMainMenu,
            )
        }

        is Route.Queue -> QueueScreen(
            playerState = playerState,
            theme = theme,
            onBack = ::pop,
            onPlayAt = playerController::playAtQueueIndex,
        )

        is Route.Sound -> SoundScreen(
            theme = theme,
            onBack = ::pop,
            onSelectPreset = playerController::setEqPreset,
        )
    }
    }
}
