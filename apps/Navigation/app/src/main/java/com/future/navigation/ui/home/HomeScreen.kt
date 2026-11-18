package com.future.navigation.ui.home

import com.future.sharednav.i18n.tr

import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.onFocusChanged
import com.future.sharednav.icons.FutureIcons
import com.future.sharednav.components.FutureAvatar
import com.future.sharednav.components.FutureChip

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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
import com.future.navigation.data.poi.PlaceSorting
import com.future.navigation.data.poi.PoiCategory
import com.future.navigation.ui.map.LocationPreviewMap
import com.future.navigation.ui.navigation.TravelMode
import com.future.sharednav.components.FutureListItem
import com.future.sharednav.components.FutureSectionHeader
import com.future.sharednav.components.FutureTabRow
import com.future.sharednav.components.FutureTextField
import com.future.sharednav.components.TopBarIconButton
import com.future.sharednav.focus.FocusableItem
import com.future.sharednav.focus.escapeTextFieldFocusTrap
import com.future.sharednav.nav.onStarKeyPress
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
 * FutureTextField, בורר נהיגה/תחבורה ציבורית הוא FutureTabRow, ושורות
 * התוצאות הן FutureListItem.
 *
 * כשהשדה ריק: אריחי בית/עבודה, מועדפים, יעדים אחרונים, קטגוריות בקרבת מקום
 * (דלק, חניה, ...), ובמצב תחבורה ציבורית גם תחנות קרובות.
 * על שורה ממוקדת, לחיצה על * פותחת תפריט פעולות (ר' PlaceActionsMenu).
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
    onOpenGtfsSetup: () -> Unit,
    /** פתיחת לוח היציאות של תחנה (שם, מיקום). */
    onOpenStation: (String, LatLng) -> Unit = { _, _ -> }
) {
    val stationResults by viewModel.stationResults.collectAsState()
    val theme = LocalFutureTheme.current
    val type = rememberFutureType()
    val mode by viewModel.mode.collectAsState()
    val query by viewModel.searchQuery.collectAsState()
    val searchHits by viewModel.searchHits.collectAsState()
    val searching by viewModel.searching.collectAsState()
    val searchFailed by viewModel.searchFailed.collectAsState()
    val homePlace by viewModel.homePlace.collectAsState(initial = null)
    val workPlace by viewModel.workPlace.collectAsState(initial = null)
    val nearbyStops by viewModel.nearbyStops.collectAsState()
    val favoriteStations by com.future.navigation.data.transit.FavoriteStations
        .get(androidx.compose.ui.platform.LocalContext.current).favorites.collectAsState()
    val currentLocation by viewModel.currentLocation.collectAsState()
    val recents by viewModel.recents.collectAsState()
    val favorites by viewModel.favorites.collectAsState()
    val poi by viewModel.poi.collectAsState()
    val searchFocusRequester = remember { FocusRequester() }
    // NV1: escapeTextFieldFocusTrap מזיז את הפוקוס ב-↑/↓ בחיפוש גיאומטרי, ומתוך
    // ה-LazyColumn הוא דילג על לשוניות "נהיגה"/"תחבורה ציבורית" וחזר לשדה החיפוש.
    // הנתיב מוגדר במפורש: חיפוש ↓ לשוניות, ואריחי בית/עבודה ↑ לשוניות.
    val modeTabsFocusRequester = remember { FocusRequester() }

    // מקום שהפוקוס עליו כרגע ברשימה, וההעתק שלו שנפתח בתפריט (*). ההעתק נדרש כי
    // השורה מאבדת פוקוס כשהתפריט נפתח.
    var focusedTarget by remember { mutableStateOf<PlaceTarget?>(null) }
    var menuTarget by remember { mutableStateOf<PlaceTarget?>(null) }
    // אריח בית/עבודה בפוקוס: * עליו פותח את המקומות השמורים (לשינוי או ניקוי)
    var focusedSlot by remember { mutableStateOf<HomeSlot?>(null) }
    // אחרי הסרה השורה נעלמת יחד עם הפוקוס שלה, ולכן הפוקוס חוזר ללשוניות
    var refocusTabs by remember { mutableStateOf(false) }
    // * מגיע מהמערכת רק כשידור (לא כ-KeyEvent), ר' onStarKeyPress.
    onStarKeyPress {
        val slot = focusedSlot
        if (slot != null) {
            onOpenSavedPlaces()
        } else if (menuTarget == null) {
            focusedTarget?.let { menuTarget = it }
        }
    }

    // שורה שנעלמה (קטגוריה נסגרה, חיפוש חדש, מועדף או אחרון שנמחק) לא נשארת יעד לתפריט *
    LaunchedEffect(query, poi.category, recents, favorites) {
        focusedTarget = null
    }
    LaunchedEffect(refocusTabs) {
        if (refocusTabs) {
            refocusTabs = false
            runCatching { modeTabsFocusRequester.requestFocus() }
        }
    }

    // חזרה (BACK) סוגרת קטגוריה פתוחה, ולא יוצאת מהמסך. בזמן חיפוש BACK נשאר לחיפוש.
    BackHandler(enabled = poi.category != null && query.isBlank()) { viewModel.clearCategory() }

    // בחירת יעד מהחיפוש, מהאחרונים או מקטגוריה: נשמר באחרונים ואז ממשיך לנווט.
    // אריחי בית/עבודה לא עוברים כאן (הם כבר שמורים).
    val pick: (GeocodeResult) -> Unit = { result ->
        viewModel.rememberRecent(result)
        onDestinationPicked(result)
    }

    // מסמן את השורה הממוקדת כיעד לתפריט *, ומנקה כשהפוקוס יוצא ממנה.
    val trackFocus: (PlaceTarget) -> Modifier = { target ->
        Modifier.onFocusChanged { state ->
            if (state.hasFocus) {
                focusedTarget = target
                focusedSlot = null
            } else if (focusedTarget == target) {
                focusedTarget = null
            }
        }
    }
    // אריח בית/עבודה: מסמן את האריח הממוקד (ר' onStarKeyPress)
    val trackSlot: (HomeSlot) -> Modifier = { slot ->
        Modifier.onFocusChanged { state ->
            if (state.hasFocus) {
                focusedTarget = null
                focusedSlot = slot
            } else if (focusedSlot == slot) {
                focusedSlot = null
            }
        }
    }

    menuTarget?.let { target ->
        val isFavorite = favorites.any { it.address == target.address }
        PlaceActionsMenu(
            target = target,
            isFavorite = isFavorite,
            theme = theme,
            onDismiss = { menuTarget = null },
            onNavigate = { pick(target.asGeocodeResult()) },
            onSaveHome = { viewModel.saveAsHome(target) },
            onSaveWork = { viewModel.saveAsWork(target) },
            onToggleFavorite = {
                // הסרה מהמועדפים מוציאה את השורה מהרשימה, ולכן הפוקוס חוזר ללשוניות
                if (isFavorite) refocusTabs = true
                viewModel.toggleFavorite(target)
            },
            onRemoveRecent = {
                refocusTabs = true
                viewModel.removeRecent(target)
            },
        )
    }

    // פוקוס D-pad התחלתי על שדה החיפוש - בלי זה נחיתה על מסך הבית משאירה אותו
    // בלי שום פריט מודגש (בדיוק כמו בשאר 5 המסכים באפליקציה הזו).
    LaunchedEffect(Unit) { runCatching { searchFocusRequester.requestFocus() } }

    // מיקום חי רק כשהמסך בחזית: מתחילים בחזרה למסך, ומפסיקים ביציאה ממנו (F32).
    val homeLifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    androidx.compose.runtime.DisposableEffect(homeLifecycleOwner, viewModel) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            when (event) {
                androidx.lifecycle.Lifecycle.Event.ON_RESUME -> viewModel.startLiveLocation()
                androidx.lifecycle.Lifecycle.Event.ON_PAUSE -> viewModel.stopLiveLocation()
                else -> {}
            }
        }
        homeLifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            homeLifecycleOwner.lifecycle.removeObserver(observer)
            viewModel.stopLiveLocation()
        }
    }

    Column(modifier = Modifier.fillMaxSize().escapeTextFieldFocusTrap()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = FutureDimens.screenPadding, vertical = FutureDimens.spacingMd),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(FutureDimens.spacingSm),
        ) {
            Icon(
                FutureIcons.LocationOn,
                contentDescription = null,
                tint = theme.mutedTextColor,
                modifier = Modifier.size(FutureDimens.iconTopBar),
            )
            Text(
                // LRI…PDI: קו הרוחב ראשון. בשורה RTL קו האורך הוצג לפניו.
                text = currentLocation?.let { "⁦%.4f, %.4f⁩".format(it.lat, it.lon) }
                    ?: stringResource(R.string.current_location_unknown),
                fontSize = type.body,
                color = theme.secondaryTextColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            // אין כאן יותר כפתור הורדת נתוני תחבורה: המסלולים והתחנות מגיעים מהרשת
            // (ר' OpenApis), ואין צורך להוריד את קובץ ה-GTFS הארצי.
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
                .padding(top = FutureDimens.itemSpacing)
                .focusProperties { down = modeTabsFocusRequester },
        )

        FutureTabRow(
            // ארבעה מצבים בשורה אחת: שם קצר לתחבורה ציבורית כדי שייכנסו ב-320dp
            items = listOf(
                stringResource(R.string.mode_drive),
                stringResource(R.string.mode_transit_short),
                stringResource(R.string.mode_walk),
                stringResource(R.string.mode_bike),
            ),
            selectedIndex = mode.ordinal,
            theme = theme,
            onSelect = { viewModel.setMode(TravelMode.entries[it]) },
            modifier = Modifier.padding(top = FutureDimens.spacingSm),
            firstItemFocusRequester = modeTabsFocusRequester,
        )

        // פתיחת קטגוריה גוללת את שורת הצ'יפים לראש הרשימה, כדי שהתוצאות שמתחתיה ייראו
        // (בלי זה הן נשארו מתחת למסך, ו"מחפש..." לא ממוקד ולכן גם לא גולל אליו)
        val homeListState = androidx.compose.foundation.lazy.rememberLazyListState()
        LaunchedEffect(poi.category) {
            if (poi.category == null) return@LaunchedEffect
            val chipsIndex = homeListState.layoutInfo.visibleItemsInfo.firstOrNull { it.key == "poi-chips" }?.index
            if (chipsIndex != null) runCatching { homeListState.animateScrollToItem(chipsIndex) }
        }
        LazyColumn(
            state = homeListState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = FutureDimens.screenPadding, vertical = FutureDimens.spacingSm),
            verticalArrangement = Arrangement.spacedBy(FutureDimens.spacingSm)
        ) {
            if (query.isNotBlank() && stationResults.isNotEmpty()) {
                item { FutureSectionHeader(text = tr("תחנות"), theme = theme, inset = false) }
                items(stationResults, key = { "station|${it.location.lat},${it.location.lon}|${it.name}" }) { station ->
                    FutureListItem(
                        title = station.name,
                        summary = station.lines.takeIf { it.isNotEmpty() }?.take(8)?.joinToString(" · ", prefix = tr("קווים: ")),
                        theme = theme,
                        onClick = { onOpenStation(station.name, station.location) },
                        leading = { RowIcon(FutureIcons.LocationOn, theme) },
                    )
                }
            }
            if (query.isNotBlank()) {
                item {
                    FutureSectionHeader(
                        text = if (searching) tr("מחפש") else tr("תוצאות חיפוש"),
                        theme = theme,
                        inset = false,
                    )
                }
                // בלי תוצאות - מסבירים למה: אין רשת, או שלא נמצא דבר (F31).
                if (!searching && searchHits.isEmpty()) {
                    item {
                        Text(
                            if (searchFailed) "אין חיבור לאינטרנט. נסו שוב כשיש רשת." else "לא נמצאו תוצאות לחיפוש הזה.",
                            color = theme.textColor.copy(alpha = 0.6f),
                            modifier = Modifier.padding(horizontal = FutureDimens.spacingSm),
                        )
                    }
                }
                // המפתח חייב להיות טיפוס שאפשר לשמור ב-Bundle (Compose שומר דרכו
                // את ה-state של השורה): key = { it } העביר את GeocodeResult עצמו
                // והפיל את האפליקציה עם IllegalArgumentException ברגע שהופיעה
                // תוצאת חיפוש ראשונה. המחרוזת יציבה לאותו מקום בין חיפושים,
                // וייחודית כי הרשימה עברה דה-דופליקציה ב-PlaceSorting.
                items(searchHits, key = { "${it.result.location.lat},${it.result.location.lon}|${it.result.label}" }) { hit ->
                    // כתובת מלאה ארוכה משורה - החלק הראשון (הרחוב/המקום) הוא
                    // הכותרת, וההמשך (עיר, מדינה) והמרחק הם שורת הסיכום.
                    val result = hit.result
                    val parts = result.label.split(", ", limit = 2)
                    FutureListItem(
                        title = parts[0],
                        summary = summaryOf(hit.distanceMeters, parts.getOrNull(1)),
                        theme = theme,
                        onClick = { pick(result) },
                        leading = { RowIcon(FutureIcons.LocationOn, theme) },
                        modifier = trackFocus(PlaceTarget(parts[0], result.label, result.location, fromRecent = false)),
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
                            modifier = Modifier.weight(1f).focusProperties { up = modeTabsFocusRequester }.then(trackSlot(HomeSlot.HOME))
                        ) { homePlace?.let { onDestinationPicked(GeocodeResult(it.address, LatLng(it.lat, it.lon))) } ?: onOpenSavedPlaces() }
                        QuickPlaceTile(
                            icon = FutureIcons.Work,
                            label = stringResource(R.string.quick_work),
                            emptyHint = stringResource(R.string.add_work_address),
                            place = workPlace,
                            theme = theme,
                            modifier = Modifier.weight(1f).focusProperties { up = modeTabsFocusRequester }.then(trackSlot(HomeSlot.WORK))
                        ) { workPlace?.let { onDestinationPicked(GeocodeResult(it.address, LatLng(it.lat, it.lon))) } ?: onOpenSavedPlaces() }
                    }
                }

                if (favorites.isNotEmpty()) {
                    item { FutureSectionHeader(text = tr("מועדפים"), theme = theme, inset = false) }
                    // מפתח לפי מזהה: אותו מקום יכול להופיע גם במועדפים וגם באחרונים
                    items(favorites, key = { "fav|${it.id}" }) { fav ->
                        val target = PlaceTarget(
                            title = fav.label.ifBlank { fav.address },
                            address = fav.address,
                            location = LatLng(fav.lat, fav.lon),
                            fromRecent = false,
                        )
                        FutureListItem(
                            title = target.title,
                            summary = restOfAddress(fav.address),
                            theme = theme,
                            onClick = { pick(target.asGeocodeResult()) },
                            leading = { RowIcon(FutureIcons.Star, theme) },
                            modifier = trackFocus(target),
                        )
                    }
                }

                if (recents.isNotEmpty()) {
                    item { FutureSectionHeader(text = tr("אחרונים"), theme = theme, inset = false) }
                    items(recents, key = { "recent|${it.lat},${it.lon}|${it.address}" }) { recent ->
                        val target = PlaceTarget(
                            title = recent.title,
                            address = recent.address,
                            location = LatLng(recent.lat, recent.lon),
                            fromRecent = true,
                        )
                        FutureListItem(
                            title = target.title,
                            summary = restOfAddress(recent.address),
                            theme = theme,
                            onClick = { pick(target.asGeocodeResult()) },
                            leading = { RowIcon(FutureIcons.History, theme) },
                            modifier = trackFocus(target),
                        )
                    }
                }

                // קטגוריות בקרבת מקום: לחיצה על צ'יפ מחפשת מקומות סביב המשתמש.
                item { FutureSectionHeader(text = tr("קרוב אליי"), theme = theme, inset = false) }
                item(key = "poi-chips") {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(FutureDimens.spacingSm)) {
                        items(PoiCategory.entries, key = { it.name }) { category ->
                            FutureChip(
                                text = category.label,
                                theme = theme,
                                selected = poi.category == category,
                                onClick = { viewModel.toggleCategory(category) },
                            )
                        }
                    }
                }

                poi.category?.let { category ->
                    item { FutureSectionHeader(text = tr("{0} לפי מרחק", category.label), theme = theme, inset = false) }
                    when {
                        poi.noLocation -> item { HintText(tr("המיקום עדיין לא ידוע. נסו שוב בעוד רגע."), theme) }
                        poi.outsideIsrael -> item { HintText(tr("חיפוש בקרבת מקום זמין בישראל בלבד."), theme) }
                        poi.loading -> item { HintText(tr("מחפש..."), theme) }
                        poi.failed -> item { HintText(tr("אין חיבור לאינטרנט. נסו שוב כשיש רשת."), theme) }
                        poi.results.isEmpty() -> item {
                            HintText(tr("לא נמצאו מקומות ברדיוס של {0} ק״מ.", category.radiusMeters / 1000), theme)
                        }
                        else -> items(poi.results, key = { "poi|${it.location.lat},${it.location.lon}|${it.name}" }) { place ->
                            FutureListItem(
                                title = place.name,
                                summary = PlaceSorting.formatDistance(place.distanceMeters),
                                theme = theme,
                                onClick = { pick(GeocodeResult(place.name, place.location)) },
                                leading = { RowIcon(FutureIcons.LocationOn, theme) },
                                modifier = trackFocus(PlaceTarget(place.name, place.name, place.location, fromRecent = false)),
                            )
                        }
                    }
                }

                // תחנות קרובות רק כשאין קטגוריה פתוחה, ובמצב תחבורה ציבורית
                if (poi.category == null && mode == TravelMode.TRANSIT) {
                    // תחנות מועדפות (נשמרות בכוכבית בלוח התחנה) לפני התחנות הקרובות
                    if (favoriteStations.isNotEmpty()) {
                        item(key = "fav-stations-header") {
                            FutureSectionHeader("תחנות מועדפות", theme, inset = false)
                        }
                        items(favoriteStations, key = { "favstop:" + it.name }) { station ->
                            FutureListItem(
                                title = station.name,
                                theme = theme,
                                onClick = { onOpenStation(station.name, station.location) },
                                leading = { RowIcon(FutureIcons.LocationOn, theme) },
                            )
                        }
                    }
                    item {
                        FutureSectionHeader(stringResource(R.string.nearby_stops), theme, inset = false)
                    }
                    // מפתח לפי שם: כשהרשימה מתרעננת (מקומית -> שרת, או כל דקה) הפוקוס
                    // נשאר על אותה תחנה ולא קופץ לתחנה אחרת שתפסה את אותו מקום ברשימה
                    items(nearbyStops, key = { "stop:" + it.stop.name }) { nearby ->
                        FutureListItem(
                            title = nearby.stop.name,
                            summary = tr("%.0f מ׳").format(nearby.distanceMeters) + (nearby.departures?.let { " · $it" } ?: ""),
                            theme = theme,
                            onClick = {
                                onOpenStation(nearby.stop.name, LatLng(nearby.stop.lat, nearby.stop.lon))
                            },
                            leading = { RowIcon(FutureIcons.LocationOn, theme) },
                        )
                    }
                }
            }
        }
    }
}

/** כותרת הקטע שנשארת אחרי השם הראשון בכתובת ("רחוב, עיר, מדינה" -> "עיר, מדינה"). */
private fun restOfAddress(address: String): String? =
    address.substringAfter(", ", "").takeIf { it.isNotEmpty() }

/** שורת סיכום: המרחק מהמשתמש, ואחריו שאר הכתובת. */
private fun summaryOf(distanceMeters: Double?, rest: String?): String? =
    listOfNotNull(distanceMeters?.let { PlaceSorting.formatDistance(it) }, rest)
        .joinToString(" · ")
        .takeIf { it.isNotEmpty() }

/** אריחי הבית והעבודה - לקיצור * (פתיחת המקומות השמורים). */
private enum class HomeSlot { HOME, WORK }

/** הודעה קצרה בתוך הרשימה (מצב ריק, טוען, אין רשת). */
@Composable
private fun HintText(text: String, theme: FutureTheme) {
    Text(
        text,
        color = theme.textColor.copy(alpha = 0.6f),
        modifier = Modifier.padding(horizontal = FutureDimens.spacingSm),
    )
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
