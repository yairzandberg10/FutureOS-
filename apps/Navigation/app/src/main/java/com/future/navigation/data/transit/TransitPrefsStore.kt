package com.future.navigation.data.transit

import android.content.Context

/** שמירת העדפות התחבורה (מסלול ואמצעי תחבורה) בין הפעלות. */
object TransitPrefsStore {
    private const val PREFS = "transit_prefs"

    fun load(context: Context): TransitPrefs {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val preference = runCatching {
            RoutePreference.valueOf(prefs.getString("preference", "").orEmpty())
        }.getOrDefault(RoutePreference.RECOMMENDED)
        return TransitPrefs(
            preference = preference,
            bus = prefs.getBoolean("bus", true),
            rail = prefs.getBoolean("rail", true),
            lightRail = prefs.getBoolean("light_rail", true),
            shareTaxi = prefs.getBoolean("share_taxi", true),
        )
    }

    fun save(context: Context, value: TransitPrefs) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString("preference", value.preference.name)
            .putBoolean("bus", value.bus)
            .putBoolean("rail", value.rail)
            .putBoolean("light_rail", value.lightRail)
            .putBoolean("share_taxi", value.shareTaxi)
            .apply()
    }
}
