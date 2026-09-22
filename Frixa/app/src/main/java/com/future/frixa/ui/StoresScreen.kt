package com.future.frixa.ui
import com.future.sharednav.theme.FutureShapes
import com.future.sharednav.theme.readableAccentColor
import com.future.sharednav.theme.mutedTextColor
import com.future.sharednav.theme.FutureMotion

import com.future.sharednav.theme.FutureTypography
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.future.frixa.data.LocationHelper
import com.future.frixa.data.Store
import com.future.frixa.data.StoreCatalog
import com.future.sharednav.theme.FutureDimens
import com.future.sharednav.theme.FutureTheme
import kotlinx.coroutines.launch

@Composable
fun StoresScreen(theme: FutureTheme) {
    val context = LocalContext.current
    val locationGranted = rememberRuntimePermission(android.Manifest.permission.ACCESS_COARSE_LOCATION)

    var myLocation by remember { mutableStateOf<Pair<Double, Double>?>(null) }
    LaunchedEffect(locationGranted.value) {
        if (locationGranted.value) myLocation = LocationHelper.lastKnownLatLon(context)
    }

    val storesWithDistance = remember(myLocation) {
        val loc = myLocation
        StoreCatalog.all
            .map { store ->
                val distance = loc?.let { (lat, lon) -> LocationHelper.distanceMeters(lat, lon, store.latitude, store.longitude) }
                store to distance
            }
            .sortedBy { it.second ?: Double.MAX_VALUE }
    }

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Column(modifier = Modifier.fillMaxSize()) {
        if (myLocation == null) {
            Text(
                if (locationGranted.value) "מאתר את מיקומך" else "אשר הרשאת מיקום כדי לראות מרחקים",
                color = theme.textColor.copy(alpha = 0.6f),
                fontSize = FutureTypography.summary,
                modifier = Modifier.padding(16.dp),
            )
        }
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .focusRequester(focusRequester)
                .focusable()
                .onKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                    when (event.key) {
                        Key.DirectionDown -> { scope.launch { listState.animateScrollBy(220f) }; true }
                        Key.DirectionUp -> { scope.launch { listState.animateScrollBy(-220f) }; true }
                        else -> false
                    }
                },
            contentPadding = PaddingValues(16.dp),
        ) {
            items(storesWithDistance, key = { it.first.id }) { (store, distance) ->
                StoreRow(store = store, distanceMeters = distance, theme = theme)
            }
        }
    }
}

@Composable
private fun StoreRow(store: Store, distanceMeters: Double?, theme: FutureTheme) {
    // שורה שאינה מקבלת פוקוס - כרטיס מידע, ולכן ברדיוס הכרטיס (16dp).
    val shape = FutureShapes.lg
    val bgColor by animateColorAsState(theme.surfaceColor, FutureMotion.focusColorSpec, label = "storeRowBg")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp)
            .clip(shape)
            .background(bgColor)
            .border(1.dp, theme.textColor.copy(alpha = 0.08f), shape)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.Storefront, contentDescription = null, tint = theme.accentColor)
        Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(store.name, color = theme.textColor, fontSize = FutureTypography.bodyLarge, fontWeight = FontWeight.Bold)
            Text(store.category, color = theme.textColor.copy(alpha = 0.6f), fontSize = FutureTypography.label)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Rounded.LocationOn,
                    contentDescription = null,
                    tint = theme.textColor.copy(alpha = 0.5f),
                    modifier = Modifier.padding(end = 2.dp),
                )
                Text(store.address, color = theme.textColor.copy(alpha = 0.5f), fontSize = FutureTypography.label)
            }
        }
        if (distanceMeters != null) {
            Text(
                formatDistance(distanceMeters),
                color = theme.accentColor,
                fontSize = FutureTypography.body,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

private fun formatDistance(meters: Double): String {
    return if (meters < 1000) "${meters.toInt()} מ'" else "%.1f ק\"מ".format(meters / 1000)
}
