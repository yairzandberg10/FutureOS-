package com.future.flashlight.ui

import android.app.Activity
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.future.flashlight.data.FlashlightController
import com.future.flashlight.data.TorchService
import com.future.sharednav.icons.FutureIcons
import com.future.sharednav.nav.onOptionsKeyPress
import com.future.sharednav.theme.FutureTheme

// העיצוב הקלאסי (קנבס "פנס תלת מימד", artboard Classic): רקע כחול, ראש פנס
// מתכתי במבט חזיתי, אלומה לבנה כשדולק, ופאנל לבן עם כפתור הפעלה עגול.
// כל הקואורדינטות כאן בפיקסלים של מסך 640x960 ומוקטנות לגודל המסך בפועל.
private const val DESIGN_W = 640f
private const val DESIGN_H = 960f

@Composable
fun FlashlightScreen(@Suppress("UNUSED_PARAMETER") theme: FutureTheme) {
    val context = LocalContext.current
    val controller = remember { FlashlightController(context) }
    var isOn by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

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
    // הדלקה דרך שירות קדמי - כך הפנס נשאר דלוק גם כשהמסך נכבה/ננעל.
    val toggle = { if (isOn) TorchService.turnOff(context) else TorchService.turnOn(context) }

    // מקש Options (התווית "סגירה"): מכבה את הפנס ויוצא. Back ("אחורה") יוצא
    // ומשאיר את הפנס במצבו.
    onOptionsKeyPress {
        if (isOn) TorchService.turnOff(context)
        (context as? Activity)?.finish()
    }

    val on by animateFloatAsState(if (isOn) 1f else 0f, tween(260), label = "torchOn")
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focusRequester.requestFocus() } }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(0f to Color(0xFF1C2466), 0.45f to Color(0xFF131B52), 1f to Color(0xFF0A0F33)))
        ) {
            // יחידת עיצוב אחת (פיקסל ב-640x960) ב-dp של המסך בפועל
            val u = maxWidth / DESIGN_W
            val v = maxHeight / DESIGN_H

            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0f to Color(0xFFD5DCF5), 0.22f to Color(0xFFA3B1E6), 0.5f to Color(0xFF6376CC),
                            0.75f to Color(0xFF2D3FA0), 1f to Color(0xFF17217A)
                        ),
                        alpha = on
                    )
            )

            Canvas(Modifier.fillMaxSize()) {
                withTransform({ scale(size.width / DESIGN_W, size.height / DESIGN_H, pivot = Offset.Zero) }) {
                    drawTorch(on)
                }
            }

            // הפאנל הלבן בתחתית
            val panelShape = RoundedCornerShape(topStart = u * 44, topEnd = u * 44)
            Box(
                Modifier
                    .offset(y = v * 720)
                    .fillMaxWidth()
                    .height(v * 240)
                    .shadow(14.dp, panelShape)
                    .clip(panelShape)
                    .background(Brush.verticalGradient(0f to Color.White, 0.55f to Color(0xFFF2F4FA), 1f to Color(0xFFDDE2EE)))
            )

            // שקע הכפתור
            Box(
                Modifier
                    .offset(x = u * 252, y = v * 740)
                    .size(u * 136)
                    .clip(CircleShape)
                    .background(Brush.verticalGradient(listOf(Color(0xFFC7CBD6), Color.White)))
            )

            if (hasFlash) {
                PowerButton(
                    isOn = isOn,
                    focusRequester = focusRequester,
                    onToggle = toggle,
                    modifier = Modifier.offset(x = u * 262, y = v * 750),
                    diameter = u * 116,
                    ringGap = u * 10,
                    ringWidth = u * 4,
                )
            } else {
                Text(
                    "לא נמצא פנס במכשיר הזה",
                    color = Color(0xFF3A3A3C),
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.offset(y = v * 790).fillMaxWidth()
                )
            }

            errorMessage?.let {
                Text(
                    it,
                    color = Color(0xFFD32F2F),
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.offset(y = v * 878).fillMaxWidth()
                )
            }

            // תוויות המקשים הרכים: ב-RTL הראשונה מימין (Options), השנייה משמאל (Back)
            Row(
                modifier = Modifier
                    .offset(y = v * 896)
                    .fillMaxWidth()
                    .height(v * 44)
                    .padding(horizontal = u * 32),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("סגירה", color = Color(0xFF3A3A3C), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text("אחורה", color = Color(0xFF3A3A3C), fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun PowerButton(
    isOn: Boolean,
    focusRequester: FocusRequester,
    onToggle: () -> Unit,
    modifier: Modifier,
    diameter: Dp,
    ringGap: Dp,
    ringWidth: Dp,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val ring = if (isOn) Color(0xFFE53935) else Color(0xFF6E6E73)
    val fill = if (isOn) {
        listOf(Color(0xFFFF8A80), Color(0xFFF2453D), Color(0xFFB71C1C))
    } else {
        listOf(Color(0xFFAEAEB2), Color(0xFF6E6E73), Color(0xFF3A3A3C))
    }

    Box(
        modifier = modifier
            .size(diameter)
            .drawBehind {
                if (isFocused) {
                    val w = ringWidth.toPx()
                    drawCircle(ring, radius = size.minDimension / 2 + ringGap.toPx() + w / 2, style = Stroke(w))
                }
            }
            .shadow(6.dp, CircleShape)
            .clip(CircleShape)
            .drawBehind {
                drawRect(
                    Brush.radialGradient(
                        0f to fill[0], 0.45f to fill[1], 1f to fill[2],
                        center = Offset(size.width / 2, size.height * 0.3f),
                        radius = size.maxDimension * 0.85f
                    )
                )
                // בליטה: הבהרה למעלה והצללה למטה
                drawRect(Brush.verticalGradient(0f to Color.White.copy(alpha = 0.45f), 0.3f to Color.Transparent))
                drawRect(Brush.verticalGradient(0.7f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.28f)))
            }
            .focusRequester(focusRequester)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onToggle)
            .focusable(interactionSource = interactionSource),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            FutureIcons.PowerSettingsNew,
            contentDescription = if (isOn) "כבה פנס" else "הדלק פנס",
            tint = Color.White,
            modifier = Modifier.size(diameter / 2)
        )
    }
}

