package com.future.tools.ui

import com.future.sharednav.components.FutureChip
import com.future.sharednav.theme.FutureDimens
import com.future.sharednav.theme.FutureTypography
import com.future.sharednav.theme.mutedTextColor
import com.future.sharednav.theme.subtleTextColor
import android.Manifest
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.future.sharednav.theme.FutureTheme
import kotlin.math.abs
import kotlin.math.log2
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sqrt

private const val TUNER_SAMPLE_RATE = 44100
private const val YIN_WINDOW = 2048
private const val YIN_MAX_TAU = 1024 // ~43Hz - מתחת למיתר E הנמוך גם בכיוון מונמך
private const val YIN_MIN_TAU = 30 // ~1470Hz - מעל כל מיתר פתוח
private const val YIN_THRESHOLD = 0.15f
private const val IN_TUNE_CENTS = 5f

/** מיתר פתוח בכיוון סטנדרטי. [label] הוא שם התו כפי שמוצג על הצ'יפ. */
private data class GuitarString(val label: String, val note: String, val frequency: Double)

private val STANDARD_TUNING = listOf(
    GuitarString("E", "E2", 82.41),
    GuitarString("A", "A2", 110.00),
    GuitarString("D", "D3", 146.83),
    GuitarString("G", "G3", 196.00),
    GuitarString("B", "B3", 246.94),
    GuitarString("e", "E4", 329.63),
)

private val NOTE_NAMES = listOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")

private fun cents(frequency: Double, reference: Double): Float = (1200.0 * log2(frequency / reference)).toFloat()

/** שם התו הכרומטי הקרוב לתדר (A4 = 440Hz), למצב אוטומטי. */
private fun nearestNoteName(frequency: Double): String {
    val midi = (69 + 12 * log2(frequency / 440.0)).roundToInt()
    return NOTE_NAMES[((midi % 12) + 12) % 12] + (midi / 12 - 1)
}

/**
 * זיהוי גובה צליל בשיטת YIN (de Cheveigné & Kawahara) - עמיד יותר משיטת
 * אוטוקורלציה פשוטה לטעויות אוקטבה, שהן הבעיה הקלאסית במיתרי גיטרה עם
 * הרמוניות חזקות. מחזיר null כשאין צליל ברור (שקט או רעש).
 */
private fun detectPitch(buffer: FloatArray, diff: FloatArray): Double? {
    diff[0] = 1f
    var runningSum = 0f
    for (tau in 1 until YIN_MAX_TAU) {
        var sum = 0f
        for (i in 0 until YIN_WINDOW) {
            val d = buffer[i] - buffer[i + tau]
            sum += d * d
        }
        runningSum += sum
        diff[tau] = if (runningSum == 0f) 1f else sum * tau / runningSum
    }
    var tau = YIN_MIN_TAU
    while (tau < YIN_MAX_TAU) {
        if (diff[tau] < YIN_THRESHOLD) {
            while (tau + 1 < YIN_MAX_TAU && diff[tau + 1] < diff[tau]) tau++
            break
        }
        tau++
    }
    if (tau >= YIN_MAX_TAU) return null
    // אינטרפולציה פרבולית סביב המינימום - דיוק של שבר דגימה, הכרחי לסנטים
    val better = if (tau in 1 until YIN_MAX_TAU - 1) {
        val s0 = diff[tau - 1]; val s1 = diff[tau]; val s2 = diff[tau + 1]
        val denom = 2f * (2f * s1 - s2 - s0)
        if (denom != 0f) tau + (s2 - s0) / denom else tau.toFloat()
    } else tau.toFloat()
    return TUNER_SAMPLE_RATE / better.toDouble()
}

