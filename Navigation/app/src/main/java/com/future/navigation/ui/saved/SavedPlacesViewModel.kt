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

    private val _searching = MutableStateFlow(false)
    val searching = _searching.asStateFlow()

    /** true כשהחיפוש האחרון נכשל (אין רשת) - מוצג בנפרד מ"לא נמצאו תוצאות" (F30). */
    private val _searchFailed = MutableStateFlow(false)
    val searchFailed = _searchFailed.asStateFlow()

    private var searchJob: Job? = null

    /** העריכה האחרונה - כדי שאחרי החזרה מחיפוש הפוקוס ישוב לאריח שנערך. */
    private var lastEditedSlot = EditingSlot.NONE

    /** מחזיר את העריכה האחרונה ומאפס אותה (נקראת פעם אחת בחזרה למסך). */
    fun takeLastEditedSlot(): EditingSlot {
        val slot = lastEditedSlot
        lastEditedSlot = EditingSlot.NONE
        return slot
    }

    fun startEditing(slot: EditingSlot) {
        // חיפוש שנשאר רץ מהעריכה הקודמת לא יכתוב תוצאות לתוך העריכה החדשה
        searchJob?.cancel()
        lastEditedSlot = slot
        _editingSlot.value = slot
        _searchQuery.value = ""
        _searchResults.value = emptyList()
        _searching.value = false
        _searchFailed.value = false
    }

    fun cancelEditing() {
        searchJob?.cancel()
        _searching.value = false
        _editingSlot.value = EditingSlot.NONE
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
        searchJob?.cancel()
        _searchFailed.value = false
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            _searching.value = false
            return
        }
        _searching.value = true
        searchJob = viewModelScope.launch {
            delay(500)
            val outcome = try {
                geocodingRepository.search(query) to false
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                emptyList<GeocodeResult>() to true
            }
            _searchResults.value = outcome.first
            _searchFailed.value = outcome.second
            _searching.value = false
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

    /** F27: מנקה את אריח הבית או העבודה. */
    fun clearSlot(slot: EditingSlot) {
        viewModelScope.launch {
            when (slot) {
                EditingSlot.HOME -> repository.clearHome()
                EditingSlot.WORK -> repository.clearWork()
                else -> {}
            }
        }
    }

    fun toggleFavorite(place: SavedPlaceEntity) {
        viewModelScope.launch { repository.toggleFavorite(place) }
    }

    fun delete(place: SavedPlaceEntity) {
        viewModelScope.launch { repository.delete(place) }
    }
}
