package com.future.navigation.ui.home

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.future.navigation.data.geocoding.GeocodeResult
import com.future.navigation.data.geocoding.GeocodingRepository
import com.future.sharednav.components.FutureListItem
import com.future.sharednav.components.FutureTextField
import com.future.sharednav.components.ScreenTopBar
import com.future.sharednav.focus.escapeTextFieldFocusTrap
import com.future.sharednav.icons.FutureIcons
import com.future.sharednav.theme.FutureDimens
import com.future.sharednav.theme.LocalFutureTheme
import com.future.sharednav.theme.mutedTextColor
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay

/**
 * "הוספת מקום מועדף" ממסך הבית: חיפוש כתובת או מקום (כמו בחירת נקודת מוצא), ובחירה
 * בתוצאה שורה אחת שומרת אותה כמועדף דרך [onPicked] (ב-MainActivity -> HomeViewModel.addFavoritePlace).
 */
@Composable
fun AddFavoritePlaceScreen(
    onBack: () -> Unit,
    onPicked: (GeocodeResult) -> Unit,
    geocoding: GeocodingRepository = remember { GeocodingRepository() },
) {
    val theme = LocalFutureTheme.current
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<GeocodeResult>>(emptyList()) }
    var searching by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }

    // חיפוש אחרי הפסקה קצרה בהקלדה (Nominatim מגביל לבקשה אחת בשנייה)
    LaunchedEffect(query) {
        if (query.isBlank()) {
            results = emptyList()
            searching = false
            failed = false
            return@LaunchedEffect
        }
        searching = true
        delay(600)
        try {
            results = geocoding.search(query)
            failed = false
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            results = emptyList()
            failed = true
        }
        searching = false
    }

    Column(modifier = Modifier.fillMaxSize().escapeTextFieldFocusTrap()) {
        ScreenTopBar(
            title = "הוספת מקום מועדף",
            textColor = theme.textColor,
            accentColor = theme.accentColor,
            onBack = onBack,
        )
        FutureTextField(
            value = query,
            onValueChange = { query = it },
            theme = theme,
            placeholder = "כתובת או שם מקום",
            autoFocus = true,
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
            items(results, key = { "r-${it.label}-${it.location.lat}-${it.location.lon}" }) { result ->
                val parts = result.label.split(", ", limit = 2)
                FutureListItem(
                    title = parts[0],
                    summary = parts.getOrNull(1),
                    theme = theme,
                    onClick = { onPicked(result) },
                    leading = { RowIcon(FutureIcons.LocationOn, theme) },
                )
            }
            if (results.isEmpty() && query.isNotBlank()) {
                item(key = "empty") {
                    Text(
                        when {
                            searching -> "מחפש..."
                            failed -> "אין חיבור לאינטרנט. נסו שוב כשיש רשת."
                            else -> "לא נמצאו תוצאות לחיפוש הזה."
                        },
                        color = theme.mutedTextColor,
                        modifier = Modifier.padding(horizontal = FutureDimens.spacingSm),
                    )
                }
            }
        }
    }
}
