package com.future.navigation.ui.navigation

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object RouteOptions : Screen("route_options")
    object Navigate : Screen("navigate")
    object Transit : Screen("transit")
    object SavedPlaces : Screen("saved_places")
    object GtfsSetup : Screen("gtfs_setup")
    object Station : Screen("station")
    object OriginPicker : Screen("origin_picker")
    /** חיפוש קו לפי מספר, עם קווים אחרונים ומועדפים. */
    object LineSearch : Screen("lines")
    /** "הוספת מקום מועדף" ממסך הבית (ר' ui/home/AddFavoritePlaceScreen). */
    object AddFavorite : Screen("add_favorite")
    /** הגדרות מפה: תצוגה אוטומטי / בהירה / כהה (ר' ui/settings/MapSettingsScreen). */
    object MapSettings : Screen("map_settings")
    /** כל התחנות של נסיעה אחת (קו) עם השעות. */
    object Trip : Screen("trip/{tripId}/{line}/{stop}") {
        fun path(tripId: String, line: String, stop: String): String =
            "trip/${android.net.Uri.encode(tripId)}/${android.net.Uri.encode(line.ifBlank { "-" })}/${android.net.Uri.encode(stop.ifBlank { "-" })}"
    }
}
