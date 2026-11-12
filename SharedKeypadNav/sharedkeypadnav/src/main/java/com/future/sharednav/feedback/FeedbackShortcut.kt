package com.future.sharednav.feedback

import android.content.Context
import android.content.Intent

/**
 * פתיחת "שלח משוב" של Settings, עם האפליקציה שמדווחים עליה כבר בחורה.
 * שום דבר לא נשלח מכאן: המשתמש כותב, רואה תצוגה מקדימה ולוחץ "שלח" בתוך Settings.
 *
 * משמש את מרכז הבקרה של FutureUI ("דווח על באג" על האפליקציה שבחזית), ואפשר לקרוא לו מכל
 * תפריט Options של אפליקציה ([FeedbackMenuRow]).
 */
object FeedbackShortcut {
    const val SETTINGS_PKG = "com.future.settings"
    const val ACTION_SEND_FEEDBACK = "com.future.settings.action.SEND_FEEDBACK"
    const val EXTRA_FEEDBACK_PKG = "com.future.settings.extra.FEEDBACK_PKG"

    private val PKG_RE = Regex("""^com\.future\.[a-z0-9_.]{1,64}$""")

    /** Intent ל-Settings. aboutPkg שאינו com.future.* נופל ל"מערכת כללית". */
    fun intent(aboutPkg: String?): Intent =
        Intent(ACTION_SEND_FEEDBACK)
            .setPackage(SETTINGS_PKG)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            .apply { if (aboutPkg != null && PKG_RE.matches(aboutPkg)) putExtra(EXTRA_FEEDBACK_PKG, aboutPkg) }

    fun isAvailable(context: Context): Boolean =
        context.packageManager.resolveActivity(intent(null), 0) != null

    /** פותח את מסך המשוב. מחזיר false אם Settings לא מותקנת. */
    fun open(context: Context, aboutPkg: String? = context.packageName): Boolean = try {
        context.startActivity(intent(aboutPkg))
        true
    } catch (e: Exception) {
        false
    }
}
