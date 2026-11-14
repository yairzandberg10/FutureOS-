package com.future.futurelauncher.ui

import android.app.Activity
import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.graphics.Rect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Rect as ComposeRect

/**
 * Icon Bloom - האפליקציה נפתחת מתוך האייקון שנבחר.
 *
 * עד עכשיו כל פתיחה השתמשה באותה החלקה (FutureActivityAnimation: שמינית
 * רוחב משמאל), בלי שום קשר לאייקון. כאן הלאנצ'ר מעביר ל-startActivity את
 * המלבן של האייקון הממוקד (ActivityOptions.makeClipRevealAnimation), וה-
 * WindowManager חושף את חלון האפליקציה מתוכו. האנימציה רצה ב-WindowManager,
 * לא בתהליך שלנו - היא לא עולה ללאנצ'ר כלום. המשך שלה נקבע על ידי המערכת.
 *
 * במקביל הבית עצמו נסוג לעומק ([launchTick]), וכשחוזרים אליו הוא "קולט"
 * את האפליקציה: גדל חזרה בקפיץ והאייקון שממנו היא נפתחה קופץ ([landTick]).
 * לאנצ'ר שאינו אפליקציית מערכת לא יכול לנהל את אנימציית הסגירה של חלון אחר,
 * ולכן הקליטה היא בצד הבית.
 */
object LaunchMotion {

    private val focusedIcon = Rect()
    private var hasIcon = false

    /** עולה בכל פתיחה מהבית - המסך נסוג לעומק. */
    var launchTick by mutableIntStateOf(0)
        private set

    /** עולה כשחוזרים לבית אחרי פתיחה - האייקון הממוקד קופץ. */
    var landTick by mutableIntStateOf(0)
        private set

    private var awaitingReturn = false

    /** האייקון הממוקד דיווח על המיקום שלו בחלון. */
    fun updateFocusedIcon(bounds: ComposeRect) {
        if (bounds.width <= 0f || bounds.height <= 0f) return
        focusedIcon.set(bounds.left.toInt(), bounds.top.toInt(), bounds.right.toInt(), bounds.bottom.toInt())
        hasIcon = true
    }

    /** נקרא ב-ON_RESUME של הבית. true - זו חזרה מאפליקציה שנפתחה מכאן. */
    fun onHomeResumed(): Boolean {
        if (!awaitingReturn) return false
        awaitingReturn = false
        landTick++
        return true
    }

    /** פותח את [intent] מתוך האייקון הממוקד. בלי מיקום ידוע - הפתיחה הרגילה. */
    fun start(context: Context, intent: Intent) {
        val activity = context as? Activity
        val view = activity?.window?.decorView
        val options = if (view != null && hasIcon) {
            ActivityOptions.makeClipRevealAnimation(
                view, focusedIcon.left, focusedIcon.top, focusedIcon.width(), focusedIcon.height(),
            ).toBundle()
        } else null
        context.startActivity(intent, options)
        launchTick++
        awaitingReturn = true
    }
}