/** מאזין למיקרופון ברקע ומחזיר את התדר המזוהה האחרון (חציון של 5 קריאות). */
@Composable
private fun rememberDetectedPitch(enabled: Boolean): Double? {
    var pitch by remember { mutableStateOf<Double?>(null) }

    DisposableEffect(enabled) {
        if (!enabled) return@DisposableEffect onDispose {}
        val runningFlag = java.util.concurrent.atomic.AtomicBoolean(true)
        val thread = Thread {
            var record: AudioRecord? = null
            try {
                val minBuf = AudioRecord.getMinBufferSize(TUNER_SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
                if (minBuf <= 0) return@Thread
                val frame = YIN_WINDOW + YIN_MAX_TAU
                @Suppress("MissingPermission")
                record = AudioRecord(
                    MediaRecorder.AudioSource.MIC, TUNER_SAMPLE_RATE,
                    AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, max(minBuf, frame * 4)
                )
                if (record.state != AudioRecord.STATE_INITIALIZED) return@Thread
                record.startRecording()
                val shorts = ShortArray(frame)
                val samples = FloatArray(frame)
                val diff = FloatArray(YIN_MAX_TAU)
                val recent = ArrayDeque<Double>()
                var silentFrames = 0
                while (runningFlag.get()) {
                    var filled = 0
                    while (filled < frame && runningFlag.get()) {
                        val read = record.read(shorts, filled, frame - filled)
                        if (read <= 0) break
                        filled += read
                    }
                    if (filled < frame) continue
                    var sumSq = 0.0
                    for (i in 0 until frame) {
                        val v = shorts[i] / 32768f
                        samples[i] = v
                        sumSq += v * v
                    }
                    val rms = sqrt(sumSq / frame)
                    val detected = if (rms > 0.01) detectPitch(samples, diff) else null
                    if (detected == null || detected < 60.0 || detected > 1200.0) {
                        // שומרים את הקריאה האחרונה עוד רגע קצר כדי שהמחוג לא יהבהב בין פריטות
                        if (++silentFrames > 8) { recent.clear(); pitch = null }
                        continue
                    }
                    silentFrames = 0
                    recent.addLast(detected)
                    if (recent.size > 5) recent.removeFirst()
                    pitch = recent.sorted()[recent.size / 2]
                }
            } catch (e: Exception) {
                android.util.Log.w("GuitarTuner", "pitch capture failed", e)
            } finally {
                try { record?.stop() } catch (_: Exception) {}
                record?.release()
            }
        }
        thread.start()
        onDispose { runningFlag.set(false) }
    }
    return pitch
}

@Composable
fun GuitarTunerScreen(theme: FutureTheme, onBack: () -> Unit) {
    val hasPermission by rememberRuntimePermission(Manifest.permission.RECORD_AUDIO)
    val pitch = rememberDetectedPitch(enabled = hasPermission)
    // -1 = זיהוי אוטומטי של המיתר הקרוב, 0..5 = מיתר שנבחר ידנית (6 עד 1)
    var selected by remember { mutableIntStateOf(-1) }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    val target: GuitarString? = when {
        selected >= 0 -> STANDARD_TUNING[selected]
        pitch != null -> STANDARD_TUNING.minBy { abs(cents(pitch, it.frequency)) }
        else -> null
    }
    val centsOff = if (pitch != null && target != null) cents(pitch, target.frequency).coerceIn(-50f, 50f) else null

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(theme.backgroundColor)
                .focusRequester(focusRequester)
                .focusable()
                .onKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                    when (event.nativeKeyEvent.keyCode) {
                        // מקשי 1-6 = מספר המיתר כמו על הגיטרה (1 = e הדק, 6 = E העבה)
                        in android.view.KeyEvent.KEYCODE_1..android.view.KeyEvent.KEYCODE_6 -> {
                            selected = 6 - (event.nativeKeyEvent.keyCode - android.view.KeyEvent.KEYCODE_0); true
                        }
                        android.view.KeyEvent.KEYCODE_0 -> { selected = -1; true }
                        android.view.KeyEvent.KEYCODE_DPAD_LEFT -> { selected = if (selected <= -1) 5 else selected - 1; true }
                        android.view.KeyEvent.KEYCODE_DPAD_RIGHT -> { selected = if (selected >= 5) -1 else selected + 1; true }
                        else -> false
                    }
                }
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                ToolsHeader(title = "מכוון גיטרה", theme = theme, onBack = onBack)

                // המיתרים מסודרים משמאל לימין כמו על צוואר הגיטרה, גם בממשק RTL
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = FutureDimens.screenPadding, vertical = FutureDimens.spacingSm),
                        horizontalArrangement = Arrangement.spacedBy(FutureDimens.spacingXs, Alignment.CenterHorizontally)
                    ) {
                        FutureChip("אוטו", theme, selected = selected == -1)
                        STANDARD_TUNING.forEachIndexed { index, string ->
                            FutureChip(string.label, theme, selected = selected == index || (selected == -1 && target == string && pitch != null))
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    if (!hasPermission) {
                        Text(
                            "נדרשת הרשאת מיקרופון כדי לכוון",
                            color = theme.mutedTextColor,
                            fontSize = FutureTypography.body,
                            modifier = Modifier.padding(horizontal = 32.dp)
                        )
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            val inTune = centsOff != null && abs(centsOff) <= IN_TUNE_CENTS
                            Text(
                                when {
                                    selected >= 0 -> target!!.note
                                    pitch != null -> nearestNoteName(pitch)
                                    else -> "--"
                                },
                                color = if (inTune) theme.successColor else theme.textColor,
                                fontSize = FutureTypography.hero,
                                fontWeight = FontWeight.Light,
                            )
                            Text(
                                if (pitch != null) "%.1f Hz".format(pitch) else "נגן מיתר אחד",
                                color = theme.mutedTextColor,
                                fontSize = FutureTypography.bodyLarge,
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            CentsMeter(cents = centsOff, theme = theme)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                when {
                                    centsOff == null -> " "
                                    inTune -> "מכוון"
                                    centsOff < 0 -> "נמוך - הדק את המיתר (${centsOff.roundToInt()})"
                                    else -> "גבוה - שחרר את המיתר (+${centsOff.roundToInt()})"
                                },
                                color = when {
                                    centsOff == null -> theme.mutedTextColor
                                    inTune -> theme.successColor
                                    abs(centsOff) < 20f -> theme.warningColor
                                    else -> theme.dangerColor
                                },
                                fontSize = FutureTypography.title,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                    }
                }

                Text(
                    "1-6 בחירת מיתר · 0 זיהוי אוטומטי · חצים למעבר",
                    color = theme.subtleTextColor,
                    fontSize = FutureTypography.caption,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp)
                )
            }
        }
    }
}

