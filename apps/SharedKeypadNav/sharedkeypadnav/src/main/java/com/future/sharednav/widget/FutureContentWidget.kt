package com.future.sharednav.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
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
 * או מסד נתונים), והתבנית אחידה (layout/future_widget): אייקון ושם האפליקציה
 * בשורה העליונה ב-60%, הערך 20sp מודגש, ושורת משנה 13sp ב-60% - צבעי ההתראה
 * הצפה של הדיזיין סיסטם (תמיד כהה, כי הווידג'ט יושב על הטפט). OK פותח את
 * האפליקציה.
 *
 * מתעדכן כשהלאנצ'ר חוזר למסך הבית (FutureLauncher שולח APPWIDGET_UPDATE לכל
 * ווידג'ט שמוצג), וכשהאפליקציה עצמה קוראת ל-[refresh] אחרי שינוי.
 */
abstract class FutureContentWidget : AppWidgetProvider() {

    /** שעון חי (TextClock) במקום הערך - ר' layout/future_widget_clock. */
    protected open val liveClock: Boolean = false

    /** התוכן. רץ מחוץ ל-thread הראשי. */
    abstract fun content(context: Context): WidgetContent

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
        val views = RemoteViews(context.packageName, if (liveClock) R.layout.future_widget_clock else R.layout.future_widget)
        views.setImageViewResource(R.id.future_widget_icon, context.applicationInfo.icon)
        views.setTextViewText(R.id.future_widget_title, content.title ?: label)
        if (!liveClock) views.setTextViewText(R.id.future_widget_value, content.value)
        if (content.subtitle.isNullOrBlank()) {
            views.setViewVisibility(R.id.future_widget_subtitle, View.GONE)
        } else {
            views.setViewVisibility(R.id.future_widget_subtitle, View.VISIBLE)
            views.setTextViewText(R.id.future_widget_subtitle, content.subtitle)
        }
        context.packageManager.getLaunchIntentForPackage(context.packageName)?.let { launch ->
            val pi = PendingIntent.getActivity(context, 0, launch, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            views.setOnClickPendingIntent(R.id.future_widget_root, pi)
        }
        ids.forEach { manager.updateAppWidget(it, views) }
    }

    companion object {
        private const val TAG = "FutureWidget"

        /** מבקש מהווידג'טים של [provider] להתעדכן עכשיו (אחרי שינוי בנתונים). */
        fun refresh(context: Context, provider: Class<out AppWidgetProvider>) {
            val component = ComponentName(context, provider)
            val ids = runCatching { AppWidgetManager.getInstance(context).getAppWidgetIds(component) }.getOrNull()
            if (ids == null || ids.isEmpty()) return
            context.sendBroadcast(
                Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
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
