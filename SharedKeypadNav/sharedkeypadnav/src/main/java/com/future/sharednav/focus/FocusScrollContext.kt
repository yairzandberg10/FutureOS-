package com.future.sharednav.focus

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.BringIntoViewSpec
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import kotlin.math.abs

/**
 * כמה מהמסך נשאר גלוי מעל ומתחת לפריט הממוקד כשרשימה גוללת אליו.
 *
 * הבעיה: ב-Compose, פריט שמקבל פוקוס גולל רק עד שהוא עצמו נכנס למסך. כותרת,
 * כותרת קטע או תמונה שמעל הפריט הראשון אינן ממוקדות, ולכן אחרי שגוללים
 * למטה וחוזרים למעלה הרשימה נעצרת על הפריט הראשון והכותרת נשארת מחוץ למסך -
 * ואין שום מקש שמחזיר אותה (במכשיר בלי מגע אין "לגרור למעלה"). זה קרה בכל
 * מסך שהכותרת שלו היא חלק מהתוכן הנגלל: ההגדרות, מסכי פרטים, בורר השעה
 * במעורר, כותרות קטעים ברשימות.
 *
 * הפתרון: כל גלילה-אל-פוקוס משאירה מרווח הקשר - [TopFraction] מגובה המכל
 * מעל הפריט ו-[BottomFraction] מתחתיו (ה-"pivot" של ממשקי טלוויזיה). כשמגיעים
 * לפריט הראשון, המרווח העליון חושף את מה שמעליו עד ראש התוכן (הגלילה נעצרת
 * ב-0, אז לא "נגללים" מעבר לכותרת). באמצע רשימה זה רק אומר שהרשימה מתחילה
 * לגלול קצת לפני שהפוקוס מגיע לקצה, כך שרואים מה הבא.
 *
 * למה בלי כיוון: אחרי כל גלילה כזו הפריט הממוקד נמצא בתוך הטווח [עליון, תחתון],
 * ולכן מעבר למטה תמיד נוחת מתחת לגבול העליון ולא גורם לגלילה הפוכה, ולהפך.
 *
 * פריט גבוה מדי בשביל המרווחים (כרטיס ארוך, טקסט מלא) מקבל את ההתנהגות
 * הרגילה של Compose.
 */
@OptIn(ExperimentalFoundationApi::class)
internal object FocusContextBringIntoViewSpec : BringIntoViewSpec {
    /** רבע מגובה המכל מעל הפריט - מספיק לכותרת גדולה (כ-110dp במסך של 480dp). */
    const val TopFraction = 0.25f

    /** שמינית מתחתיו - רואים שיש עוד שורה. */
    const val BottomFraction = 0.125f

    override fun calculateScrollDistance(offset: Float, size: Float, containerSize: Float): Float {
        val before = containerSize * TopFraction
        val after = containerSize * BottomFraction
        return if (size + before + after <= containerSize) {
            distance(offset - before, offset + size + after, containerSize)
        } else {
            distance(offset, offset + size, containerSize)
        }
    }

    /** ההתנהגות של ברירת המחדל של Compose: הגלילה הקטנה ביותר שמכניסה את הטווח. */
    private fun distance(leading: Float, trailing: Float, containerSize: Float): Float = when {
        leading >= 0f && trailing <= containerSize -> 0f
        leading < 0f && trailing > containerSize -> 0f
        abs(leading) < abs(trailing - containerSize) -> leading
        else -> trailing - containerSize
    }
}

private val LocalFocusScrollContextProvided = staticCompositionLocalOf { false }

/**
 * מפעיל את מרווח ההקשר ([FocusContextBringIntoViewSpec]) לכל הרשימות והמכלים
 * הנגללים בתוך [content]. כבר מסופק ב-FutureMaterialTheme, ב-ScreenScaffold
 * וב-AnimatedScreenHost; אפליקציה בלי אף אחד מהם עוטפת בזה את setContent.
 * קריאה מקוננת לא עושה כלום.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ProvideFocusScrollContext(content: @Composable () -> Unit) {
    if (LocalFocusScrollContextProvided.current) {
        content()
        return
    }
    CompositionLocalProvider(
        LocalBringIntoViewSpec provides FocusContextBringIntoViewSpec,
        LocalFocusScrollContextProvided provides true,
        content = content,
    )
}
