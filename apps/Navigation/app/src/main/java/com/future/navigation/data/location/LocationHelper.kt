package com.future.navigation.data.location

import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * מיקום דרך LocationManager הרגיל - בלי Google Play Services (לא זמין במכשיר,
 * בדיוק כמו ב-Calendar/LocationHelper.kt). GPS_PROVIDER מועדף כשקיים כי הוא
 * מדויק יותר לניווט; NETWORK_PROVIDER הוא גיבוי כשאין קליטת לוויין.
 */
object LocationHelper {
    fun hasPermission(context: Context): Boolean {
        return context.checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            context.checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    fun lastKnownLatLon(context: Context): Pair<Double, Double>? {
        if (!hasPermission(context)) return null
        return try {
            val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            bestLastKnown(manager, manager.getProviders(true))?.let { it.latitude to it.longitude }
        } catch (e: Exception) {
            null
        }
    }

    /** זרם מיקום חי, לשימוש במסך הניווט. דורש הרשאה שכבר אושרה (hasPermission). */
    fun observeLocation(context: Context, minIntervalMs: Long = 2000L, minDistanceMeters: Float = 5f): Flow<Location> = callbackFlow {
        if (!hasPermission(context)) {
            close()
            return@callbackFlow
        }
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val listener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                trySend(location)
            }

            @Deprecated("Deprecated in Java")
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
            override fun onProviderEnabled(provider: String) {}
            override fun onProviderDisabled(provider: String) {}
        }

        // שני הספקים יחד: קודם נבחר רק GPS כשהוא דולק, ובחניון/מנהרה/תחילת
        // נסיעה בלי קליטת לוויין לא הגיע שום מיקום. מיקום רשת מתקבל רק כשאין
        // קיבוע GPS טרי, כדי שלא "יקפיץ" את הסמן אחורה.
        var lastGpsFixAt = 0L
        var lastSent: Location? = null
        val merging = object : LocationListener by listener {
            override fun onLocationChanged(location: Location) {
                val now = android.os.SystemClock.elapsedRealtime()
                if (location.provider == LocationManager.GPS_PROVIDER) {
                    lastGpsFixAt = now
                } else if (now - lastGpsFixAt <= GPS_STALE_MS || addsNothing(location, lastSent)) {
                    return
                }
                lastSent = location
                trySend(location)
            }
        }
        val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
            .filter { runCatching { manager.isProviderEnabled(it) }.getOrDefault(false) }

        if (providers.isEmpty()) {
            close()
            return@callbackFlow
        }

        try {
            providers.forEach { manager.requestLocationUpdates(it, minIntervalMs, minDistanceMeters, merging, android.os.Looper.getMainLooper()) }
            bestLastKnown(manager, providers)?.let { lastSent = it; trySend(it) }
        } catch (e: SecurityException) {
            close(e)
        }

        awaitClose { manager.removeUpdates(merging) }
    }

    /**
     * המיקום האחרון הכי מדויק מבין הספקים (ולא פשוט של הספק הראשון ברשימה): מיקום
     * רשת יכול לסטות בקילומטרים, ובחירה לפי סדר הספקים הקפיצה את המיקום בין שתי נקודות.
     */
    private fun bestLastKnown(manager: LocationManager, providers: List<String>): Location? =
        providers
            .mapNotNull { runCatching { manager.getLastKnownLocation(it) }.getOrNull() }
            // מיקום ישן (מכמה שעות אחורה) לא מתאים כנקודת מוצא של מסלול - מדלגים עליו.
            .filter { ageMs(it) <= MAX_LAST_KNOWN_AGE_MS }
            .minByOrNull { if (it.hasAccuracy()) it.accuracy else Float.MAX_VALUE }

    /**
     * מיקום רשת גס שהמיקום המדויק הקודם נמצא בתוך עיגול הדיוק שלו לא מוסיף מידע -
     * רק "מקפיץ" את הסמן ואת התחנות הקרובות קילומטרים הצידה. מתעלמים ממנו כל עוד
     * המיקום הקודם טרי (חצי שעה, כמו מיקום אחרון שמותר להשתמש בו).
     */
    private fun addsNothing(coarse: Location, previous: Location?): Boolean {
        if (previous == null || !coarse.hasAccuracy() || !previous.hasAccuracy()) return false
        if (coarse.accuracy <= previous.accuracy) return false
        if (ageMs(previous) > COARSE_IGNORE_MS) return false
        return coarse.distanceTo(previous) <= coarse.accuracy
    }

    private fun ageMs(loc: Location): Long =
        (android.os.SystemClock.elapsedRealtimeNanos() - loc.elapsedRealtimeNanos) / 1_000_000L

    private const val COARSE_IGNORE_MS = 30 * 60 * 1000L
    private const val GPS_STALE_MS = 10_000L
    private const val MAX_LAST_KNOWN_AGE_MS = 30 * 60 * 1000L
}
