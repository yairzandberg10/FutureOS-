package com.future.navigation.ui.home
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material.icons.rounded.Work

import com.future.sharednav.icons.FutureIcons
import com.future.sharednav.components.FutureAvatar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.future.navigation.R
import com.future.navigation.data.common.LatLng
import com.future.navigation.data.geocoding.GeocodeResult
import com.future.navigation.data.gtfs.SavedPlaceEntity
import com.future.navigation.ui.map.LocationPreviewMap
import com.future.navigation.ui.navigation.TravelMode
import com.future.sharednav.components.FutureListItem
import com.future.sharednav.components.FutureSectionHeader
import com.future.sharednav.components.FutureTabRow
import com.future.sharednav.components.FutureTextField
import com.future.sharednav.components.TopBarIconButton
import com.future.sharednav.focus.FocusableItem
import com.future.sharednav.focus.escapeTextFieldFocusTrap
import com.future.sharednav.theme.FutureDimens
import com.future.sharednav.theme.FutureShapes
import com.future.sharednav.theme.FutureTheme
import com.future.sharednav.theme.FutureTypography
import com.future.sharednav.theme.LocalFutureTheme
import com.future.sharednav.theme.idleChipColor
import com.future.sharednav.theme.idleFieldColor
import com.future.sharednav.theme.mutedTextColor
import com.future.sharednav.theme.rememberFutureType
import com.future.sharednav.theme.secondaryTextColor

/**
 * מסך הבית של הניווט. הרכיבים הם של הדיזיין סיסטם: שדה החיפוש הוא
 * FutureTextField (היה OutlinedTextField), בורר נהיגה/תחבורה ציבורית הוא
 * FutureTabRow (היה כפתור-פלח בנוי ידנית), ושורות התוצאות הן FutureListItem.
 *
 * צבע ההדגשה לא צובע יותר אייקונים דקורטיביים (סיכת מיקום, מקום בתוצאה):
 * "Accent means exactly one thing: this is where your focus or your
 * selection is" (README של הדיזיין סיסטם).
 */
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onDestinationPicked: (GeocodeResult) -> Unit,
    onOpenSavedPlaces: () -> Unit,
    onOpenGtfsSetup: () -> Unit
) {
    val theme = LocalFutureTheme.current
    val type = rememberFutureType()
    val mode by viewModel.mode.collectAsState()
    val query by viewModel.searchQuery.collectAsState()
    val results by viewModel.searchResults.collectAsState()
    val searching by viewModel.searching.collectAsState()
    val homePlace by viewModel.homePlace.collectAsState(initial = null)
    val workPlace by viewModel.workPlace.collectAsState(initial = null)
    val nearbyStops by viewModel.nearbyStops.collectAsState()
    val currentLocation by viewModel.currentLocation.collectAsState()
    val searchFocusRequester = remember { FocusRequester() }

    // פוקוס D-pad התחלתי על שדה החיפוש - בלי זה נחיתה על מסך הבית משאירה אותו
    // בלי שום פריט מודגש (בדיוק כמו בשאר 5 המסכים באפליקציה הזו).
    LaunchedEffect(Unit) { runCatching { searchFocusRequester.requestFocus() } }

    Column(modifier = Modifier.fillMaxSize().escapeTextFieldFocusTrap()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = FutureDimens.screenPadding, vertical = FutureDimens.spacingMd),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(FutureDimens.spacingSm),
        ) {
            Icon(
                Icons.Rounded.LocationOn,
                contentDescription = null,
                tint = theme.mutedTextColor,
                modifier = Modifier.size(FutureDimens.iconTopBar),
            )
            Text(
                text = currentLocation?.let { "%.4f, %.4f".format(it.lat, it.lon) }
                    ?: stringResource(R.string.current_location_unknown),
                fontSize = type.body,
                color = theme.secondaryTextColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            TopBarIconButton(
                icon = Icons.Rounded.Place,
                contentDescription = stringResource(R.string.gtfs_setup_title),
                textColor = theme.textColor,
                accentColor = theme.accentColor,
                onClick = onOpenGtfsSetup,
            )
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = FutureDimens.screenPadding)
                .height(MapPreviewHeight),
            shape = FutureShapes.lg,
        ) {
            LocationPreviewMap(location = currentLocation)
        }

        FutureTextField(
            value = query,
            onValueChange = viewModel::onSearchQueryChanged,
            theme = theme,
            placeholder = stringResource(R.string.search_placeholder),
            focusRequester = searchFocusRequester,
            leading = {
                Icon(
                    FutureIcons.Search,
                    contentDescription = null,
                    tint = theme.mutedTextColor,
                    modifier = Modifier.size(FutureDimens.iconTopBar),
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = FutureDimens.screenPadding)
                .padding(top = FutureDimens.itemSpacing),
        )

        FutureTabRow(
            items = listOf(stringResource(R.string.mode_drive), stringResource(R.string.mode_transit)),
            selectedIndex = mode.ordinal,
            theme = theme,
            onSelect = { viewModel.setMode(TravelMode.entries[it]) },
            modifier = Modifier.padding(top = FutureDimens.spacingSm),
        )

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = FutureDimens.screenPadding, vertical = FutureDimens.spacingSm),
            verticalArrangement = Arrangement.spacedBy(FutureDimens.spacingSm)
        ) {
            if (query.isNotBlank()) {
                item {
                    FutureSectionHeader(
                        text = if (searching) "מחפש" else "תוצאות חיפוש",
                        theme = theme,
                        inset = false,
                    )
                }
                // המפתח חייב להיות טיפוס שאפשר לשמור ב-Bundle (Compose שומר דרכו
                // את ה-state של השורה): key = { it } העביר את GeocodeResult עצמו
                // והפיל את האפליקציה עם IllegalArgumentException ברגע שהופיעה
                // תוצאת חיפוש ראשונה. המחרוזת יציבה לאותו מקום בין חיפושים,
                // וייחודית כי GeocodingRepository מחזיר רשימה בלי כפילויות.
                items(results, key = { "${it.location.lat},${it.location.lon}|${it.label}" }) { result ->
                    // כתובת מלאה ארוכה משורה - החלק הראשון (הרחוב/המקום) הוא
                    // הכותרת, וההמשך (עיר, מדינה) הוא שורת הסיכום.
                    val parts = result.label.split(", ", limit = 2)
                    FutureListItem(
                        title = parts[0],
                        summary = parts.getOrNull(1),
                        theme = theme,
                        onClick = { onDestinationPicked(result) },
                        leading = { RowIcon(Icons.Rounded.Place, theme) },
                    )
                }
            } else {
                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(FutureDimens.itemSpacing)) {
                        QuickPlaceTile(
                            icon = FutureIcons.Home,
                            label = stringResource(R.string.quick_home),
                            emptyHint = stringResource(R.string.add_home_address),
                            place = homePlace,
                            theme = theme,
                            modifier = Modifier.weight(1f)
                        ) { homePlace?.let { onDestinationPicked(GeocodeResult(it.address, LatLng(it.lat, it.lon))) } ?: onOpenSavedPlaces() }
                        QuickPlaceTile(
                            icon = Icons.Rounded.Work,
                            label = stringResource(R.string.quick_work),
                            emptyHint = stringResource(R.string.add_work_address),
                            place = workPlace,
                            theme = theme,
                            modifier = Modifier.weight(1f)
                        ) { workPlace?.let { onDestinationPicked(GeocodeResult(it.address, LatLng(it.lat, it.lon))) } ?: onOpenSavedPlaces() }
                    }
                }

                if (mode == TravelMode.TRANSIT) {
                    item {
                        FutureSectionHeader(stringResource(R.string.nearby_stops), theme, inset = false)
                    }
                    items(nearbyStops) { nearby ->
                        FutureListItem(
                            title = nearby.stop.name,
                            summary = "%.0f מ׳".format(nearby.distanceMeters),
                            theme = theme,
                            onClick = {
                                onDestinationPicked(GeocodeResult(nearby.stop.name, LatLng(nearby.stop.lat, nearby.stop.lon)))
                            },
                            leading = { RowIcon(Icons.Rounded.Place, theme) },
                        )
                    }
                }
            }
        }
    }
}

