package com.future.calendar.data

import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager

/** מיקום אחרון ידוע דרך LocationManager הרגיל - בלי Google Play Services (לא זמין במכשיר). */
object LocationHelper {
    // מיקום מטמון ישן יותר מהסף הזה נחשב לא-אמין (ה-GPS/רשת עשויים להיות
    // כבויים זמן רב, כך שהמיקום האחרון הידוע שייך לעבר רחוק) - במקרה כזה
    // מחזירים null כדי שהקורא ייפול חזרה לאזור ברירת המחדל במקום להציג
    // זמני זמנים שגויים בלי להתריע.
    private const val MAX_LOCATION_AGE_MILLIS = 2 * 60 * 60 * 1000L // שעתיים

    fun hasPermission(context: Context): Boolean {
        return context.checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            context.checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    fun lastKnownLatLon(context: Context): Pair<Double, Double>? {
        if (!hasPermission(context)) return null
        return try {
            val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            // המיקום הטרי ביותר מכל הספקים - לא הספק הראשון שמיקומו בתוך הטווח
            val now = System.currentTimeMillis()
            val best = manager.getProviders(true)
                .mapNotNull { manager.getLastKnownLocation(it) }
                .filter { now - it.time <= MAX_LOCATION_AGE_MILLIS }
                .maxByOrNull { it.time }
            best?.let { it.latitude to it.longitude }
        } catch (e: Exception) {
            null
        }
    }
}
