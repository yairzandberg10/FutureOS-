package com.future.navigation.ui.routes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.future.navigation.data.common.LatLng
import com.future.navigation.data.geocoding.GeocodeResult
import com.future.navigation.data.geocoding.GeocodingRepository
import com.future.navigation.data.gtfs.SavedPlaceEntity
import com.future.navigation.ui.home.RowIcon
import com.future.navigation.ui.navigation.Destination
import com.future.sharednav.components.FutureListItem
import com.future.sharednav.components.FutureSectionHeader
import com.future.sharednav.components.FutureTextField
import com.future.sharednav.components.ScreenTopBar
import com.future.sharednav.focus.escapeTextFieldFocusTrap
import com.future.sharednav.icons.FutureIcons
import com.future.sharednav.theme.FutureDimens
import com.future.sharednav.theme.LocalFutureTheme
import com.future.sharednav.theme.mutedTextColor
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * "בחירת נקודת התחלה למסלול" (כמו באגד): המיקום הנוכחי, בית, עבודה, מועדפים,
 * או חיפוש של כל כתובת/מקום. בחירה מחזירה Destination; null = המיקום הנוכחי.
 */
@Composable
fun OriginPickerScreen(
    onBack: () -> Unit,
    onPicked: (Destination?) -> Unit,
    savedPlaces: Flow<List<SavedPlaceEntity>> = flowOf(emptyList()),
    geocoding: GeocodingRepository = remember { GeocodingRepository() },
) {
    val theme = LocalFutureTheme.current
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<GeocodeResult>>(emptyList()) }
    var searching by remember { mutableStateOf(false) }
    val places by savedPlaces.collectAsState(initial = emptyList())

    // חיפוש אחרי הפסקה קצרה בהקלדה - כל אות היא לא בקשה (Nominatim מגביל לבקשה בשנייה)
    LaunchedEffect(query) {
        if (query.isBlank()) {
            results = emptyList()
            searching = false
            return@LaunchedEffect
        }
        searching = true
        delay(600)
        results = geocoding.search(query)
        searching = false
    }

    fun SavedPlaceEntity.toDestination() =
        Destination(name = label.ifBlank { address }, address = address, location = LatLng(lat, lon))

    Column(modifier = Modifier.fillMaxSize().escapeTextFieldFocusTrap()) {
        ScreenTopBar(
            title = "בחירת נקודת מוצא",
            textColor = theme.textColor,
            accentColor = theme.accentColor,
            onBack = onBack,
        )
        FutureTextField(
            value = query,
            onValueChange = { query = it },
            theme = theme,
            placeholder = "תחנה, כתובת או שם מקום",
            leading = {
                Icon(
                    FutureIcons.Search,
                    contentDescription = null,
                    tint = theme.mutedTextColor,
                    modifier = Modifier.size(FutureDimens.iconTopBar),
                )
            },
            modifier = Modifier.fillMaxWidth().padding(horizontal = FutureDimens.screenPadding),
        )
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = FutureDimens.screenPadding, vertical = FutureDimens.spacingSm),
            verticalArrangement = Arrangement.spacedBy(FutureDimens.spacingSm),
        ) {
            if (query.isBlank()) {
                item(key = "current") {
                    FutureListItem(
                        title = "המיקום שלך",
                        summary = "יציאה מהמיקום הנוכחי",
                        theme = theme,
                        onClick = { onPicked(null) },
                        leading = { RowIcon(FutureIcons.MyLocation, theme) },
                    )
                }
                val home = places.firstOrNull { it.isHome }
                val work = places.firstOrNull { it.isWork }
                home?.let { place ->
                    item(key = "home") {
                        FutureListItem(
                            title = "בית",
                            summary = place.address,
                            theme = theme,
                            onClick = { onPicked(place.toDestination()) },
                            leading = { RowIcon(FutureIcons.Home, theme) },
                        )
                    }
                }
                work?.let { place ->
                    item(key = "work") {
                        FutureListItem(
                            title = "עבודה",
                            summary = place.address,
                            theme = theme,
                            onClick = { onPicked(place.toDestination()) },
                            leading = { RowIcon(FutureIcons.Work, theme) },
                        )
                    }
                }
                val favorites = places.filter { it.isFavorite && !it.isHome && !it.isWork }
                if (favorites.isNotEmpty()) {
                    item(key = "fav-header") { FutureSectionHeader("מועדפים", theme, inset = false) }
                    items(favorites, key = { "fav-${it.id}" }) { place ->
                        FutureListItem(
                            title = place.label.ifBlank { place.address },
                            summary = place.address,
                            theme = theme,
                            onClick = { onPicked(place.toDestination()) },
                            leading = { RowIcon(FutureIcons.LocationOn, theme) },
                        )
                    }
                }
            } else {
                items(results, key = { "r-${it.label}-${it.location.lat}-${it.location.lon}" }) { result ->
                    val parts = result.label.split(", ", limit = 2)
                    FutureListItem(
                        title = parts[0],
                        summary = parts.getOrNull(1),
                        theme = theme,
                        onClick = {
                            onPicked(Destination(name = parts[0], address = result.label, location = result.location))
                        },
                        leading = { RowIcon(FutureIcons.LocationOn, theme) },
                    )
                }
                if (results.isEmpty()) {
                    item(key = "empty") {
                        Text(
                            if (searching) "מחפש..." else "לא נמצאו תוצאות (או שאין רשת).",
                            color = theme.mutedTextColor,
                            modifier = Modifier.padding(horizontal = FutureDimens.spacingSm),
                        )
                    }
                }
            }
        }
    }
}
