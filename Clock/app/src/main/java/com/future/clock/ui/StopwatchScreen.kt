package com.future.clock.ui
import com.future.sharednav.components.FutureSnackbarHost
import com.future.sharednav.theme.readableAccentColor
import com.future.sharednav.components.FutureProgressBar
import com.future.sharednav.theme.FutureTypography
import com.future.sharednav.focus.bringIntoViewOnFocus

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.future.sharednav.theme.FutureTheme
import com.future.sharednav.theme.FutureDimens
import com.future.sharednav.theme.mutedTextColor
import com.future.sharednav.components.FutureButtonVariant
import kotlinx.coroutines.delay

private fun formatElapsed(millis: Long): String {
    val totalCentis = millis / 10
    val minutes = totalCentis / 6000
    val seconds = (totalCentis / 100) % 60
    val centis = totalCentis % 100
    return "%02d:%02d.%02d".format(minutes, seconds, centis)
}

@Composable
fun StopwatchScreen(theme: FutureTheme, onBack: () -> Unit) {
    var isRunning by remember { mutableStateOf(false) }
    var elapsedBeforeStart by remember { mutableLongStateOf(0L) }
    var startedAtElapsedRealtime by remember { mutableLongStateOf(0L) }
    var displayedElapsed by remember { mutableLongStateOf(0L) }
    val laps = remember { mutableStateListOf<Long>() }
    val snackbar = com.future.sharednav.components.rememberFutureSnackbarState()
    // אותה תקלת "אין פוקוס" שתועדה ותוקנה במחשבון/ממיר יחידות - ראו שם.
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    LaunchedEffect(isRunning) {
        while (isRunning) {
            displayedElapsed = elapsedBeforeStart + (android.os.SystemClock.elapsedRealtime() - startedAtElapsedRealtime)
            delay(30)
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Box(modifier = Modifier.fillMaxSize().background(theme.backgroundColor)) {
            Column(modifier = Modifier.fillMaxSize()) {
                ToolsHeader(title = "שעון עצר", theme = theme, onBack = onBack)

                Box(modifier = Modifier.fillMaxWidth().padding(vertical = FutureDimens.spacingXl), contentAlignment = Alignment.Center) {
                    Text(
                        formatElapsed(displayedElapsed),
                        color = theme.textColor,
                        fontSize = FutureTypography.hero,
                        fontWeight = FutureTypography.weightLight,
                        fontFamily = FutureTypography.monoFamily
                    )
                }

                // סיבוב השניות מתחת לשעון (ui_kits/clock). המספרים הגדולים
                // משתנים לאט מכדי למסור "רץ" במבט חטוף; הפס עושה את זה.
                if (isRunning) {
                    FutureProgressBar(
                        progress = (displayedElapsed % 60_000L) / 60_000f,
                        theme = theme,
                        modifier = Modifier.padding(start = FutureDimens.spacingXxl, end = FutureDimens.spacingXxl, bottom = FutureDimens.spacingLg),
                    )
                }

                // כפתורי המערכת (Button.jsx) - גלולה אטומה 44dp, ולא העיגולים
                // השקופים של 84dp שהיו כאן: אלה לא היו בדיזיין סיסטם ולקחו
                // חצי מגובה המסך, כך שכמעט לא נשאר מקום לרשימת ההקפות.
                ClockActionRow(
                    primaryLabel = if (isRunning) "עצור" else "התחל",
                    primaryVariant = if (isRunning) FutureButtonVariant.Destructive else FutureButtonVariant.Primary,
                    onPrimary = {
                        if (isRunning) {
                            elapsedBeforeStart = displayedElapsed
                            isRunning = false
                        } else {
                            startedAtElapsedRealtime = android.os.SystemClock.elapsedRealtime()
                            isRunning = true
                        }
                    },
                    secondaryLabel = if (isRunning) "הקפה" else "איפוס",
                    onSecondary = {
                        if (isRunning) {
                            laps.add(0, displayedElapsed)
                        } else {
                            elapsedBeforeStart = 0L
                            displayedElapsed = 0L
                            laps.clear()
                        }
                    },
                    theme = theme,
                    primaryFocus = focusRequester,
                )

                if (laps.isNotEmpty()) {
                    LazyColumn(
                        modifier = Modifier.weight(1f).padding(top = FutureDimens.spacingLg),
                        contentPadding = PaddingValues(horizontal = FutureDimens.spacingXl, vertical = FutureDimens.spacingXs),
                    ) {
                        itemsIndexed(laps) { index, lap ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = FutureDimens.spacingSm),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text("הקפה ${laps.size - index}", color = theme.mutedTextColor, fontSize = FutureTypography.summary)
                                Text(formatElapsed(lap), color = theme.textColor, fontSize = FutureTypography.bodyLarge, fontFamily = FutureTypography.monoFamily)
                            }
                        }
                    }
                }
            }
            FutureSnackbarHost(snackbar, theme)
        }
    }
    ClockShortcutMenu(route = ClockRoute.Stopwatch, title = "שעון עצר", theme = theme, onMessage = snackbar::show)
}
