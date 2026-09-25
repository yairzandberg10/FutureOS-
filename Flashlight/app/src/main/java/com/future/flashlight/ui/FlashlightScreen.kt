package com.future.flashlight.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import com.future.flashlight.data.FlashlightController
import com.future.flashlight.data.TorchService
import com.future.sharednav.components.FutureButton
import com.future.sharednav.components.ScreenTopBar
import com.future.sharednav.theme.FutureTheme
import com.future.sharednav.theme.FutureTypography

// העיצוב מהקנבס "פנס תלת מימד" (artboard Main/On): פנס מתכת באלכסון, ראשו
// מופנה למעלה-שמאלה ומעט אל הצופה. כשדולק - העדשה בוהקת ואלומה יוצאת ממנה.
// כל הקואורדינטות בפיקסלים של מסך 640x960 ומוקטנות לגודל המסך בפועל. בתוך
// inTorch הציר הוא ציר הפנס: x מהזנב (0) אל העדשה (470), y לרוחב (למעלה = הצד המואר).
private const val DESIGN_W = 640f
private const val DESIGN_H = 960f

@Composable
fun FlashlightScreen(theme: FutureTheme) {
    val context = LocalContext.current
    val controller = remember { FlashlightController(context) }
    var isOn by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    // המסך תמיד כהה - הפנס מצויר על במה שחורה גם כשהמערכת במצב בהיר
    val dark = remember(theme.accentColor) { FutureTheme(isDarkMode = true, accentColor = theme.accentColor) }

    // אין כאן בקשת הרשאה: setTorchMode לא צריך הרשאת CAMERA. המצב נקרא
    // מהמערכת (TorchCallback), כולל הדלקה/כיבוי ממרכז הבקרה, וכשהמצלמה
    // תופסת את הפנס.
    DisposableEffect(Unit) {
        val stop = controller.observe { on, available ->
            isOn = on
            errorMessage = if (!available) "הפנס לא זמין כשהמצלמה פתוחה" else null
        }
        onDispose { stop() }
    }

    val hasFlash = remember { controller.hasFlash() }
    val on by animateFloatAsState(if (isOn) 1f else 0f, tween(260), label = "torchOn")
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focusRequester.requestFocus() } }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize().background(Color.Black)) {
            val u = maxWidth / DESIGN_W
            val v = maxHeight / DESIGN_H

            DesignLayer(Modifier) { drawStage(on) }
            if (on > 0f) {
                DesignLayer(Modifier.glow(u * 40, on)) {
                    inTorch { drawOval(Color(0xFFFFD60A), Offset(350f, -230f), Size(300f, 460f), alpha = 0.35f) }
                }
                DesignLayer(Modifier.glow(u * 6, on)) { inTorch { drawBeam() } }
                DesignLayer(Modifier.glow(u * 14, on)) { inTorch { drawBeamCore() } }
            }
            DesignLayer(Modifier) { inTorch { drawTorch(on) } }
            if (on > 0f) {
                DesignLayer(Modifier.glow(u * 14, on)) {
                    inTorch { drawOval(Color(0xFFFFFBEA), Offset(440f, -86f), Size(68f, 172f), alpha = 0.7f) }
                }
            }

            ScreenTopBar(title = "פנס", textColor = dark.textColor, accentColor = dark.accentColor, onBack = null)

            Column(
                modifier = Modifier.offset(y = v * 772).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(v * 20),
            ) {
                if (!hasFlash) {
                    Text("לא נמצא פנס במכשיר הזה", color = dark.textColor.copy(alpha = 0.6f), fontSize = FutureTypography.body, textAlign = TextAlign.Center)
                } else {
                    Text(
                        errorMessage ?: if (isOn) "דלוק" else "כבוי",
                        color = if (errorMessage != null) dark.dangerColor else dark.textColor.copy(alpha = 0.6f),
                        fontSize = FutureTypography.body,
                        textAlign = TextAlign.Center,
                    )
                    FutureButton(
                        text = if (isOn) "כבה פנס" else "הדלק פנס",
                        theme = dark,
                        // הדלקה דרך שירות קדמי - כך הפנס נשאר דלוק גם כשהמסך נכבה/ננעל.
                        onClick = { if (isOn) TorchService.turnOff(context) else TorchService.turnOn(context) },
                        focusRequester = focusRequester,
                        modifier = Modifier.width(u * 320),
                    )
                }
            }
        }
    }
}

@Composable
private fun DesignLayer(modifier: Modifier, draw: DrawScope.() -> Unit) {
    Canvas(modifier.fillMaxSize()) {
        withTransform({ scale(size.width / DESIGN_W, size.height / DESIGN_H, pivot = Offset.Zero) }) { draw() }
    }
}

