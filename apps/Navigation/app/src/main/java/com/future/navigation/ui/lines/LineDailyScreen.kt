package com.future.navigation.ui.lines

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import com.future.navigation.data.lines.LineDeparture
import com.future.navigation.data.lines.LineLogic
import com.future.navigation.data.lines.LineTimes
import com.future.sharednav.components.FutureListItem
import com.future.sharednav.components.KeypadLazyColumn
import com.future.sharednav.components.ScreenTopBar
import com.future.sharednav.nav.rememberFocusListState
import com.future.sharednav.theme.FutureDimens
import com.future.sharednav.theme.LocalFutureTheme
import com.future.sharednav.theme.focusFillColor
import com.future.sharednav.theme.mutedTextColor
import com.future.sharednav.theme.rememberFutureType

/**
 * לוח זמנים יומי של קו אחד בתחנה אחת: כל היציאות של היום (שעון ישראל), עם סימון
 * של מה שכבר עבר ומה הבא. נטען פעם אחת כשנפתח. BACK חוזר ללוח הנסיעה.
 */
@Composable
fun LineDailyScreen(
    line: String,
    headsign: String,
    stopName: String,
    stopId: String?,
    onBack: () -> Unit,
) {
    val theme = LocalFutureTheme.current
    val type = rememberFutureType()
    var departures by remember { mutableStateOf<List<LineDeparture>?>(null) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(line, headsign, stopId) {
        loading = true
        val (dayStart, dayEnd) = LineLogic.dayBounds(System.currentTimeMillis())
        departures = stopId?.let { id ->
            LineTimes.dayAtStop(id, dayStart, dayEnd)?.let { LineLogic.dailyFor(it, line, headsign, dayStart, dayEnd) }
        }
        loading = false
    }

    Column(modifier = Modifier.fillMaxSize()) {
        ScreenTopBar(
            title = "לוח יומי · קו $line",
            textColor = MaterialTheme.colorScheme.onBackground,
            accentColor = MaterialTheme.colorScheme.primary,
            onBack = onBack,
        )
        Text(
            listOfNotNull(
                headsign.takeIf { it.isNotBlank() }?.let { "לכיוון $it" },
                "תחנה: $stopName",
            ).joinToString(" · "),
            color = theme.mutedTextColor,
            fontSize = type.summary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth().padding(horizontal = FutureDimens.screenPadding, vertical = FutureDimens.spacingXs),
        )

        val list = departures
        when {
            loading -> Text("טוען לוח יומי…", color = theme.mutedTextColor, modifier = Modifier.padding(FutureDimens.screenPadding))
            stopId == null -> Text("אין מזהה לתחנה הזו - לא ניתן לטעון לוח יומי", color = theme.mutedTextColor, modifier = Modifier.padding(FutureDimens.screenPadding))
            list == null -> Text("אין חיבור לשרת - לא ניתן להציג לוח יומי", color = theme.mutedTextColor, modifier = Modifier.padding(FutureDimens.screenPadding))
            list.isEmpty() -> Text("אין יציאות של הקו הזה בתחנה היום", color = theme.mutedTextColor, modifier = Modifier.padding(FutureDimens.screenPadding))
            else -> DailyList(list, theme, modifier = Modifier.weight(1f).fillMaxWidth())
        }
    }
}

@Composable
private fun DailyList(
    departures: List<LineDeparture>,
    theme: com.future.sharednav.theme.FutureTheme,
    modifier: Modifier,
) {
    val now = remember { System.currentTimeMillis() }
    val nextIndex = departures.indexOfFirst { it.departsAtMillis >= now }
    val focus = rememberFocusListState(itemCount = departures.size, initialIndex = nextIndex.coerceAtLeast(0))
    KeypadLazyColumn(
        items = departures,
        focusState = focus,
        onSelect = { _, _ -> },
        modifier = modifier,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            horizontal = FutureDimens.screenPadding,
            vertical = FutureDimens.spacingSm,
        ),
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(FutureDimens.spacingXs),
    ) { index, departure, isFocused ->
        val past = departure.departsAtMillis < now
        FutureListItem(
            title = LineLogic.clock(departure.departsAtMillis),
            summary = listOfNotNull(
                if (past) "עבר" else if (index == nextIndex) "הבא" else null,
                if (departure.realtime) "זמן אמת" else null,
            ).joinToString(" · ").ifBlank { null },
            theme = theme,
            onClick = {},
            titleColor = if (past) theme.mutedTextColor else theme.textColor,
            idleBackgroundColor = if (isFocused) theme.focusFillColor else Color.Transparent,
        )
    }
}
