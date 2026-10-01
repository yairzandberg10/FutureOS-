package com.future.music.ui

import com.future.sharednav.icons.FutureIcons
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.runtime.Composable
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
import com.future.music.data.AlbumGroup
import com.future.music.data.ArtistGroup
import com.future.music.data.PlaylistStore
import com.future.music.data.SongRepository
import com.future.music.playback.PlayerController
import com.future.music.ui.components.ConfirmDialog
import com.future.music.ui.components.NameInputDialog
import androidx.compose.runtime.DisposableEffect
import com.future.music.ui.screens.AlbumsScreen
import com.future.music.ui.screens.ArtistsScreen
import com.future.music.ui.screens.HomeScreen
import com.future.music.ui.screens.NowPlayingScreen
import com.future.music.ui.screens.PlaylistsScreen
import com.future.music.ui.screens.QueueScreen
import com.future.music.ui.screens.SearchScreen
import com.future.music.ui.screens.SongListScreen
import com.future.music.ui.screens.EqualizerScreen
import com.future.music.ui.screens.AudioDevicesScreen
import com.future.music.ui.screens.PlaylistAddSongsScreen
import com.future.music.ui.screens.JamScreen
import com.future.music.ui.screens.JamAddSongsScreen
import com.future.music.ui.screens.JamMembersScreen
import com.future.music.data.Song
import com.future.music.jam.JamPhase
import com.future.music.jam.JamSession
import com.future.music.jam.isJamSongId
import com.future.sharednav.components.FutureSnackbarHost
import com.future.sharednav.components.rememberFutureSnackbarState
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
    // מעבר הלוך ושוב בין הג'אם למסך הניגון לא בונה מחסנית: אם היעד הוא המסך
    // שמתחת, חוזרים אליו.
    fun goTo(route: Route) {
        if (backStack.getOrNull(backStack.lastIndex - 1) == route) pop() else push(route)
    }

    // גרסת הספרייה - עולה כשמשהו משתנה ב-MediaStore (שיר שהורד, הקלטה חדשה,
    // קובץ שנמחק) או כשחוזרים לאפליקציה, והרשימות נטענות מחדש לבד.
    var libraryVersion by remember { mutableIntStateOf(0) }
    val context = androidx.compose.ui.platform.LocalContext.current
    DisposableEffect(Unit) {
        val handler = android.os.Handler(android.os.Looper.getMainLooper())
        val bump = Runnable { libraryVersion++ }
        val observer = object : android.database.ContentObserver(handler) {
            override fun onChange(selfChange: Boolean) {
                // שינויים מגיעים בצרורות (סריקה של תיקייה) - טעינה אחת בסוף.
                handler.removeCallbacks(bump)
                handler.postDelayed(bump, 800)
            }
        }
        context.contentResolver.registerContentObserver(android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, true, observer)
        onDispose {
            handler.removeCallbacks(bump)
            context.contentResolver.unregisterContentObserver(observer)
        }
    }
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val obs = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                kotlin.concurrent.thread { runCatching { repository.scanMediaFolders() } }
                libraryVersion++
            }
        }
        lifecycleOwner.lifecycle.addObserver(obs)
        onDispose { lifecycleOwner.lifecycle.removeObserver(obs) }
    }
    // הרשימה הקודמת נשארת עד שהחדשה מוכנה - בלי הבהוב ובלי לאבד את הפוקוס.
    var allSongs by remember { mutableStateOf(emptyList<com.future.music.data.Song>()) }
    LaunchedEffect(libraryVersion) {
        val fresh = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { repository.getAllSongs() }
        if (fresh != allSongs) allSongs = fresh
    }
    var renamingArtist by remember { mutableStateOf<String?>(null) }
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
    // רק השלב ומספר המשתתפים - לא כל עדכון של הג'אם (התקדמות העלאה, מיקום)
    // צריך להרכיב מחדש את כל המסך.
    val jamPhase by remember { androidx.compose.runtime.derivedStateOf { JamSession.state.phase } }
    val jamMembers by remember { androidx.compose.runtime.derivedStateOf { JamSession.state.members.size } }
    LaunchedEffect(allSongs, playerState.isConnected, jamPhase) {
        if (playerState.isConnected) playerController.adoptLibrary(allSongs)
        // בזמן ג'אם (או התחברות אליו) הנגן שייך לג'אם - לא טוענים מעליו את התור הישן.
        if (!restoredLastQueue && allSongs.isNotEmpty() && playerState.isConnected && playerState.currentSong == null &&
            jamPhase == JamPhase.IDLE
        ) {
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
        if (s.queue.isNotEmpty() && s.queue.none { isJamSongId(it.id) }) playlistStore.saveLastQueue(s.queue.map { it.id }, s.currentIndex, s.positionMs)
    }
    LaunchedEffect(playerState.isPlaying) {
        while (playerController.state.isPlaying) {
            delay(5000)
            playerController.tick()
            val s = playerController.state
            if (s.queue.isNotEmpty() && s.queue.none { isJamSongId(it.id) }) playlistStore.saveLastQueue(s.queue.map { it.id }, s.currentIndex, s.positionMs)
        }
    }

    // בזמן ג'אם הפקדים שולטים בג'אם (לכולם, או רק בהאזנה שלי - ראו JamSession),
    // ו-OK על שיר מהספרייה מוסיף אותו לתור המשותף במקום להחליף את מה שמתנגן.
    fun togglePlay() {
        if (JamSession.isActive) JamSession.togglePlay() else playerController.togglePlayPause()
    }
    fun playList(songs: List<Song>, index: Int) {
        if (JamSession.isActive) songs.getOrNull(index)?.let { JamSession.addSongs(listOf(it)) }
        else playerController.playQueue(songs, index)
    }

    // הג'אם הסתיים (או שהוסרנו) כשאנחנו במסך ההוספה/המשתתפים - חוזרים למסך הג'אם.
    LaunchedEffect(jamPhase) {
        if (jamPhase == JamPhase.IDLE) {
            while (backStack.size > 1 && (backStack.last() is Route.JamAdd || backStack.last() is Route.JamMembers)) {
                backStack.removeAt(backStack.lastIndex)
            }
        }
    }

    /** מה שמתנגן עכשיו ממשיך כשיר הראשון בג'אם חדש, עם עד שלושה שירים שאחריו. */
    fun jamCarry(): JamSession.Carry? {
        val s = playerController.state
        val current = s.queue.getOrNull(s.currentIndex) ?: return null
        if (current.id < 0) return null
        return JamSession.Carry(
            songs = s.queue.drop(s.currentIndex).filter { it.id >= 0 }.take(4),
            positionMs = s.positionMs,
            playing = s.isPlaying,
        )
    }

    val snackbar = rememberFutureSnackbarState()
    LaunchedEffect(Unit) { JamSession.messages.collect { snackbar.show(it) } }

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
    // צורך אותו ללחיצה ארוכה ל"אפליקציות אחרונות") ומשודר כשידור גלובלי.
    // הרכיב המשותף onOptionsKeyPress הוא הדרך שכל שאר האפליקציות מאזינות לו;
    // כאן היה במקומו BroadcastReceiver ידני - אותה לוגיקה, בעותק נפרד.
    com.future.sharednav.nav.onOptionsKeyPress { openMainMenu() }

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
    // כל push/pop מחליק בכיוון הניווט (ר' AnimatedBackStackHost).
    com.future.sharednav.components.AnimatedBackStackHost(backStack) { route ->
    when (route) {
        is Route.Home -> HomeScreen(
            theme = theme,
            playerState = playerState,
            onOpenAllSongs = { lastOpenedHomeItemId = "1"; push(Route.AllSongs) },
            onOpenArtists = { lastOpenedHomeItemId = "2"; push(Route.Artists) },
            onOpenAlbums = { lastOpenedHomeItemId = "3"; push(Route.Albums) },
            onOpenPlaylists = { lastOpenedHomeItemId = "4"; push(Route.Playlists) },
            onOpenFavorites = { lastOpenedHomeItemId = "5"; push(Route.Favorites) },
            onOpenSearch = { lastOpenedHomeItemId = "6"; push(Route.Search) },
            onOpenEqualizer = { lastOpenedHomeItemId = "7"; push(Route.Sound) },
            onOpenDevices = { lastOpenedHomeItemId = "8"; push(Route.Devices) },
            // בזמן ג'אם התור הוא התור המשותף.
            onOpenQueue = { lastOpenedHomeItemId = "9"; push(if (JamSession.isActive) Route.Jam else Route.Queue) },
            onOpenJam = { lastOpenedHomeItemId = "0"; push(Route.Jam) },
            jamSubtitle = if (jamPhase == JamPhase.ACTIVE) "פעיל · $jamMembers משתתפים" else "האזנה משותפת עם חברים",
            onOpenNowPlaying = { push(Route.NowPlaying) },
            onTogglePlay = ::togglePlay,
            lastOpenedItemId = lastOpenedHomeItemId,
        )

        is Route.AllSongs -> SongListScreen(
            title = "כל השירים",
            songs = allSongs,
            theme = theme,
            playerState = playerState,
            onBack = ::pop,
            onPlaySong = { index -> playList(allSongs, index) },
            onOpenNowPlaying = { push(Route.NowPlaying) },
            onTogglePlay = ::togglePlay,
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
                onPlaySong = { index -> playList(songs, index) },
                onOpenNowPlaying = { push(Route.NowPlaying) },
                onTogglePlay = ::togglePlay,
                topBarTrailingIcon = FutureIcons.Edit,
                topBarTrailingDescription = "שינוי שם האמן",
                onTopBarTrailingClick = { renamingArtist = route.artist },
            )
            renamingArtist?.let { artist ->
                NameInputDialog(
                    title = "שינוי שם האמן",
                    theme = theme,
                    initialValue = artist,
                    confirmLabel = "שמור",
                    onDismiss = { renamingArtist = null },
                    onConfirm = { newName ->
                        renamingArtist = null
                        if (newName.isNotBlank() && newName != artist) {
                            repository.renameArtist(artist, newName)
                            backStack[backStack.lastIndex] = Route.ArtistSongs(newName.trim())
                            libraryVersion++
                        }
                    },
                )
            }
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
                onPlaySong = { index -> playList(songs, index) },
                onOpenNowPlaying = { push(Route.NowPlaying) },
                onTogglePlay = ::togglePlay,
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
                onPlaySong = { index -> playList(songs, index) },
                onOpenNowPlaying = { push(Route.NowPlaying) },
                onTogglePlay = ::togglePlay,
                emptyMessage = "אין שירים בפלייליסט הזה עדיין",
                headerActionLabel = "הוספת שירים",
                onHeaderAction = { push(Route.PlaylistAdd(route.playlistId, route.name)) },
                topBarTrailingIcon = FutureIcons.Delete,
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

        is Route.PlaylistAdd -> {
            val playlist = playlists.find { it.id == route.playlistId }
            PlaylistAddSongsScreen(
                playlistName = route.name,
                allSongs = allSongs,
                memberIds = playlist?.songIds ?: emptyList(),
                theme = theme,
                onToggle = { songId -> togglePlaylistMembership(route.playlistId, songId) },
                onBack = ::pop,
            )
        }

        is Route.Favorites -> SongListScreen(
            title = "מועדפים",
            songs = favorites,
            theme = theme,
            playerState = playerState,
            onBack = ::pop,
            onPlaySong = { index -> playList(favorites, index) },
            onOpenNowPlaying = { push(Route.NowPlaying) },
            onTogglePlay = ::togglePlay,
            emptyMessage = "אין עדיין שירים מועדפים",
        )

        is Route.Search -> SearchScreen(
            allSongs = allSongs,
            theme = theme,
            playerState = playerState,
            onBack = ::pop,
            onPlayResults = { songs, index -> playList(songs, index) },
            onOpenNowPlaying = { push(Route.NowPlaying) },
            onTogglePlay = ::togglePlay,
        )

        is Route.NowPlaying -> {
            // שיר מהג'אם (מזהה שלילי) לא נמצא בספרייה - אין לו מועדף או פלייליסט.
            val currentSongId = playerState.currentSong?.id?.takeIf { it >= 0 }
            val inJam = jamPhase == JamPhase.ACTIVE
            NowPlayingScreen(
                theme = theme,
                playerState = playerState,
                isFavorite = currentSongId != null && currentSongId in favoriteIds,
                playlists = playlists,
                onBack = if (backStack.size > 1) ::pop else null,
                onTick = playerController::tick,
                onTogglePlay = ::togglePlay,
                onNext = { if (JamSession.isActive) JamSession.next() else playerController.skipToNext() },
                onPrevious = { if (JamSession.isActive) JamSession.previous() else playerController.skipToPrevious() },
                onSeekRelative = { delta -> if (JamSession.isActive) JamSession.seekRelative(delta) else playerController.seekRelative(delta) },
                onToggleShuffle = playerController::toggleShuffle,
                onCycleRepeat = playerController::cycleRepeatMode,
                onToggleFavorite = { currentSongId?.let(::toggleFavorite) },
                onTogglePlaylistMembership = { playlistId -> currentSongId?.let { togglePlaylistMembership(playlistId, it) } },
                onOpenDevices = { push(Route.Devices) },
                onOpenSound = { push(Route.Sound) },
                onOpenMenu = ::openMainMenu,
                onOpenJam = { goTo(Route.Jam) },
                title = if (inJam) "מתנגן בג'אם" else "מתנגן כעת",
            )
        }

        is Route.Queue -> QueueScreen(
            playerState = playerState,
            theme = theme,
            onBack = ::pop,
            onPlayAt = playerController::playAtQueueIndex,
        )

        is Route.Sound -> EqualizerScreen(theme = theme, onBack = ::pop)

        is Route.Devices -> AudioDevicesScreen(
            theme = theme,
            onBack = ::pop,
            onOpenEqualizer = { push(Route.Sound) },
        )

        is Route.Jam -> JamScreen(
            theme = theme,
            onBack = ::pop,
            onOpenAdd = { push(Route.JamAdd) },
            onOpenMembers = { push(Route.JamMembers) },
            onOpenNowPlaying = { goTo(Route.NowPlaying) },
            carry = ::jamCarry,
        )

        is Route.JamAdd -> JamAddSongsScreen(allSongs = allSongs, theme = theme, onBack = ::pop)

        is Route.JamMembers -> JamMembersScreen(theme = theme, onBack = ::pop)
    }
    }
    FutureSnackbarHost(snackbar, theme)
    }
}
