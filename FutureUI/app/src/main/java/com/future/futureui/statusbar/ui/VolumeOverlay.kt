package com.future.futureui.statusbar.ui

import com.future.sharednav.theme.FutureMotion
import com.future.sharednav.theme.FutureShapes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * חלון עצמאי, זמני, שמופיע כשלוחצים על מקשי הווליום ונעלם אחרי כמה שניות -
 * מחליף את חלונית הווליום המקורית של אנדרואיד (שמוסתרת ע"י צריכת האירוע בשירות).
 * העיצוב תואם בכוונה למראה "זכוכית כהה בהירה" של SliderBar במרכז הבקרה,
 * כדי שההדגשות הזמניות ירגישו כמו חלק מאותה שפת עיצוב - לא רכיב זר.
 *
 * תנועה: נופל משורת המצב בקפיץ ומתנפח מעט, נאסף חזרה למעלה ביציאה
 * ([visibleState] - השירות מסיר את החלון אחרי שהיציאה נגמרת). המילוי עובר
 * לערך החדש בקפיץ toggle עם overshoot קטן; בהחזקת מקש הקפיץ ממשיך מהמהירות
 * הנוכחית, כך שהמילוי זורם במקום לקפוץ.
 */
@Composable
fun VolumeOverlay(
    level: Float,
    modifier: Modifier = Modifier,
    visibleState: MutableTransitionState<Boolean>? = null,
) {
    val shape = FutureShapes.xxl
    val state = visibleState ?: remember { MutableTransitionState(false).apply { targetState = true } }
    val fill by animateFloatAsState(
        level.coerceIn(0f, 1f),
        FutureMotion.Springs.toggle(),
        label = "volumeFill",
    )

    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.TopCenter
    ) {
        AnimatedVisibility(
            visibleState = state,
            enter = slideInVertically(FutureMotion.Springs.dialogOffset) { -it } +
                scaleIn(FutureMotion.Springs.dialog(), initialScale = 0.8f) +
                fadeIn(FutureMotion.enter()),
            exit = slideOutVertically(FutureMotion.exit()) { -it / 2 } + fadeOut(FutureMotion.exit()),
        ) {
            Box(
                modifier = Modifier
                    .padding(top = 44.dp)
                    .width(200.dp)
                    .height(47.dp)
                    .clip(shape)
                    .background(Color(0xE6E0E0E0))
                    .border(width = 0.5.dp, color = Color.White.copy(alpha = 0.6f), shape = shape),
                contentAlignment = Alignment.CenterStart
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(fill.coerceIn(0.01f, 1f))
                        .clip(shape)
                        .background(Color(0xFFBDBDBD))
                )
                Icon(
                    imageVector = if (level <= 0f) Icons.AutoMirrored.Rounded.VolumeOff else Icons.AutoMirrored.Rounded.VolumeUp,
                    contentDescription = null,
                    tint = Color(0xFF616161),
                    modifier = Modifier.padding(start = 14.dp).size(22.dp)
                )
            }
        }
    }
}
