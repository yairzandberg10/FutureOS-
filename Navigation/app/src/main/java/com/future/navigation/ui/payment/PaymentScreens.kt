package com.future.navigation.ui.payment

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.view.WindowManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import com.future.navigation.R
import com.future.navigation.data.gtfs.TransitItinerary
import com.future.navigation.data.payment.FareCalculator
import com.future.navigation.data.payment.FareMode
import com.future.navigation.data.payment.PaymentConfig
import com.future.navigation.data.payment.PaymentProfile
import com.future.navigation.data.payment.PaymentProfileStore
import com.future.navigation.data.payment.RideFare
import com.future.navigation.ui.home.RowIcon
import com.future.sharednav.components.FutureButton
import com.future.sharednav.components.FutureButtonVariant
import com.future.sharednav.components.FutureCard
import com.future.sharednav.components.FutureDivider
import com.future.sharednav.components.FutureFormField
import com.future.sharednav.components.FutureListItem
import com.future.sharednav.components.FutureSectionHeader
import com.future.sharednav.components.ScreenTopBar
import com.future.sharednav.focus.escapeTextFieldFocusTrap
import com.future.sharednav.icons.FutureIcons
import com.future.sharednav.theme.FutureDimens
import com.future.sharednav.theme.FutureTheme
import com.future.sharednav.theme.FutureTypography
import com.future.sharednav.theme.LocalFutureTheme
import com.future.sharednav.theme.mutedTextColor
import com.future.sharednav.theme.rememberFutureType

/**
 * תשלום על נסיעה ממכשיר כשר: המחיר לפי "דרך שווה", ושיחה למוקד הטעינה
 * הטלפוני של רב-קו עם מספר הכרטיס מוכן לשליחה (ר' PaymentConfig).
 */
@Composable
fun TransitPaymentScreen(
    itinerary: TransitItinerary,
    profile: PaymentProfile,
    onBack: () -> Unit,
    onScan: () -> Unit,
    onCall: () -> Unit,
    onEditDetails: () -> Unit,
) {
    val theme = LocalFutureTheme.current
    val type = rememberFutureType()
    val fare = remember(itinerary) { FareCalculator.calculate(itinerary) }
    val scanFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) { runCatching { scanFocusRequester.requestFocus() } }

    Column(modifier = Modifier.fillMaxSize()) {
        ScreenTopBar(
            title = stringResource(R.string.payment_title),
            textColor = theme.textColor,
            accentColor = theme.accentColor,
            onBack = onBack
        )

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(vertical = FutureDimens.spacingSm),
            verticalArrangement = Arrangement.spacedBy(FutureDimens.spacingSm),
        ) {
            item { FutureSectionHeader(stringResource(R.string.payment_fare_section), theme) }
            item {
                FutureCard(theme) {
                    fare.rides.forEachIndexed { index, ride ->
                        if (index > 0) FutureDivider(theme)
                        FareRow(ride, theme)
                    }
                    FutureDivider(theme)
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = FutureDimens.spacingLg, vertical = FutureDimens.spacingMd),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            stringResource(R.string.payment_total),
                            color = theme.textColor,
                            fontSize = type.title,
                            fontWeight = FutureTypography.weightBold,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            FareCalculator.format(fare.totalAgorot) + if (fare.isComplete) "" else "+",
                            color = theme.textColor,
                            fontSize = type.title,
                            fontWeight = FutureTypography.weightBold,
                        )
                    }
                }
            }

            item { FutureSectionHeader(stringResource(R.string.payment_methods_section), theme) }
            item {
                FutureListItem(
                    title = stringResource(R.string.scan_title),
                    summary = stringResource(R.string.scan_entry_summary),
                    summaryMaxLines = 2,
                    theme = theme,
                    onClick = onScan,
                    focusRequester = scanFocusRequester,
                    leading = { RowIcon(FutureIcons.QrCodeScanner, theme) },
                    modifier = Modifier.padding(horizontal = FutureDimens.screenPadding),
                )
            }
            item {
                FutureListItem(
                    title = stringResource(R.string.payment_call_title),
                    summary = if (profile.isEmpty) {
                        stringResource(R.string.payment_call_summary)
                    } else {
                        stringResource(R.string.payment_call_summary_card, profile.ravKavNumber.takeLast(4))
                    },
                    summaryMaxLines = 2,
                    theme = theme,
                    onClick = onCall,
                    leading = { RowIcon(FutureIcons.Call, theme) },
                    modifier = Modifier.padding(horizontal = FutureDimens.screenPadding),
                )
            }
            item {
                FutureListItem(
                    title = stringResource(R.string.payment_details_title),
                    summary = if (profile.isEmpty) {
                        stringResource(R.string.payment_details_add)
                    } else {
                        stringResource(R.string.payment_details_card, profile.ravKavNumber.takeLast(4))
                    },
                    theme = theme,
                    onClick = onEditDetails,
                    leading = { RowIcon(FutureIcons.Badge, theme) },
                    modifier = Modifier.padding(horizontal = FutureDimens.screenPadding),
                )
            }
            item {
                Text(
                    stringResource(R.string.payment_call_note),
                    color = theme.mutedTextColor,
                    fontSize = type.summary,
                    modifier = Modifier.padding(horizontal = FutureDimens.spacingXl),
                )
            }
        }
    }
}

