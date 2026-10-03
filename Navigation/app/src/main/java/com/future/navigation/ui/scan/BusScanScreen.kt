package com.future.navigation.ui.scan

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.future.navigation.R
import com.future.navigation.data.payment.FareCalculator
import com.future.navigation.data.payment.FarePaymentResult
import com.future.navigation.data.payment.FareRadius
import com.future.navigation.data.scan.AlightingOption
import com.future.navigation.data.scan.BoardingOption
import com.future.navigation.ui.home.RowIcon
import com.future.sharednav.components.EmptyState
import com.future.sharednav.components.FutureButton
import com.future.sharednav.components.FutureButtonVariant
import com.future.sharednav.components.FutureCard
import com.future.sharednav.components.FutureDivider
import com.future.sharednav.components.FutureListItem
import com.future.sharednav.components.FutureSectionHeader
import com.future.sharednav.components.FutureSpinner
import com.future.sharednav.components.ScreenTopBar
import com.future.sharednav.icons.FutureIcons
import com.future.sharednav.theme.FutureDimens
import com.future.sharednav.theme.FutureShapes
import com.future.sharednav.theme.FutureTheme
import com.future.sharednav.theme.FutureTypography
import com.future.sharednav.theme.LocalFutureTheme
import com.future.sharednav.theme.mutedTextColor
import com.future.sharednav.theme.rememberFutureType

/**
 * "סורקים ומשלמים" בעלייה לאוטובוס - כמו במוביט וברב-פס, אבל חלק מהאפליקציה
 * עצמה: מצלמה, זיהוי הקו, תחנת ירידה עם המחיר שלה, אישור ותשלום. הכל
 * במקשים - חזור הוא צעד אחורה, OK בוחר.
 */
@Composable
fun BusScanScreen(viewModel: BusScanViewModel, onClose: () -> Unit) {
    val theme = LocalFutureTheme.current
    val step by viewModel.step.collectAsState()
    val goBack = { if (!viewModel.back()) onClose() }

    BackHandler(onBack = goBack)

    Column(modifier = Modifier.fillMaxSize()) {
        ScreenTopBar(
            title = stringResource(if (step is ScanStep.Confirm || step is ScanStep.Paying) R.string.scan_confirm_title else R.string.scan_title),
            textColor = theme.textColor,
            accentColor = theme.accentColor,
            onBack = goBack
        )
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when (val s = step) {
                ScanStep.Scanning -> ScanningStep(theme, onQr = viewModel::onScanned, onSkip = viewModel::skipScan)
                ScanStep.Identifying -> Waiting(theme, stringResource(R.string.scan_identifying))
                is ScanStep.PickLine -> PickLineStep(s, theme, onPick = viewModel::selectLine, onRescan = viewModel::restart)
                is ScanStep.PickStop -> PickStopStep(s, theme, onPick = viewModel::selectStop, onChangeLine = viewModel::changeLine)
                is ScanStep.Confirm -> ConfirmStep(s, theme, onPay = viewModel::pay, onBack = goBack)
                is ScanStep.Paying -> Waiting(theme, stringResource(R.string.scan_paying))
                is ScanStep.Done -> DoneStep(s.result, theme, onFinish = onClose)
                is ScanStep.Problem -> Message(theme, stringResource(s.message), stringResource(R.string.scan_rescan), viewModel::restart)
            }
        }
    }
}

// ---------------------------------------------------------------- סריקה

@Composable
private fun ScanningStep(theme: FutureTheme, onQr: (String) -> Unit, onSkip: () -> Unit) {
    val context = LocalContext.current
    val type = rememberFutureType()
    var hasCamera by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { hasCamera = it }
    LaunchedEffect(Unit) { if (!hasCamera) launcher.launch(Manifest.permission.CAMERA) }
    val firstButton = remember { FocusRequester() }
    LaunchedEffect(hasCamera) { runCatching { firstButton.requestFocus() } }

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = FutureDimens.screenPadding, vertical = FutureDimens.spacingSm),
        verticalArrangement = Arrangement.spacedBy(FutureDimens.spacingSm),
    ) {
        Box(
            modifier = Modifier.weight(1f).fillMaxWidth().clip(FutureShapes.xl),
            contentAlignment = Alignment.Center,
        ) {
            if (hasCamera) {
                BusQrCamera(modifier = Modifier.fillMaxSize(), onQr = onQr)
                Box(modifier = Modifier.size(ViewfinderSize).border(2.dp, theme.accentColor, FutureShapes.lg))
            } else {
                Text(
                    stringResource(R.string.scan_camera_permission),
                    color = theme.mutedTextColor,
                    fontSize = type.body,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(FutureDimens.spacingXl),
                )
            }
        }
        Text(
            stringResource(R.string.scan_hint),
            color = theme.mutedTextColor,
            fontSize = type.summary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        if (!hasCamera) {
            FutureButton(
                stringResource(R.string.scan_grant_camera), theme,
                onClick = { launcher.launch(Manifest.permission.CAMERA) },
                fillMaxWidth = true,
                focusRequester = firstButton,
            )
        }
        FutureButton(
            stringResource(R.string.scan_skip), theme, onSkip,
            variant = if (hasCamera) FutureButtonVariant.Secondary else FutureButtonVariant.Quiet,
            fillMaxWidth = true,
            focusRequester = if (hasCamera) firstButton else null,
        )
    }
}

