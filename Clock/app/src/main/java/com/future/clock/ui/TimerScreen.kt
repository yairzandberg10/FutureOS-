package com.future.clock.ui

import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.future.sharednav.theme.FutureTheme
import kotlinx.coroutines.delay

private fun mmssToText(mmss: Int): String = "%02d:%02d".format(mmss / 100, mmss % 100)

private fun vibrate(context: android.content.Context) {
    try {
        val vibrator = if (Build.VERSION.SDK_INT >= 31) {
            (context.getSystemService(android.content.Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(android.content.Context.VIBRATOR_SERVICE) as Vibrator
        }
        vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 400, 200, 400, 200, 400), -1))
    } catch (e: Exception) {}
}

@Composable
fun TimerScreen(theme: FutureTheme, onBack: () -> Unit) {
    val context = LocalContext.current
    var mmss by remember { mutableIntStateOf(0) }
    var remainingMillis by remember { mutableLongStateOf(0L) }
    // זמן היעד (elapsedRealtime) שבו הטיימר אמור להסתיים - נגזר מחדש בכל טיק
    // במקום להוריד 100ms בכל פעם, כך שאין סחיפה (drift) מעיכובי תזמון של
    // הקורוטינה, וכשהאפליקציה חוזרת מהרקע (אחרי delay שנעצר) הזמן הנותר
    // מחושב נכון מהשעון היחסי ולא ממשיך לספור מאיפה שהפסיק. אותו דפוס בדיוק
    // כמו ב-StopwatchScreen (startedAtElapsedRealtime).
    var deadlineElapsedRealtime by remember { mutableLongStateOf(0L) }
    var isRunning by remember { mutableStateOf(false) }
    var isFinished by remember { mutableStateOf(false) }
    // אותה תקלת "אין פוקוס" שתועדה ותוקנה במחשבון/ממיר יחידות - ראו שם. בלי
    // פריט ממוקד כלשהו בעץ, גם onKeyEvent של ה-Box החיצוני (להקשת דקות/שניות)
    // לא היה מקבל אירועי מקש בכלל.
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    LaunchedEffect(isRunning) {
        while (isRunning) {
            val remaining = deadlineElapsedRealtime - android.os.SystemClock.elapsedRealtime()
            if (remaining <= 0) {
                remainingMillis = 0
                isRunning = false
                isFinished = true
                vibrate(context)
            } else {
                remainingMillis = remaining
                delay(100)
            }
        }
    }

    fun startTimer() {
        val minutes = mmss / 100
        val seconds = mmss % 100
        val totalMs = (minutes * 60L + seconds) * 1000L
        if (totalMs <= 0) return
        remainingMillis = totalMs
        deadlineElapsedRealtime = android.os.SystemClock.elapsedRealtime() + totalMs
        isFinished = false
        isRunning = true
    }

    fun resetTimer() {
        isRunning = false
        isFinished = false
        remainingMillis = 0
        mmss = 0
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(theme.backgroundColor)
                .onKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown || isRunning) return@onKeyEvent false
                    digitForKey(event.key)?.let { digit ->
                        // רגיסטר הזזה של 4 ספרות (MMSS). ה-mod הישן היה 6000, מה
                        // שאִפשר להקליד שניות לא חוקיות כמו "09:99" - כל ספרה
                        // שהייתה הופכת את חלק השניות ל-60 ומעלה נדחית כדי לשמור
                        // על ערך MM:SS תקין תמיד.
                        val candidate = ((mmss * 10) + digit.toInt()) % 10000
                        if (candidate % 100 <= 59) {
                            mmss = candidate
                            isFinished = false
                        }
                        true
                    } ?: false
                }
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                ToolsHeader(title = "טיימר", theme = theme, onBack = onBack)

                Box(modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp), contentAlignment = Alignment.Center) {
                    Text(
                        text = if (isRunning || remainingMillis > 0) {
                            val totalSeconds = (remainingMillis + 999) / 1000
                            "%02d:%02d".format(totalSeconds / 60, totalSeconds % 60)
                        } else mmssToText(mmss),
                        color = if (isFinished) theme.dangerColor else theme.textColor,
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Light
                    )
                }

                if (!isRunning && remainingMillis <= 0) {
                    Text(
                        "הקלד דקות ושניות במקלדת",
                        color = theme.textColor.copy(alpha = 0.4f),
                        fontSize = 13.sp,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }

                if (isFinished) {
                    Text(
                        "הזמן נגמר!",
                        color = theme.dangerColor,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    TimerActionButton(
                        label = if (isRunning) "השהה" else "התחל",
                        color = if (isRunning) theme.warningColor else theme.successColor,
                        focusRequester = focusRequester
                    ) {
                        if (isRunning) {
                            // עוצרים זמנית - שומרים את הזמן שנותר במקום להמשיך
                            // לגזור אותו מ-deadline ישן שכבר לא רלוונטי.
                            remainingMillis = (deadlineElapsedRealtime - android.os.SystemClock.elapsedRealtime()).coerceAtLeast(0L)
                            isRunning = false
                        } else if (remainingMillis > 0) {
                            deadlineElapsedRealtime = android.os.SystemClock.elapsedRealtime() + remainingMillis
                            isRunning = true
                        } else {
                            startTimer()
                        }
                    }
                    TimerActionButton(label = "איפוס", color = theme.textColor.copy(alpha = 0.12f)) { resetTimer() }
                }
            }
        }
    }
}

@Composable
private fun TimerActionButton(label: String, color: Color, focusRequester: FocusRequester? = null, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val bgColor by animateColorAsState(if (isFocused) color.copy(alpha = 1f) else color.copy(alpha = 0.8f), label = "timerActionBg")
    Box(
        modifier = Modifier
            .size(84.dp)
            .clip(CircleShape)
            .background(bgColor)
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .focusable(interactionSource = interactionSource),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = Color.Black, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}