@Composable
private fun FareRow(ride: RideFare, theme: FutureTheme) {
    val type = rememberFutureType()
    val leg = ride.leg
    val line = leg.routeShortName?.takeIf { it.isNotBlank() } ?: leg.routeLongName ?: "קו"
    val modeLabel = when (ride.mode) {
        FareMode.BUS -> stringResource(R.string.payment_mode_bus, line)
        FareMode.LIGHT_RAIL -> stringResource(R.string.payment_mode_light_rail, line)
        FareMode.RAIL -> stringResource(R.string.payment_mode_rail)
    }
    val detail = when {
        ride.isFreeTransfer -> stringResource(R.string.payment_free_transfer)
        ride.distanceKm != null && ride.radius != null ->
            stringResource(R.string.payment_distance_radius, "%.1f".format(ride.distanceKm), ride.radius.label)
        else -> stringResource(R.string.payment_distance_unknown)
    }
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = FutureDimens.spacingLg, vertical = FutureDimens.spacingSm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // שתי שורות בלבד - המסך הוא 320x480dp, ואמצעי התשלום צריכים להיכנס
        // מתחת לכרטיס בלי גלילה. שמות התחנות כבר מופיעים במסך המסלול.
        Column(modifier = Modifier.weight(1f)) {
            Text(modeLabel, color = theme.textColor, fontSize = type.body, fontWeight = FutureTypography.weightMedium, maxLines = 1)
            Text(detail, color = theme.mutedTextColor, fontSize = type.summary, maxLines = 1)
        }
        Text(
            ride.agorot?.let { FareCalculator.format(it) } ?: "—",
            color = theme.textColor,
            fontSize = type.body,
            fontWeight = FutureTypography.weightBold,
        )
    }
}

/**
 * מספר כרטיס הרב-קו, שנשלח למוקד בלחיצה במהלך השיחה. נשמר מוצפן במכשיר
 * בלבד (PaymentProfileStore).
 */
@Composable
fun PaymentDetailsScreen(store: PaymentProfileStore, onBack: () -> Unit, onSaved: () -> Unit) {
    SecureWindow()
    val theme = LocalFutureTheme.current
    val type = rememberFutureType()
    val saved = store.profile.value
    var ravKav by rememberSaveable { mutableStateOf(saved.ravKavNumber) }

    Column(modifier = Modifier.fillMaxSize().escapeTextFieldFocusTrap()) {
        ScreenTopBar(
            title = stringResource(R.string.payment_details_title),
            textColor = theme.textColor,
            accentColor = theme.accentColor,
            onBack = onBack
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = FutureDimens.screenPadding, vertical = FutureDimens.spacingSm),
            verticalArrangement = Arrangement.spacedBy(FutureDimens.spacingMd),
        ) {
            FutureFormField(
                stringResource(R.string.payment_field_ravkav), ravKav, { ravKav = it.filter(Char::isDigit).take(MAX_CARD_DIGITS) }, theme,
                placeholder = stringResource(R.string.payment_field_ravkav_hint),
                autoFocus = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
            Text(stringResource(R.string.payment_details_privacy), color = theme.mutedTextColor, fontSize = type.summary)
            FutureButton(
                text = stringResource(R.string.payment_save),
                theme = theme,
                fillMaxWidth = true,
                onClick = {
                    store.save(PaymentProfile(ravKavNumber = ravKav))
                    onSaved()
                },
            )
            if (!saved.isEmpty) {
                FutureButton(
                    text = stringResource(R.string.payment_clear),
                    theme = theme,
                    variant = FutureButtonVariant.Secondary,
                    fillMaxWidth = true,
                    onClick = {
                        store.clear()
                        onBack()
                    },
                )
            }
        }
    }
}

private const val MAX_CARD_DIGITS = 12

/**
 * פותח את החייגן של המערכת עם מספר המוקד ומספר הכרטיס (אחרי ";"). המשתמש
 * לוחץ חיוג בעצמו - שום שיחה לא יוצאת בלי לחיצה שלו.
 */
fun Context.dialRavKavLoading(profile: PaymentProfile) {
    val intent = Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", PaymentConfig.dialString(profile.ravKavNumber), null))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { startActivity(intent) }
}

/** מספר הכרטיס לא נכנס לצילומי מסך ולתמונת המסכים האחרונים (FLAG_SECURE). */
@Composable
private fun SecureWindow() {
    val activity = LocalContext.current.findActivity()
    DisposableEffect(activity) {
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        onDispose { activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE) }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
