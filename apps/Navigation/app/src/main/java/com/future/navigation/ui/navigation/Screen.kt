package com.future.navigation.ui.navigation

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object RouteOptions : Screen("route_options")
    object Navigate : Screen("navigate")
    object Transit : Screen("transit")
    object SavedPlaces : Screen("saved_places")
    object GtfsSetup : Screen("gtfs_setup")
}
