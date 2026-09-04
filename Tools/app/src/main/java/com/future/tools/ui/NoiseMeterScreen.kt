package com.future.tools.ui

import android.Manifest
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.future.sharednav.theme.FutureTheme
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.roundToInt

private const val SAMPLE_RATE = 44100

/** קורא רמת עוצמה גולמית מהמיקרופון וממיר אותה לדציבלים משוערים (יחסי - לא
 * כיול אבסולוטי מול מד SPL מקצועי, אבל שימושי להשוואה בין סביבות). */
@Composable
private fun rememberDecibels(enabled: Boolean): Float {
    var db by remember { mutableFloatStateOf(0f) }

    DisposableEffect(enabled) {
        if (!enabled) return@DisposableEffect onDispose {}

        var record: AudioRecord? = null
        var running = true
        val thread = Thread {
            try {
                val minBufSize = AudioRecord.getMinBufferSize(
                    SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT
                )
                if (minBufSize <= 0) return@Thread
                val bufSize = max(minBufSize, 2048)
                @Suppress("MissingPermission")
                record = AudioRecord(
                    MediaRecorder.AudioSource.MIC, SAMPLE_RATE,
                    AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufSize
                )
                if (record?.state != AudioRecord.STATE_INITIALIZED) return@Thread
                record?.startRecording()
                val buffer = ShortArray(bufSize)
                while (running) {
                    val read = record?.read(buffer, 0, bufSize) ?: 0
                    if (read > 0) {
                        var sum = 0.0
                        for (i in 0 until read) sum += (buffer[i] * buffer[i]).toDouble()
                        val rms = kotlin.math.sqrt(sum / read)
                        val calculatedDb = if (rms > 1.0) 20 * log10(rms) else 0.0
                        db = calculatedDb.toFloat().coerceIn(0f, 120f)
                    }
                }
            } catch (e: Exception) {
                // אין הרשאה/מיקרופון תפוס - נשארים על 0
            } finally {
                try { record?.stop() } catch (e: Exception) {}
                record?.release()
            }
        }
        thread.start()

        onDispose {
            running = false
            thread.interrupt()
        }
    }

    return db
}

private fun levelLabel(db: Float): String = when {
    db < 30f -> "שקט"
    db < 50f -> "רגוע"
    db < 65f -> "שיחה רגילה"
    db < 80f -> "רועש"
    db < 95f -> "רועש מאוד"
    else -> "חשוף לנזק שמיעה"
}

@Composable
fun NoiseMeterScreen(theme: FutureTheme, onBack: () -> Unit) {
    val hasPermission by rememberRuntimePermission(Manifest.permission.RECORD_AUDIO)
    val db = rememberDecibels(enabled = hasPermission)

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Box(modifier = Modifier.fillMaxSize().background(theme.backgroundColor)) {
            Column(modifier = Modifier.fillMaxSize()) {
                ToolsHeader(title = "מד רעש", theme = theme, onBack = onBack)

                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    if (!hasPermission) {
                        Text(
                            "נדרשת הרשאת מיקרופון כדי למדוד רעש",
                            color = theme.textColor.copy(alpha = 0.6f),
                            fontSize = 14.sp,
                            modifier = Modifier.padding(horizontal = 32.dp)
                        )
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            NoiseGauge(db = db, theme = theme)
                            Spacer(modifier = Modifier.height(20.dp))
                            Text("${db.roundToInt()} dB", color = theme.textColor, fontSize = 40.sp, fontWeight = FontWeight.Light)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(levelLabel(db), color = theme.accentColor, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NoiseGauge(db: Float, theme: FutureTheme) {
    val size = 220.dp
    val fraction = (db / 120f).coerceIn(0f, 1f)
    val gaugeColor = when {
        db < 50f -> theme.successColor
        db < 80f -> theme.warningColor
        else -> theme.dangerColor
    }
    Box(modifier = Modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 16.dp.toPx()
            val arcSize = Size(this.size.width - strokeWidth, this.size.height - strokeWidth)
            val topLeft = Offset(strokeWidth / 2, strokeWidth / 2)
            drawArc(
                color = theme.textColor.copy(alpha = 0.1f),
                startAngle = 135f, sweepAngle = 270f, useCenter = false,
                topLeft = topLeft, size = arcSize,
                style = Stroke(width = strokeWidth, cap = androidx.compose.ui.graphics.StrokeCap.Round)
            )
            drawArc(
                color = gaugeColor,
                startAngle = 135f, sweepAngle = 270f * fraction, useCenter = false,
                topLeft = topLeft, size = arcSize,
                style = Stroke(width = strokeWidth, cap = androidx.compose.ui.graphics.StrokeCap.Round)
            )
        }
    }
}