// ---------------------------------------------------------------- בחירת קו

@Composable
private fun PickLineStep(s: ScanStep.PickLine, theme: FutureTheme, onPick: (BoardingOption) -> Unit, onRescan: () -> Unit) {
    if (s.options.isEmpty()) {
        Message(theme, stringResource(R.string.scan_no_lines), stringResource(R.string.scan_rescan), onRescan)
        return
    }
    val type = rememberFutureType()
    val first = remember { FocusRequester() }
    LaunchedEffect(s) { runCatching { first.requestFocus() } }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = FutureDimens.screenPadding, vertical = FutureDimens.spacingSm),
        verticalArrangement = Arrangement.spacedBy(FutureDimens.spacingSm),
    ) {
        item { FutureSectionHeader(stringResource(R.string.scan_pick_line), theme, inset = false) }
        item {
            Text(
                s.qr.vehicleNumber?.let { stringResource(R.string.scan_vehicle_not_matched, it) }
                    ?: stringResource(R.string.scan_no_vehicle),
                color = theme.mutedTextColor,
                fontSize = type.summary,
            )
        }
        itemsIndexed(s.options, key = { _, o -> "${o.stop.stopId}|${o.departure.tripId}" }) { index, option ->
            FutureListItem(
                title = stringResource(R.string.scan_line_title, option.lineName, option.departure.tripHeadsign),
                summary = stringResource(
                    R.string.scan_line_summary,
                    option.stop.name,
                    clock(option.departure.departureSeconds),
                    option.distanceMeters.toInt(),
                ),
                theme = theme,
                onClick = { onPick(option) },
                focusRequester = if (index == 0) first else null,
                leading = { RowIcon(FutureIcons.Route, theme) },
            )
        }
    }
}

// ---------------------------------------------------------------- תחנת ירידה

@Composable
private fun PickStopStep(s: ScanStep.PickStop, theme: FutureTheme, onPick: (AlightingOption) -> Unit, onChangeLine: () -> Unit) {
    val type = rememberFutureType()
    val first = remember { FocusRequester() }
    val stops = s.stops
    LaunchedEffect(stops != null) { runCatching { first.requestFocus() } }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = FutureDimens.screenPadding, vertical = FutureDimens.spacingSm),
        verticalArrangement = Arrangement.spacedBy(FutureDimens.spacingSm),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(FutureDimens.spacingXxs)) {
                Text(
                    stringResource(R.string.scan_line_title, s.boarding.lineName, s.boarding.departure.tripHeadsign),
                    color = theme.textColor,
                    fontSize = type.title,
                    fontWeight = FutureTypography.weightBold,
                    maxLines = 1,
                )
                Text(stringResource(R.string.scan_boarding_at, s.boarding.stop.name), color = theme.mutedTextColor, fontSize = type.summary, maxLines = 1)
                if (s.identified && s.qr.vehicleNumber != null) {
                    Text(stringResource(R.string.scan_identified, s.qr.vehicleNumber), color = theme.mutedTextColor, fontSize = type.summary, maxLines = 1)
                }
            }
        }
        if (s.options.isNotEmpty()) {
            item {
                FutureListItem(
                    title = stringResource(R.string.scan_not_my_line),
                    theme = theme,
                    onClick = onChangeLine,
                    leading = { RowIcon(FutureIcons.SwapHoriz, theme) },
                )
            }
        }
        item { FutureSectionHeader(stringResource(R.string.scan_pick_stop), theme, inset = false) }
        when {
            stops == null -> item { FutureSpinner(theme = theme, label = stringResource(R.string.scan_loading_stops)) }
            stops.isEmpty() -> item { Text(stringResource(R.string.scan_no_stops), color = theme.mutedTextColor, fontSize = type.body) }
            else -> itemsIndexed(stops, key = { i, o -> "$i|${o.stop.stopId}" }) { index, stop ->
                val fare = stop.fare
                FutureListItem(
                    title = stop.stop.name,
                    summary = if (fare.distanceKm != null && fare.radius != null) {
                        stringResource(R.string.scan_stop_summary, clock(stop.arrivalSeconds), km(fare.distanceKm), fare.radius.label)
                    } else clock(stop.arrivalSeconds),
                    theme = theme,
                    onClick = { onPick(stop) },
                    focusRequester = if (index == 0) first else null,
                    trailing = {
                        Text(
                            fare.agorot?.let { FareCalculator.format(it) } ?: "—",
                            color = theme.textColor,
                            fontSize = type.body,
                            fontWeight = FutureTypography.weightBold,
                        )
                    },
                )
            }
        }
    }
}

// ---------------------------------------------------------------- אישור

