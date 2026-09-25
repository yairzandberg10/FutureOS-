package com.future.frixa.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.future.frixa.data.FricasseStores
import com.future.frixa.data.LocationHelper
import com.future.frixa.data.Store
import com.future.sharednav.components.EmptyState
import com.future.sharednav.components.FutureAvatar
import com.future.sharednav.components.FutureIndeterminateProgressBar
import com.future.sharednav.components.FutureListItem
import com.future.sharednav.components.FutureMenuRow
import com.future.sharednav.components.FutureOptionsMenu
import com.future.sharednav.icons.FutureIcons
import com.future.sharednav.nav.onOptionsKeyPress
import com.future.sharednav.theme.FutureTheme
import com.future.sharednav.theme.FutureTypography
import com.future.sharednav.theme.mutedTextColor
import com.future.sharednav.theme.readableAccentColor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * מקומות שמוכרים פריקסה, מהקרוב לרחוק (FricasseStores - OpenStreetMap).
 * OK פותח ניווט למקום; מקש Options - רענון, וחיוג כשיש למקום טלפון.
 */
@Composable
fun StoresScreen(theme: FutureTheme) {
    val context = LocalContext.current
    val locationGranted = rememberRuntimePermission(android.Manifest.permission.ACCESS_COARSE_LOCATION)
    var myLocation by remember { mutableStateOf<Pair<Double, Double>?>(null) }
    var stores by remember { mutableStateOf(FricasseStores.cached(context)) }
    var loading by remember { mutableStateOf(false) }
    var offline by remember { mutableStateOf(false) }
    var refreshKey by remember { mutableIntStateOf(0) }
    var focused by remember { mutableStateOf<Store?>(null) }
    var menuOpen by remember { mutableStateOf(false) }

    LaunchedEffect(locationGranted.value) {
        if (locationGranted.value) myLocation = LocationHelper.lastKnownLatLon(context)
    }
    LaunchedEffect(myLocation, refreshKey) {
        loading = true
        val result = withContext(Dispatchers.IO) { runCatching { FricasseStores.search(context, myLocation) } }
        result.onSuccess { stores = it; offline = false }.onFailure { offline = true }
        loading = false
    }
    onOptionsKeyPress { menuOpen = !menuOpen }

    val sorted = remember(stores, myLocation) {
        val loc = myLocation
        stores.map { s -> s to loc?.let { (lat, lon) -> LocationHelper.distanceMeters(lat, lon, s.latitude, s.longitude) } }
            .sortedBy { it.second ?: Double.MAX_VALUE }
    }
    val first = remember { FocusRequester() }
    LaunchedEffect(sorted.isNotEmpty()) { if (sorted.isNotEmpty()) runCatching { first.requestFocus() } }

    fun navigate(store: Store) {
        val uri = Uri.parse("geo:${store.latitude},${store.longitude}?q=${store.latitude},${store.longitude}(${Uri.encode(store.name)})")
        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        if (loading) FutureIndeterminateProgressBar(theme = theme, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp))
        val note = when {
            offline && stores.isNotEmpty() -> "אין חיבור - מוצגות התוצאות האחרונות"
            myLocation == null && !locationGranted.value -> "אשר מיקום כדי לראות מה קרוב אליך"
            myLocation == null -> "בלי מיקום - מוצגים מקומות בכל הארץ"
            else -> null
        }
        note?.let { Text(it, color = theme.mutedTextColor, fontSize = FutureTypography.summary, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) }

        when {
            sorted.isEmpty() && loading -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("מחפש מקומות", color = theme.mutedTextColor, fontSize = FutureTypography.body)
            }
            sorted.isEmpty() -> EmptyState(
                icon = FutureIcons.Storefront,
                title = if (offline) "אין חיבור לרשת" else "לא נמצאו מקומות",
                subtitle = "לחץ על מקש התפריט כדי לרענן",
                textColor = theme.textColor,
            )
            else -> LazyColumn(contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)) {
                itemsIndexed(sorted, key = { _, it -> it.first.id }) { index, (store, distance) ->
                    FutureListItem(
                        title = store.name,
                        summary = store.address.ifBlank { store.openingHours ?: "" }.ifBlank { null },
                        theme = theme,
                        onClick = { navigate(store) },
                        focusRequester = if (index == 0) first else null,
                        modifier = Modifier.onFocusChanged { if (it.isFocused) focused = store },
                        leading = { FutureAvatar(theme = theme, icon = FutureIcons.Storefront) },
                        trailing = {
                            distance?.let {
                                Text(formatDistance(it), color = theme.readableAccentColor, fontSize = FutureTypography.body, fontWeight = FutureTypography.weightBold)
                            }
                        },
                    )
                }
            }
        }
    }

    if (menuOpen) {
        FutureOptionsMenu(theme = theme, onDismissRequest = { menuOpen = false }, header = focused?.name ?: "חנויות") {
            focused?.let { store ->
                FutureMenuRow("נווט", FutureIcons.DirectionsCar, theme, { menuOpen = false; navigate(store) })
                store.phone?.let { phone ->
                    FutureMenuRow("התקשר", FutureIcons.Call, theme, {
                        menuOpen = false
                        runCatching { context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                    })
                }
            }
            FutureMenuRow("רענן", FutureIcons.Refresh, theme, { menuOpen = false; refreshKey++ })
        }
    }
}

private fun formatDistance(meters: Double): String =
    if (meters < 1000) "${meters.toInt()} מ'" else "%.1f ק\"מ".format(meters / 1000)
