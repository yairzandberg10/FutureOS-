package com.future.music.playback

import android.app.PendingIntent
import android.content.Intent
import android.media.audiofx.Equalizer
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
    private var equalizer: Equalizer? = null

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

        setupEqualizer(player.audioSessionId)

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
        equalizer?.release()
        equalizer = null
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }

    private fun setupEqualizer(audioSessionId: Int) {
        equalizer = try {
            Equalizer(0, audioSessionId).apply { enabled = true }
        } catch (e: Exception) {
            null
        }
    }

    private fun applyEqPreset(preset: Int) {
        val eq = equalizer ?: return
        try {
            // עדכון המקור-האמת המשותף - כדי ש-SoundScreen יוכל לאתחל את הבחירה
            // המודגשת לפי הפריסט האמיתי הפעיל, גם אחרי חזרה למסך מבלי לעבור
            // דרך המסך הזה (בלי זה הוא תמיד אתחל בטעות ל"רגיל").
            _currentEqPreset.value = preset
            val bands = eq.numberOfBands
            val maxLevel: Int = eq.bandLevelRange[1].toInt()
            val minLevel: Int = eq.bandLevelRange[0].toInt()
            val partialLevel: Int = maxLevel / 3
            for (b in 0 until bands) {
                val band = b.toShort()
                val centerFreqHz = eq.getCenterFreq(band) / 1000
                val level: Int = when (preset) {
                    EQ_PRESET_BASS -> if (centerFreqHz < 500) maxLevel else partialLevel
                    EQ_PRESET_TREBLE -> if (centerFreqHz > 4000) maxLevel else partialLevel
                    EQ_PRESET_VOCAL -> if (centerFreqHz in 1000..4000) maxLevel else partialLevel
                    else -> 0
                }
                eq.setBandLevel(band, level.coerceIn(minLevel, maxLevel).toShort())
            }
        } catch (e: Exception) {
            android.util.Log.e("MusicPlaybackService", "Error applying EQ preset", e)
        }
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
