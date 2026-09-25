package com.future.sharednav.systemui

import android.app.Activity
import android.app.Application
import android.content.ContentProvider
import android.content.ContentValues
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.database.Cursor
import android.graphics.Insets
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import androidx.compose.ui.graphics.toArgb
import com.future.sharednav.theme.FutureTheme
import com.future.sharednav.theme.ThemeClient

private const val TAG = "SharedNav/StatusBarInset"

/**
 * המרווח משורת המצב של FutureUI, לכל Activity בכל אפליקציה - בלי שאף מסך
 * יצטרך לזכור אותו.
 *
 * שורת המצב של FutureUI היא חלון overlay בגובה [HEIGHT_DP] בראש המסך, מעל
 * כל אפליקציה. רוב האפליקציות מצוירות edge-to-edge (enableEdgeToEdge), כך
 * שהתוכן שלהן התחיל ב-y=0 ונחתך מתחתיה; אחרות התחילו מתחת לשורת המצב
 * המקורית של אנדרואיד (24dp), שנמוכה מזו שלנו ב-4dp. כאן נמדד כמה מהחלון
 * של האפליקציה באמת מוסתר, ובדיוק הכמות הזו נוספת כ-padding עליון ל-
 * android.R.id.content, בצבע הרקע של הערכה.
 *
 * באותו מקום ה-inset של שורת המצב נצרך, כדי ש-Scaffold או
 * WindowInsets.statusBars בתוך Compose לא יוסיפו מרווח שני מעל הראשון.
 *
 * מותקן אוטומטית בכל אפליקציה שתלויה בספרייה (ר' [StatusBarInsetInstaller]
 * במניפסט של הספרייה). Activity שמצייר בעצמו עד הקצה (מסך הבית עם הרקע)
 * מוותר עליו ב-meta-data:
 * ```
 * <meta-data android:name="com.future.sharednav.STATUS_BAR_INSET" android:value="false" />
 * ```
 */
object StatusBarInset {
    /** גובה שורת המצב של FutureUI. FutureUI קורא את אותו קבוע לגובה החלון שלה. */
    const val HEIGHT_DP = 28

    /**
     * ריווח קטן מעל כותרת שהייתה נחתכת מתחת לשורת המצב (2026-09-25). מוסף
     * ידנית רק במסכים שהכותרת שלהם יושבת בראש החלון - ScreenTopBar מתחיל
     * ב-12dp, ועם 16dp נוספים הכותרת מתחילה בדיוק מתחת לשורה בגובה 28dp.
     * מסכים שכבר רחוקים ממנה (Scaffold של Material, מסכים ממורכזים) לא צריכים.
     */
    const val TITLE_GAP_DP = 16

    const val META_DATA_KEY = "com.future.sharednav.STATUS_BAR_INSET"

    /** כבוי לבקשת המשתמש (2026-09-23): בלי מרווח עליון באף אפליקציה. */
    private const val ENABLED = false

    private val callbacks = object : Application.ActivityLifecycleCallbacks {
        private val observers = HashMap<Activity, ContentObserver>()

        override fun onActivityPostCreated(activity: Activity, savedInstanceState: Bundle?) {
            OptionsKeyFallback.install(activity)
            if (!ENABLED || !isEnabledFor(activity)) return
            val content = activity.findViewById<ViewGroup>(android.R.id.content) ?: return
            apply(activity, content)
            val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
                override fun onChange(selfChange: Boolean) = paint(activity, content)
            }
            try {
                activity.contentResolver.registerContentObserver(ThemeClient.THEME_URI, false, observer)
                observers[activity] = observer
            } catch (e: Exception) {
                Log.w(TAG, "registerContentObserver failed; the inset color will not follow theme changes", e)
            }
        }

        override fun onActivityDestroyed(activity: Activity) {
            observers.remove(activity)?.let { activity.contentResolver.unregisterContentObserver(it) }
        }

        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
        override fun onActivityStarted(activity: Activity) {}
        override fun onActivityResumed(activity: Activity) {}
        override fun onActivityPaused(activity: Activity) {}
        override fun onActivityStopped(activity: Activity) {}
        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
    }

    fun install(application: Application) {
        application.registerActivityLifecycleCallbacks(callbacks)
    }

    private fun isEnabledFor(activity: Activity): Boolean = try {
        val info = activity.packageManager.getActivityInfo(activity.componentName, PackageManager.GET_META_DATA)
        info.metaData?.getBoolean(META_DATA_KEY, true) ?: true
    } catch (e: Exception) {
        true
    }

    private fun apply(activity: Activity, content: ViewGroup) {
        val barPx = (HEIGHT_DP * activity.resources.displayMetrics.density).toInt()
        paint(activity, content)

        // שורת המצב המקורית כבר "מטופלת" על ידי המרווח שלנו - מי שקורא את
        // ה-insets בתוך התוכן לא צריך לראות אותה שוב.
        content.setOnApplyWindowInsetsListener { _, insets ->
            val status = insets.getInsets(WindowInsets.Type.statusBars())
            if (status.top == 0) insets
            else WindowInsets.Builder(insets)
                .setInsets(WindowInsets.Type.statusBars(), Insets.of(status.left, 0, status.right, status.bottom))
                .build()
        }

        // כמה מהתוכן מוסתר בפועל תלוי במקום שבו החלון מתחיל (0 ב-edge-to-edge,
        // מתחת לשורה המקורית אחרת) - לכן נמדד אחרי layout ולא מחושב מראש.
        val location = IntArray(2)
        content.addOnLayoutChangeListener { v, _, _, _, _, _, _, _, _ ->
            v.getLocationOnScreen(location)
            val needed = (barPx - location[1]).coerceAtLeast(0)
            if (v.paddingTop != needed) {
                v.setPadding(v.paddingLeft, needed, v.paddingRight, v.paddingBottom)
            }
        }
        content.requestLayout()
    }

    private fun paint(activity: Activity, content: View) {
        val shared = ThemeClient.getTheme(activity)
        content.setBackgroundColor(FutureTheme(isDarkMode = shared.isDarkMode).backgroundColor.toArgb())
    }
}

/**
 * מתקין את [StatusBarInset] בעליית התהליך, לפני ה-Activity הראשון - אותו
 * דפוס של androidx.startup, בלי התלות. רשום במניפסט של הספרייה, כך שכל
 * אפליקציה שתלויה בה מקבלת אותו במיזוג המניפסט.
 */
class StatusBarInsetInstaller : ContentProvider() {
    override fun onCreate(): Boolean {
        (context?.applicationContext as? Application)?.let { StatusBarInset.install(it) }
        return true
    }

    override fun query(uri: Uri, projection: Array<out String>?, selection: String?, selectionArgs: Array<out String>?, sortOrder: String?): Cursor? = null
    override fun getType(uri: Uri): String? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int = 0
}
