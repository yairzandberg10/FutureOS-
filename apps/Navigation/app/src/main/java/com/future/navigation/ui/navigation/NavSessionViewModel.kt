package com.future.navigation.ui.navigation

import androidx.lifecycle.ViewModel
import com.future.navigation.data.common.LatLng
import com.future.navigation.data.gtfs.TransitItinerary
import com.future.navigation.data.routing.DrivingRoute
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class TravelMode { DRIVE, TRANSIT }

data class Destination(val name: String, val address: String, val location: LatLng)

/**
 * מצב חוצה-מסכים לזרימת "יעד -> בחירת מסלול -> ניווט/פירוט נסיעה". אין
 * מסגרת הזרקת תלויות בפרויקט (כמו בשאר FutureOS) - זהו ViewModel יחיד
 * שנבנה פעם אחת ב-MainActivity ומועבר בין המסכים, במקום להעביר אובייקטים
 * מורכבים (DrivingRoute/TransitItinerary) כארגומנטים סדרתיים ב-Navigation.
 */
class NavSessionViewModel : ViewModel() {
    private val _mode = MutableStateFlow(TravelMode.DRIVE)
    val mode = _mode.asStateFlow()

    private val _destination = MutableStateFlow<Destination?>(null)
    val destination = _destination.asStateFlow()

    private val _selectedDrivingRoute = MutableStateFlow<DrivingRoute?>(null)
    val selectedDrivingRoute = _selectedDrivingRoute.asStateFlow()

    private val _selectedItinerary = MutableStateFlow<TransitItinerary?>(null)
    val selectedItinerary = _selectedItinerary.asStateFlow()

    fun setMode(mode: TravelMode) {
        _mode.value = mode
    }

    fun setDestination(destination: Destination) {
        _destination.value = destination
    }

    fun setSelectedDrivingRoute(route: DrivingRoute) {
        _selectedDrivingRoute.value = route
    }

    fun setSelectedItinerary(itinerary: TransitItinerary) {
        _selectedItinerary.value = itinerary
    }

    fun clearSelection() {
        _selectedDrivingRoute.value = null
        _selectedItinerary.value = null
    }
}
