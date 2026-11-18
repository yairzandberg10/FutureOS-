package com.future.navigation.ui.navigate

import com.future.sharednav.i18n.tr

import com.future.sharednav.icons.FutureIcons
import com.future.sharednav.components.TopBarIconButton
import com.future.sharednav.theme.FutureDimens
import com.future.sharednav.theme.FutureElevation
import com.future.sharednav.theme.LocalFutureTheme
import com.future.sharednav.theme.elevatedSurfaceColor
import com.future.sharednav.theme.mutedTextColor
import com.future.sharednav.theme.onStatusColor

import com.future.sharednav.theme.FutureShapes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.future.navigation.R
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import com.future.navigation.data.reports.ReportType
import com.future.navigation.data.routing.Maneuver
import com.future.navigation.data.routing.RoutingProfile
import com.future.navigation.ui.map.MapCameraController
import com.future.navigation.ui.map.NavMapView
import com.future.sharednav.components.ConfirmDialog

/**
 * מסך ניווט במתכונת Waze / Google Maps: המפה ממלאת את כל המסך, כרטיס פנייה
 * קומפקטי למעלה, ושורה תחתונה דקה (שעת הגעה, דקות, ק"מ) עם מהירות בפינה.
 * המפה מסתובבת לכיוון הנסיעה במצב מעקב. מקשים: 5 - תצוגת מסלול מלא (ושוב
 * למעקב), 0 - דיווחים, 3 - שיתוף, * / # - זום, מרכז - חזרה למיקום שלי.
 */
