package com.future.fitness.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** מעקב ריצה חי מבוסס GPS גולמי (LocationManager) - בכוונה בלי תלות ב-Google
 * Play services/Fused Location (המכשירים בסוויטה הזו הם מכשירי כיס ייעודיים,
 * לא תמיד עם GMS מלא מותקן), רק android.location הסטנדרטי שקיים בכל מכשיר
 * אנדרואיד. מצטבר מרחק בין קיבועי GPS עוקבים (Location.distanceTo). */
@SuppressLint("MissingPermission") // הקוד הקורא אחראי לבקש ACCESS_FINE_LOCATION לפני שימוש
class RunTracker(private val context: Context) {
    var isTracking by mutableStateOf(false)
        private set
    var distanceMeters by mutableDoubleStateOf(0.0)
        private set
    var hasFix by mutableStateOf(false)
        private set

    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
    private var lastLocation: Location? = null

    fun isLocationAvailable(): Boolean = locationManager != null

    fun start() {
        val manager = locationManager ?: return
        distanceMeters = 0.0
        lastLocation = null
        hasFix = false
        isTracking = true
        try {
            val provider = when {
                manager.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
                manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
                else -> return
            }
            manager.requestLocationUpdates(provider, 2000L, 3f, listener)
        } catch (e: SecurityException) {
            isTracking = false
        }
    }

    fun stop() {
        try {
            locationManager?.removeUpdates(listener)
        } catch (e: SecurityException) {
            // אין הרשאה - אין עדכונים פעילים ממילא
        }
        isTracking = false
    }

    private val listener = LocationListener { location ->
        hasFix = true
        val last = lastLocation
        if (last != null) {
            distanceMeters += last.distanceTo(location)
        }
        lastLocation = location
    }

    fun distanceKm(): Double = distanceMeters / 1000.0

    /** קצב ריצה בדקות לק"מ - null אם עדיין אין מרחק משמעותי כדי לא להציג
     * ערך שקרי-מדויק בתחילת הריצה. */
    fun paceMinPerKm(elapsedSeconds: Int): Double? {
        val km = distanceKm()
        if (km < 0.05) return null
        return (elapsedSeconds / 60.0) / km
    }
}