/** סרגל סטייה של ±50 סנט: מחוג שזז שמאלה כשהצליל נמוך וימינה כשהוא גבוה. */
@Composable
private fun CentsMeter(cents: Float?, theme: FutureTheme) {
    val position by animateFloatAsState((cents ?: 0f) / 50f, label = "tunerNeedle")
    val needleColor = when {
        cents == null -> theme.textColor.copy(alpha = 0.2f)
        abs(cents) <= IN_TUNE_CENTS -> theme.successColor
        abs(cents) < 20f -> theme.warningColor
        else -> theme.dangerColor
    }
    Canvas(modifier = Modifier.width(260.dp).height(64.dp)) {
        val midY = size.height / 2f
        val centerX = size.width / 2f
        val half = size.width / 2f - 8.dp.toPx()
        drawLine(theme.textColor.copy(alpha = 0.15f), Offset(centerX - half, midY), Offset(centerX + half, midY), 3.dp.toPx(), StrokeCap.Round)
        for (step in -5..5) {
            val x = centerX + half * step / 5f
            val tall = step == 0
            val h = if (tall) size.height * 0.45f else size.height * 0.18f
            drawLine(
                if (tall) theme.successColor else theme.textColor.copy(alpha = 0.35f),
                Offset(x, midY - h), Offset(x, midY + h),
                if (tall) 3.dp.toPx() else 1.5.dp.toPx(), StrokeCap.Round
            )
        }
        val x = centerX + half * position
        drawLine(needleColor, Offset(x, 0f), Offset(x, size.height), 4.dp.toPx(), StrokeCap.Round)
        drawCircle(needleColor, 7.dp.toPx(), Offset(x, midY))
    }
}
