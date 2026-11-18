package com.future.navigation.ui.navigation

import androidx.lifecycle.ViewModel
import com.future.navigation.data.common.LatLng
import com.future.navigation.data.gtfs.TransitItinerary
import com.future.navigation.data.routing.DrivingRoute
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** WALK/BIKE נוספו בסוף כדי שהאינדקסים של מסך הבית (TravelMode.entries[i]) לא יזוזו. */
enum class TravelMode { DRIVE, TRANSIT, WALK, BIKE }

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

    /** נקודת המוצא של המסלול. null = המיקום הנוכחי (ברירת המחדל, כמו "יציאה ממיקומך הנוכחי" באגד). */
    private val _origin = MutableStateFlow<Destination?>(null)
    val origin = _origin.asStateFlow()

    fun setOrigin(origin: Destination?) {
        _origin.value = origin
    }

    /**
     * החלפת כיוון: היעד נהיה המוצא והמוצא נהיה היעד. כשהמוצא הוא המיקום הנוכחי,
     * היעד החדש הוא המיקום הנוכחי (current), כמו כפתור ההחלפה באגד.
     */
    fun swapOriginAndDestination(current: LatLng?) {
        val dest = _destination.value ?: return
        val newDestination = _origin.value
            ?: current?.let { Destination(name = "המיקום שלי", address = "המיקום שלי", location = it) }
            ?: return
        _origin.value = dest
        _destination.value = newDestination
    }

    private val _selectedDrivingRoute = MutableStateFlow<DrivingRoute?>(null)
    val selectedDrivingRoute = _selectedDrivingRoute.asStateFlow()

    /** התחנה שנפתחה במסך לוח היציאות (מחיפוש או מ"תחנות קרובות"). */
    private val _selectedStation = MutableStateFlow<Destination?>(null)
    val selectedStation = _selectedStation.asStateFlow()

    fun setSelectedStation(station: Destination) {
        _selectedStation.value = station
    }

    private val _selectedItinerary = MutableStateFlow<TransitItinerary?>(null)
    val selectedItinerary = _selectedItinerary.asStateFlow()

    fun setMode(mode: TravelMode) {
        _mode.value = mode
    }

    /** יעד חדש מתחיל תמיד מהמיקום הנוכחי - מוצא שנבחר לנסיעה קודמת לא "נדבק" לנסיעה הבאה. */
    fun setDestination(destination: Destination) {
        _destination.value = destination
        _origin.value = null
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
