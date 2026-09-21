package com.future.sharednav.focus

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.future.sharednav.theme.FutureMotion
import kotlinx.coroutines.delay

/**
 * התנועה של פריט ממוקד: גדל מעט כשהפוקוס עליו, ומתכווץ לרגע כשלוחצים
 * OK. זה המשוב היחיד שיש למשתמש על לחיצת מקש במכשיר בלי מגע, והוא היה
 * קיים עד עכשיו רק בחלק מהרכיבים (FocusableItem עם scale, רוב הכפתורים
 * בלי כלום, ואף רכיב לא הגיב ללחיצה עצמה).
 *
 * הלחיצה מגיעה מ-clickable, שמתרגם DPAD_CENTER/ENTER ל-PressInteraction
 * בדיוק כמו נגיעה - לכן זה עובד עם אותו interactionSource שכבר מועבר
 * ל-clickable, בלי טיפול נפרד במקשים.
 *
 * ה-scale נקרא בתוך graphicsLayer ולא ב-Modifier.scale, כדי שהאנימציה
 * תרוץ בשלב הציור בלבד ולא תגרום ל-recomposition בכל פריים.
 */
@Composable
fun Modifier.focusMotion(
    interactionSource: InteractionSource,
    focusedScale: Float = 1.02f,
    pressedScale: Float = 0.97f,
    enabled: Boolean = true,
): Modifier {
    val isFocused by interactionSource.collectIsFocusedAsState()
    val isPressed by interactionSource.collectIsPressedAsState()
    val target = when {
        !enabled -> 1f
        isPressed -> pressedScale
        isFocused -> focusedScale
        else -> 1f
    }
    val scale = animateFloatAsState(target, FutureMotion.focusScaleSpec, label = "focusMotion")
    return graphicsLayer {
        scaleX = scale.value
        scaleY = scale.value
    }
}

/**
 * כניסה מדורגת של פריטים כשמסך נפתח: כל פריט עולה ומופיע מעט אחרי זה
 * שלפניו. רק [FutureMotion.StaggerMaxItems] הפריטים הראשונים - רשימה של
 * 200 שירים לא אמורה "להיבנות" במשך חמש שניות, ומה שמתחת לקצה המסך ממילא
 * לא נראה.
 *
 * המצב "כבר נכנס" נשמר ב-rememberSaveable: LazyColumn שומר אותו לפי המפתח
 * של הפריט, ולכן פריט שגלל החוצה וחזר לא מונפש שוב - אחרת גלילה מהירה
 * עם חץ מוחזק הייתה מהבהבת.
 */
@Composable
fun Modifier.staggeredEntrance(index: Int): Modifier {
    if (index >= FutureMotion.StaggerMaxItems) return this
    var hasEntered by rememberSaveable { mutableStateOf(false) }
    val progress = remember { Animatable(if (hasEntered) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (!hasEntered) {
            delay((index * FutureMotion.StaggerStepMillis).toLong())
            progress.animateTo(1f, FutureMotion.enter())
            hasEntered = true
        }
    }
    return graphicsLayer {
        alpha = progress.value
        translationY = (1f - progress.value) * 10.dp.toPx()
    }
}
