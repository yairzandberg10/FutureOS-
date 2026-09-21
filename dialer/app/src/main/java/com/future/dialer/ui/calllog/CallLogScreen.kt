package com.future.dialer.ui.calllog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CallMade
import androidx.compose.material.icons.rounded.CallMissed
import androidx.compose.material.icons.rounded.CallReceived
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.future.dialer.data.model.CallRecord
import com.future.dialer.data.model.CallType
import com.future.dialer.ui.dialpad.DialpadViewModel
import com.future.sharednav.components.EmptyState
import com.future.sharednav.components.FutureCard
import com.future.sharednav.components.FutureChip
import com.future.sharednav.components.FutureDivider
import com.future.sharednav.components.FutureSectionHeader
import com.future.sharednav.components.FutureSettingItem
import com.future.sharednav.theme.FutureDimens
import com.future.sharednav.theme.FutureTypography
import com.future.sharednav.theme.LocalFutureTheme
import com.future.sharednav.theme.subtleTextColor
import com.future.sharednav.theme.rememberFutureType
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * יומן השיחות - הטאב הראשון של החייגן. השיחות מקובצות לפי יום, כל יום
 * הוא כרטיס אחד של שורות שקופות, ומעליהן שני צ'יפים לסינון
 * (ui_kits/calls). קודם היומן היה רשימה שטוחה בתוך מסך החיוג, בלי קיבוץ
 * ובלי סינון.
 *
 * מקש f מחליף בין "הכל" ל"לא נענו" - אותו דפוס של מקש-אות שכבר קיים
 * בשאר המערכת, כי אין מסך מגע ללחוץ על הצ'יפ.
 */
@Composable
fun CallLogScreen(
    viewModel: DialpadViewModel,
    onOpen: (String, String) -> Unit,
    showMissedOnly: Boolean = false,
) {
    val theme = LocalFutureTheme.current
    val type = rememberFutureType()
    val recentCalls by viewModel.recentCalls.collectAsState()

    val shown = remember(recentCalls, showMissedOnly) {
        if (showMissedOnly) recentCalls.filter { it.type == CallType.MISSED } else recentCalls
    }
    val groups = remember(shown) { groupByDay(shown) }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = FutureDimens.spacingLg, vertical = FutureDimens.spacingSm),
            horizontalArrangement = Arrangement.spacedBy(FutureDimens.spacingSm),
        ) {
            FutureChip("הכל", theme = theme, selected = !showMissedOnly)
            FutureChip("לא נענו", theme = theme, selected = showMissedOnly)
        }

        if (groups.isEmpty()) {
            EmptyState(
                icon = Icons.Rounded.CallMissed,
                title = if (showMissedOnly) "אין שיחות שלא נענו" else "אין שיחות",
                subtitle = if (showMissedOnly) "לחץ f כדי לחזור לכל השיחות" else "חייג מספר כדי להתחיל",
                textColor = theme.textColor,
                modifier = Modifier.weight(1f).fillMaxWidth(),
            )
        } else {
            Column(
                modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
            ) {
                groups.forEach { (day, calls) ->
                    FutureSectionHeader(day, theme = theme)
                    FutureCard(theme = theme) {
                        calls.forEachIndexed { index, call ->
                            if (index > 0) FutureDivider(theme = theme)
                            FutureSettingItem(
                                title = call.name ?: call.phoneNumber,
                                summary = summaryOf(call),
                                icon = iconOf(call.type),
                                theme = theme,
                                showChevron = false,
                                onClick = { onOpen(call.name ?: call.phoneNumber, call.phoneNumber) },
                                trailing = {
                                    Text(
                                        timeFormat.format(Date(call.timestamp)),
                                        color = theme.subtleTextColor,
                                        fontSize = type.summary,
                                    )
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
private val dayFormat = SimpleDateFormat("EEEE, d בMMMM", Locale("he"))

private fun iconOf(type: CallType): ImageVector = when (type) {
    CallType.INCOMING -> Icons.Rounded.CallReceived
    CallType.OUTGOING -> Icons.Rounded.CallMade
    CallType.MISSED, CallType.REJECTED -> Icons.Rounded.CallMissed
}

private fun labelOf(type: CallType): String = when (type) {
    CallType.INCOMING -> "נכנסת"
    CallType.OUTGOING -> "יוצאת"
    CallType.MISSED -> "לא נענתה"
    CallType.REJECTED -> "נדחתה"
}

/** "נכנסת · 2:14" - הכיוון, ואחריו המשך רק אם הייתה שיחה בפועל. */
private fun summaryOf(call: CallRecord): String {
    val label = labelOf(call.type)
    if (call.duration <= 0L) return label
    val seconds = call.duration
    return "$label · %d:%02d".format(seconds / 60, seconds % 60)
}

/**
 * קיבוץ לפי יום, בסדר יורד. "היום"/"אתמול" ולא תאריך מלא - בשתי השורות
 * הראשונות של היומן התאריך לא מוסיף מידע שהמשתמש לא יודע.
 */
private fun groupByDay(calls: List<CallRecord>): List<Pair<String, List<CallRecord>>> {
    if (calls.isEmpty()) return emptyList()
    val today = Calendar.getInstance()
    val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }

    fun sameDay(a: Calendar, b: Calendar) =
        a.get(Calendar.YEAR) == b.get(Calendar.YEAR) &&
            a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR)

    return calls
        .sortedByDescending { it.timestamp }
        .groupBy { call ->
            val at = Calendar.getInstance().apply { timeInMillis = call.timestamp }
            when {
                sameDay(at, today) -> "היום"
                sameDay(at, yesterday) -> "אתמול"
                else -> dayFormat.format(Date(call.timestamp))
            }
        }
        .toList()
}
