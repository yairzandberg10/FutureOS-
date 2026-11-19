package com.future.navigation.ui.routes

import com.future.sharednav.i18n.tr
import com.future.sharednav.components.FutureButton
import com.future.sharednav.components.FutureProgressBar
import com.future.sharednav.components.FutureSpinner
import com.future.sharednav.theme.FutureDimens
import com.future.sharednav.theme.LocalFutureTheme
import com.future.sharednav.theme.mutedTextColor

import com.future.sharednav.theme.FutureShapes
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.future.navigation.R
import com.future.navigation.data.gtfs.LegType
import com.future.navigation.data.gtfs.TransitItinerary
import com.future.navigation.data.transit.TransitPlanning
import com.future.navigation.data.transit.TransitPrefs
import com.future.navigation.data.transit.TripWhen
import com.future.navigation.data.transit.TripWhenKind
import com.future.sharednav.components.FutureListItem
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.key.Key
import com.future.navigation.data.routing.DrivingRoute
import com.future.navigation.ui.navigation.Destination
import com.future.navigation.ui.navigation.TravelMode
import com.future.sharednav.components.ScreenTopBar
import com.future.sharednav.focus.FocusableItem
import com.future.sharednav.icons.FutureIcons
import androidx.compose.foundation.layout.width

@Composable
fun RouteOptionsScreen(
    viewModel: RouteOptionsViewModel,
    mode: TravelMode,
    destination: Destination,
    onBack: () -> Unit,
    onStartDriving: (DrivingRoute) -> Unit,
    onStartTransit: (TransitItinerary) -> Unit,
    /** נקודת המוצא שנבחרה, null = המיקום הנוכחי. */
    origin: Destination? = null,
    /** פותח את בחירת נקודת המוצא (OK על שורת מ/אל). */
    onPickOrigin: () -> Unit = {},
    /** מקש 7: החלפת כיוון בין המוצא ליעד. */
    onSwap: () -> Unit = {},
) {
    val loading by viewModel.loading.collectAsState()
    val downloadFraction by viewModel.downloadFraction.collectAsState()
    val error by viewModel.error.collectAsState()
    val drivingRoute by viewModel.drivingRoute.collectAsState()
    val itineraries by viewModel.itineraries.collectAsState()
    val askCellularDownload by viewModel.askCellularDownload.collectAsState()
    val actionFocusRequester = remember { FocusRequester() }
    val retryFocusRequester = remember { FocusRequester() }
    val serverError by viewModel.serverError.collectAsState()
    // שגיאת שרת: הפוקוס ההתחלתי על "נסה שוב", כדי שאפשר לנסות ב-OK מיד
    LaunchedEffect(serverError) {
        if (serverError) runCatching { retryFocusRequester.requestFocus() }
    }
    val theme = LocalFutureTheme.current

    LaunchedEffect(destination, mode, origin) {
        viewModel.search(mode, destination, from = origin?.location)
    }

    // הורדה של ~130MB ברשת סלולרית - שואלים לפני (F09). הברירה הבטוחה היא "לא עכשיו".
    if (askCellularDownload) {
        com.future.sharednav.components.ConfirmDialog(
            message = "נדרשת הורדה של כ-130MB לנתוני התחבורה, ברשת סלולרית. להוריד עכשיו?",
            surfaceColor = theme.surfaceColor,
            textColor = theme.textColor,
            dangerColor = theme.dangerColor,
            onCancel = viewModel::cancelCellularDownload,
            onConfirm = viewModel::confirmCellularDownload,
            cancelLabel = "לא עכשיו",
            confirmLabel = "הורד",
        )
    }

    // פוקוס D-pad התחלתי על תוצאת המסלול ברגע שהיא מוכנה - בלי זה נחיתה על
    // המסך משאירה אותו בלי שום פריט מודגש (כמו בשאר מסכי האפליקציה).
    LaunchedEffect(loading, error, mode, drivingRoute, itineraries) {
        if (!loading && error == null) {
            if (mode != TravelMode.TRANSIT && drivingRoute != null) actionFocusRequester.requestFocus()
            else if (mode == TravelMode.TRANSIT && itineraries.isNotEmpty()) actionFocusRequester.requestFocus()
        }
    }

    val avoidTolls by viewModel.avoidTolls.collectAsState()
    val departInMinutes by viewModel.departInMinutes.collectAsState()
    val alternatives by viewModel.alternatives.collectAsState()
    val selectedIndex by viewModel.selectedIndex.collectAsState()
    val drivingOptions = androidx.compose.runtime.remember { com.future.navigation.data.backend.FutureServer.isAvailable }
    val tripWhen by viewModel.tripWhen.collectAsState()
    val transitPrefs by viewModel.transitPrefs.collectAsState()
    // פאנל פתוח מעל המסך (מתי / העדפות). null = מסך התוצאות הרגיל.
    val panelState = remember { mutableStateOf<RoutePanel?>(null) }
    val panel = panelState.value
    // סגירת פאנל מחזירה את הפוקוס לשורה הראשונה של התוצאות
    LaunchedEffect(panel) {
        if (panel == null) runCatching { actionFocusRequester.requestFocus() }
    }
    if (panel != null) {
        when (panel) {
            RoutePanel.WHEN -> TransitWhenPanel(
                onPick = { choice ->
                    viewModel.setTripWhen(choice)
                    panelState.value = null
                },
                onClose = { panelState.value = null },
            )
            RoutePanel.PREFS -> TransitPrefsPanel(
                prefs = transitPrefs,
                onChange = viewModel::setTransitPrefs,
                onClose = { panelState.value = null },
            )
        }
        return
    }

    Column(
        modifier = Modifier.fillMaxSize().onPreviewKeyEvent { event ->
            // במקשים: 1 הימנעות מאגרה (נהיגה, רק דרך HERE בשרת), 3 שעת יציאה.
            // בלי שרת הנהיגה היא OSRM, שלא יודע להימנע מאגרה ולא מכיר שעת יציאה.
            if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
            when (com.future.sharednav.nav.digitForKey(event.key)) {
                "7" -> { onSwap(); true }
                "1" -> if (mode == TravelMode.DRIVE && drivingOptions) { viewModel.toggleAvoidTolls(); true } else false
                // תחבורה: 2 העדפות, 3 מתי, 4/6 הזזה של ±10 דק'. בנהיגה 3 נשאר מחזור שעת היציאה.
                "2" -> if (mode == TravelMode.TRANSIT) { panelState.value = RoutePanel.PREFS; true } else false
                "3" -> when {
                    mode == TravelMode.TRANSIT -> { panelState.value = RoutePanel.WHEN; true }
                    mode == TravelMode.DRIVE && drivingOptions -> { viewModel.cycleDepartureTime(); true }
                    else -> false
                }
                "4" -> if (mode == TravelMode.TRANSIT) { viewModel.shiftTripTime(-TransitPlanning.SHIFT_STEP_MINUTES); true } else false
                "6" -> if (mode == TravelMode.TRANSIT) { viewModel.shiftTripTime(TransitPlanning.SHIFT_STEP_MINUTES); true } else false
                else -> false
            }
        }
    ) {
        ScreenTopBar(
            title = stringResource(R.string.route_options_title),
            textColor = MaterialTheme.colorScheme.onBackground,
            accentColor = MaterialTheme.colorScheme.primary,
            onBack = onBack
        )

        // מ / אל, כמו באגד: OK בוחר נקודת מוצא אחרת, 7 מחליף כיוון
        Box(modifier = Modifier.padding(horizontal = FutureDimens.screenPadding)) {
            FocusableItem(
                onClick = onPickOrigin,
                accentColor = MaterialTheme.colorScheme.primary,
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(FutureDimens.spacingSm)) {
                    Text(
                        text = "מ: " + (origin?.let { it.name.ifBlank { it.address } } ?: "המיקום שלך"),
                        style = MaterialTheme.typography.bodyMedium,
                        color = theme.mutedTextColor,
                        maxLines = 1,
                    )
                    Text(
                        text = "אל: " + destination.address.ifBlank { destination.name },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                    )
                    Text(
                        text = "OK שינוי נקודת מוצא · 7 החלפת כיוון",
                        style = MaterialTheme.typography.labelSmall,
                        color = theme.mutedTextColor,
                        maxLines = 1,
                    )
                }
            }
        }

        val departLabel = "3 " + (if (departInMinutes == 0) "יציאה עכשיו" else "יציאה בעוד $departInMinutes דק׳")
        val optionsHint = when {
            mode == TravelMode.TRANSIT -> "3 " + TransitPlanning.whenLabel(tripWhen) + " · 4/6 ±10 דק׳ · 2 העדפות"
            mode == TravelMode.DRIVE && drivingOptions -> "1 " + (if (avoidTolls) "בלי כבישי אגרה ✓" else "כולל כבישי אגרה") + " · " + departLabel
            else -> null
        }
        if (optionsHint != null) {
            Text(
                text = optionsHint,
                color = theme.mutedTextColor,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = FutureDimens.screenPadding, vertical = FutureDimens.spacingSm),
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when {
                loading -> Column(
                    modifier = Modifier.align(Alignment.Center).padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // אורך ידוע (הורדה) = פס התקדמות; אורך לא ידוע (חישוב) = ספינר.
                    if (downloadFraction != null) {
                        Text(
                            text = tr("מוריד נתוני תחבורה ציבורית לאזור הזה %d%%").format((downloadFraction!! * 100).toInt()),
                            color = theme.mutedTextColor
                        )
                        Spacer(modifier = Modifier.height(FutureDimens.spacingMd))
                        FutureProgressBar(progress = downloadFraction!!, theme = theme)
                    } else {
                        FutureSpinner(theme = theme, label = stringResource(R.string.calculating_route))
                    }
                }
                error != null -> Column(
                    modifier = Modifier.align(Alignment.Center).padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text(
                        text = error ?: "",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    if (serverError) {
                        PrimaryButton("נסה שוב", focusRequester = retryFocusRequester) { viewModel.retry() }
                    }
                }
                mode != TravelMode.TRANSIT && drivingRoute != null -> RouteAlternatives(
                    routes = alternatives,
                    selectedIndex = selectedIndex,
                    onSelect = viewModel::selectRoute
                )
                mode == TravelMode.TRANSIT -> {
                    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        itemsIndexed(itineraries) { index, itinerary ->
                            ItineraryCard(
                                itinerary,
                                onClick = { onStartTransit(itinerary) },
                                focusRequester = if (index == 0) actionFocusRequester else null
                            )
                        }
                    }
                }
            }
        }

        if (mode != TravelMode.TRANSIT && drivingRoute != null) {
            Box(modifier = Modifier.fillMaxWidth().padding(FutureDimens.screenPadding)) {
                PrimaryButton(stringResource(R.string.start_navigation), focusRequester = actionFocusRequester) {
                    // ההתחלה במסלול שנבחר ברשימה (ברירת המחדל - הראשון)
                    onStartDriving(alternatives.getOrNull(selectedIndex) ?: drivingRoute!!)
                }
            }
        }
    }
}

