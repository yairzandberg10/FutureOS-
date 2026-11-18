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

    Column(
        modifier = Modifier.fillMaxSize().onPreviewKeyEvent { event ->
            // במקשים: 1 הימנעות מאגרה (נהיגה, רק דרך HERE בשרת), 3 שעת יציאה.
            // בלי שרת הנהיגה היא OSRM, שלא יודע להימנע מאגרה ולא מכיר שעת יציאה.
            if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
            when (com.future.sharednav.nav.digitForKey(event.key)) {
                "7" -> { onSwap(); true }
                "1" -> if (mode == TravelMode.DRIVE && drivingOptions) { viewModel.toggleAvoidTolls(); true } else false
                "3" -> if (mode == TravelMode.TRANSIT || (mode == TravelMode.DRIVE && drivingOptions)) { viewModel.cycleDepartureTime(); true } else false
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
            mode == TravelMode.TRANSIT -> departLabel
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
            Text(
                text = stringResource(R.string.schedule_disclaimer),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            )
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
