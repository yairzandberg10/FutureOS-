package com.future.tools.ui

import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.future.sharednav.theme.FutureTheme
import kotlinx.coroutines.delay

private enum class PomodoroPhase(val label: String, val minutes: Int, val color: Color) {
    FOCUS("מיקוד", 25, Color(0xFFFF6B6B)),
    SHORT_BREAK("הפסקה קצרה", 5, Color(0xFF32D74B)),
    LONG_BREAK("הפסקה ארוכה", 15, Color(0xFF64D2FF))
}

private fun vibrate(context: android.content.Context) {
    try {
        val vibrator = if (Build.VERSION.SDK_INT >= 31) {
            (context.getSystemService(android.content.Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(android.content.Context.VIBRATOR_SERVICE) as Vibrator
        }
        vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 300, 150, 300), -1))
    } catch (e: Exception) {}
}

@Composable
fun PomodoroScreen(theme: FutureTheme, onBack: () -> Unit) {
    val context = LocalContext.current
    var phase by remember { mutableStateOf(PomodoroPhase.FOCUS) }
    var completedFocusCycles by remember { mutableIntStateOf(0) }
    var remainingMillis by remember { mutableLongStateOf(PomodoroPhase.FOCUS.minutes * 60_000L) }
    var isRunning by remember { mutableStateOf(false) }

    fun startNextPhase() {
        val next = when (phase) {
            PomodoroPhase.FOCUS -> {
                completedFocusCycles += 1
                if (completedFocusCycles % 4 == 0) PomodoroPhase.LONG_BREAK else PomodoroPhase.SHORT_BREAK
            }
            PomodoroPhase.SHORT_BREAK, PomodoroPhase.LONG_BREAK -> PomodoroPhase.FOCUS
        }
        phase = next
        remainingMillis = next.minutes * 60_000L
        vibrate(context)
    }

    LaunchedEffect(isRunning) {
        while (isRunning) {
            delay(200)
            remainingMillis -= 200
            if (remainingMillis <= 0) {
                startNextPhase()
            }
        }
    }

    fun resetAll() {
        isRunning = false
        phase = PomodoroPhase.FOCUS
        completedFocusCycles = 0
        remainingMillis = PomodoroPhase.FOCUS.minutes * 60_000L
    }

    val totalSeconds = (remainingMillis + 999) / 1000
    val minutesText = (totalSeconds / 60).coerceAtLeast(0)
    val secondsText = (totalSeconds % 60).coerceAtLeast(0)

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Box(modifier = Modifier.fillMaxSize().background(theme.backgroundColor)) {
            Column(modifier = Modifier.fillMaxSize()) {
                ToolsHeader(title = "פומודורו", theme = theme, onBack = onBack)

                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(phase.label, color = phase.color, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "%02d:%02d".format(minutesText, secondsText),
                            color = theme.textColor,
                            fontSize = 56.sp,
                            fontWeight = FontWeight.Light
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("מחזורי מיקוד שהושלמו: $completedFocusCycles", color = theme.textColor.copy(alpha = 0.5f), fontSize = 13.sp)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 24.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    PomodoroActionButton(
                        label = if (isRunning) "השהה" else "התחל",
                        color = if (isRunning) theme.warningColor else theme.successColor
                    ) { isRunning = !isRunning }
                    PomodoroActionButton(label = "דלג", color = theme.textColor.copy(alpha = 0.12f)) {
                        startNextPhase()
                    }
                    PomodoroActionButton(label = "איפוס", color = theme.textColor.copy(alpha = 0.12f)) {
                        resetAll()
                    }
                }
            }
        }
    }
}

@Composable
private fun PomodoroActionButton(label: String, color: Color, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val bgColor = if (isFocused) color.copy(alpha = 1f) else color.copy(alpha = 0.8f)
    Box(
        modifier = Modifier
            .size(76.dp)
            .clip(CircleShape)
            .background(bgColor)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .focusable(interactionSource = interactionSource),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}
