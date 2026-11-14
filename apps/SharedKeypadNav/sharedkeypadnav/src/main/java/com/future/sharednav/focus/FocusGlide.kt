package com.future.sharednav.focus

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.SpringSpec
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onPlaced
import com.future.sharednav.theme.FutureMotion
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Elastic Focus - סמן פוקוס אחד שעובר בין הפריטים, במקום ששני פריטים
 * יעשו fade בכל לחיצת חץ.
 *
 * עד עכשיו כל FocusableItem צייר לעצמו מילוי ומסגרת: הפריט שאיבד פוקוס
 * דהה החוצה והפריט החדש דהה פנימה, באותו מקום של כל אחד. לא היה שום קשר
 * במרחב בין המקום שבו הפוקוס היה לבין המקום שאליו הוא עבר, וזה המשוב שהמשתמש
 * רואה הכי הרבה פעמים ביום במכשיר בלי מגע.
 *
 * כאן יש מארח אחד למסך ([focusGlideHost]) שמצייר סמן אחד, והפריט הממוקד רק
 * מדווח איפה הוא ([report]). כל אחד מארבעת הקצוות של הסמן הוא קפיץ נפרד:
 * הקצה שבכיוון התנועה רץ בקפיץ קשיח ([FutureMotion.Springs.glideLead]), הקצה
 * שמאחור מגיע בקפיץ רך ([FutureMotion.Springs.glideTrail]), ולכן הסמן נמתח
 * לכיוון שאליו הוא הולך ומתכווץ כשהוא נוחת. בהחזקת חץ היעד מתחלף לפני
 * שהקפיצים נחו, והם ממשיכים מהמהירות הנוכחית - הסמן זורם.
 *
 * העלות: מלבן מעוגל אחד וקו אחד שמצוירים במארח, במקום שתי אנימציות צבע
 * (רקע + מסגרת) בשני פריטים. הכול נקרא בשלב הציור, בלי recomposition.
 *
 * שכבות: המילוי מצויר מאחורי התוכן והמסגרת מעליו. כך המילוי לא מכהה טקסט,
 * ובתוך כרטיס אטום (שמסתיר את המילוי) המסגרת עדיין נראית - הפוקוס אף פעם
 * לא נעלם.
 */
@Stable
class FocusGlideState internal constructor(private val scope: CoroutineScope) {

    internal var host: LayoutCoordinates? = null

    private val left = Animatable(0f)
    private val top = Animatable(0f)
    private val right = Animatable(0f)
    private val bottom = Animatable(0f)
    private val alpha = Animatable(0f)

    private var activeId: Any? = null
    private var fadeJob: Job? = null
    private val edgeSpecs = arrayOfNulls<SpringSpec<Float>>(4)

    private var fill by mutableStateOf(Color.Transparent)
    private var ring by mutableStateOf(Color.Transparent)
    private var radius by mutableFloatStateOf(0f)
    private var ringWidth by mutableFloatStateOf(0f)

