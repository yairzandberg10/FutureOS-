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
import androidx.compose.foundation.border
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
    androidx.compose.runtime.LaunchedEffect(state.currentLocation, state.bearing) {
        cameraController.setPosition(state.currentLocation, state.bearing)
    }
    // חץ כיוון במקום נקודה, החלק שנסענו באפור, וזום ניווט - רק במסך הזה
    androidx.compose.runtime.DisposableEffect(cameraController) {
        cameraController.setNavigationMode(true)
        onDispose { cameraController.setNavigationMode(false) }
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
                cameraController.resetNavZoom()
                state.currentLocation?.let { cameraController.followHeading(it, state.bearing) }
            },
            onManualPan = { followMode = false }
        )

        // כרטיס הפנייה הבאה (כמו Waze): פס בצבע ההדגשה למעלה עם חץ גדול, מרחק
        // גדול ושם הרחוב, ומתחתיו תווית קטנה "ואז" כשהפנייה קרובה
        val next = state.nextStep
        val showThen = next != null && state.distanceToManeuverMeters < THEN_PREVIEW_METERS
        if (!state.ended) {
            val onAccent = theme.onStatusColor(theme.accentColor)
            Column(modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth().padding(6.dp)) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = FutureShapes.lg,
                    color = theme.accentColor,
                    shadowElevation = FutureElevation.card(theme.isDarkMode)
                ) {
                    Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        ManeuverIcon(state.currentStep, tint = onAccent, size = 52.dp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = formatManeuverDistance(state.distanceToManeuverMeters),
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.headlineLarge,
                                color = onAccent,
                                maxLines = 1
                            )
                            Text(
                                text = state.currentStep.streetName.takeIf { it.isNotBlank() } ?: maneuverText(state.currentStep),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = onAccent,
                                maxLines = 1
                            )
                        }
                    }
                }
                if (showThen && next != null) {
                    Surface(
                        modifier = Modifier.padding(top = 4.dp),
                        shape = FutureShapes.md,
                        color = MaterialTheme.colorScheme.surface,
                        shadowElevation = FutureElevation.card(theme.isDarkMode)
                    ) {
                        Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("ואז", style = MaterialTheme.typography.bodyMedium, color = theme.mutedTextColor)
                            Spacer(modifier = Modifier.width(6.dp))
                            ManeuverIcon(next, tint = theme.textColor, size = 22.dp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(maneuverText(next), style = MaterialTheme.typography.bodyMedium, color = theme.textColor, maxLines = 1)
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
                    // על גבי המפה הטקסט יושב על תווית, כדי שיהיה קריא מעל הכבישים
                    Surface(
                        shape = FutureShapes.md,
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                        modifier = Modifier.padding(bottom = 4.dp),
                    ) {
                        Text(
                            text = notice,
                            color = when {
                                reportMenu -> theme.textColor
                                report != null && state.reportMessage == null -> theme.dangerColor
                                else -> theme.mutedTextColor
                            },
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        )
                    }
                }

                // מהירות בעיגול בפינה, מעל השורה התחתונה, על גבי המפה
                val speed = state.speedKmh
                if (showSpeed && speed != null) {
                    Box(
                        modifier = Modifier.padding(bottom = 6.dp).size(58.dp).clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surface)
                            .border(3.dp, theme.textColor.copy(alpha = 0.25f), CircleShape),
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

                // שורה תחתונה (כמו Waze): שעת הגעה גדולה במרכז, ומתחת דקות וק"מ שנותרו;
                // השתק וסיום בצדדים
                Surface(
                    shape = FutureShapes.lg,
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = FutureElevation.card(theme.isDarkMode),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)
                ) {
                    Row(modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        TopBarIconButton(if (state.muted) FutureIcons.AutoMirrored.VolumeOff else FutureIcons.AutoMirrored.VolumeUp, tr("השתק"), theme.textColor, theme.accentColor, viewModel::toggleMute)
                        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                arrivalClock(state.remainingDurationSeconds),
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.headlineSmall,
                                color = theme.textColor,
                                maxLines = 1,
                                softWrap = false
                            )
                            Text(
                                formatRemainingTime(state.remainingDurationSeconds) + "  ·  " +
                                    tr("%.1f ק״מ").format(state.remainingDistanceMeters / 1000.0),
                                style = MaterialTheme.typography.bodyMedium,
                                color = theme.mutedTextColor,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                        TopBarIconButton(FutureIcons.Close, tr("סיים ניווט"), theme.dangerColor, theme.accentColor, onClick = {
                            // "בטל/צא מהניווט": לא רק לסמן ended=true אלא גם לצאת מהמסך.
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

/** מרחק לפנייה: מטרים בעיגול ל-10 (ול-50 מעל 300 מ'), ומעל ק"מ - בעשיריות. */
private fun formatManeuverDistance(meters: Double): String = when {
    meters >= 1000 -> tr("%.1f ק״מ").format(meters / 1000)
    meters >= 300 -> "${((meters / 50).toInt() * 50)} מ׳"
    else -> "${((meters / 10).toInt() * 10)} מ׳"
}

/** זמן שנותר: "12 דק׳", ומעל שעה "1:05 שע׳". */
private fun formatRemainingTime(seconds: Double): String {
    val minutes = kotlin.math.ceil(seconds / 60).toInt()
    return if (minutes >= 60) "%d:%02d שע׳".format(minutes / 60, minutes % 60) else tr("%d דק׳").format(minutes)
}

@Composable
private fun ManeuverIcon(step: Maneuver, tint: androidx.compose.ui.graphics.Color, size: androidx.compose.ui.unit.Dp) {
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
    val arrive = step.type == "arrive"
    Icon(
        if (arrive) FutureIcons.LocationOn else FutureIcons.ArrowUpward,
        contentDescription = null,
        tint = tint,
        modifier = Modifier.size(size).rotate(if (arrive) 0f else angle)
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
