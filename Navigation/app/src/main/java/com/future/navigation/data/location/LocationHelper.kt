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
            val providers = manager.getProviders(true)
            for (provider in providers) {
                val loc = manager.getLastKnownLocation(provider)
                if (loc != null) return loc.latitude to loc.longitude
            }
            null
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

        val provider = when {
            manager.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
            manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
            else -> null
        }

        if (provider == null) {
            close()
            return@callbackFlow
        }

        try {
            manager.requestLocationUpdates(provider, minIntervalMs, minDistanceMeters, listener)
            manager.getLastKnownLocation(provider)?.let { trySend(it) }
        } catch (e: SecurityException) {
            close(e)
        }

        awaitClose { manager.removeUpdates(listener) }
    }
}
