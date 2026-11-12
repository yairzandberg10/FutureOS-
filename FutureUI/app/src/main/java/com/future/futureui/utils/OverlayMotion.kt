package com.future.futureui.utils

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.future.sharednav.theme.FutureMotion
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first

/**
 * המצב של שכבת מערכת (מרכז בקרה, מרכז התראות) בין "מוצגת" ל"נסגרת".
 *
 * עד עכשיו השירותים קראו ל-windowManager.removeView ברגע הסגירה, ולכן
 * אנימציית היציאה שהוגדרה ב-AnimatedVisibility אף פעם לא רצה - החלון נעלם
 * בפריים אחד. כאן הסגירה רק מחליפה את היעד ל-false; השירות מסיר את החלון
 * כשהמסך מדווח שהיציאה נגמרה (ר' [OverlayExitWatcher]). פתיחה מחדש באמצע
 * היציאה פשוט מחזירה את היעד ל-true, והאנימציה מתהפכת מהנקודה שבה היא נמצאת.
 *
 * [direction] קובע מאיפה השכבה נכנסת ולאן היא יוצאת: 0 - מהקצה העליון
 * (פתיחה רגילה), -1/1 - מהצד, כשעוברים בין מרכז הבקרה למרכז ההתראות. שתי
 * השכבות באותו עומק, ולכן המעבר ביניהן אופקי.
 */
class OverlayMotion {
    val visible = MutableTransitionState(false)
    var direction by mutableIntStateOf(0)
    var closing = false
        private set

    fun enter(fromDirection: Int = 0) {
        direction = fromDirection
        closing = false
        visible.targetState = true
    }

    fun exit(toDirection: Int = 0) {
        direction = toDirection
        closing = true
        visible.targetState = false
    }

    /** כניסה: נפילה מהקצה העליון בקפיץ layer, או החלקה מהצד. */
    fun enterTransition(): EnterTransition =
        if (direction == 0) {
            slideInVertically(FutureMotion.Springs.layerOffset) { -(it * DropFraction).toInt() } +
                fadeIn(FutureMotion.enter())
        } else {
            val dir = direction
            slideInHorizontally(FutureMotion.Springs.layerOffset) { (dir * it * SideFraction).toInt() } +
                fadeIn(FutureMotion.enter())
        }

    /** יציאה: נאספת חזרה למעלה, או ממשיכה הצידה. מהירה מהכניסה - מה שעוזב לא צריך תשומת לב. */
    fun exitTransition(): ExitTransition =
        if (direction == 0) {
            slideOutVertically(FutureMotion.exit()) { -(it * DropFraction).toInt() } + fadeOut(FutureMotion.exit())
        } else {
            val dir = direction
            slideOutHorizontally(FutureMotion.exit()) { (dir * it * SideFraction).toInt() } + fadeOut(FutureMotion.exit())
        }

    private companion object {
        /** כמה מהגובה השכבה "נופלת". מסך שלם היה ארוך מדי; שליש מספיק כדי לראות שהיא ירדה מלמעלה. */
        const val DropFraction = 0.35f
        const val SideFraction = 0.35f
    }
}

/** מדווח פעם אחת כשהיציאה של [motion] נגמרה, כדי שהשירות יסיר את החלון. */
@Composable
fun OverlayExitWatcher(motion: OverlayMotion, onExited: () -> Unit) {
    val callback by rememberUpdatedState(onExited)
    LaunchedEffect(motion) {
        while (true) {
            snapshotFlow { motion.visible.isIdle && !motion.visible.currentState && !motion.visible.targetState }
                .filter { it }
                .first()
            callback()
            // ממתינים לפתיחה הבאה (אותו אובייקט יכול לשמש שוב אם החלון לא הוסר)
            snapshotFlow { motion.visible.targetState }.filter { it }.first()
        }
    }
}

/**
 * כניסה בגל: הפריט "צונח" ומתנפח מ-0.6 בקפיץ lift, באיחור קטן לפי
 * [index]. בשביל קבוצות במסך מערכת (שורת הכותרת, שורת המתגים, הרשת) - לא
 * בשביל כל פריט ברשימה ארוכה. רץ פעם אחת לכל הרכבה של המסך, כלומר בכל
 * פתיחה של השכבה. graphicsLayer בלבד, בלי layout.
 */
fun Modifier.cascadeIn(index: Int, baseDelayMillis: Int = 50, stepMillis: Int = 36): Modifier = composed {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay((baseDelayMillis + index * stepMillis).toLong())
        progress.animateTo(1f, FutureMotion.Springs.lift())
    }
    graphicsLayer {
        val p = progress.value
        alpha = p.coerceIn(0f, 1f)
        val s = 0.6f + 0.4f * p
        scaleX = s
        scaleY = s
        translationY = (1f - p) * -16.dp.toPx()
    }
}
