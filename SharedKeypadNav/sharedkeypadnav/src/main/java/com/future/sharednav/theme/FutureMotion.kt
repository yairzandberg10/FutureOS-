package com.future.sharednav.theme

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.VisibilityThreshold
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
     * מהצד) על מסך 640px נמשך יותר מדי זמן ומושך את העין למקום הלא נכון.
     * שישית מהרוחב (הערך הקודם) הייתה נכונה בכיוון אבל כמעט לא הורגשה על
     * מסך של 3.5 אינץ'; רבע, עם קפיץ, מורגש ועדיין לא ארוך יותר.
     */
    const val SlideFraction: Int = 4

    /** הגודל שאליו "נסוג" מסך כשנכנסים ממנו פנימה (ר' FutureTransitions.forward). */
    const val DepthScale: Float = 0.90f

    /** משך ההשהיה של אנימציית כניסה מדורגת בין פריט לפריט ברשימה. */
    const val StaggerStepMillis: Int = 22

    /** מעבר לא מדורג מעבר לפריט הזה - רשימה ארוכה לא אמורה "להיבנות" לאט. */
    const val StaggerMaxItems: Int = 6

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

    // ---- גודל התנועה (Amplitude) ----
    //
    // התנועה צריכה להיות מורגשת בלי להאריך זמן: מה שגורם לאנימציה להירגש
    // הוא המרחק, העומק והפיזיקה (overshoot), לא המשך. כל הערכים כאן הם
    // transform/alpha בלבד, כך שהגדלתם לא עולה כלום בביצועים.

    /** לחיצת OK: הפריט יורד לכאן ב-[pressDownSpec] וחוזר בקפיץ [Springs.press]. */
    const val PressScale: Float = 0.94f

    /** דיאלוג ותפריט נפתחים מהגודל הזה בקפיץ עם overshoot קטן. */
    const val DialogFromScale: Float = 0.85f

    /** המסך שנסוג לעומק זז גם מעט הצידה (parallax) - שבריר מהרוחב. */
    const val ParallaxFraction: Float = 0.06f

    /** כמה dp עולה פריט בכניסה מדורגת. */
    const val StaggerDistanceDp: Float = 24f

    /** ירידה מהירה ללחיצה - כמעט מיידית, כדי שהמשוב יגיע בפריים הראשון. */
    val pressDownSpec: FiniteAnimationSpec<Float> = tween(70, easing = EasingStandard)

    /**
     * קפיצי המערכת. כולם "התקפה מהירה, נחיתה רכה": עיקר התנועה נגמר תוך
     * 120-160ms, וה-overshoot נוחת מאחורי הלחיצה הבאה בלי לחסום אותה.
     * קפיץ ממשיך מהמהירות הנוכחית כשהיעד משתנה, ולכן לחיצות מהירות לא
     * יוצרות קפיצות בתנועה.
     */
    object Springs {
        /** אייקון ממוקד, אריח או כרטיס שנכנס בגל. ζ0.5 · k650. */
        fun <T> lift(threshold: T? = null): SpringSpec<T> = spring(0.5f, 650f, threshold)
        /** חזרה אחרי לחיצה, נשימה של התראה. ζ0.42 · k1300. */
        fun <T> press(threshold: T? = null): SpringSpec<T> = spring(0.42f, 1300f, threshold)
        /** מתג, מחוון, מילוי. ζ0.58 · k800. */
        fun <T> toggle(threshold: T? = null): SpringSpec<T> = spring(0.58f, 800f, threshold)
        /** דיאלוג, תפריט, החלפת תוכן. ζ0.62 · k520. */
        fun <T> dialog(threshold: T? = null): SpringSpec<T> = spring(0.62f, 520f, threshold)
        /** מסכים ושכבות מערכת (CC, NC, נעילה). ζ0.78 · k380. */
        fun <T> layer(threshold: T? = null): SpringSpec<T> = spring(0.78f, 380f, threshold)
        /** התראה צפה שגדלה משורת המצב. ζ0.66 · k420. */
        fun <T> island(threshold: T? = null): SpringSpec<T> = spring(0.66f, 420f, threshold)
        /** יציאה וחזרה - בלי overshoot. ζ0.9 · k520. */
        fun <T> settle(threshold: T? = null): SpringSpec<T> = spring(0.9f, 520f, threshold)

        /**
         * Elastic Focus: הקצה המוביל של סמן הפוקוס רץ קדימה בקפיץ קשיח, והקצה
         * האחורי מגיע אחריו בקפיץ רך - ההפרש ביניהם הוא המתיחה. ζ0.72 · k950.
         */
        val glideLead: SpringSpec<Float> = spring(0.72f, 950f, 0.5f)
        /** הקצה האחורי של הסמן. ζ0.86 · k340. */
        val glideTrail: SpringSpec<Float> = spring(0.86f, 340f, 0.5f)

        val layerOffset: SpringSpec<IntOffset> = spring(0.78f, 380f, IntOffset.VisibilityThreshold)
        val dialogOffset: SpringSpec<IntOffset> = spring(0.62f, 520f, IntOffset.VisibilityThreshold)
    }
}
