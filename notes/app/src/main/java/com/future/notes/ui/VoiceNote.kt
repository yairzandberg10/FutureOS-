package com.future.notes.ui

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import java.io.File

/** הקלטה והשמעה של הקלטה קולית לפתק (AAC ב-m4a, בתיקייה פרטית של האפליקציה). */
class VoiceNote(private val context: Context) {
    private var recorder: MediaRecorder? = null
    private var player: MediaPlayer? = null
    private var recordingFile: File? = null

    val isRecording get() = recorder != null
    val isPlaying get() = player?.isPlaying == true

    fun startRecording(): Boolean {
        stopPlayback()
        val dir = File(context.filesDir, "audio").apply { mkdirs() }
        val file = File(dir, "note_${System.currentTimeMillis()}.m4a")
        return try {
            @Suppress("DEPRECATION")
            val r = if (Build.VERSION.SDK_INT >= 31) MediaRecorder(context) else MediaRecorder()
            r.setAudioSource(MediaRecorder.AudioSource.MIC)
            r.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            r.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            r.setAudioEncodingBitRate(64_000)
            r.setAudioSamplingRate(44_100)
            r.setOutputFile(file.absolutePath)
            r.prepare()
            r.start()
            recorder = r
            recordingFile = file
            true
        } catch (e: Exception) {
            Log.e(TAG, "record failed", e)
            file.delete()
            false
        }
    }

    /** מחזיר את נתיב הקובץ שהוקלט, או null אם ההקלטה נכשלה. */
    fun stopRecording(): String? {
        val r = recorder ?: return null
        recorder = null
        val file = recordingFile
        recordingFile = null
        return try {
            r.stop()
            file?.absolutePath
        } catch (e: Exception) {
            // stop מיד אחרי start (הקלטה ריקה) זורק - אין מה לשמור.
            file?.delete()
            null
        } finally {
            r.release()
        }
    }

    fun play(path: String, onDone: () -> Unit): Boolean {
        stopPlayback()
        return try {
            player = MediaPlayer().apply {
                setDataSource(path)
                setOnCompletionListener { stopPlayback(); onDone() }
                prepare()
                start()
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "play failed", e)
            stopPlayback()
            false
        }
    }

    fun stopPlayback() {
        player?.runCatching { stop() }
        player?.release()
        player = null
    }

    fun release() {
        stopRecording()
        stopPlayback()
    }

    companion object {
        private const val TAG = "VoiceNote"

        fun durationMs(path: String): Long = try {
            val mmr = android.media.MediaMetadataRetriever()
            mmr.setDataSource(path)
            val d = mmr.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            mmr.release()
            d
        } catch (e: Exception) { 0L }

        fun format(ms: Long): String {
            val s = ms / 1000
            return "%d:%02d".format(s / 60, s % 60)
        }
    }
}
