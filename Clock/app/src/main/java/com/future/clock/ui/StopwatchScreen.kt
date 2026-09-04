package com.future.clock.ui
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

                Box(modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp), contentAlignment = Alignment.Center) {
                    Text(
                        formatElapsed(displayedElapsed),
                        color = theme.textColor,
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Light
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StopwatchActionButton(
                        label = if (isRunning) "עצור" else "התחל",
                        isPrimary = true,
                        color = if (isRunning) theme.dangerColor else theme.successColor,
                        theme = theme,
                        focusRequester = focusRequester
                    ) {
                        if (isRunning) {
                            elapsedBeforeStart = displayedElapsed
                            isRunning = false
                        } else {
                            startedAtElapsedRealtime = android.os.SystemClock.elapsedRealtime()
                            isRunning = true
                        }
                    }
                    StopwatchActionButton(
                        label = if (isRunning) "הקפה" else "איפוס",
                        isPrimary = false,
                        color = theme.textColor.copy(alpha = 0.12f),
                        theme = theme
                    ) {
                        if (isRunning) {
                            laps.add(0, displayedElapsed)
                        } else {
                            isRunning = false
                            elapsedBeforeStart = 0L
                            displayedElapsed = 0L
                            laps.clear()
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                if (laps.isNotEmpty()) {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        itemsIndexed(laps) { index, lap ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("הקפה ${laps.size - index}", color = theme.textColor.copy(alpha = 0.5f), fontSize = 13.sp)
                                Text(formatElapsed(lap), color = theme.textColor, fontSize = 15.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StopwatchActionButton(label: String, isPrimary: Boolean, color: Color, theme: FutureTheme, focusRequester: FocusRequester? = null, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val bgColor by animateColorAsState(
        if (isFocused) color.copy(alpha = 1f) else color.copy(alpha = if (isPrimary) 0.75f else 1f),
        label = "swActionBg"
    )
    Box(
        modifier = Modifier
            .size(84.dp)
            .clip(CircleShape)
            .background(bgColor)
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .focusable(interactionSource = interactionSource).bringIntoViewOnFocus(),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = if (isPrimary) Color.Black else theme.textColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}
