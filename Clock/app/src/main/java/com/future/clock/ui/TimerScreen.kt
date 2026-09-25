package com.future.clock.ui
import com.future.sharednav.components.FutureSnackbarHost
import com.future.sharednav.theme.readableAccentColor
import com.future.sharednav.theme.LocalFutureTheme

import com.future.sharednav.theme.FutureTypography
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
import com.future.sharednav.nav.digitForKey
import com.future.sharednav.theme.FutureTheme
import com.future.sharednav.theme.FutureDimens
import com.future.sharednav.theme.mutedTextColor
import com.future.sharednav.components.FutureButtonVariant
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
    } catch (e: Exception) {
        android.util.Log.w("TimerScreen", "vibrate failed", e)
    }
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
    // האורך המלא של הספירה הנוכחית - הבסיס של הטבעת (כמה עבר / כמה נשאר).
    var totalMillis by remember { mutableLongStateOf(0L) }
    val snackbar = com.future.sharednav.components.rememberFutureSnackbarState()
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
        totalMillis = totalMs
        deadlineElapsedRealtime = android.os.SystemClock.elapsedRealtime() + totalMs
        isFinished = false
        isRunning = true
    }

    fun resetTimer() {
        isRunning = false
        isFinished = false
        remainingMillis = 0
        totalMillis = 0
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

                // הטבעת: החלק הצבוע הוא מה שנשאר, והוא מתקצר עם הזמן.
                val progress = when {
                    isFinished -> 0f
                    totalMillis > 0 -> (remainingMillis.toFloat() / totalMillis).coerceIn(0f, 1f)
                    else -> 1f
                }
                val ringColor = if (isFinished) theme.dangerColor else theme.readableAccentColor
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = FutureDimens.spacingLg),
                    contentAlignment = Alignment.Center
                ) {
                    TimerRing(
                        progress = progress,
                        active = totalMillis > 0,
                        color = ringColor,
                        track = theme.textColor.copy(alpha = 0.10f),
                        modifier = Modifier.size(TimerRingSize),
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (isRunning || remainingMillis > 0) {
                                val totalSeconds = (remainingMillis + 999) / 1000
                                "%02d:%02d".format(totalSeconds / 60, totalSeconds % 60)
                            } else mmssToText(mmss),
                            color = if (isFinished) theme.dangerColor else theme.textColor,
                            fontSize = FutureTypography.hero,
                            fontWeight = FutureTypography.weightLight,
                            fontFamily = FutureTypography.monoFamily
                        )
                        val caption = when {
                            isFinished -> "הזמן נגמר"
                            !isRunning && remainingMillis <= 0 -> "הקלד דקות ושניות"
                            totalMillis > 0 -> {
                                val passed = ((totalMillis - remainingMillis) / 1000).coerceAtLeast(0)
                                "עברו %d:%02d".format(passed / 60, passed % 60)
                            }
                            else -> ""
                        }
                        if (caption.isNotEmpty()) {
                            Text(
                                caption,
                                color = if (isFinished) theme.dangerColor else theme.mutedTextColor,
                                fontSize = FutureTypography.summary,
                            )
                        }
                    }
                }

                ClockActionRow(
                    primaryLabel = if (isRunning) "השהה" else "התחל",
                    primaryVariant = FutureButtonVariant.Primary,
                    onPrimary = {
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
                    },
                    secondaryLabel = "איפוס",
                    onSecondary = { resetTimer() },
                    theme = theme,
                    primaryFocus = focusRequester,
                )
            }
            FutureSnackbarHost(snackbar, theme)
        }
    }
    ClockShortcutMenu(route = ClockRoute.Timer, title = "טיימר", theme = theme, onMessage = snackbar::show)
}

/** 190dp - הטבעת סביב הזמן; נכנסת עם הכפתורים והסרגל התחתון במסך אחד. */
private val TimerRingSize = 190.dp

/**
 * טבעת ההתקדמות של הטיימר: מסלול ב-10% מהטקסט, ומעליו קשת בהדגשה עם
 * קצוות מעוגלים (כמו FutureSpinner) שמתחילה למעלה ונסגרת עם כיוון השעון.
 */
@Composable
private fun TimerRing(progress: Float, active: Boolean, color: Color, track: Color, modifier: Modifier) {
    val animated by androidx.compose.animation.core.animateFloatAsState(
        progress,
        com.future.sharednav.theme.FutureMotion.fast(),
        label = "timerRing",
    )
    androidx.compose.foundation.Canvas(modifier = modifier) {
        val stroke = 8.dp.toPx()
        val inset = stroke / 2
        val arcSize = androidx.compose.ui.geometry.Size(size.width - stroke, size.height - stroke)
        val topLeft = androidx.compose.ui.geometry.Offset(inset, inset)
        drawArc(track, 0f, 360f, false, topLeft, arcSize, style = androidx.compose.ui.graphics.drawscope.Stroke(stroke))
        if (active && animated > 0f) {
            drawArc(
                color, -90f, 360f * animated, false, topLeft, arcSize,
                style = androidx.compose.ui.graphics.drawscope.Stroke(stroke, cap = androidx.compose.ui.graphics.StrokeCap.Round),
            )
        }
    }
}
