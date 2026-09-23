package com.future.dialer.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CallMade
import androidx.compose.material.icons.rounded.CallMissed
import androidx.compose.material.icons.rounded.CallReceived
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
        CallType.INCOMING -> Icons.Rounded.CallReceived
        CallType.OUTGOING -> Icons.Rounded.CallMade
        CallType.MISSED, CallType.REJECTED -> Icons.Rounded.CallMissed
    }

    fun labelOf(type: CallType): String = when (type) {
        CallType.INCOMING -> "נכנסת"
        CallType.OUTGOING -> "יוצאת"
        CallType.MISSED -> "לא נענתה"
        CallType.REJECTED -> "נדחתה"
    }

    /** "4:12" - או null כשלא הייתה שיחה בפועל. */
    fun durationOf(call: CallRecord): String? =
        if (call.duration <= 0L) null else duration(call.duration)

    fun duration(seconds: Long): String = "%d:%02d".format(seconds / 60, seconds % 60)

    /** "נכנסת · 4:12" - הכיוון, ואחריו המשך רק אם הייתה שיחה. */
    fun summaryOf(call: CallRecord): String =
        durationOf(call)?.let { "${labelOf(call.type)} · $it" } ?: labelOf(call.type)

    fun timeOf(call: CallRecord): String = timeFormat.format(Date(call.timestamp))

    /** שעה להיום, "אתמול", ותאריך קצר לפני כן - למסך איש הקשר, שאין בו כותרות יום. */
    fun whenOf(call: CallRecord): String = when (dayOffset(call.timestamp)) {
        0 -> timeOf(call)
        1 -> "אתמול"
        else -> shortDate.format(Date(call.timestamp))
    }

    /** כותרת יום ביומן: "היום", "אתמול", או "יום שני, 21 בספטמבר". */
    fun dayTitle(timestamp: Long): String = when (dayOffset(timestamp)) {
        0 -> "היום"
        1 -> "אתמול"
        else -> dayFormat.format(Date(timestamp))
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

    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    private val shortDate = SimpleDateFormat("d.M", Locale.getDefault())
    private val dayFormat = SimpleDateFormat("EEEE, d בMMMM", Locale("he"))
}
