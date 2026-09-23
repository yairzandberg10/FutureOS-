package com.future.sharednav.theme

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntOffset

/**
 * סקאלת התנועה של המערכת - הטוקנים היחידים שמותר לגזור מהם משך אנימציה
 * או עקומת האצה. עד עכשיו לא היה כאן שום טוקן: בסריקה נמצאו 20 משכים
 * שונים שנכתבו ידנית ברחבי הקוד (60, 100, 120, 150, 160, 180, 200, 220,
 * 250, 280, 300, 320, 450, 500, 600...), רובם ב-tween() בלי שום נימוק,
 * ובחלק הארי של המסכים לא הייתה אנימציה בכלל.
 *
 * המשכים כאן קצרים בכוונה. ההערה שכבר הייתה בקוד ההגדרות מנסחת את הכלל
 * הזה נכון: במכשיר בלי מסך מגע כל מעבר בין מסכים נעשה בלחיצת מקש, המשתמש
 * לוחץ הרבה ומהר, וכל מילישנייה של אנימציה מצטברת להרגשה של איטיות. לכן
 * התקן כאן הוא 200ms למעבר מסך - לא 300ms שהוא ברירת המחדל של Material -
 * ו-90ms לשינוי צבע של פוקוס, שחייב להרגיש מיידי כי הוא המשוב היחיד
 * שהמשתמש מקבל על לחיצת חץ.
 */
object FutureMotion {

    /** שינוי צבע/רקע בעקבות פוקוס - חייב להיות מתחת לסף התפיסה כדי שלא
     *  ייווצר פיגור מורגש בין לחיצת החץ לבין סימון השורה. */
    const val DurationInstant: Int = 90

    /** שינויים קטנים בתוך המסך (הופעת שורה, החלפת אייקון, סרגל התקדמות). */
    const val DurationFast: Int = 140

    /** מעבר בין מסכים, פתיחת דיאלוג - התקן. */
    const val DurationStandard: Int = 200

    /** מעברים גדולים בלבד (מסך מלא שנכנס מלמטה, מצב ריק שנחשף). */
    const val DurationSlow: Int = 280

    /** עקומת ברירת המחדל - יציאה מהירה, נחיתה רכה. */
    val EasingStandard: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)

    /** לרכיב שנכנס למסך: מאיץ מיד ומאט בסוף. */
    val EasingDecelerate: Easing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)

    /** לרכיב שיוצא מהמסך: מתחיל לאט ומאיץ החוצה. */
    val EasingAccelerate: Easing = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)

    fun <T> instant(): FiniteAnimationSpec<T> = tween(DurationInstant, easing = EasingStandard)

    fun <T> fast(): FiniteAnimationSpec<T> = tween(DurationFast, easing = EasingStandard)

    fun <T> standard(): FiniteAnimationSpec<T> = tween(DurationStandard, easing = EasingStandard)

    fun <T> enter(): FiniteAnimationSpec<T> = tween(DurationStandard, easing = EasingDecelerate)

    fun <T> exit(): FiniteAnimationSpec<T> = tween(DurationFast, easing = EasingAccelerate)

    /** צבע רקע/מסגרת של פוקוס. */
    val focusColorSpec: AnimationSpec<Color> = tween(DurationInstant, easing = EasingStandard)

    /**
     * הגדלת פריט ממוקד. קפיץ ולא tween: כשמחזיקים חץ והפוקוס עובר בין
     * שורות במהירות, קפיץ ממשיך מהמצב הנוכחי במקום להתחיל כל פעם מ-1.0,
     * ולכן הרשימה לא "מרצדת". dampingRatio מתחת ל-1 בכוונה מעט - מספיק
     * כדי שתהיה תחושת חיים, לא מספיק כדי שייראה קופצני.
     */
    val focusScaleSpec: AnimationSpec<Float> = spring(
        dampingRatio = 0.75f,
        stiffness = Spring.StiffnessMediumLow,
    )

    /** תזוזת פריט ברשימה (הוספה/מחיקה/מיון מחדש). */
    val listItemSpec: FiniteAnimationSpec<IntOffset> = tween(DurationStandard, easing = EasingStandard)

    /**
     * שבריר מרוחב המסך שממנו נכנס מסך חדש. מעבר "מלא" (מסך שלם שנכנס
     * מהצד) על מסך 640px נמשך יותר מדי זמן ומושך את העין למקום הלא נכון;
     * הסטה של שמינית מהרוחב מספיקה כדי למסור את כיוון הניווט.
     */
    const val SlideFraction: Int = 8

    /** משך ההשהיה של אנימציית כניסה מדורגת בין פריט לפריט ברשימה. */
    const val StaggerStepMillis: Int = 24

    /** מעבר לא מדורג מעבר לפריט הזה - רשימה ארוכה לא אמורה "להיבנות" לאט. */
    const val StaggerMaxItems: Int = 8

    /**
     * סיבוב אחד של הספינר - הרכיב המסתובב היחיד במערכת, בקצב לינארי
     * (components/feedback/Spinner.jsx). לא נגזר מהסקאלה כי הוא לולאה
     * ולא מעבר.
     */
    const val SpinnerRotationMillis: Int = 900

    /**
     * מעבר אחד של המקטע בפס התקדמות לא-מוגדר (חיפוש מכשירים, התחברות) -
     * לינארי, כמו כל פס התקדמות במערכת, ולולאה ולא מעבר.
     */
    const val ProgressSweepMillis: Int = 1400
}
