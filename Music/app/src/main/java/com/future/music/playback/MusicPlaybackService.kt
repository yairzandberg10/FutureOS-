package com.future.music.playback

import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Foreground service שמחזיק את ה-ExoPlayer האמיתי + MediaSession - כדי
 * שהניגון ימשיך ברקע/מסך נעול, עם התראת מדיה שהמערכת מייצרת אוטומטית
 * מה-session (אין צורך לבנות Notification בעצמנו). גם מחזיק Equalizer
 * אמיתי (לא מדומה) הקשור ל-audioSessionId של הנגן, נשלט דרך custom session
 * command כי MediaController בצד ה-Activity לא חשוף ל-ExoPlayer עצמו.
 */
class MusicPlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()

        val player = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                /* handleAudioFocus= */ true
            )
            .setWakeMode(C.WAKE_MODE_LOCAL)
            .build()

        // אקולייזר מלא, באס, 3D והגברה - ופרופיל סאונד לכל התקן פלט (ר' AudioFx).
        AudioFx.attach(this, player.audioSessionId)
        AudioFx.onResumeRequested = {
            if (player.mediaItemCount > 0 && !player.isPlaying) player.play()
        }

        val sessionActivityIntent = packageManager?.getLaunchIntentForPackage(packageName)?.let {
            PendingIntent.getActivity(this, 0, it, PendingIntent.FLAG_IMMUTABLE)
        }

        mediaSession = MediaSession.Builder(this, player)
            .setCallback(sessionCallback)
            .apply { sessionActivityIntent?.let { setSessionActivity(it) } }
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player ?: return
        if (!player.playWhenReady || player.mediaItemCount == 0 || player.playbackState == Player.STATE_ENDED) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        AudioFx.onResumeRequested = null
        AudioFx.release()
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }

    /** הפריסטים הישנים (פקודת session) ממופים לאפקטים של AudioFx. */
    private fun applyEqPreset(preset: Int) {
        _currentEqPreset.value = preset
        AudioFx.useEffect(
            when (preset) {
                EQ_PRESET_BASS -> AudioFx.Effect.BASS
                EQ_PRESET_TREBLE -> AudioFx.Effect.CLEAR
                EQ_PRESET_VOCAL -> AudioFx.Effect.VOCAL
                else -> AudioFx.Effect.FLAT
            }
        )
    }

    private val sessionCallback = object : MediaSession.Callback {
        override fun onConnect(session: MediaSession, controller: MediaSession.ControllerInfo): MediaSession.ConnectionResult {
            val defaultResult = super.onConnect(session, controller)
            val sessionCommands = defaultResult.availableSessionCommands.buildUpon()
                .add(SessionCommand(CMD_SET_EQ_PRESET, Bundle.EMPTY))
                .build()
            return MediaSession.ConnectionResult.accept(sessionCommands, defaultResult.availablePlayerCommands)
        }

        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: SessionCommand,
            args: Bundle,
        ): ListenableFuture<SessionResult> {
            if (customCommand.customAction == CMD_SET_EQ_PRESET) {
                applyEqPreset(args.getInt(ARG_PRESET, EQ_PRESET_NORMAL))
                return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
            }
            return super.onCustomCommand(session, controller, customCommand, args)
        }
    }

    companion object {
        const val CMD_SET_EQ_PRESET = "com.future.music.SET_EQ_PRESET"
        const val ARG_PRESET = "preset"
        const val EQ_PRESET_NORMAL = 0
        const val EQ_PRESET_BASS = 1
        const val EQ_PRESET_TREBLE = 2
        const val EQ_PRESET_VOCAL = 3

        private val _currentEqPreset = MutableStateFlow(EQ_PRESET_NORMAL)
        /** הפריסט האמיתי הפעיל כרגע ב-Equalizer - ראו applyEqPreset. */
        val currentEqPreset: StateFlow<Int> = _currentEqPreset
    }
}
