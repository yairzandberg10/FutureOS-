package com.future.sharednav.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import android.util.SizeF
import android.view.View
import android.widget.RemoteViews
import com.future.sharednav.R

/** מה שהווידג'ט מציג: ערך ראשי, שורת משנה, וכותרת (ברירת מחדל - שם האפליקציה). */
data class WidgetContent(
    val value: String,
    val subtitle: String? = null,
    val title: String? = null,
)

/**
 * ווידג'ט עם תוכן אמיתי לכל אפליקציית FutureOS. קודם כמעט כל הווידג'טים היו
 * אייקון ושם האפליקציה בלבד, ו-updatePeriodMillis=0 - כלומר לא התעדכנו אף פעם.
 *
 * כל אפליקציה מממשת [content] (רץ ב-thread רקע, אז מותר לקרוא בו ContentProvider
 * או מסד נתונים), והתבנית אחידה לפי רכיב Widget של הדיזיין סיסטם
 * (layout/future_widget): משטח ההתראה הצפה (#1C1C1E ב-90%, קו שיער 0.5dp),
 * אייקון האפליקציה 18dp ושם 12sp ב-60% לבן, ערך 20sp מודגש (28sp לשעון) ושורת
 * משנה 13sp ב-60%. בשורה אחת - layout/future_widget_compact. OK פותח את האפליקציה.
 *
 * מתעדכן כשהלאנצ'ר חוזר למסך הבית (FutureLauncher שולח APPWIDGET_UPDATE לכל
 * ווידג'ט שמוצג), וכשהאפליקציה עצמה קוראת ל-[refresh] אחרי שינוי.
 */
abstract class FutureContentWidget : AppWidgetProvider() {

    /** שעון חי (TextClock) במקום הערך - ר' layout/future_widget_clock. */
    protected open val liveClock: Boolean = false

    /** התוכן. רץ מחוץ ל-thread הראשי. */
    abstract fun content(context: Context): WidgetContent

    /**
     * APPWIDGET_UPDATE הוא שידור מוגן - רק המערכת יכולה לשלוח אותו, ומאפליקציה
     * (הלאנצ'ר או [refresh]) הוא נזרק ב-SecurityException. לכן הרענון עובר
     * בפעולה משלנו, בשידור מפורש לרכיב (לא צריך intent-filter).
     */
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_REFRESH) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS)
                ?: runCatching { manager.getAppWidgetIds(ComponentName(context, javaClass)) }.getOrNull()
            if (ids != null && ids.isNotEmpty()) onUpdate(context, manager, ids)
            return
        }
        super.onReceive(context, intent)
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val pending = goAsync()
        val app = context.applicationContext
        Thread {
            try {
                render(app, appWidgetManager, appWidgetIds)
            } catch (e: Exception) {
                Log.w(TAG, "widget render failed", e)
            } finally {
                pending.finish()
            }
        }.start()
    }

    private fun render(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val label = context.applicationInfo.loadLabel(context.packageManager).toString()
        val content = runCatching { content(context) }.getOrElse {
            Log.w(TAG, "widget content failed", it)
            WidgetContent(label)
        }
        val launch = context.packageManager.getLaunchIntentForPackage(context.packageName)?.let {
            PendingIntent.getActivity(context, 0, it, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        }
        fun build(layout: Int) = RemoteViews(context.packageName, layout).also { views ->
            views.setImageViewResource(R.id.future_widget_icon, context.applicationInfo.icon)
            views.setTextViewText(R.id.future_widget_title, content.title ?: label)
            if (!liveClock) views.setTextViewText(R.id.future_widget_value, content.value)
            if (content.subtitle.isNullOrBlank()) {
                views.setViewVisibility(R.id.future_widget_subtitle, View.GONE)
            } else {
                views.setViewVisibility(R.id.future_widget_subtitle, View.VISIBLE)
                views.setTextViewText(R.id.future_widget_subtitle, content.subtitle)
            }
            launch?.let { views.setOnClickPendingIntent(R.id.future_widget_root, it) }
        }
        // פריסה רספונסיבית (API 31): בשורה אחת - הערך ליד שורת המשנה; משתי
        // שורות - התבנית המלאה, שלוש שורות.
        val views = RemoteViews(
            mapOf(
                SizeF(1f, 1f) to build(if (liveClock) R.layout.future_widget_clock_compact else R.layout.future_widget_compact),
                SizeF(1f, TALL_MIN_HEIGHT_DP) to build(if (liveClock) R.layout.future_widget_clock else R.layout.future_widget),
            )
        )
        ids.forEach { manager.updateAppWidget(it, views) }
    }

    companion object {
        private const val TAG = "FutureWidget"
        /** מהגובה הזה (dp) - התבנית המלאה; שורה אחת בלאנצ'ר היא ~76dp. */
        private const val TALL_MIN_HEIGHT_DP = 110f

        /** רענון ווידג'ט מבחוץ (הלאנצ'ר, האפליקציה עצמה) - ר' [onReceive]. */
        const val ACTION_REFRESH = "com.future.sharednav.action.WIDGET_REFRESH"

        /** מבקש מהווידג'טים של [provider] להתעדכן עכשיו (אחרי שינוי בנתונים). */
        fun refresh(context: Context, provider: Class<out AppWidgetProvider>) {
            val component = ComponentName(context, provider)
            val ids = runCatching { AppWidgetManager.getInstance(context).getAppWidgetIds(component) }.getOrNull()
            if (ids == null || ids.isEmpty()) return
            context.sendBroadcast(
                Intent(ACTION_REFRESH)
                    .setComponent(component)
                    .putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            )
        }

        /** "לפני 5 דקות" / "לפני שעתיים" / "אתמול" - לשורות משנה. */
        fun ago(millis: Long, now: Long = System.currentTimeMillis()): String {
            val minutes = ((now - millis) / 60_000L).coerceAtLeast(0)
            return when {
                minutes < 1 -> "עכשיו"
                minutes < 60 -> "לפני $minutes דקות"
                minutes < 120 -> "לפני שעה"
                minutes < 24 * 60 -> "לפני ${minutes / 60} שעות"
                minutes < 48 * 60 -> "אתמול"
                else -> "לפני ${minutes / (24 * 60)} ימים"
            }
        }
    }
}