/**
 * רשימת המסלולים (המסלול הראשי + חלופות). כל שורה: זמן, מרחק והכבישים העיקריים
 * של המסלול. לחיצה (OK) בוחרת מסלול - ההתחלה בכפתור "התחל ניווט" למטה.
 */
@Composable
private fun RouteAlternatives(routes: List<DrivingRoute>, selectedIndex: Int, onSelect: (Int) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = FutureDimens.screenPadding, vertical = FutureDimens.spacingSm),
        verticalArrangement = Arrangement.spacedBy(FutureDimens.spacingSm),
    ) {
        itemsIndexed(routes) { index, route ->
            val selected = index == selectedIndex
            FocusableItem(
                onClick = { onSelect(index) },
                accentColor = MaterialTheme.colorScheme.primary,
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(FutureDimens.spacingSm)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = tr("%d דק׳").format((route.durationSeconds / 60).toInt()),
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = tr("%.1f ק״מ").format(route.distanceMeters / 1000.0),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        if (selected) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(FutureIcons.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                    val roads = route.steps.map { it.streetName }.filter { it.isNotBlank() }.distinct().take(3)
                    if (roads.isNotEmpty()) {
                        Text(
                            text = roads.joinToString(" · "),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ItineraryCard(itinerary: TransitItinerary, onClick: () -> Unit, focusRequester: FocusRequester? = null) {
    FocusableItem(
        onClick = onClick,
        accentColor = MaterialTheme.colorScheme.primary,
        focusRequester = focusRequester
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(FutureDimens.spacingSm)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = tr("%d דק׳").format((itinerary.totalDurationSeconds / 60)),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    // שעת היציאה חשובה כמו משך הנסיעה: האוטובוס הבא יכול לצאת רק בעוד שעה
                    text = secondsToClock(itinerary.departureSeconds) + " ← " + secondsToClock(itinerary.arrivalSeconds),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = itinerary.legs.filter { it.type == LegType.RIDE }.joinToString(" + ") { it.routeShortName ?: it.routeLongName ?: "" },
                style = MaterialTheme.typography.bodyMedium
            )
            // כמה זמן עד היציאה הראשונה, ומאיזו תחנה
            val firstRide = itinerary.legs.firstOrNull { it.type == LegType.RIDE }
            val departure = firstRide?.departureSeconds
            if (firstRide != null && departure != null) {
                val now = TransitPlanning.secondsOfDay(java.time.ZonedDateTime.now(com.future.navigation.data.transit.ISRAEL_ZONE))
                Text(
                    text = tr("בעוד %d דק׳ מתחנה %s").format(TransitPlanning.minutesUntil(departure, now), firstRide.fromStopName),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                )
            }
            Text(
                text = tr("מחיר משוער: %s").format(TransitPlanning.formatShekel(TransitPlanning.estimatedFare(itinerary))),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(R.string.schedule_disclaimer),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            )
        }
    }
}

private enum class RoutePanel { WHEN, PREFS }

/**
 * פאנל "מתי" (תחבורה ציבורית): יציאה עכשיו / בזמן / הגעה עד / קווים אחרונים.
 * לשעה מקלידים 4 ספרות (למשל 0830) ו-OK. BACK חוזר לבחירה או סוגר.
 */
@Composable
private fun TransitWhenPanel(onPick: (TripWhen) -> Unit, onClose: () -> Unit) {
    val theme = LocalFutureTheme.current
    var entryKind by remember { mutableStateOf<TripWhenKind?>(null) }
    var digits by remember { mutableStateOf("") }
    val firstFocus = remember { FocusRequester() }
    BackHandler {
        if (entryKind != null) {
            entryKind = null
            digits = ""
        } else {
            onClose()
        }
    }
    LaunchedEffect(entryKind) { runCatching { firstFocus.requestFocus() } }

    val kind = entryKind
    Column(modifier = Modifier.fillMaxSize().background(theme.surfaceColor).padding(FutureDimens.screenPadding)) {
        Text(
            text = when (kind) {
                null -> "מתי לנסוע?"
                TripWhenKind.ARRIVE_BY -> "שעת הגעה"
                else -> "שעת יציאה"
            },
            color = theme.textColor,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(modifier = Modifier.height(FutureDimens.spacingMd))
        if (kind == null) {
            Column(verticalArrangement = Arrangement.spacedBy(FutureDimens.itemSpacing)) {
                FutureListItem("יציאה עכשיו", theme, onClick = { onPick(TripWhen(TripWhenKind.NOW)) }, focusRequester = firstFocus)
                FutureListItem("בחירת זמן יציאה", theme, onClick = {
                    entryKind = TripWhenKind.DEPART_AT
                    digits = ""
                })
                FutureListItem("בחירת זמן הגעה", theme, onClick = {
                    entryKind = TripWhenKind.ARRIVE_BY
                    digits = ""
                })
                FutureListItem(
                    "קווים אחרונים להיום",
                    theme,
                    onClick = { onPick(TripWhen(TripWhenKind.LAST_LINES)) },
                    summary = "הנסיעות שיוצאות מ-${TransitPlanning.LAST_LINES_HOUR}:00 והלאה",
                )
            }
        } else {
            val valid = digits.length == 4 && digits.take(2).toInt() < 24 && digits.takeLast(2).toInt() < 60
            val shown = digits.padEnd(4, '_')
            Column(
                modifier = Modifier.fillMaxWidth().onPreviewKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    val digit = com.future.sharednav.nav.digitForKey(event.key)
                    when {
                        digit != null -> {
                            if (digits.length < 4) digits += digit
                            true
                        }
                        event.key == Key.Backspace -> {
                            digits = digits.dropLast(1)
                            true
                        }
                        else -> false
                    }
                },
            ) {
                Text(
                    text = shown.take(2) + ":" + shown.drop(2),
                    color = theme.textColor,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.headlineMedium,
                )
                Spacer(modifier = Modifier.height(FutureDimens.spacingSm))
                Text(
                    text = "הקלד 4 ספרות (למשל 0830), ואז OK. BACK = ביטול",
                    color = theme.mutedTextColor,
                    style = MaterialTheme.typography.bodySmall,
                )
                Spacer(modifier = Modifier.height(FutureDimens.spacingMd))
                FutureButton(
                    text = "אישור",
                    theme = theme,
                    onClick = {
                        onPick(TripWhen(kind, digits.take(2).toInt(), digits.takeLast(2).toInt()))
                    },
                    fillMaxWidth = true,
                    focusRequester = firstFocus,
                    enabled = valid,
                )
            }
        }
    }
}

/**
 * העדפות תחבורה: מסלול (מומלץ / פחות הליכה / פחות החלפות / נגישות) ואמצעים
 * (אוטובוס, רכבת, רכבת קלה, שאטל/מונית). נשמר מיד, והחיפוש מתחדש.
 */
@Composable
private fun TransitPrefsPanel(prefs: TransitPrefs, onChange: (TransitPrefs) -> Unit, onClose: () -> Unit) {
    val theme = LocalFutureTheme.current
    val firstFocus = remember { FocusRequester() }
    BackHandler(onBack = onClose)
    LaunchedEffect(Unit) { runCatching { firstFocus.requestFocus() } }

    fun status(on: Boolean) = if (on) "פעיל" else "כבוי"
    // לפחות אמצעי אחד חייב להישאר פעיל, אחרת הבקשה לא מוגבלת לכלום
    fun setMode(next: TransitPrefs) {
        if (next.bus || next.rail || next.lightRail) onChange(next)
    }

    Column(modifier = Modifier.fillMaxSize().background(theme.surfaceColor).padding(FutureDimens.screenPadding)) {
        Text(
            text = "העדפות מסלול",
            color = theme.textColor,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(modifier = Modifier.height(FutureDimens.spacingSm))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(FutureDimens.itemSpacing)) {
            item {
                FutureListItem(
                    title = "מסלול: " + prefs.preference.label,
                    theme = theme,
                    onClick = { onChange(prefs.copy(preference = prefs.preference.next())) },
                    focusRequester = firstFocus,
                    summary = "OK להחלפה",
                )
            }
            item {
                FutureListItem("אוטובוס: " + status(prefs.bus), theme, onClick = { setMode(prefs.copy(bus = !prefs.bus)) })
            }
            item {
                FutureListItem("רכבת: " + status(prefs.rail), theme, onClick = { setMode(prefs.copy(rail = !prefs.rail)) })
            }
            item {
                FutureListItem("רכבת קלה: " + status(prefs.lightRail), theme, onClick = { setMode(prefs.copy(lightRail = !prefs.lightRail)) })
            }
            item {
                FutureListItem(
                    "שאטל / מונית שירות: " + status(prefs.shareTaxi),
                    theme,
                    onClick = { onChange(prefs.copy(shareTaxi = !prefs.shareTaxi)) },
                    summary = "נשמר להעדפה בלבד",
                )
            }
            item {
                FutureButton("סיום", theme, onClick = onClose, fillMaxWidth = true)
            }
        }
    }
}

@Composable
private fun PrimaryButton(label: String, focusRequester: FocusRequester? = null, onClick: () -> Unit) {
    FutureButton(label, LocalFutureTheme.current, onClick, fillMaxWidth = true, focusRequester = focusRequester)
}

private fun secondsToClock(seconds: Int): String {
    val h = (seconds / 3600) % 24
    val m = (seconds % 3600) / 60
    return "%02d:%02d".format(h, m)
}
