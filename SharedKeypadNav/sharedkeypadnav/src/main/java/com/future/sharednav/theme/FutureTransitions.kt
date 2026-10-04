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
 * התנועה עצמה: המסך הנכנס מחליק רבע רוחב בקפיץ ([FutureMotion.Springs.layerOffset]),
 * והמסך שיוצא נסוג לעומק ([FutureMotion.DepthScale]) וזז מעט לכיוון ההפוך
 * ([FutureMotion.ParallaxFraction]). שתי השכבות זזות במהירויות שונות, וזה מה
 * שנותן תחושת עומק ברורה. הכול translation/scale/alpha - שכבה גרפית אחת, בלי
 * מדידה או ציור מחדש.
 *
 * הבנייה היא דרך ה-constructor של ContentTransform ולא `togetherWith ...
 * using`: האופרטור using מוגדר רק בתוך AnimatedContentTransitionScope,
 * והמעברים כאן צריכים להיות ערכים שאפשר לקרוא להם מכל מקום.
 */
object FutureTransitions {

    private fun slideDistance(fullWidth: Int): Int = fullWidth / FutureMotion.SlideFraction
    private fun parallax(fullWidth: Int): Int = (fullWidth * FutureMotion.ParallaxFraction).toInt()

    /** SizeTransform בלי clip: המסכים ממלאים את כל השטח ממילא, וחיתוך היה
     *  קוטע את ההסטה האופקית בקצה. */
    private fun transform(enter: EnterTransition, exit: ExitTransition) =
        ContentTransform(enter, exit, sizeTransform = SizeTransform(clip = false))

    private val forwardEnter: EnterTransition
        get() = fadeIn(FutureMotion.enter()) +
            slideInHorizontally(FutureMotion.Springs.layerOffset) { -slideDistance(it) }

    private val forwardExit: ExitTransition
        get() = fadeOut(FutureMotion.exit()) +
            scaleOut(FutureMotion.exit(), targetScale = FutureMotion.DepthScale) +
            slideOutHorizontally(FutureMotion.exit()) { parallax(it) }

    private val backwardEnter: EnterTransition
        get() = fadeIn(FutureMotion.enter()) +
            scaleIn(FutureMotion.Springs.layer(), initialScale = FutureMotion.DepthScale) +
            slideInHorizontally(FutureMotion.Springs.layerOffset) { parallax(it) }

    private val backwardExit: ExitTransition
        get() = fadeOut(FutureMotion.exit()) +
            slideOutHorizontally(FutureMotion.exit()) { -slideDistance(it) }

    /** כניסה למסך עמוק יותר: החדש מחליק פנימה בקפיץ, הקודם נסוג לעומק ונדחף מעט. */
    fun forward(): ContentTransform = transform(forwardEnter, forwardExit)

    /** חזרה אחורה - אותה תנועה הפוכה: הקודם חוזר מהעומק, העמוק מחליק החוצה. */
    fun backward(): ContentTransform = transform(backwardEnter, backwardExit)

    /** החלפה בין שני מסכים שווי-רמה (טאבים). */
    fun fadeThrough(): ContentTransform = transform(
        fadeIn(FutureMotion.enter()) + scaleIn(FutureMotion.Springs.dialog(), initialScale = 0.94f),
        fadeOut(FutureMotion.exit()),
    )

    /** הופעה של תוכן במקום (הודעת שגיאה, מצב ריק, כותרת שהתחלפה). */
    fun appear(): ContentTransform = transform(
        fadeIn(FutureMotion.enter()),
        fadeOut(FutureMotion.exit()),
    )

    // ---- דיאלוגים ----

    /** נפתח מ-[FutureMotion.DialogFromScale] בקפיץ עם overshoot קטן - "זה נפתח עכשיו מעליי". */
    val dialogEnter: EnterTransition =
        fadeIn(FutureMotion.enter()) + scaleIn(FutureMotion.Springs.dialog(), initialScale = FutureMotion.DialogFromScale)

    val dialogExit: ExitTransition =
        fadeOut(FutureMotion.exit()) + scaleOut(FutureMotion.exit(), targetScale = 0.9f)

    // ---- NavHost (androidx.navigation) ----
    //
    // אותם מעברים בדיוק, כערכים מוכנים ל-NavHost(enterTransition = ...).
    // הכיוון ידוע מראש מהפרמטר עצמו (enter/pop), אז אין צורך להשוות בין
    // שני מצבים.

    val navEnter: EnterTransition = forwardEnter
    val navExit: ExitTransition = forwardExit
    val navPopEnter: EnterTransition = backwardEnter
    val navPopExit: ExitTransition = backwardExit
}