/** אייקון בתחילת שורה: עיגול 36dp ב-8% מהטקסט, האייקון עצמו בצבע הטקסט. */
/** אייקון בתחילת שורה - האווטאר של הדיזיין סיסטם (Avatar.jsx), עם אייקון. */
@Composable
internal fun RowIcon(icon: ImageVector, theme: FutureTheme) {
    FutureAvatar(theme = theme, icon = icon)
}

/**
 * אריח "בית" / "עבודה" - שניים זה לצד זה, ולכן אריח (6% ברקע) ולא שורת
 * רשימה שקופה. הפוקוס הוא של שורת רשימה (FocusableItem).
 */
@Composable
private fun QuickPlaceTile(
    icon: ImageVector,
    label: String,
    emptyHint: String,
    place: SavedPlaceEntity?,
    theme: FutureTheme,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val type = rememberFutureType()
    FocusableItem(
        onClick = onClick,
        accentColor = theme.accentColor,
        modifier = modifier,
        idleBackgroundColor = theme.idleChipColor,
        contentPadding = FutureDimens.spacingSm,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(FutureDimens.spacingSm),
        ) {
            RowIcon(icon, theme)
            Column(modifier = Modifier.weight(1f)) {
                Text(label, color = theme.textColor, fontSize = type.title, fontWeight = FutureTypography.weightMedium, maxLines = 1)
                Text(
                    place?.address ?: emptyHint,
                    fontSize = type.summary,
                    color = theme.mutedTextColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** 140dp - התצוגה המקדימה של המפה במסך הבית. */
private val MapPreviewHeight = 140.dp