@Composable
fun NavigateScreen(viewModel: NavigateViewModel, onClose: () -> Unit = {}) {
    val theme = LocalFutureTheme.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val cameraController = remember { MapCameraController() }
    var followMode by remember { mutableStateOf(true) }
    // תצוגת מסלול מלא (מקש 5): המצלמה מתאימה את כל המסלול, בלי מעקב
    var overview by remember { mutableStateOf(false) }
    // סיום ניווט מבקש אישור (F18), ולכן הכפתור רק פותח את הדיאלוג.
    var confirmEnd by remember { mutableStateOf(false) }
    // תפריט הדיווח (מקש 0) - בזמן שהוא פתוח, הספרות מדווחות במקום להזיז את המפה
    var reportMenu by remember { mutableStateOf(false) }
    // GPS כבוי - מציגים הודעה במקום מפה שאינה מתעדכנת (F19). נבדק מחדש בכל חזרה למסך.
    val gpsContext = androidx.compose.ui.platform.LocalContext.current
    var gpsOff by remember { mutableStateOf(!isGpsEnabled(gpsContext)) }
    val lifecycleOwnerForGps = androidx.compose.ui.platform.LocalLifecycleOwner.current
    androidx.compose.runtime.DisposableEffect(lifecycleOwnerForGps) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) gpsOff = !isGpsEnabled(gpsContext)
        }
        lifecycleOwnerForGps.lifecycle.addObserver(observer)
        onDispose { lifecycleOwnerForGps.lifecycle.removeObserver(observer) }
    }

    // המסך לא נכבה באמצע ניווט (קודם הוא כבה אחרי זמן ההמתנה הרגיל)
    val view = androidx.compose.ui.platform.LocalView.current
    androidx.compose.runtime.DisposableEffect(view) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }

    // קישור שיתוף חדש: פותחים הודעה, והמשתמש בוחר למי לשלוח באפליקציית ההודעות
    val shareLink by viewModel.shareLink.collectAsStateWithLifecycle()
    androidx.compose.runtime.LaunchedEffect(shareLink) {
        val link = shareLink ?: return@LaunchedEffect
        viewModel.consumeShareLink()
        val intent = android.content.Intent(android.content.Intent.ACTION_SENDTO, android.net.Uri.parse("smsto:"))
            .putExtra("sms_body", "אני בדרך, אפשר לעקוב כאן: $link")
            .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { gpsContext.startActivity(intent) }
    }

    // קו המסלול ונקודת המיקום על המפה
    val accentArgb = theme.accentColor.toArgb()
    androidx.compose.runtime.LaunchedEffect(state.route, accentArgb) {
        cameraController.setAccent(accentArgb)
        cameraController.setRoute(state.route.polyline)
    }
    androidx.compose.runtime.LaunchedEffect(state.currentLocation) {
        cameraController.setPosition(state.currentLocation)
    }

    // מעקב במצב "הכיוון שלי למעלה"; בתצוגת מסלול מלא המעקב מושבת
    androidx.compose.runtime.LaunchedEffect(state.currentLocation, followMode, overview, state.bearing) {
        val loc = state.currentLocation
        if (loc != null && followMode && !overview) {
            cameraController.followHeading(loc, state.bearing)
        }
    }
    androidx.compose.runtime.LaunchedEffect(overview) {
        if (overview) cameraController.fitRoute(state.route.polyline)
    }

    if (confirmEnd) {
        ConfirmDialog(
            message = "לסיים את הניווט?",
            surfaceColor = theme.surfaceColor,
            textColor = theme.textColor,
            dangerColor = theme.dangerColor,
            onCancel = { confirmEnd = false },
            onConfirm = {
                confirmEnd = false
                viewModel.endNavigation()
                onClose()
            },
            cancelLabel = "ביטול",
            confirmLabel = "סיים",
        )
    }

    // מהירות מוצגת רק בנהיגה; הליכה ורכיבה - בלי תצוגת מהירות
    val showSpeed = viewModel.profile == RoutingProfile.DRIVING

    Box(
        modifier = Modifier.fillMaxSize().onPreviewKeyEvent { event ->
            // הספרות 1-6 בתפריט הדיווח, 7 "עדיין שם", 9 "לא שם", חזרה סוגרת.
            if (event.type != KeyEventType.KeyDown || state.ended) return@onPreviewKeyEvent false
            if (reportMenu && event.key == Key.Back) { reportMenu = false; return@onPreviewKeyEvent true }
            val digit = com.future.sharednav.nav.digitForKey(event.key) ?: return@onPreviewKeyEvent false
            if ((digit == "0" || (!reportMenu && digit == "3")) && !viewModel.serverFeatures) {
                viewModel.explainNoServer(); return@onPreviewKeyEvent true
            }
            if (digit == "0") { reportMenu = !reportMenu; return@onPreviewKeyEvent true }
            // 3 מחוץ לתפריט: שיתוף הנסיעה
            if (!reportMenu && digit == "3") { viewModel.shareTrip(); return@onPreviewKeyEvent true }
            // 5 מחוץ לתפריט: תצוגת מסלול מלא, ושוב לחיצה חוזרת למעקב
            if (!reportMenu && digit == "5") {
                overview = !overview
                if (!overview) followMode = true
                return@onPreviewKeyEvent true
            }
            if (!reportMenu) return@onPreviewKeyEvent false
            when (digit) {
                "7" -> viewModel.voteNearestReport(true)
                "9" -> viewModel.voteNearestReport(false)
                else -> ReportType.fromDigit(digit.toInt())?.let(viewModel::report) ?: return@onPreviewKeyEvent true
            }
            reportMenu = false
            true
        }
    ) {
        // המפה ברקע, על כל המסך
        NavMapView(
            modifier = Modifier.fillMaxSize(),
            cameraController = cameraController,
            initialCenter = state.currentLocation,
            onRecenterRequested = {
                overview = false
                followMode = true
                state.currentLocation?.let { cameraController.recenter(it) }
            },
            onManualPan = { followMode = false }
        )

        // כרטיס הפנייה הבאה - למעלה, קומפקטי: חץ, מרחק גדול, שם הרחוב, ו"ואז"
        val next = state.nextStep
        val showThen = next != null && state.distanceToManeuverMeters < THEN_PREVIEW_METERS
        if (!state.ended) {
            Surface(
                modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth().padding(6.dp),
                shape = FutureShapes.lg,
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = FutureElevation.card(theme.isDarkMode)
            ) {
                Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(56.dp).clip(FutureShapes.md).background(theme.elevatedSurfaceColor),
                        contentAlignment = Alignment.Center
                    ) {
                        ManeuverIcon(state.currentStep)
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = state.distanceToManeuverMeters.let { if (it >= 1000) tr("%.1f ק״מ").format(it / 1000) else "${((it / 10).toInt() * 10)} מ׳" },
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.headlineMedium,
                            color = theme.textColor,
                            maxLines = 1
                        )
                        Text(
                            text = state.currentStep.streetName.takeIf { it.isNotBlank() } ?: maneuverText(state.currentStep),
                            style = MaterialTheme.typography.titleMedium,
                            color = theme.textColor,
                            maxLines = 1
                        )
                        if (showThen && next != null) {
                            Text(
                                text = "ואז " + maneuverText(next),
                                style = MaterialTheme.typography.bodyMedium,
                                color = theme.mutedTextColor,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }

        // הודעות קצרות (GPS, דיווח, תפריט) וההתחלה של השורה התחתונה
        Column(
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(horizontal = 6.dp),
        ) {
            if (gpsOff && !state.ended) {
                Text(
                    "GPS כבוי - הפעילו מיקום כדי שהמפה תראה איפה אתם",
                    color = theme.dangerColor,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
            }
            if (!state.ended) {
                // דיווח קרוב על המסלול, או אישור אחרי דיווח; בלי שניהם - תזכורת למקשים
                val report = state.nearestReport
                val notice = when {
                    reportMenu -> ReportType.entries.joinToString(" · ") { "${it.digit} ${it.label}" } +
                        (if (report != null) " · 7 עדיין שם · 9 לא שם" else "")
                    state.reportMessage != null -> state.reportMessage!!
                    report != null -> "${report.type.label} בעוד ${state.nearestReportMeters.toInt()} מ׳ · 0 לעדכן"
                    viewModel.serverFeatures -> "0 לדיווח · 3 לשיתוף · 5 מסלול מלא"
                    else -> "5 מסלול מלא"
                }
                if (notice.isNotEmpty()) {
                    Text(
                        text = notice,
                        color = when {
                            reportMenu -> theme.textColor
                            report != null && state.reportMessage == null -> theme.dangerColor
                            else -> theme.mutedTextColor
                        },
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        modifier = Modifier.padding(bottom = 4.dp),
                    )
                }

                // מהירות בעיגול בפינה, מעל השורה התחתונה, על גבי המפה
                val speed = state.speedKmh
                if (showSpeed && speed != null) {
                    Box(
                        modifier = Modifier.padding(bottom = 6.dp).size(56.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surface),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "$speed",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge,
                            color = theme.textColor,
                        )
                    }
                }

                // שורה תחתונה דקה: שעת הגעה, דקות, ק"מ, עם השתק וסיום בצדדים
                Surface(shape = FutureShapes.lg, color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        TopBarIconButton(if (state.muted) FutureIcons.AutoMirrored.VolumeOff else FutureIcons.AutoMirrored.VolumeUp, tr("השתק"), theme.textColor, theme.accentColor, viewModel::toggleMute)
                        Row(
                            modifier = Modifier.weight(1f),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(arrivalClock(state.remainingDurationSeconds), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge, maxLines = 1, softWrap = false)
                            Text("  ·  ", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                            Text(tr("%d דק׳").format(kotlin.math.ceil(state.remainingDurationSeconds / 60).toInt()), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge, maxLines = 1, softWrap = false)
                            Text("  ·  ", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                            Text(tr("%.1f ק״מ").format(state.remainingDistanceMeters / 1000.0), style = MaterialTheme.typography.titleMedium, maxLines = 1, softWrap = false)
                        }
                        TopBarIconButton(FutureIcons.Close, tr("סיים ניווט"), theme.dangerColor, theme.accentColor, onClick = {
                            // הכפתור הזה משמעו "בטל/צא מהניווט" - לא רק לסמן ended=true (זה
                            // קורה גם אוטומטית בהגעה בפועל ליעד) אלא גם לצאת בפועל מהמסך.
                            // לפני כן מבקשים אישור (F18), והברירה הבטוחה היא "ביטול".
                            confirmEnd = true
                        })
                    }
                }
            } else {
                // הגעה: כרטיס קצר עם שעת ההגעה ויציאה מהמסך (F17)
                Surface(shape = FutureShapes.lg, color = theme.successColor.copy(alpha = 0.15f), modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.padding(FutureDimens.spacingSm), verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(FutureDimens.rowHeightTopBarButton).clip(CircleShape).background(theme.successColor), contentAlignment = Alignment.Center) {
                            Icon(FutureIcons.Check, contentDescription = null, tint = theme.onStatusColor(theme.successColor))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.arrived_at_destination), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            Text(arrivalClock(0.0), style = MaterialTheme.typography.bodyMedium, color = theme.mutedTextColor)
                        }
                        TopBarIconButton(FutureIcons.Close, "יציאה", theme.textColor, theme.accentColor, onClick = onClose)
                    }
                }
            }
        }
    }
}