@Composable
private fun ConfirmStep(s: ScanStep.Confirm, theme: FutureTheme, onPay: () -> Unit, onBack: () -> Unit) {
    val type = rememberFutureType()
    val pay = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { pay.requestFocus() } }
    val boarding = s.pick.boarding
    val fare = s.alighting.fare
    val price = fare.agorot?.let { FareCalculator.format(it) } ?: "—"

    Column(
        modifier = Modifier.fillMaxSize().padding(vertical = FutureDimens.spacingSm),
        verticalArrangement = Arrangement.spacedBy(FutureDimens.spacingSm),
    ) {
        FutureCard(theme) {
            InfoRow(stringResource(R.string.scan_row_line), stringResource(R.string.scan_line_title, boarding.lineName, boarding.departure.tripHeadsign), theme)
            FutureDivider(theme)
            InfoRow(stringResource(R.string.scan_row_from), boarding.stop.name, theme)
            FutureDivider(theme)
            InfoRow(stringResource(R.string.scan_row_to), s.alighting.stop.name, theme)
            if (fare.distanceKm != null && fare.radius != null) {
                FutureDivider(theme)
                InfoRow(stringResource(R.string.scan_row_distance), stringResource(R.string.scan_distance_value, km(fare.distanceKm), fare.radius.label), theme)
            }
            FutureDivider(theme)
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = FutureDimens.spacingLg, vertical = FutureDimens.spacingSm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(stringResource(R.string.payment_total), color = theme.textColor, fontSize = type.title, fontWeight = FutureTypography.weightBold, modifier = Modifier.weight(1f))
                Text(price, color = theme.textColor, fontSize = type.headline, fontWeight = FutureTypography.weightBold)
            }
        }
        Column(
            modifier = Modifier.padding(horizontal = FutureDimens.screenPadding),
            verticalArrangement = Arrangement.spacedBy(FutureDimens.spacingSm),
        ) {
            if (fare.radius == FareRadius.YELLOW) {
                Text(stringResource(R.string.scan_transfer_note), color = theme.mutedTextColor, fontSize = type.summary)
            }
            FutureButton(stringResource(R.string.scan_pay, price), theme, onPay, fillMaxWidth = true, focusRequester = pay)
            FutureButton(stringResource(R.string.scan_back), theme, onBack, variant = FutureButtonVariant.Quiet, fillMaxWidth = true)
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String, theme: FutureTheme) {
    val type = rememberFutureType()
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = FutureDimens.spacingLg, vertical = FutureDimens.spacingXs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(FutureDimens.spacingSm),
    ) {
        Text(label, color = theme.mutedTextColor, fontSize = type.summary)
        Text(value, color = theme.textColor, fontSize = type.body, maxLines = 1, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
    }
}

// ---------------------------------------------------------------- תוצאה

@Composable
private fun DoneStep(result: FarePaymentResult, theme: FutureTheme, onFinish: () -> Unit) {
    when (result) {
        FarePaymentResult.NotConnected -> Message(
            theme,
            stringResource(R.string.scan_not_connected_body),
            stringResource(R.string.scan_finish),
            onFinish,
            title = stringResource(R.string.scan_not_connected_title),
            icon = FutureIcons.Info,
        )
        is FarePaymentResult.Approved -> Message(
            theme, result.confirmation, stringResource(R.string.scan_finish), onFinish,
            title = stringResource(R.string.scan_paid_title), icon = FutureIcons.Check,
        )
        is FarePaymentResult.Declined -> Message(
            theme, result.reason, stringResource(R.string.scan_finish), onFinish,
            title = stringResource(R.string.scan_declined_title), icon = FutureIcons.Error,
        )
    }
}

// ---------------------------------------------------------------- עזרים

@Composable
private fun Waiting(theme: FutureTheme, label: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        FutureSpinner(theme = theme, label = label)
    }
}

/** הודעה עם כפתור אחד (ממוקד), במרכז המסך. */
@Composable
private fun Message(
    theme: FutureTheme,
    body: String,
    action: String,
    onAction: () -> Unit,
    title: String? = null,
    icon: androidx.compose.ui.graphics.vector.ImageVector = FutureIcons.QrCodeScanner,
) {
    val button = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { button.requestFocus() } }
    Column(
        modifier = Modifier.fillMaxSize().padding(FutureDimens.spacingXl),
        verticalArrangement = Arrangement.spacedBy(FutureDimens.spacingLg, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        EmptyState(icon = icon, title = title ?: body, textColor = theme.textColor, subtitle = if (title != null) body else null)
        FutureButton(action, theme, onAction, fillMaxWidth = true, focusRequester = button)
    }
}

private fun clock(secondsSinceMidnight: Int): String {
    val h = (secondsSinceMidnight / 3600) % 24
    val m = (secondsSinceMidnight % 3600) / 60
    return "%02d:%02d".format(h, m)
}

private fun km(distanceKm: Double): String = "%.1f".format(distanceKm)

/** מסגרת הכיוון מעל המצלמה - 160dp, חצי מרוחב המסך. */
private val ViewfinderSize = 160.dp