private fun Modifier.glow(radius: Dp, alpha: Float) =
    graphicsLayer { this.alpha = alpha }.blur(radius, BlurredEdgeTreatment.Unbounded)

private inline fun DrawScope.inTorch(block: DrawScope.() -> Unit) {
    withTransform({
        translate(468f, 722f)
        rotate(-130f, pivot = Offset.Zero)
        scale(1f, -1f, pivot = Offset.Zero)
    }) { block() }
}

// --- צבעים (גווני האפור של מערכת העיצוב + הצהוב שלה לאור) ---
private fun alu(r: Float) = Brush.verticalGradient(
    0f to Color(0xFF2C2C2E), 0.05f to Color(0xFF8E8E93), 0.10f to Color(0xFFE5E5EA), 0.16f to Color(0xFF6E6E73),
    0.34f to Color(0xFF2C2C2E), 0.62f to Color(0xFF141416), 0.86f to Color(0xFF08080A), 0.94f to Color(0xFF3A3A3C),
    1f to Color(0xFF111113), startY = -r, endY = r
)
private fun steel(r: Float) = Brush.verticalGradient(
    0f to Color(0xFF636366), 0.07f to Color.White, 0.16f to Color(0xFFD1D1D6), 0.38f to Color(0xFF8E8E93),
    0.66f to Color(0xFF3A3A3C), 0.88f to Color(0xFF1C1C1E), 0.95f to Color(0xFF636366), 1f to Color(0xFF2C2C2E),
    startY = -r, endY = r
)
private fun rubber(r: Float) = Brush.verticalGradient(
    0f to Color(0xFF1C1C1E), 0.10f to Color(0xFF48484A), 0.30f to Color(0xFF1C1C1E), 0.80f to Color(0xFF050506),
    1f to Color(0xFF1C1C1E), startY = -r, endY = r
)

private fun DrawScope.drawStage(on: Float) {
    drawRect(
        Brush.radialGradient(0f to Color(0xFF1C1C1E), 0.6f to Color(0xFF0B0B0C), 1f to Color.Black, center = Offset(330f, 540f), radius = 440f),
        size = Size(DESIGN_W, DESIGN_H)
    )
    if (on > 0f) {
        drawRect(
            Brush.radialGradient(
                0f to Color(0xFFFFE9A0).copy(alpha = 0.34f), 0.35f to Color(0xFFFFD60A).copy(alpha = 0.12f),
                1f to Color(0xFFFFD60A).copy(alpha = 0f), center = Offset(150f, 330f), radius = 560f
            ),
            size = Size(DESIGN_W, DESIGN_H), alpha = on
        )
    }
}

private fun DrawScope.drawBeam() {
    drawPath(
        polygon(470f to -58f, 1250f to -440f, 1250f to 440f, 470f to 58f),
        Brush.horizontalGradient(
            0f to Color(0xFFFFF8E1).copy(alpha = 0.9f), 0.14f to Color(0xFFFFF1BF).copy(alpha = 0.55f),
            0.45f to Color(0xFFFFE58A).copy(alpha = 0.2f), 1f to Color(0xFFFFD60A).copy(alpha = 0f),
            startX = 470f, endX = 1250f
        )
    )
}

private fun DrawScope.drawBeamCore() {
    drawPath(
        polygon(470f to -30f, 1250f to -150f, 1250f to 150f, 470f to 30f),
        Brush.horizontalGradient(
            0f to Color.White.copy(alpha = 0.85f), 0.3f to Color.White.copy(alpha = 0.3f), 1f to Color.White.copy(alpha = 0f),
            startX = 470f, endX = 1250f
        )
    )
}

/** קטע גלילי מ-[x0] (רדיוס [r0]) עד [x1] (רדיוס [r1]); הקצה האחורי מעוגל כחצי אליפסה לפי הפרספקטיבה. */
private fun section(x0: Float, x1: Float, r0: Float, r1: Float = r0) = Path().apply {
    moveTo(x0, -r0)
    lineTo(x1, -r1)
    lineTo(x1, r1)
    lineTo(x0, r0)
    arcTo(Rect(x0 - r0 * 0.36f, -r0, x0 + r0 * 0.36f, r0), 90f, 180f, false)
    close()
}

private fun DrawScope.drawSection(path: Path, brush: Brush) {
    drawPath(path, brush)
    drawPath(path, Color.Black.copy(alpha = 0.5f), style = Stroke(1f))
}

