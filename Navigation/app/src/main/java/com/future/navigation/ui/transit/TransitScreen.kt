package com.future.navigation.ui.transit

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.future.navigation.R
import com.future.navigation.data.gtfs.LegType
import com.future.navigation.data.gtfs.TransitItinerary
import com.future.navigation.data.gtfs.TransitLeg
import com.future.sharednav.components.ScreenTopBar
import com.future.sharednav.focus.FocusableItem

@Composable
fun TransitScreen(itinerary: TransitItinerary, onBack: () -> Unit) {
    var expandedLeg by remember { mutableStateOf(-1) }
    val firstRideFocusRequester = remember { FocusRequester() }
    val firstRideIndex = remember(itinerary) { itinerary.legs.indexOfFirst { it.type == LegType.RIDE } }

    // פוקוס D-pad התחלתי על קטע הנסיעה הראשון (WALK אינו לחיץ) - בלי זה נחיתה
    // על המסך משאירה אותו בלי שום פריט מודגש.
    LaunchedEffect(Unit) { if (firstRideIndex >= 0) firstRideFocusRequester.requestFocus() }

    Column(modifier = Modifier.fillMaxSize()) {
        ScreenTopBar(
            title = stringResource(R.string.transit_itinerary_title),
            textColor = MaterialTheme.colorScheme.onBackground,
            accentColor = MaterialTheme.colorScheme.primary,
            onBack = onBack
        )

        Surface(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                Stat(stringResource(R.string.total_time), "%d דק׳".format(itinerary.totalDurationSeconds / 60))
                Stat(stringResource(R.string.arrival_time), secondsToClock(itinerary.arrivalSeconds))
                Stat(stringResource(R.string.walk_time), "%d דק׳".format(itinerary.legs.filter { it.type == LegType.WALK }.sumOf { it.walkDurationSeconds ?: 0 } / 60))
            }
        }

        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.schedule_disclaimer),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth(), contentPadding = PaddingValues(16.dp)) {
            itemsIndexed(itinerary.legs) { index, leg ->
                LegRow(
                    leg = leg,
                    isLast = index == itinerary.legs.size - 1,
                    expanded = expandedLeg == index,
                    onToggleExpand = { expandedLeg = if (expandedLeg == index) -1 else index },
                    focusRequester = if (index == firstRideIndex) firstRideFocusRequester else null
                )
            }
        }
    }
}

@Composable
private fun Stat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleMedium)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
    }
}

@Composable
private fun LegRow(leg: TransitLeg, isLast: Boolean, expanded: Boolean, onToggleExpand: () -> Unit, focusRequester: FocusRequester? = null) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(38.dp)) {
            val badgeColor = when (leg.type) {
                LegType.WALK -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
                LegType.RIDE -> MaterialTheme.colorScheme.primary
            }
            Surface(modifier = Modifier.size(38.dp), shape = CircleShape, color = badgeColor) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = if (leg.type == LegType.RIDE) (leg.routeShortName?.takeIf { it.isNotBlank() } ?: "?") else "",
                        color = if (leg.type == LegType.RIDE) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
            if (!isLast) {
                Box(modifier = Modifier.width(2.dp).height(28.dp).background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.14f)))
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f).padding(bottom = 20.dp)) {
            if (leg.type == LegType.WALK) {
                Text("הליכה", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                Text(
                    "%d דק׳ · %d מ׳".format((leg.walkDurationSeconds ?: 0) / 60, (leg.walkDistanceMeters ?: 0.0).toInt()),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            } else {
                FocusableItem(
                    onClick = onToggleExpand,
                    accentColor = MaterialTheme.colorScheme.primary,
                    contentPadding = 0.dp,
                    idleBackgroundColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f),
                    focusedBackgroundColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.22f),
                    borderWidth = 2.dp,
                    cornerRadius = 16.dp,
                    focusRequester = focusRequester
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(4.dp)) {
                        Text(leg.routeLongName?.takeIf { it.isNotBlank() } ?: leg.routeShortName ?: "קו", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "מ${leg.fromStopName} · אל ${leg.toStopName}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }
                if (expanded && leg.intermediateStopNames.isNotEmpty()) {
                    Column(modifier = Modifier.padding(top = 8.dp, start = 4.dp)) {
                        leg.intermediateStopNames.forEach { stopName ->
                            Text(
                                "· $stopName",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun secondsToClock(seconds: Int): String {
    val h = (seconds / 3600) % 24
    val m = (seconds % 3600) / 60
    return "%02d:%02d".format(h, m)
}
