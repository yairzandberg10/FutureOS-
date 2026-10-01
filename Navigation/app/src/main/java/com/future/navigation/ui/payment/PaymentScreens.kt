package com.future.navigation.ui.payment

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.view.WindowManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.viewinterop.AndroidView
import com.future.navigation.R
import com.future.navigation.data.gtfs.TransitItinerary
import com.future.navigation.data.payment.FareCalculator
import com.future.navigation.data.payment.FareMode
import com.future.navigation.data.payment.PaymentApp
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
import com.future.sharednav.components.FutureProgressBar
import com.future.sharednav.components.FutureSectionHeader
import com.future.sharednav.components.ScreenTopBar
import com.future.sharednav.focus.escapeTextFieldFocusTrap
import com.future.sharednav.icons.FutureIcons
import com.future.sharednav.theme.FutureDimens
import com.future.sharednav.theme.FutureTheme
import com.future.sharednav.theme.FutureTypography
import com.future.sharednav.theme.LocalFutureTheme
import com.future.sharednav.theme.mutedTextColor
import com.future.sharednav.theme.readableAccentColor
import com.future.sharednav.theme.rememberFutureType

/**
 * תשלום על נסיעה: המחיר לפי "דרך שווה", ושלוש דרכים לשלם - רב-קו אונליין
 * בדפדפן עם הפרטים ממולאים, אפליקציית תשלום מורשית אם מותקנת, ועריכת
 * הפרטים השמורים.
 */
