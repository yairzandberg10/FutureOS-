package com.future.files

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.os.Environment
import android.os.StatFs
import android.widget.RemoteViews
import java.util.Locale

/**
 * ווידג'ט "קבצים" למסך הבית - מציג נפח פנוי מתוך הכולל באחסון הפנימי (סטטיסטיקת
 * מערכת קבצים בלבד, לא דורשת הרשאת אחסון), לחיצה פותחת את האפליקציה.
 */
class FilesWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val storageText = try {
            val stat = StatFs(Environment.getExternalStorageDirectory().path)
            val totalGb = stat.totalBytes / (1024.0 * 1024.0 * 1024.0)
            val freeGb = stat.availableBytes / (1024.0 * 1024.0 * 1024.0)
            String.format(Locale.getDefault(), "%.1fGB פנויים מתוך %.1fGB", freeGb, totalGb)
        } catch (e: Exception) {
            ""
        }

        appWidgetIds.forEach { widgetId ->
            val views = RemoteViews(context.packageName, R.layout.app_widget)
            views.setTextViewText(R.id.widget_storage_info, storageText)

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
