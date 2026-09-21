package com.future.sharednav.theme

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally

/**
 * מעברי המסך של המערכת. שלושה סוגים בלבד, וכולם נגזרים מ-[FutureMotion]:
 *
 * - [forward]  - כניסה למסך עמוק יותר (בית -> פריט).
 * - [backward] - חזרה אחורה. אותה תנועה בדיוק, הפוכה בכיוון.
 * - [fadeThrough] - החלפה בין שני מסכים באותה רמה (מעבר בין טאבים), שאין
 *   ביניהם יחס של "פנימה/החוצה" ולכן תנועה אופקית הייתה משקרת.
 *
 * הכיוון כאן מותאם ל-RTL ידנית ובכוונה: כל המערכת כפויה
 * LayoutDirection.Rtl, ובעברית ההתקדמות "פנימה" היא שמאלה (בדיוק כמו
 * החץ שמצביע שמאלה בשורות ההגדרות). ההיסטים של slideIn/slideOut הם
 * פיקסלים אבסולוטיים ולא מושפעים מכיוון הפריסה, ולכן מסך נכנס מתחיל
 * בהיסט שלילי (שמאל) והמסך שיוצא נדחף להיסט חיובי (ימין).
 *
 * הבנייה היא דרך ה-constructor של ContentTransform ולא `togetherWith ...
 * using`: האופרטור using מוגדר רק בתוך AnimatedContentTransitionScope,
 * והמעברים כאן צריכים להיות ערכים שאפשר לקרוא להם מכל מקום.
 */
object FutureTransitions {

    private fun slideDistance(fullWidth: Int): Int = fullWidth / FutureMotion.SlideFraction

    /** SizeTransform בלי clip: המסכים ממלאים את כל השטח ממילא, וחיתוך היה
     *  קוטע את ההסטה האופקית בקצה. */
    private fun transform(enter: EnterTransition, exit: ExitTransition) =
        ContentTransform(enter, exit, sizeTransform = SizeTransform(clip = false))

    /** כניסה למסך עמוק יותר. */
    fun forward(): ContentTransform = transform(
        fadeIn(FutureMotion.enter()) +
            slideInHorizontally(FutureMotion.enter()) { -slideDistance(it) },
        fadeOut(FutureMotion.exit()) +
            slideOutHorizontally(FutureMotion.exit()) { slideDistance(it) },
    )

    /** חזרה אחורה. */
    fun backward(): ContentTransform = transform(
        fadeIn(FutureMotion.enter()) +
            slideInHorizontally(FutureMotion.enter()) { slideDistance(it) },
        fadeOut(FutureMotion.exit()) +
            slideOutHorizontally(FutureMotion.exit()) { -slideDistance(it) },
    )

    /** החלפה בין שני מסכים שווי-רמה (טאבים). */
    fun fadeThrough(): ContentTransform = transform(
        fadeIn(FutureMotion.enter()) + scaleIn(FutureMotion.enter(), initialScale = 0.97f),
        fadeOut(FutureMotion.exit()),
    )

    /** הופעה של תוכן במקום (הודעת שגיאה, מצב ריק, כותרת שהתחלפה). */
    fun appear(): ContentTransform = transform(
        fadeIn(FutureMotion.enter()),
        fadeOut(FutureMotion.exit()),
    )

    // ---- דיאלוגים ----

    /** קופץ מעט קטן יותר ומתייצב - הגודל הקטן מסמן "זה נפתח עכשיו מעליי". */
    val dialogEnter: EnterTransition =
        fadeIn(FutureMotion.enter()) + scaleIn(FutureMotion.enter(), initialScale = 0.92f)

    val dialogExit: ExitTransition =
        fadeOut(FutureMotion.exit()) + scaleOut(FutureMotion.exit(), targetScale = 0.96f)

    // ---- NavHost (androidx.navigation) ----
    //
    // אותם מעברים בדיוק, כערכים מוכנים ל-NavHost(enterTransition = ...).
    // הכיוון ידוע מראש מהפרמטר עצמו (enter/pop), אז אין צורך להשוות בין
    // שני מצבים.

    val navEnter: EnterTransition =
        fadeIn(FutureMotion.enter()) + slideInHorizontally(FutureMotion.enter()) { -slideDistance(it) }

    val navExit: ExitTransition =
        fadeOut(FutureMotion.exit()) + slideOutHorizontally(FutureMotion.exit()) { slideDistance(it) }

    val navPopEnter: EnterTransition =
        fadeIn(FutureMotion.enter()) + slideInHorizontally(FutureMotion.enter()) { slideDistance(it) }

    val navPopExit: ExitTransition =
        fadeOut(FutureMotion.exit()) + slideOutHorizontally(FutureMotion.exit()) { -slideDistance(it) }
}