    /**
     * הפריט [id] ממוקד ונמצא ב-[coords]. נקרא בכל פעם שהוא זז (גלילה, מעבר
     * מסך), לא רק כשהפוקוס עובר אליו.
     */
    fun report(id: Any, coords: LayoutCoordinates, style: FocusGlideStyle) {
        val hostCoords = host ?: return
        if (!hostCoords.isAttached || !coords.isAttached) return
        val rect = hostCoords.localBoundingBoxOf(coords, clipBounds = true)
        if (rect.width <= 0f || rect.height <= 0f) return

        fill = style.fill
        ring = style.ring
        radius = style.cornerRadiusPx
        ringWidth = style.ringWidthPx
        fadeJob?.cancel()
        fadeJob = null

        val targets = floatArrayOf(rect.left, rect.top, rect.right, rect.bottom)
        val edges = arrayOf(left, top, right, bottom)
        val visible = alpha.value > 0.01f || alpha.targetValue > 0f

        when {
            // הופעה ראשונה במסך (או אחרי שהפוקוס יצא מהמארח) - אין מאיפה להחליק
            !visible -> {
                scope.launch { edges.forEachIndexed { i, e -> e.snapTo(targets[i]) } }
                scope.launch { alpha.animateTo(1f, FutureMotion.instant()) }
            }
            // אותו פריט זז (גלילה, מעבר מסך): אם הסמן באמצע מעבר - ממשיכים לרדוף
            // אחרי היעד באותם קפיצים; אם הוא נח - הוא זז יחד עם הפריט, בלי לפגר.
            id == activeId -> {
                val running = edges.any { it.isRunning }
                edges.forEachIndexed { i, e ->
                    val spec = edgeSpecs[i]
                    scope.launch { if (running && spec != null) e.animateTo(targets[i], spec) else e.snapTo(targets[i]) }
                }
            }
            // פוקוס חדש: הקצה שבכיוון התנועה מוביל, השני נגרר אחריו
            else -> {
                val dx = (targets[0] + targets[2]) / 2f - (left.value + right.value) / 2f
                val dy = (targets[1] + targets[3]) / 2f - (top.value + bottom.value) / 2f
                edgeSpecs[0] = if (dx < 0f) FutureMotion.Springs.glideLead else FutureMotion.Springs.glideTrail
                edgeSpecs[2] = if (dx > 0f) FutureMotion.Springs.glideLead else FutureMotion.Springs.glideTrail
                edgeSpecs[1] = if (dy < 0f) FutureMotion.Springs.glideLead else FutureMotion.Springs.glideTrail
                edgeSpecs[3] = if (dy > 0f) FutureMotion.Springs.glideLead else FutureMotion.Springs.glideTrail
                if (dx == 0f) { edgeSpecs[0] = FutureMotion.Springs.glideLead; edgeSpecs[2] = FutureMotion.Springs.glideLead }
                if (dy == 0f) { edgeSpecs[1] = FutureMotion.Springs.glideLead; edgeSpecs[3] = FutureMotion.Springs.glideLead }
                edges.forEachIndexed { i, e -> scope.launch { e.animateTo(targets[i], edgeSpecs[i]!!) } }
                if (alpha.targetValue < 1f) scope.launch { alpha.animateTo(1f, FutureMotion.instant()) }
            }
        }
        activeId = id
    }

    /**
     * הפריט [id] איבד פוקוס. אם תוך רגע אף פריט אחר במארח לא מדווח (הפוקוס
     * יצא לשורת הכותרת, לדיאלוג), הסמן דוהה. אם כן - הוא פשוט מחליק אליו.
     */
    fun release(id: Any) {
        if (activeId != id) return
        activeId = null
        fadeJob?.cancel()
        fadeJob = scope.launch {
            delay(48)
            alpha.animateTo(0f, FutureMotion.fast())
        }
    }

    internal fun drawFill(scope: DrawScope) {
        val a = alpha.value
        if (a <= 0f || fill.alpha <= 0f) return
        scope.drawRoundRect(
            color = fill,
            topLeft = Offset(left.value, top.value),
            size = Size((right.value - left.value).coerceAtLeast(0f), (bottom.value - top.value).coerceAtLeast(0f)),
            cornerRadius = CornerRadius(radius, radius),
            alpha = a,
        )
    }

    internal fun drawRing(scope: DrawScope) {
        val a = alpha.value
        if (a <= 0f || ring.alpha <= 0f || ringWidth <= 0f) return
        // הקו על חצי עובי פנימה, כמו animatedFocusSurface
        val inset = ringWidth / 2f
        val r = (radius - inset).coerceAtLeast(0f)
        scope.drawRoundRect(
            color = ring,
            topLeft = Offset(left.value + inset, top.value + inset),
            size = Size((right.value - left.value - ringWidth).coerceAtLeast(0f), (bottom.value - top.value - ringWidth).coerceAtLeast(0f)),
            cornerRadius = CornerRadius(r, r),
            style = Stroke(ringWidth),
            alpha = a,
        )
    }
}

/** איך הסמן נראה כשהוא על פריט מסוים - הצבעים והצורה של הפריט עצמו. */
class FocusGlideStyle(
    val fill: Color,
    val ring: Color,
    val cornerRadiusPx: Float,
    val ringWidthPx: Float,
)

/** המארח הקרוב (מסך, דיאלוג). null - אין מארח, והפריט מצייר את הפוקוס בעצמו כמו קודם. */
val LocalFocusGlide = staticCompositionLocalOf<FocusGlideState?> { null }

@Composable
fun rememberFocusGlideState(): FocusGlideState {
    val scope = rememberCoroutineScope()
    return remember { FocusGlideState(scope) }
}

/**
 * מצייר את הסמן של [state] בתוך האלמנט הזה. צריך לספק גם את [state] דרך
 * [LocalFocusGlide] לתוכן (ר' ScreenScaffold, AnimatedScreenHost, AppDialog).
 */
fun Modifier.focusGlideHost(state: FocusGlideState): Modifier = this
    .onPlaced { state.host = it }
    .drawWithContent {
        state.drawFill(this)
        drawContent()
        state.drawRing(this)
    }
