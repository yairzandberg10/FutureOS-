package com.future.dialer.telecom

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.media.MediaRecorder
import android.os.Build
import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.InCallService
import android.telecom.VideoProfile
import android.util.Log
import androidx.core.app.NotificationCompat
import com.future.dialer.MainActivity
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * שירות ה-InCallService האמיתי של המערכת - נקשר אליו ע"י Telecom Framework כשהאפליקציה
 * הזו מוגדרת כאפליקציית הטלפון המשלימה (ROLE_DIALER). כאן מגיעה כל שיחה אמיתית -
 * נכנסת או יוצאת - ולא רק כשמתקשרים דרך המסך שלנו.
 */
class CallService : InCallService() {

    private val callCallback = object : Call.Callback() {
        override fun onStateChanged(call: Call, state: Int) {
            _activeCall.value = call
            _callState.value = state
            onCallStateKnown(state)
        }
    }

    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)
        call.registerCallback(callCallback)
        _activeCall.value = call
        _callState.value = call.details.state
        instance = this
        onCallStateKnown(call.details.state)
    }

    override fun onCallRemoved(call: Call) {
        super.onCallRemoved(call)
        call.unregisterCallback(callCallback)
        if (_activeCall.value == call) {
            _activeCall.value = null
            _callState.value = null
            stopRecording()
        }
        notifyCallNoLongerRinging()
    }

    /**
     * onCallAdded/onStateChanged הם המקום היחיד שבו יש בכלל הזדמנות להציג משהו למשתמש -
     * InCallService לא מקבל שום דבר "בחינם" מהמערכת: אף Activity לא נפתח אוטומטית ואף
     * מסך לא מתעורר. שיחה נכנסת שהמכשיר לא פתוח באפליקציית הטלפון עליו נשארת בלתי
     * נראית לגמרי בלי הקוד הזה.
     */
    private fun onCallStateKnown(state: Int) {
        if (state == Call.STATE_RINGING) {
            notifyCallRinging()
        } else {
            notifyCallNoLongerRinging()
        }
    }

    /**
     * שיחה נכנסת מוצגת דרך fullScreenIntent בהתראת CATEGORY_CALL בערוץ IMPORTANCE_HIGH -
     * זו הדרך התקנית היחידה שגורמת למערכת להציג את המסך במלואו כשהמסך כבוי/נעול; כשהמשתמש
     * כבר פעיל באפליקציה אחרת, אותה התראה מוצגת כ-heads-up בלבד במקום לקפוץ ולתפוס את המסך.
     * startActivity ישיר (כפי שהיה כאן קודם) לא נותן את ההבחנה הזו - הוא תמיד "קופץ" קדימה.
     */
    private fun notifyCallRinging() {
        if (ringingNotified) return
        ringingNotified = true

        val launchIntent = Intent(applicationContext, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            applicationContext, 0, launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val manager = applicationContext.getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CALL_CHANNEL_ID, "שיחות נכנסות", NotificationManager.IMPORTANCE_HIGH)
            manager.createNotificationChannel(channel)
        }

        val number = _activeCall.value?.details?.handle?.schemeSpecificPart ?: "מספר לא ידוע"
        val notification = NotificationCompat.Builder(applicationContext, CALL_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.sym_call_incoming)
            .setContentTitle("שיחה נכנסת")
            .setContentText(number)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOngoing(true)
            .setAutoCancel(false)
            .setContentIntent(fullScreenPendingIntent)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .build()
        try {
            manager.notify(CALL_NOTIFICATION_ID, notification)
        } catch (e: Exception) {
            Log.e(TAG, "failed to post ringing-call notification", e)
        }

        // FutureUI (מסך הנעילה המותאם-אישית) לא בהכרח מותקן בכל build - אם השידור
        // נכשל, שיחה עדיין תיענה, פשוט בלי לפנות אוטומטית את מסך הנעילה שלו.
        try {
            val intent = Intent("com.future.futureui.ACTION_CALL_RINGING").setPackage("com.future.futureui")
            applicationContext.sendBroadcast(intent)
        } catch (e: Exception) {
            Log.w(TAG, "failed to notify FutureUI of ringing call", e)
        }
    }

    private fun notifyCallNoLongerRinging() {
        if (!ringingNotified) return
        ringingNotified = false
        applicationContext.getSystemService(NotificationManager::class.java)?.cancel(CALL_NOTIFICATION_ID)
        try {
            val intent = Intent("com.future.futureui.ACTION_CALL_ENDED").setPackage("com.future.futureui")
            applicationContext.sendBroadcast(intent)
        } catch (e: Exception) {
            Log.w(TAG, "failed to notify FutureUI that ringing ended", e)
        }
    }

    // CallAudioState/setAudioRoute מוחלפות ב-CallEndpoint (API 34+), אבל minSdk כאן הוא 31
    // (אנדרואיד 12) - נשארים עם ה-API הישן והתואם לכל הגרסאות הנתמכות, בכוונה.
    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun onCallAudioStateChanged(audioState: CallAudioState) {
        super.onCallAudioStateChanged(audioState)
        _isMuted.value = audioState.isMuted
        _isSpeakerOn.value = audioState.route == CallAudioState.ROUTE_SPEAKER
    }

    override fun onDestroy() {
        if (instance == this) instance = null
        super.onDestroy()
    }

    companion object {
        private const val TAG = "CallService"
        private const val CALL_CHANNEL_ID = "incoming_call"
        private const val CALL_NOTIFICATION_ID = 7001
        private var instance: CallService? = null

        private val _activeCall = MutableStateFlow<Call?>(null)
        val activeCall: StateFlow<Call?> = _activeCall.asStateFlow()

        private val _callState = MutableStateFlow<Int?>(null)
        val callState: StateFlow<Int?> = _callState.asStateFlow()

        private val _isMuted = MutableStateFlow(false)
        val isMuted: StateFlow<Boolean> = _isMuted.asStateFlow()

        private val _isSpeakerOn = MutableStateFlow(false)
        val isSpeakerOn: StateFlow<Boolean> = _isSpeakerOn.asStateFlow()

        private val _isRecording = MutableStateFlow(false)
        val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

        private var recorder: MediaRecorder? = null
        private var recordingFile: File? = null

        @Volatile
        private var ringingNotified = false

        fun answer() {
            _activeCall.value?.answer(VideoProfile.STATE_AUDIO_ONLY)
        }

        fun reject() {
            _activeCall.value?.reject(false, null)
        }

        fun disconnect() {
            _activeCall.value?.disconnect()
            stopRecording()
        }

        fun setMuted(muted: Boolean) {
            instance?.setMuted(muted)
        }

        @Suppress("DEPRECATION")
        fun setSpeaker(enabled: Boolean) {
            instance?.setAudioRoute(if (enabled) CallAudioState.ROUTE_SPEAKER else CallAudioState.ROUTE_EARPIECE)
        }

        /** שולח טון DTMF בודד לצד השני של השיחה (למשל תפריטי IVR). */
        fun playDtmfTone(digit: Char) {
            _activeCall.value?.playDtmfTone(digit)
        }

        fun stopDtmfTone() {
            _activeCall.value?.stopDtmfTone()
        }

        /**
         * מתחיל הקלטת שיחה למקום אחסון פרטי של האפליקציה. איכות ההקלטה תלויה במקור השמע
         * שהמכשיר מאפשר עבורו - VOICE_CALL אינו זמין בכל המכשירים (מוגבל ע"י ה-OEM/אבטחה),
         * ולכן יש נפילה חזרה למקור המיקרופון הרגיל אם היצירה נכשלת.
         */
        fun startRecording(context: android.content.Context): Boolean {
            if (_isRecording.value) return true
            if (_activeCall.value == null) return false

            val dir = File(context.getExternalFilesDir(null), "recordings").apply { mkdirs() }
            val name = "call_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}.m4a"
            val file = File(dir, name)

            val audioSources = intArrayOf(MediaRecorder.AudioSource.VOICE_CALL, MediaRecorder.AudioSource.MIC)
            for (source in audioSources) {
                val mr = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(context) else @Suppress("DEPRECATION") MediaRecorder()
                try {
                    mr.setAudioSource(source)
                    mr.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                    mr.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                    mr.setOutputFile(file.absolutePath)
                    mr.prepare()
                    mr.start()
                    recorder = mr
                    recordingFile = file
                    _isRecording.value = true
                    return true
                } catch (e: Exception) {
                    Log.w(TAG, "recording start failed for source=$source", e)
                    mr.release()
                }
            }
            return false
        }

        fun stopRecording() {
            if (!_isRecording.value) return
            try {
                recorder?.stop()
            } catch (e: Exception) {
                Log.w(TAG, "recording stop failed", e)
            } finally {
                recorder?.release()
                recorder = null
                _isRecording.value = false
            }
        }
    }
}