/** חריץ היקפי: חצי האליפסה שפונה לזנב, קו כהה ולידו קו אור דק. */
private fun DrawScope.groove(x: Float, r: Float, width: Float) {
    val rx = r * 0.36f
    drawArc(Color.Black.copy(alpha = 0.8f), 90f, 180f, false, Offset(x - rx, -r), Size(rx * 2, r * 2), style = Stroke(width))
    drawArc(Color.White.copy(alpha = 0.13f), 90f, 180f, false, Offset(x + 3f - rx, -r), Size(rx * 2, r * 2), style = Stroke(1.5f))
}

private fun DrawScope.drawTorch(on: Float) {
    drawSection(section(0f, 36f, 38f), rubber(38f))
    drawSection(section(36f, 44f, 41f), steel(41f))
    drawSection(section(44f, 302f, 40f), alu(40f))

    // אחיזה מחורצת (knurling)
    clipRect(88f, -40f, 234f, 40f) {
        val knurl = Color.Black.copy(alpha = 0.6f)
        var c = 42f
        while (c <= 280f) {
            drawLine(knurl, Offset(c - 40f, -40f), Offset(c + 40f, 40f), 1.7f)
            drawLine(knurl, Offset(c + 40f, -40f), Offset(c - 40f, 40f), 1.7f)
            c += 7f
        }
    }
    groove(88f, 40f, 3f)
    groove(234f, 40f, 3f)

    // מתג צד
    drawOval(Color(0xFF1C1C1E), Offset(255f, -46f), Size(30f, 12f))
    drawOval(Color.White.copy(alpha = 0.25f), Offset(259f, -44.2f), Size(18f, 4.4f))

    drawSection(section(302f, 312f, 43f), steel(43f))
    drawSection(section(312f, 372f, 43f, 68f), alu(68f))
    drawSection(section(372f, 452f, 68f), alu(68f))
    for (x in listOf(388f, 404f, 420f, 436f)) groove(x, 68f, 4f)
    drawSection(section(452f, 470f, 72f), steel(72f))
    if (on > 0f) {
        drawPath(
            section(372f, 470f, 68f, 72f),
            Brush.horizontalGradient(0f to Color(0x00FFD60A), 1f to Color(0x59FFD60A), startX = 347f, endX = 470f),
            alpha = on
        )
    }

    // חזית: טבעת פלדה, זכוכית, רפלקטור / אור
    drawOval(
        Brush.linearGradient(
            0f to Color.White, 0.25f to Color(0xFFAEAEB2), 0.55f to Color(0xFF3A3A3C), 0.8f to Color(0xFF1C1C1E),
            1f to Color(0xFF8E8E93), start = Offset(444f, -72f), end = Offset(496f, 72f)
        ),
        Offset(444f, -72f), Size(52f, 144f)
    )
    drawOval(Color(0xFF050506), Offset(447f, -65f), Size(46f, 130f))
    lens(
        on = 1f - on,
        stops = arrayOf(
            0f to Color(0xFF1C1C1E), 0.12f to Color(0xFF48484A), 0.2f to Color(0xFFC7C7CC), 0.3f to Color(0xFF3A3A3C),
            0.44f to Color(0xFFAEAEB2), 0.56f to Color(0xFF2C2C2E), 0.7f to Color(0xFF8E8E93), 0.82f to Color(0xFF1C1C1E),
            0.93f to Color(0xFF636366), 1f to Color(0xFF0A0A0A)
        )
    )
    if (on < 1f) {
        drawOval(Color(0xFF1C1C1E), Offset(464f, -14f), Size(12f, 28f), alpha = 1f - on)
        drawOval(Color(0xFFD9C877), Offset(466.6f, -8f), Size(6.8f, 16f), alpha = 1f - on)
    }
    lens(
        on = on,
        stops = arrayOf(
            0f to Color.White, 0.4f to Color.White, 0.62f to Color(0xFFFFF8DC), 0.85f to Color(0xFFFFE580), 1f to Color(0xFFFFC800)
        )
    )
    // השתקפות על הזכוכית
    drawOval(Color.White.copy(alpha = 0.2f), Offset(459f, -50f), Size(14f, 40f))
}

/** העדשה: גרדיאנט עגול שנמתח לאליפסה (21.5x61). */
private fun DrawScope.lens(on: Float, stops: Array<Pair<Float, Color>>) {
    if (on <= 0f) return
    withTransform({ scale(21.5f / 61f, 1f, pivot = Offset(470f, 0f)) }) {
        drawCircle(Brush.radialGradient(*stops, center = Offset(470f, 0f), radius = 61f), 61f, Offset(470f, 0f), alpha = on)
    }
}

private fun polygon(vararg points: Pair<Float, Float>) = Path().apply {
    points.forEachIndexed { i, (x, y) -> if (i == 0) moveTo(x, y) else lineTo(x, y) }
    close()
}

