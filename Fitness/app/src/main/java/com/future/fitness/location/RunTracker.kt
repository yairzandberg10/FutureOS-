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
    // גובה נוכחי וסך העלייה המצטברת (מ-Location.altitude, כשה-GPS מספק אותו) -
    // מבוסס אך ורק על GPS, בלי חיישן ברומטרי, ולכן רועש יחסית; מסוכם רק עליות
    // (לא ירידות) בין קיבועים עוקבים, כמו "Elevation Gain" באפל ווטש.
    var currentAltitudeMeters by mutableStateOf<Double?>(null)
        private set
    var elevationGainMeters by mutableDoubleStateOf(0.0)
        private set
    // מהירות רגעית (מ-Location.speed, קמ"ש) - למסך הרכיבה, שם קצב דקות-לק"מ
    // לא רלוונטי.
    var currentSpeedKmh by mutableStateOf<Double?>(null)
        private set

    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
    private var lastLocation: Location? = null
    // בהשהיה של האימון קיבועים לא נספרים (המרחק והקצב נשארים עקביים עם הזמן שעמד).
    private var paused = false
    private var altitudeAnchor: Double? = null

    fun start() {
        val manager = locationManager ?: return
        distanceMeters = 0.0
        lastLocation = null
        paused = false
        hasFix = false
        currentAltitudeMeters = null
        elevationGainMeters = 0.0
        altitudeAnchor = null
        currentSpeedKmh = null
        isTracking = true
        try {
            val provider = when {
                manager.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
                manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
                // המיקום כבוי - קודם isTracking נשאר true והמסך "עקב" בלי לקבל כלום
                else -> { isTracking = false; return }
            }
            manager.requestLocationUpdates(provider, 2000L, 3f, listener)
        } catch (e: SecurityException) {
            isTracking = false
        }
    }

    /** השהיה: מתעלמים מקיבועים, ומאפסים את הנקודה האחרונה כדי שההמשך לא יספור קפיצה. */
    fun pause() {
        paused = true
        lastLocation = null
    }

    fun resume() {
        paused = false
        lastLocation = null
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
        if (paused) return@LocationListener
        // קיבוע לא מדויק (בתוך בניין, תחילת ריצה) קופץ עשרות מטרים הלוך וחזור,
        // וכל קפיצה נספרה כמרחק - גם בעמידה במקום. מדלגים על קיבועים גרועים,
        // ועל תזוזה קטנה מטווח השגיאה של שני הקיבועים.
        if (location.hasAccuracy() && location.accuracy > MAX_ACCURACY_METERS) return@LocationListener
        // F25: "עוקב" רק אחרי קיבוע שנקבל - קיבוע שנדחה לא משנה את המצב.
        hasFix = true
        val last = lastLocation
        // F47: סף הרעש קובע רק אם המרחק נספר - גובה ומהירות מתעדכנים גם בעמידה.
        var moved = last == null
        if (last != null) {
            val step = last.distanceTo(location)
            val noise = maxOf(if (last.hasAccuracy()) last.accuracy else 0f, if (location.hasAccuracy()) location.accuracy else 0f)
            if (step >= noise / 2) {
                distanceMeters += step
                moved = true
            }
        }
        if (location.hasAltitude()) {
            val altitude = location.altitude
            currentAltitudeMeters = altitude
            // F24: רעש הגובה של GPS קופץ הלוך וחזור. עולים רק כשהעלייה מהנקודה
            // הנמוכה האחרונה חורגת מסף הרעש, ונקודת הייחוס יורדת עם הירידות.
            val anchor = altitudeAnchor
            if (anchor == null) {
                altitudeAnchor = altitude
            } else {
                val delta = altitude - anchor
                if (delta >= ELEVATION_NOISE_METERS) {
                    elevationGainMeters += delta
                    altitudeAnchor = altitude
                } else if (delta <= -ELEVATION_NOISE_METERS) {
                    altitudeAnchor = altitude
                }
            }
        }
        currentSpeedKmh = if (location.hasSpeed()) (location.speed * 3.6) else null
        // נקודת הייחוס לחישוב המרחק מתעדכנת רק אחרי תזוזה שנספרה, כדי שרעש לא יצטבר.
        if (moved) lastLocation = location
    }

    fun distanceKm(): Double = distanceMeters / 1000.0

    private companion object {
        const val MAX_ACCURACY_METERS = 35f
        const val ELEVATION_NOISE_METERS = 3.0
    }

    /** קצב ריצה בדקות לק"מ - null אם עדיין אין מרחק משמעותי כדי לא להציג
     * ערך שקרי-מדויק בתחילת הריצה. */
    fun paceMinPerKm(elapsedSeconds: Int): Double? {
        val km = distanceKm()
        if (km < 0.05) return null
        return (elapsedSeconds / 60.0) / km
    }
}
