package com.future.music.playback

import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionToken
import com.future.music.data.Song
import com.google.common.util.concurrent.MoreExecutors

data class PlayerUiState(
    val currentSong: Song? = null,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val queue: List<Song> = emptyList(),
    val currentIndex: Int = -1,
    val shuffleEnabled: Boolean = false,
    val repeatMode: Int = Player.REPEAT_MODE_OFF,
    val isConnected: Boolean = false,
)

/** עוטף MediaController שמתחבר ל-MusicPlaybackService - מקור אמת יחיד
 * לניגון שכל המסכים קוראים ממנו כ-Compose state. */
class PlayerController(private val context: Context) {
    private var controller: MediaController? = null
    private var currentQueue: List<Song> = emptyList()

    var state by mutableStateOf(PlayerUiState())
        private set

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            refreshState(player)
        }
    }

    fun connect(onReady: () -> Unit = {}) {
        val sessionToken = SessionToken(context, ComponentName(context, MusicPlaybackService::class.java))
        val future = MediaController.Builder(context, sessionToken).buildAsync()
        future.addListener({
            val c = try {
                future.get()
            } catch (e: Exception) {
                android.util.Log.e("PlayerController", "Failed to connect to MusicPlaybackService", e)
                return@addListener
            }
            controller = c
            c.addListener(listener)
            refreshState(c)
            onReady()
        }, MoreExecutors.directExecutor())
    }

    fun disconnect() {
        controller?.removeListener(listener)
        controller?.release()
        controller = null
    }

    fun playQueue(songs: List<Song>, startIndex: Int, startPositionMs: Long = 0L) {
        val c = controller ?: return
        currentQueue = songs
        c.setMediaItems(songs.map { it.toMediaItem() }, startIndex.coerceIn(0, songs.lastIndex), startPositionMs)
        c.prepare()
        c.play()
    }

    /** טוען תור בלי להתחיל לנגן - לשחזור "המשך מהיכן שהפסקת" בפתיחת האפליקציה,
     * בלי להפתיע את המשתמש עם ניגון אוטומטי. */
    fun restoreQueue(songs: List<Song>, startIndex: Int, startPositionMs: Long = 0L) {
        val c = controller ?: return
        currentQueue = songs
        c.setMediaItems(songs.map { it.toMediaItem() }, startIndex.coerceIn(0, songs.lastIndex), startPositionMs)
        c.prepare()
        refreshState(c)
    }

    fun togglePlayPause() {
        val c = controller ?: return
        if (c.isPlaying) c.pause() else c.play()
    }

    fun seekTo(positionMs: Long) {
        controller?.seekTo(positionMs.coerceAtLeast(0))
    }

    fun seekRelative(deltaMs: Long) {
        val c = controller ?: return
        val duration = c.duration.takeIf { it > 0 } ?: Long.MAX_VALUE
        c.seekTo((c.currentPosition + deltaMs).coerceIn(0, duration))
    }

    fun skipToNext() = controller?.seekToNextMediaItem()
    fun skipToPrevious() = controller?.seekToPreviousMediaItem()
    fun playAtQueueIndex(index: Int) = controller?.seekTo(index, 0)

    fun toggleShuffle() {
        controller?.let { it.shuffleModeEnabled = !it.shuffleModeEnabled }
    }

    fun cycleRepeatMode() {
        val c = controller ?: return
        c.repeatMode = when (c.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
    }

    fun setEqPreset(preset: Int) {
        val c = controller ?: return
        val args = Bundle().apply { putInt(MusicPlaybackService.ARG_PRESET, preset) }
        c.sendCustomCommand(SessionCommand(MusicPlaybackService.CMD_SET_EQ_PRESET, Bundle.EMPTY), args)
    }

    /** קורא מיקום עדכני מה-controller בלי לחכות ל-onEvents (לפס התקדמות חי). */
    fun tick() {
        controller?.let { refreshState(it) }
    }

    private fun refreshState(player: Player) {
        val index = player.currentMediaItemIndex
        state = PlayerUiState(
            currentSong = currentQueue.getOrNull(index),
            isPlaying = player.isPlaying,
            positionMs = player.currentPosition.coerceAtLeast(0),
            durationMs = player.duration.takeIf { it > 0 } ?: 0L,
            queue = currentQueue,
            currentIndex = index,
            shuffleEnabled = player.shuffleModeEnabled,
            repeatMode = player.repeatMode,
            isConnected = true,
        )
    }

    private fun Song.toMediaItem(): MediaItem {
        val metadata = MediaMetadata.Builder()
            .setTitle(title)
            .setArtist(artist)
            .setAlbumTitle(album)
            .build()
        return MediaItem.Builder()
            .setUri(uri)
            .setMediaId(id.toString())
            .setMediaMetadata(metadata)
            .build()
    }
}
