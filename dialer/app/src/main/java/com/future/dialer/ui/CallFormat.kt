package com.future.dialer.ui
import androidx.compose.ui.graphics.Color
import com.future.sharednav.theme.FutureTheme
import com.future.sharednav.theme.callMissedColor
import com.future.sharednav.theme.callOutgoingColor
import com.future.sharednav.theme.callReceivedColor
import com.future.sharednav.theme.callRejectedColor
import com.future.sharednav.theme.subtleTextColor

import com.future.sharednav.icons.FutureIcons

import androidx.compose.ui.graphics.vector.ImageVector
import com.future.dialer.data.model.CallRecord
import com.future.dialer.data.model.CallType
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** הניסוחים והאייקונים של כיוון שיחה - משותפים ליומן ולמסך איש הקשר. */
object CallFormat {

    fun iconOf(type: CallType): ImageVector = when (type) {
        CallType.INCOMING, CallType.VOICEMAIL -> FutureIcons.CallReceived
        CallType.OUTGOING -> FutureIcons.CallMade
        CallType.MISSED, CallType.REJECTED -> FutureIcons.CallMissed
        CallType.BLOCKED -> FutureIcons.Block
    }

    fun labelOf(type: CallType): String = when (type) {
        CallType.INCOMING -> "התקבלה"
        CallType.OUTGOING -> "חויגה"
        CallType.MISSED -> "לא נענתה"
        CallType.REJECTED -> "נדחתה"
        CallType.BLOCKED -> "נחסמה"
        CallType.VOICEMAIL -> "תא קולי"
    }

    /**
     * הצבע של סוג השיחה: אדום - לא נענתה, ירוק - התקבלה, כתום - נדחתה,
     * כחול - חויגה (ר' callMissedColor וחבריו בערכה). נחסמה - אפורה.
     */
    fun colorOf(type: CallType, theme: FutureTheme): Color = when (type) {
        CallType.MISSED -> theme.callMissedColor
        CallType.INCOMING, CallType.VOICEMAIL -> theme.callReceivedColor
        CallType.REJECTED -> theme.callRejectedColor
        CallType.OUTGOING -> theme.callOutgoingColor
        CallType.BLOCKED -> theme.subtleTextColor
    }

    /** "4:12" - או null כשלא הייתה שיחה בפועל. */
    fun durationOf(call: CallRecord): String? =
        if (call.duration <= 0L) null else duration(call.duration)

    fun duration(seconds: Long): String = "%d:%02d".format(seconds / 60, seconds % 60)

    /** "נכנסת · 4:12" - הכיוון, ואחריו המשך רק אם הייתה שיחה. */
    fun summaryOf(call: CallRecord): String =
        durationOf(call)?.let { "${labelOf(call.type)} · $it" } ?: labelOf(call.type)

    fun timeOf(call: CallRecord): String = timeFormat.formatSafely(call.timestamp)

    /** שעה להיום, "אתמול", ותאריך קצר לפני כן - למסך איש הקשר, שאין בו כותרות יום. */
    fun whenOf(call: CallRecord): String = when (dayOffset(call.timestamp)) {
        0 -> timeOf(call)
        1 -> "אתמול"
        else -> shortDate.formatSafely(call.timestamp)
    }

    /** כותרת יום ביומן: "היום", "אתמול", או "יום שני, 21 בספטמבר". */
    fun dayTitle(timestamp: Long): String = when (dayOffset(timestamp)) {
        0 -> "היום"
        1 -> "אתמול"
        else -> dayFormat.formatSafely(timestamp)
    }

    private fun dayOffset(timestamp: Long): Int {
        val today = Calendar.getInstance()
        val at = Calendar.getInstance().apply { timeInMillis = timestamp }
        fun same(a: Calendar, b: Calendar) =
            a.get(Calendar.YEAR) == b.get(Calendar.YEAR) && a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR)
        if (same(at, today)) return 0
        val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
        return if (same(at, yesterday)) 1 else 2
    }

    // SimpleDateFormat לא בטוח לשימוש מכמה threads, והיומן מפורמט עכשיו ברקע
    // (CallsViewModel.callDays) בזמן שמסך איש הקשר מפרמט על ה-main thread.
    private fun SimpleDateFormat.formatSafely(timestamp: Long): String =
        synchronized(this) { format(Date(timestamp)) }

    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    private val shortDate = SimpleDateFormat("d.M", Locale.getDefault())
    private val dayFormat = SimpleDateFormat("EEEE, d בMMMM", Locale("he"))
}