/** ציור ראש הפנס והאלומה במרחב 640x960. [on] בין 0 ל-1 (מונפש). */
private fun DrawScope.drawTorch(on: Float) {
    if (on > 0f) {
        // האלומה החיצונית - שכבות מורחבות בשקיפות יורדת במקום טשטוש (blur לא קיים לפני API 31)
        val outer = Brush.verticalGradient(
            0f to Color.White.copy(alpha = 0.98f), 0.35f to Color.White.copy(alpha = 0.8f),
            0.75f to Color.White.copy(alpha = 0.35f), 1f to Color.White.copy(alpha = 0.1f),
            startY = 410f, endY = -20f
        )
        for ((grow, a) in listOf(14f to 0.18f, 7f to 0.3f, 0f to 1f)) {
            drawPath(trapezoid(30f - grow, 610f + grow, 169f - grow * 0.6f, 471f + grow * 0.6f, -20f, 410f), outer, alpha = a * on)
        }
        val core = Brush.verticalGradient(
            0f to Color.White, 0.6f to Color.White.copy(alpha = 0.6f), 1f to Color.Transparent,
            startY = 410f, endY = 150f
        )
        for ((grow, a) in listOf(24f to 0.15f, 12f to 0.3f, 0f to 0.7f)) {
            drawPath(trapezoid(200f - grow, 440f + grow, 230f - grow * 0.6f, 410f + grow * 0.6f, 150f, 410f), core, alpha = a * on)
        }
        // הילה מעל קצה הראש
        withTransform({ scale(1f, 0.25f, pivot = Offset(320f, 405f)) }) {
            drawCircle(
                Brush.radialGradient(
                    0f to Color.White, 0.55f to Color.White.copy(alpha = 0.8f), 1f to Color.Transparent,
                    center = Offset(320f, 405f), radius = 180f
                ),
                radius = 180f, center = Offset(320f, 405f), alpha = on
            )
        }
    }

    // גוף וצוואר
    drawRect(
        Brush.horizontalGradient(
            0f to Color(0xFF070B26), 0.22f to Color(0xFF1B2562), 0.38f to Color(0xFF4A5AA8),
            0.6f to Color(0xFF1B2562), 1f to Color(0xFF070B26), startX = 240f, endX = 400f
        ),
        topLeft = Offset(240f, 690f), size = Size(160f, 120f)
    )
    drawPath(
        trapezoid(196f, 444f, 240.6f, 399.4f, 628f, 700f),
        Brush.horizontalGradient(
            0f to Color(0xFF070B26), 0.18f to Color(0xFF1F2A6E), 0.34f to Color(0xFF5363B4), 0.42f to Color(0xFF9AA6DC),
            0.56f to Color(0xFF3A4A9E), 0.8f to Color(0xFF141D55), 1f to Color(0xFF070B26), startX = 196f, endX = 444f
        )
    )

    // טבעת כהה מתחת לראש
    val band = Path().apply {
        addRoundRect(
            RoundRect(
                left = 180f, top = 596f, right = 460f, bottom = 640f,
                topLeftCornerRadius = CornerRadius.Zero, topRightCornerRadius = CornerRadius.Zero,
                bottomRightCornerRadius = CornerRadius(30f), bottomLeftCornerRadius = CornerRadius(30f)
            )
        )
    }
    drawPath(
        band,
        Brush.horizontalGradient(
            0f to Color(0xFF04071C), 0.16f to Color(0xFF111A4E), 0.34f to Color(0xFF2E3C88), 0.42f to Color(0xFF5A69B4),
            0.58f to Color(0xFF222F78), 0.82f to Color(0xFF0B1240), 1f to Color(0xFF04071C), startX = 180f, endX = 460f
        )
    )
    clipPath(band) {
        drawRect(Brush.verticalGradient(0f to Color.Black.copy(alpha = 0.55f), 1f to Color.Transparent, startY = 596f, endY = 612f),
            topLeft = Offset(180f, 596f), size = Size(280f, 16f))
    }

    // צל רך מתחת לראש
    for ((grow, a) in listOf(24f to 0.08f, 14f to 0.12f, 6f to 0.16f)) {
        drawRoundRect(Color.Black, Offset(168f - grow, 420f - grow), Size(304f + grow * 2, 200f + grow * 2), CornerRadius(40f + grow), alpha = a)
    }

    // ראש הפנס
    val head = Path().apply { addRoundRect(RoundRect(168f, 404f, 472f, 604f, CornerRadius(40f))) }
    drawPath(
        head,
        Brush.horizontalGradient(
            0f to Color(0xFF111B5A), 0.08f to Color(0xFF26359A), 0.2f to Color(0xFF4F62C4), 0.34f to Color(0xFF9DAAE4),
            0.43f to Color(0xFFE3E8FA), 0.52f to Color(0xFFA7B3E8), 0.68f to Color(0xFF5064C0), 0.86f to Color(0xFF2A3890),
            1f to Color(0xFF111B5A), startX = 168f, endX = 472f
        )
    )
    clipPath(head) {
        drawRect(Color.White.copy(alpha = 0.35f), Offset(168f, 404f), Size(304f, 4f))
        drawRect(Brush.verticalGradient(0f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.45f), startY = 574f, endY = 604f),
            Offset(168f, 574f), Size(304f, 30f))
        // כבוי: הראש מעומעם. דלוק: האור מאיר את חלקו העליון
        drawRect(Color(0xFF04071C), Offset(168f, 404f), Size(304f, 200f), alpha = 0.38f * (1f - on))
        drawRect(
            Brush.verticalGradient(
                0f to Color.White.copy(alpha = 0.55f), 0.3f to Color.White.copy(alpha = 0.12f), 0.55f to Color.Transparent,
                startY = 404f, endY = 604f
            ),
            Offset(168f, 404f), Size(304f, 200f), alpha = on
        )
    }

    // קצה הראש הזוהר
    if (on > 0f) {
        for ((grow, a) in listOf(18f to 0.15f, 10f to 0.3f, 4f to 0.5f)) {
            drawRoundRect(Color.White, Offset(196f - grow, 404f - grow), Size(248f + grow * 2, 6f + grow * 2), CornerRadius(3f + grow), alpha = a * on)
        }
        drawRoundRect(Color.White, Offset(196f, 404f), Size(248f, 6f), CornerRadius(3f), alpha = on)
    }
}

/** טרפז: קצה עליון [topL]..[topR] בגובה [top], קצה תחתון [botL]..[botR] בגובה [bottom]. */
private fun trapezoid(topL: Float, topR: Float, botL: Float, botR: Float, top: Float, bottom: Float) = Path().apply {
    moveTo(topL, top)
    lineTo(topR, top)
    lineTo(botR, bottom)
    lineTo(botL, bottom)
    close()
}
