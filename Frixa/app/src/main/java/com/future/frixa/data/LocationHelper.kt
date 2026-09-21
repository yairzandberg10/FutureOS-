package com.future.frixa.data

import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/** מיקום דרך LocationManager הרגיל - בלי Google Play Services (לא זמין
 * במכשיר), בדיוק כמו ב-Navigation/Calendar. מספיק last-known בשביל מיון
 * חנויות לפי מרחק, אין צורך בזרם מיקום חי. */
object LocationHelper {
    fun hasPermission(context: Context): Boolean {
        return context.checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            context.checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    fun lastKnownLatLon(context: Context): Pair<Double, Double>? {
        if (!hasPermission(context)) return null
        return try {
            val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            for (provider in manager.getProviders(true)) {
                val loc = manager.getLastKnownLocation(provider)
                if (loc != null) return loc.latitude to loc.longitude
            }
            null
        } catch (e: Exception) {
            null
        }
    }

    /** מרחק במטרים בין שתי נקודות lat/lon (נוסחת הברסין). */
    fun distanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadius = 6371000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return earthRadius * c
    }
}