@Composable
fun TransitPaymentScreen(
    itinerary: TransitItinerary,
    profile: PaymentProfile,
    onBack: () -> Unit,
    onPayOnWeb: () -> Unit,
    onEditDetails: () -> Unit,
) {
    val theme = LocalFutureTheme.current
    val type = rememberFutureType()
    val context = LocalContext.current
    val fare = remember(itinerary) { FareCalculator.calculate(itinerary) }
    val installedApps = remember { PaymentConfig.APPS.filter { context.isInstalled(it.packageName) } }
    val webFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) { runCatching { webFocusRequester.requestFocus() } }

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
            item {
                Text(
                    stringResource(R.string.payment_fare_note),
                    color = theme.mutedTextColor,
                    fontSize = type.summary,
                    modifier = Modifier.padding(horizontal = FutureDimens.spacingXl),
                )
            }

            item { FutureSectionHeader(stringResource(R.string.payment_methods_section), theme) }
            item {
                FutureListItem(
                    title = stringResource(R.string.payment_web_title),
                    summary = if (profile.ravKavNumber.isNotBlank()) {
                        stringResource(R.string.payment_web_summary_card, profile.ravKavNumber.takeLast(4))
                    } else {
                        stringResource(R.string.payment_web_summary)
                    },
                    summaryMaxLines = 2,
                    theme = theme,
                    onClick = onPayOnWeb,
                    focusRequester = webFocusRequester,
                    leading = { RowIcon(FutureIcons.Language, theme) },
                    modifier = Modifier.padding(horizontal = FutureDimens.screenPadding),
                )
            }
            if (installedApps.isEmpty()) {
                item {
                    Text(
                        stringResource(R.string.payment_apps_missing),
                        color = theme.mutedTextColor,
                        fontSize = type.summary,
                        modifier = Modifier.padding(horizontal = FutureDimens.spacingXl),
                    )
                }
            } else {
                items(installedApps, key = { it.packageName }) { app ->
                    FutureListItem(
                        title = app.title,
                        summary = app.summary,
                        theme = theme,
                        onClick = { context.launchApp(app) },
                        leading = { RowIcon(FutureIcons.Smartphone, theme) },
                        modifier = Modifier.padding(horizontal = FutureDimens.screenPadding),
                    )
                }
            }

            item { FutureSectionHeader(stringResource(R.string.payment_details_section), theme) }
            item {
                FutureListItem(
                    title = profile.fullName.ifBlank { stringResource(R.string.payment_details_add) },
                    summary = detailsSummary(profile),
                    theme = theme,
                    onClick = onEditDetails,
                    leading = { RowIcon(FutureIcons.Person, theme) },
                    modifier = Modifier.padding(horizontal = FutureDimens.screenPadding),
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
        Column(modifier = Modifier.weight(1f)) {
            // שתי שורות בלבד - המסך הוא 320x480dp, ואמצעי התשלום צריכים להיכנס
            // מתחת לכרטיס בלי גלילה. שמות התחנות כבר מופיעים במסך המסלול.
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

private fun detailsSummary(profile: PaymentProfile): String? {
    val parts = buildList {
        if (profile.idNumber.isNotBlank()) add("ת״ז ••${profile.idNumber.takeLast(3)}")
        if (profile.ravKavNumber.isNotBlank()) add("רב-קו ••${profile.ravKavNumber.takeLast(4)}")
        if (profile.phone.isNotBlank()) add(profile.phone)
    }
    return parts.takeIf { it.isNotEmpty() }?.joinToString(" · ")
}

/**
 * הפרטים שהדפדפן ממלא לבד. נשמרים מוצפנים במכשיר בלבד (PaymentProfileStore),
 * בלי פרטי אשראי - אותם מקלידים רק בעמוד הסליקה של גורם התשלום.
 */
@Composable
fun PaymentDetailsScreen(store: PaymentProfileStore, onBack: () -> Unit, onSaved: () -> Unit) {
    SecureWindow()
    val theme = LocalFutureTheme.current
    val type = rememberFutureType()
    val saved = store.profile.value
    var firstName by rememberSaveable { mutableStateOf(saved.firstName) }
    var lastName by rememberSaveable { mutableStateOf(saved.lastName) }
    var idNumber by rememberSaveable { mutableStateOf(saved.idNumber) }
    var phone by rememberSaveable { mutableStateOf(saved.phone) }
    var email by rememberSaveable { mutableStateOf(saved.email) }
    var ravKav by rememberSaveable { mutableStateOf(saved.ravKavNumber) }
    var showIdError by remember { mutableStateOf(false) }
    val idValid = idNumber.isBlank() || PaymentProfileStore.isValidIsraeliId(idNumber)

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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = FutureDimens.screenPadding, vertical = FutureDimens.spacingSm),
            verticalArrangement = Arrangement.spacedBy(FutureDimens.spacingMd),
        ) {
            FutureFormField(stringResource(R.string.payment_field_first_name), firstName, { firstName = it }, theme, autoFocus = true)
            FutureFormField(stringResource(R.string.payment_field_last_name), lastName, { lastName = it }, theme)
            FutureFormField(
                stringResource(R.string.payment_field_id), idNumber, { idNumber = it.filter(Char::isDigit).take(9); showIdError = false }, theme,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
            if (showIdError && !idValid) {
                Text(stringResource(R.string.payment_id_invalid), color = theme.dangerColor, fontSize = type.summary)
            }
            FutureFormField(
                stringResource(R.string.payment_field_phone), phone, { phone = it.filter { c -> c.isDigit() || c == '+' }.take(15) }, theme,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            )
            FutureFormField(
                stringResource(R.string.payment_field_email), email, { email = it.trim() }, theme,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            )
            FutureFormField(
                stringResource(R.string.payment_field_ravkav), ravKav, { ravKav = it.filter(Char::isDigit).take(12) }, theme,
                placeholder = stringResource(R.string.payment_field_ravkav_hint),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
            Text(stringResource(R.string.payment_details_privacy), color = theme.mutedTextColor, fontSize = type.summary)
            FutureButton(
                text = stringResource(R.string.payment_save),
                theme = theme,
                fillMaxWidth = true,
                onClick = {
                    if (!idValid) {
                        showIdError = true
                    } else {
                        store.save(PaymentProfile(firstName, lastName, idNumber, phone, email, ravKav))
                        onSaved()
                    }
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

/** רב-קו אונליין בתוך האפליקציה, עם הסמן של המקשים ומילוי הפרטים (ר' KeypadBrowser). */
@Composable
fun PaymentWebScreen(url: String, profile: PaymentProfile, onClose: () -> Unit) {
    SecureWindow()
    val theme = LocalFutureTheme.current
    val type = rememberFutureType()
    var progress by remember { mutableIntStateOf(0) }
    var title by remember { mutableStateOf("") }
    val cursorFill = theme.readableAccentColor.copy(alpha = 0.55f).toArgb()
    val cursorRing = theme.textColor.toArgb()

    Column(modifier = Modifier.fillMaxSize()) {
        ScreenTopBar(
            title = title.ifBlank { stringResource(R.string.payment_web_title) },
            textColor = theme.textColor,
            accentColor = theme.accentColor,
            onBack = onClose
        )
        Text(
            stringResource(R.string.payment_web_keys_hint),
            color = theme.mutedTextColor,
            fontSize = type.summary,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = FutureDimens.screenPadding),
        )
        if (progress in 1..99) {
            FutureProgressBar(
                progress = progress / 100f,
                theme = theme,
                mini = true,
                modifier = Modifier.fillMaxWidth().padding(top = FutureDimens.spacingXs),
            )
        }
        AndroidView(
            modifier = Modifier.weight(1f).fillMaxWidth().padding(top = FutureDimens.spacingXs),
            factory = { ctx ->
                KeypadBrowser(
                    ctx,
                    cursorFill = cursorFill,
                    cursorRing = cursorRing,
                    onProgress = { progress = it },
                    onTitle = { title = it },
                ).apply {
                    this.profile = profile
                    load(url)
                    // אין מסך מגע - בלי פוקוס יזום ה-WebView לא מקבל אף מקש.
                    post { webView.requestFocus() }
                }
            },
            update = { it.profile = profile },
            onRelease = { it.destroy() },
        )
    }
}

/**
 * תעודת זהות, מספר כרטיס ועמוד סליקה לא נכנסים לצילומי מסך ולתמונת
 * המסכים האחרונים - כמו מסך הגדרות הנעילה (FLAG_SECURE).
 */
@Composable
private fun SecureWindow() {
    val activity = LocalContext.current.findActivity()
    DisposableEffect(activity) {
        // ספירה ולא הדלקה/כיבוי: במעבר מהפרטים לדפדפן המסך הישן נהרס רק
        // אחרי שהחדש כבר עלה, וכיבוי ישיר היה מוריד את ההגנה מהמסך החדש.
        if (secureScreens++ == 0) activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        onDispose {
            if (--secureScreens == 0) activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }
}

private var secureScreens = 0

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

private fun Context.isInstalled(packageName: String): Boolean =
    packageManager.getLaunchIntentForPackage(packageName) != null

private fun Context.launchApp(app: PaymentApp) {
    val intent = packageManager.getLaunchIntentForPackage(app.packageName) ?: return
    runCatching { startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
}
