package com.future.navigation.ui.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.future.navigation.data.common.LatLng
import com.future.navigation.data.common.distanceMetersTo
import com.future.navigation.data.geocoding.GeocodeResult
import com.future.navigation.data.geocoding.GeocodingRepository
import com.future.navigation.data.gtfs.GtfsDao
import com.future.navigation.data.gtfs.StopEntity
import com.future.navigation.data.location.LocationHelper
import com.future.navigation.data.places.SavedPlaceRepository
import com.future.navigation.ui.navigation.TravelMode
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class NearbyStopUi(val stop: StopEntity, val distanceMeters: Double)

class HomeViewModel(
    private val appContext: Context,
    private val savedPlaceRepository: SavedPlaceRepository,
    private val gtfsDao: GtfsDao,
    private val geocodingRepository: GeocodingRepository
) : ViewModel() {

    private val _mode = MutableStateFlow(TravelMode.DRIVE)
    val mode = _mode.asStateFlow()

    private val _currentLocation = MutableStateFlow<LatLng?>(null)
    val currentLocation = _currentLocation.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow<List<GeocodeResult>>(emptyList())
    val searchResults = _searchResults.asStateFlow()

    private val _searching = MutableStateFlow(false)
    val searching = _searching.asStateFlow()

    val homePlace = savedPlaceRepository.homePlace
    val workPlace = savedPlaceRepository.workPlace

    val nearbyStops = _currentLocation
        .flatMapLatest { location ->
            if (location == null) {
                flowOf(emptyList())
            } else {
                val radiusDeg = NEARBY_STOP_RADIUS_METERS / 111_320.0
                gtfsDao.nearbyStops(
                    minLat = location.lat - radiusDeg, maxLat = location.lat + radiusDeg,
                    minLon = location.lon - radiusDeg, maxLon = location.lon + radiusDeg
                ).map { stops ->
                    stops
                        .map { NearbyStopUi(it, location.distanceMetersTo(LatLng(it.lat, it.lon))) }
                        .filter { it.distanceMeters <= NEARBY_STOP_RADIUS_METERS }
                        .sortedBy { it.distanceMeters }
                        .take(6)
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var searchJob: Job? = null

    init {
        refreshLocation()
    }

    fun refreshLocation() {
        _currentLocation.value = LocationHelper.lastKnownLatLon(appContext)?.let { LatLng(it.first, it.second) }
    }

    fun setMode(mode: TravelMode) {
        _mode.value = mode
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
        searchJob?.cancel()
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            _searching.value = false
            return
        }
        searchJob = viewModelScope.launch {
            _searching.value = true
            delay(SEARCH_DEBOUNCE_MS)
            _searchResults.value = geocodingRepository.search(query)
            _searching.value = false
        }
    }

    companion object {
        private const val NEARBY_STOP_RADIUS_METERS = 700.0
        private const val SEARCH_DEBOUNCE_MS = 500L
    }
}
