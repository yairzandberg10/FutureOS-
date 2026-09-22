package com.future.navigation.ui.navigate
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.future.navigation.R
import com.future.navigation.data.routing.Maneuver
import com.future.navigation.ui.map.MapCameraController
import com.future.navigation.ui.map.NavMapView

@Composable
fun NavigateScreen(viewModel: NavigateViewModel, onClose: () -> Unit = {}) {
    val theme = LocalFutureTheme.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val cameraController = remember { MapCameraController() }
    var followMode by remember { mutableStateOf(true) }

    androidx.compose.runtime.LaunchedEffect(state.currentLocation, followMode) {
        val loc = state.currentLocation
        if (loc != null && followMode) {
            cameraController.recenter(loc)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxWidth().height(420.dp)) {
            NavMapView(
                cameraController = cameraController,
                initialCenter = state.currentLocation,
                onRecenterRequested = {
                    followMode = true
                    state.currentLocation?.let { cameraController.recenter(it) }
                },
                onManualPan = { followMode = false }
            )
        }

        // כרטיס ההוראה הבאה. היה מלא בצבע ההדגשה עם צל של 8dp - אבל ההדגשה
        // אינה מילוי מותג, והצל היחיד במערכת הוא של כרטיס (FutureElevation.card).
        Surface(
            modifier = Modifier.fillMaxWidth().padding(horizontal = FutureDimens.screenPadding).offset(y = (-30).dp),
            shape = FutureShapes.xl,
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = FutureElevation.card(theme.isDarkMode)
        ) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(52.dp).clip(FutureShapes.md).background(theme.elevatedSurfaceColor),
                    contentAlignment = Alignment.Center
                ) {
                    ManeuverIcon(state.currentStep)
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = maneuverText(state.currentStep),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge,
                        color = theme.textColor
                    )
                    Text(
                        text = "בעוד %d מ׳%s".format(
                            state.distanceToManeuverMeters.toInt(),
                            state.currentStep.streetName.takeIf { it.isNotBlank() }?.let { " · ל$it" } ?: ""
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = theme.mutedTextColor
                    )
                }
            }
        }

        Column(modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = FutureDimens.screenPadding, vertical = FutureDimens.spacingSm)) {
            if (!state.ended) {
                Surface(shape = FutureShapes.lg, color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        TopBarIconButton(if (state.muted) Icons.AutoMirrored.Rounded.VolumeOff else Icons.AutoMirrored.Rounded.VolumeUp, "השתק", theme.textColor, theme.accentColor, viewModel::toggleMute)
                        Spacer(modifier = Modifier.width(12.dp))
                        Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("%d דק׳".format((state.remainingDurationSeconds / 60).toInt()), fontWeight = FontWeight.Bold)
                            Text("·", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
                            Text("%.1f ק״מ".format(state.remainingDistanceMeters / 1000.0))
                        }
                        TopBarIconButton(Icons.Rounded.Close, "סיים ניווט", theme.dangerColor, theme.accentColor, onClick = {
                            // הכפתור הזה משמעו "בטל/צא מהניווט" - לא רק לסמן ended=true (זה
                            // קורה גם אוטומטית בהגעה בפועל ליעד) אלא גם לצאת בפועל מהמסך,
                            // אחרת המשתמש נשאר תקוע על מסך המפה עם באנר "הגעת ליעד" שגוי.
                            viewModel.endNavigation()
                            onClose()
                        })
                    }
                }
            } else {
                Surface(shape = FutureShapes.lg, color = theme.successColor.copy(alpha = 0.15f), modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.padding(FutureDimens.spacingMd), verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(FutureDimens.rowHeightTopBarButton).clip(CircleShape).background(theme.successColor), contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.Check, contentDescription = null, tint = theme.onStatusColor(theme.successColor))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(stringResource(R.string.arrived_at_destination), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

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
        Icons.Rounded.ArrowUpward,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.rotate(angle)
    )
}

private fun maneuverText(step: Maneuver): String = when (step.type) {
    "depart" -> "יציאה לדרך"
    "arrive" -> "הגעה ליעד"
    "roundabout", "rotary" -> "כיכר - המשך לפי השילוט"
    else -> when (step.modifier) {
        "left" -> "פנה שמאלה"
        "slight left" -> "פנה מעט שמאלה"
        "sharp left" -> "פנה חדות שמאלה"
        "right" -> "פנה ימינה"
        "slight right" -> "פנה מעט ימינה"
        "sharp right" -> "פנה חדות ימינה"
        "uturn" -> "פנה פרסה"
        else -> "המשך ישר"
    }
}
