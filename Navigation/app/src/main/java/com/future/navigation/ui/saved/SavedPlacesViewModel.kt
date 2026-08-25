package com.future.navigation.ui.saved

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.future.navigation.data.geocoding.GeocodeResult
import com.future.navigation.data.geocoding.GeocodingRepository
import com.future.navigation.data.gtfs.SavedPlaceEntity
import com.future.navigation.data.places.SavedPlaceRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class EditingSlot { NONE, HOME, WORK, FAVORITE }

class SavedPlacesViewModel(
    private val repository: SavedPlaceRepository,
    private val geocodingRepository: GeocodingRepository
) : ViewModel() {
    val homePlace = repository.homePlace
    val workPlace = repository.workPlace
    val allPlaces = repository.allPlaces

    private val _editingSlot = MutableStateFlow(EditingSlot.NONE)
    val editingSlot = _editingSlot.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow<List<GeocodeResult>>(emptyList())
    val searchResults = _searchResults.asStateFlow()

    private var searchJob: Job? = null

    fun startEditing(slot: EditingSlot) {
        _editingSlot.value = slot
        _searchQuery.value = ""
        _searchResults.value = emptyList()
    }

    fun cancelEditing() {
        _editingSlot.value = EditingSlot.NONE
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
        searchJob?.cancel()
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            return
        }
        searchJob = viewModelScope.launch {
            delay(500)
            _searchResults.value = geocodingRepository.search(query)
        }
    }

    fun pickResult(result: GeocodeResult) {
        viewModelScope.launch {
            when (_editingSlot.value) {
                EditingSlot.HOME -> repository.setHome(label = "בית", address = result.label, lat = result.location.lat, lon = result.location.lon)
                EditingSlot.WORK -> repository.setWork(label = "עבודה", address = result.label, lat = result.location.lat, lon = result.location.lon)
                EditingSlot.FAVORITE -> repository.addFavorite(label = result.label.substringBefore(","), address = result.label, lat = result.location.lat, lon = result.location.lon)
                EditingSlot.NONE -> {}
            }
            _editingSlot.value = EditingSlot.NONE
        }
    }

    fun toggleFavorite(place: SavedPlaceEntity) {
        viewModelScope.launch { repository.toggleFavorite(place) }
    }

    fun delete(place: SavedPlaceEntity) {
        viewModelScope.launch { repository.delete(place) }
    }
}
