package com.future.calendar

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.icu.text.DateFormat
import android.icu.util.ULocale
import android.widget.RemoteViews
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * ווידג'ט "לוח שנה" למסך הבית - מציג את התאריך הלועזי והעברי של היום (באותה
 * שיטת ICU שבה משתמש FutureUI במרכז הבקרה/התראות), לחיצה פותחת את האפליקציה.
 */
class CalendarWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val now = Calendar.getInstance().time
        val gregorianText = SimpleDateFormat("EEEE, d בMMMM", Locale("iw", "IL")).format(now)
        val hebrewDateFormat = DateFormat.getDateInstance(
            DateFormat.FULL,
            ULocale.forLanguageTag("he-IL-u-ca-hebrew")
        )
        val hebrewText = hebrewDateFormat.format(now)

        appWidgetIds.forEach { widgetId ->
            val views = RemoteViews(context.packageName, R.layout.app_widget)
            views.setTextViewText(R.id.widget_gregorian_date, gregorianText)
            views.setTextViewText(R.id.widget_hebrew_date, hebrewText)

            val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
            if (launchIntent != null) {
                val pendingIntent = PendingIntent.getActivity(
                    context, 0, launchIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)
            }
            appWidgetManager.updateAppWidget(widgetId, views)
        }
    }
}
