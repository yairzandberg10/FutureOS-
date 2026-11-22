package com.future.navigation.data.location

import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
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
            val system = bestLastKnown(manager, manager.getProviders(true))
            // מיקום חי שהאפליקציה עצמה קיבלה (מסך הבית/ניווט) טרי יותר מה-lastKnown של המערכת
            val best = listOfNotNull(system, latest?.takeIf { ageMs(it) <= MAX_LAST_KNOWN_AGE_MS })
                .minByOrNull { ageMs(it) }
            best?.let { it.latitude to it.longitude }
        } catch (e: Exception) {
            null
        }
    }

    /** המיקום האחרון שהאפליקציה קיבלה בפועל (זרם חי או בקשה טרייה). */
    @Volatile
    private var latest: Location? = null

    private fun remember(location: Location) {
        val prev = latest
        if (prev == null || location.elapsedRealtimeNanos >= prev.elapsedRealtimeNanos) latest = location
    }

    /**
     * מיקום אמיתי עכשיו - לנקודת המוצא של מסלול. ה-lastKnown של המערכת יכול להיות מלפני
     * חצי שעה (ממקום אחר לגמרי) כי אף אחד לא ביקש מיקום מאז, ולכן מבקשים קיבוע טרי:
     * GPS עד [gpsTimeoutMs], ואם אין לוויין - מיקום רשת, ורק אם אין כלום - lastKnown.
     * מיקום חי טרי (עד 20 שנ') שכבר יש - מוחזר מיד.
     */
    suspend fun currentLatLon(context: Context, gpsTimeoutMs: Long = 8_000L): Pair<Double, Double>? {
        if (!hasPermission(context)) return null
        latest?.takeIf { ageMs(it) <= LIVE_FRESH_MS && it.provider == LocationManager.GPS_PROVIDER }
            ?.let { return it.latitude to it.longitude }
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val enabled = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
            .filter { runCatching { manager.isProviderEnabled(it) }.getOrDefault(false) }
        var network: Location? = null
        val gps = kotlinx.coroutines.coroutineScope {
            val net = if (LocationManager.NETWORK_PROVIDER in enabled) {
                kotlinx.coroutines.async { requestOne(context, manager, LocationManager.NETWORK_PROVIDER, gpsTimeoutMs) }
            } else null
            val g = if (LocationManager.GPS_PROVIDER in enabled) {
                requestOne(context, manager, LocationManager.GPS_PROVIDER, gpsTimeoutMs)
            } else null
            network = net?.await()
            g
        }
        val fresh = gps ?: network
        fresh?.let { remember(it); return it.latitude to it.longitude }
        return lastKnownLatLon(context)
    }

    private suspend fun requestOne(context: Context, manager: LocationManager, provider: String, timeoutMs: Long): Location? =
        withTimeoutOrNull(timeoutMs) {
            suspendCancellableCoroutine<Location?> { cont ->
                val signal = android.os.CancellationSignal()
                cont.invokeOnCancellation { runCatching { signal.cancel() } }
                try {
                    manager.getCurrentLocation(provider, signal, context.mainExecutor) { loc ->
                        if (cont.isActive) cont.resumeWith(Result.success(loc))
                    }
                } catch (e: Exception) {
                    if (cont.isActive) cont.resumeWith(Result.success(null))
                }
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
        var lastGps: Location? = null
        var lastSent: Location? = null
        // GPS שעומד במקום לא שולח עדכונים (minDistance), אז "טרי" נמדד ביחס למרווח הבקשה
        val gpsStaleMs = maxOf(GPS_STALE_MS, minIntervalMs * 3)
        val merging = object : LocationListener by listener {
            override fun onLocationChanged(location: Location) {
                val now = android.os.SystemClock.elapsedRealtime()
                if (location.provider == LocationManager.GPS_PROVIDER) {
                    lastGpsFixAt = now
                    lastGps = location
                } else if (lastSent != null && ageMs(lastSent!!) > COARSE_IGNORE_MS) {
                    // המיקום הקודם ישן (lastKnown ממקום אחר) - מיקום רשת טרי עדיף עליו
                } else if (now - lastGpsFixAt <= gpsStaleMs ||
                    addsNothing(location, lastSent) ||
                    impossibleJump(location, lastGps)
                ) {
                    return
                }
                lastSent = location
                remember(location)
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
            bestLastKnown(manager, providers)?.let {
                lastSent = it
                if (it.provider == LocationManager.GPS_PROVIDER) lastGps = it
                trySend(it)
            }
        } catch (e: SecurityException) {
            close(e)
        }

        awaitClose { manager.removeUpdates(merging) }
    }

    /**
     * המיקום האחרון הכי מדויק מבין הספקים (ולא פשוט של הספק הראשון ברשימה): מיקום
     * רשת יכול לסטות בקילומטרים, ובחירה לפי סדר הספקים הקפיצה את המיקום בין שתי נקודות.
     */
    private fun bestLastKnown(manager: LocationManager, providers: List<String>): Location? {
        val fixes = providers
            .mapNotNull { runCatching { manager.getLastKnownLocation(it) }.getOrNull() }
            // מיקום ישן (מכמה שעות אחורה) לא מתאים כנקודת מוצא של מסלול - מדלגים עליו.
            .filter { ageMs(it) <= MAX_LAST_KNOWN_AGE_MS }
        val freshest = fixes.minByOrNull { ageMs(it) } ?: return null
        // מיקום מדויק יותר מנצח רק אם הוא כמעט טרי באותה מידה. בטלפון קיבוע GPS מלפני
        // רבע שעה (מעלה לבונה) ניצח מיקום טרי (עלי), והמסלול התחיל 2 ק"מ מהמשתמש.
        return fixes
            .filter { ageMs(it) - ageMs(freshest) <= SAME_TIME_WINDOW_MS }
            .minByOrNull { if (it.hasAccuracy()) it.accuracy else Float.MAX_VALUE }
            ?: freshest
    }

    private const val SAME_TIME_WINDOW_MS = 2 * 60 * 1000L

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

    /**
     * מיקום רשת שמחייב מהירות בלתי אפשרית מאז קיבוע ה-GPS האחרון (יותר מ-180 קמ"ש)
     * הוא מיקום רשת שגוי (אנטנה/Wi-Fi רחוקים) - בטלפון הוא הקפיץ את המיקום 2 ק"מ הלוך ושוב.
     */
    private fun impossibleJump(network: Location, gps: Location?): Boolean {
        if (gps == null) return false
        val seconds = ((network.elapsedRealtimeNanos - gps.elapsedRealtimeNanos) / 1_000_000_000.0).coerceAtLeast(1.0)
        return network.distanceTo(gps) / seconds > MAX_PLAUSIBLE_SPEED_MPS
    }

    private const val MAX_PLAUSIBLE_SPEED_MPS = 50.0

    private fun ageMs(loc: Location): Long =
        (android.os.SystemClock.elapsedRealtimeNanos() - loc.elapsedRealtimeNanos) / 1_000_000L

    private const val COARSE_IGNORE_MS = 2 * 60 * 1000L
    private const val LIVE_FRESH_MS = 20_000L
    private const val GPS_STALE_MS = 10_000L
    private const val MAX_LAST_KNOWN_AGE_MS = 30 * 60 * 1000L
}
