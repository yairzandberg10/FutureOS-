package com.future.sharednav.focus

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import com.future.sharednav.theme.FutureDimens
import com.future.sharednav.theme.FutureMotion

/**
 * אבני הבניין של מעבר פוקוס לרכיב שנבנה ידנית (לא FocusableItem/FutureButton).
 * בסריקה נמצאו עשרות מקומות שכתבו `.background(if (isFocused) A else B)` -
 * הסימון קפץ בבת אחת בין פריטים, בעוד שהרכיבים המשותפים מנפישים אותו. כאן
 * אותה תנועה בדיוק כמו ב-FocusableItem, כדי שכל מעבר פוקוס במערכת ירגיש אחד:
 * צבע ב-[FutureMotion.focusColorSpec], גודל בקפיץ press בכניסה לפוקוס
 * ו-[FutureMotion.focusScaleSpec] ביציאה (ר' [focusMotion]).
 */
@Composable
fun animateFocusColor(focused: Boolean, focusedColor: Color, idleColor: Color): State<Color> =
    animateColorAsState(if (focused) focusedColor else idleColor, FutureMotion.focusColorSpec, label = "focusColor")

/** עובי מסגרת שמשתנה עם הפוקוס (למשל 1dp במנוחה, 2dp בפוקוס) - בלי קפיצה. */
@Composable
fun animateFocusDp(focused: Boolean, focusedValue: Dp, idleValue: Dp): State<Dp> =
    animateDpAsState(if (focused) focusedValue else idleValue, FutureMotion.instant(), label = "focusDp")

/** שקיפות שמשתנה עם הפוקוס. */
@Composable
fun animateFocusFloat(focused: Boolean, focusedValue: Float, idleValue: Float): State<Float> =
    animateFloatAsState(if (focused) focusedValue else idleValue, FutureMotion.instant(), label = "focusFloat")

/**
 * גדילה בפוקוס לרכיב שמחזיק isFocused משלו (onFocusChanged) ואין לו
 * interactionSource - המקבילה של [focusMotion]. רץ ב-graphicsLayer, כך
 * שהאנימציה לא מריצה recomposition ולא משנה את ה-layout של השכנים.
 */
@Composable
fun Modifier.focusScale(focused: Boolean, focusedScale: Float = FutureDimens.focusScale): Modifier {
    val scale by animateFloatAsState(
        if (focused) focusedScale else 1f,
        if (focused) FutureMotion.Springs.press() else FutureMotion.focusScaleSpec,
        label = "focusScale",
    )
    return graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}
