package com.future.systemui.volume

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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.future.sharednav.theme.FutureMotion
import com.future.sharednav.theme.FutureShapes

/**
 * תוכן חלון הווליום: שם הערוץ ופס עוצמה. הצבעים, הפינות והתנועה נלקחים
 * מערכת העיצוב המשותפת (ר' FutureAppTheme) ולא מוקלדים כאן.
 * הפס והחלון מאותה שפה של [com.future.futureui] (זכוכית בהירה, קפיץ
 * בכניסה, נאסף למעלה ביציאה).
 *
 * [visibleState] שולט בכניסה וביציאה; השירות מסיר את החלון אחרי היציאה.
 */
@Composable
fun VolumeOverlayContent(
    streamLabel: String,
    level: Float,
    visibleState: MutableTransitionState<Boolean>,
) {
    val shape = FutureShapes.xxl
    val fill by animateFloatAsState(
        targetValue = level.coerceIn(0f, 1f),
        animationSpec = FutureMotion.Springs.toggle(),
        label = "volumeFill",
    )
    val colors = MaterialTheme.colorScheme

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.TopCenter
    ) {
        AnimatedVisibility(
            visibleState = visibleState,
            enter = slideInVertically(FutureMotion.Springs.dialogOffset) { -it } +
                scaleIn(FutureMotion.Springs.dialog(), initialScale = 0.8f) +
                fadeIn(FutureMotion.enter()),
            exit = slideOutVertically(FutureMotion.exit()) { -it / 2 } + fadeOut(FutureMotion.exit()),
        ) {
            Column(
                modifier = Modifier
                    .padding(top = 44.dp)
                    .width(200.dp)
                    .clip(shape)
                    .background(colors.surfaceContainerHigh)
                    .border(width = 0.5.dp, color = colors.outline, shape = shape)
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = streamLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.onSurface,
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(FutureShapes.pill)
                        .background(colors.surfaceVariant)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(fill.coerceIn(0.01f, 1f))
                            .clip(FutureShapes.pill)
                            .background(colors.primary)
                    )
                }
            }
        }
    }
}