/** שעת ההגעה המשוערת (שעון המכשיר), בפורמט HH:mm. */
private fun arrivalClock(remainingSeconds: Double): String {
    val at = System.currentTimeMillis() + (remainingSeconds * 1000).toLong()
    return java.text.SimpleDateFormat("HH:mm", java.util.Locale.US).format(java.util.Date(at))
}

/** האם שירות מיקום GPS דולק במכשיר. לא דורש הרשאה. */
private fun isGpsEnabled(context: android.content.Context): Boolean =
    runCatching {
        (context.getSystemService(android.content.Context.LOCATION_SERVICE) as android.location.LocationManager)
            .isProviderEnabled(android.location.LocationManager.GPS_PROVIDER)
    }.getOrDefault(true)

/** "ואז" מוצג כשהפנייה הנוכחית קרובה מ-150 מ'. */
private const val THEN_PREVIEW_METERS = 150.0

@Composable
private fun ManeuverIcon(step: Maneuver) {
    val angle = when (step.modifier) {
        "left" -> -90f
        "slight left" -> -45f
        "sharp left" -> -135f
        "right" -> 90f
        "slight right" -> 45f
        "sharp right" -> 135f
        "uturn" -> 180f
        else -> 0f
    }
    Icon(
        FutureIcons.ArrowUpward,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.rotate(angle)
    )
}

internal fun maneuverText(step: Maneuver): String = when (step.type) {
    "depart" -> "יציאה לדרך"
    "arrive" -> "הגעה ליעד"
    "roundabout", "rotary" -> step.exit?.let { "בכיכר צא ביציאה ה-$it" } ?: "כיכר - המשך לפי השילוט"
    "exit roundabout", "exit rotary" -> "צא מהכיכר"
    "fork" -> if (step.modifier?.contains("left") == true) "בפיצול הישאר שמאלה" else "בפיצול הישאר ימינה"
    "off ramp" -> if (step.modifier?.contains("left") == true) "צא מהכביש שמאלה" else "צא מהכביש ימינה"
    "on ramp" -> "עלה על הכביש"
    "merge" -> "השתלב בתנועה"
    else -> when (step.modifier) {
        "left" -> "פנה שמאלה"
        "slight left" -> "פנה מעט שמאלה"
        "sharp left" -> "פנה חדות שמאלה"
        "right" -> "פנה ימינה"
        "slight right" -> "פנה מעט ימינה"
        "sharp right" -> "פנה חדות ימינה"
        "uturn" -> "פנה פרסה"
        else -> tr("המשך ישר")
    }
}
