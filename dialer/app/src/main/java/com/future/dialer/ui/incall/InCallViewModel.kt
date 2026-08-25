package com.future.dialer.ui.incall

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Telephony
import android.telecom.Call
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.future.dialer.R
import com.future.dialer.telecom.CallService
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** מציג את מצב השיחה האמיתי מ-[CallService] - לא סימולציה מקומית. */
class InCallViewModel : ViewModel() {

    val callState: StateFlow<Int?> = CallService.callState
    val isMuted: StateFlow<Boolean> = CallService.isMuted
    val isSpeakerOn: StateFlow<Boolean> = CallService.isSpeakerOn
    val isRecording: StateFlow<Boolean> = CallService.isRecording

    private val _callDuration = MutableStateFlow(0L)
    val callDuration: StateFlow<Long> = _callDuration.asStateFlow()

    private val _isDialpadVisible = MutableStateFlow(false)
    val isDialpadVisible: StateFlow<Boolean> = _isDialpadVisible.asStateFlow()

    private val _dtmfDigits = MutableStateFlow("")
    val dtmfDigits: StateFlow<String> = _dtmfDigits.asStateFlow()

    private val _isQuickMessageVisible = MutableStateFlow(false)
    val isQuickMessageVisible: StateFlow<Boolean> = _isQuickMessageVisible.asStateFlow()

    val isRinging: StateFlow<Boolean> = MutableStateFlow(false).also { flow ->
        viewModelScope.launch {
            CallService.callState.collect { state ->
                flow.value = state == Call.STATE_RINGING
            }
        }
    }

    fun startDurationTimer() {
        viewModelScope.launch {
            while (true) {
                val connectTime = CallService.activeCall.value?.details?.connectTimeMillis ?: 0L
                _callDuration.value = if (connectTime > 0) {
                    (System.currentTimeMillis() - connectTime) / 1000
                } else 0L
                delay(1000)
            }
        }
    }

    fun answer() = CallService.answer()
    fun reject() = CallService.reject()
    fun hangUp() = CallService.disconnect()
    fun toggleMute() = CallService.setMuted(!isMuted.value)
    fun toggleSpeaker() = CallService.setSpeaker(!isSpeakerOn.value)

    fun toggleDialpad() {
        _isDialpadVisible.value = !_isDialpadVisible.value
        if (_isDialpadVisible.value) _isQuickMessageVisible.value = false
    }

    fun toggleQuickMessage() {
        _isQuickMessageVisible.value = !_isQuickMessageVisible.value
        if (_isQuickMessageVisible.value) _isDialpadVisible.value = false
    }

    fun onDtmfDigitPressed(digit: Char) {
        _dtmfDigits.value += digit
        CallService.playDtmfTone(digit)
    }

    fun onDtmfDigitReleased() {
        CallService.stopDtmfTone()
    }

    fun toggleRecording(context: Context) {
        if (isRecording.value) {
            CallService.stopRecording()
            return
        }
        val appContext = context.applicationContext
        val started = CallService.startRecording(appContext)
        if (!started) {
            // בלי זה כישלון (למשל מקור שמע VOICE_CALL לא זמין במכשיר הזה) נראה זהה
            // בדיוק להקלטה שכן פועלת - שום דבר לא מסמן למשתמש שההקלטה לא התחילה.
            Toast.makeText(appContext, appContext.getString(R.string.recording_failed), Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * שולח מסרון קצר לצד השני בלי לצאת מהשיחה, דרך ACTION_RESPOND_VIA_MESSAGE - אותו
     * מנגנון סטנדרטי שמשמש לדחיית שיחה עם הודעה. אפליקציית ה-Messages כבר מטפלת בזה
     * (HeadlessSmsSendService), כך שאין צורך לפתוח אותה או לעזוב את מסך השיחה.
     */
    fun sendQuickMessage(context: Context, phoneNumber: String, message: String) {
        _isQuickMessageVisible.value = false
        if (phoneNumber.isBlank()) return

        val appContext = context.applicationContext
        val uri = Uri.parse("smsto:$phoneNumber")
        val smsPackage = try {
            Telephony.Sms.getDefaultSmsPackage(appContext)
        } catch (e: Exception) {
            null
        }

        val respondIntent = Intent("android.intent.action.RESPOND_VIA_MESSAGE").apply {
            data = uri
            putExtra(Intent.EXTRA_TEXT, message)
            if (smsPackage != null) setPackage(smsPackage)
        }

        try {
            appContext.startService(respondIntent)
            Toast.makeText(appContext, appContext.getString(R.string.quick_message_sent), Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.w("InCallViewModel", "RESPOND_VIA_MESSAGE failed, falling back to compose UI", e)
            try {
                val fallbackIntent = Intent(Intent.ACTION_SENDTO, uri).apply {
                    putExtra("sms_body", message)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                appContext.startActivity(fallbackIntent)
            } catch (e2: Exception) {
                Log.e("InCallViewModel", "Unable to send quick message", e2)
            }
        }
    }

    companion object {
        /** תשובות מהירות קבועות מראש - אין צורך במקלדת כדי לשלוח אחת מהן. */
        val quickMessages = listOf(
            "אני בדרך, מגיע/ה בקרוב",
            "לא יכול/ה לדבר עכשיו, אחזור אליך",
            "אני בפגישה",
            "אתקשר אליך מיד כשאסיים"
        )
    }
}
