package com.future.navigation.data.places

import com.future.navigation.data.gtfs.SavedPlaceDao
import com.future.navigation.data.gtfs.SavedPlaceEntity
import kotlinx.coroutines.flow.Flow

/** עטיפה דקה מעל ה-DAO, כמו NoteRepository ב-notes/ - ללא לוגיקה נוספת. */
class SavedPlaceRepository(private val dao: SavedPlaceDao) {
    val allPlaces: Flow<List<SavedPlaceEntity>> = dao.allPlaces()
    val homePlace: Flow<SavedPlaceEntity?> = dao.homePlace()
    val workPlace: Flow<SavedPlaceEntity?> = dao.workPlace()

    suspend fun setHome(label: String, address: String, lat: Double, lon: Double) {
        dao.clearHome()
        dao.upsert(SavedPlaceEntity(label = label, address = address, lat = lat, lon = lon, isHome = true))
    }

    suspend fun setWork(label: String, address: String, lat: Double, lon: Double) {
        dao.clearWork()
        dao.upsert(SavedPlaceEntity(label = label, address = address, lat = lat, lon = lon, isWork = true))
    }

    suspend fun addFavorite(label: String, address: String, lat: Double, lon: Double) {
        dao.upsert(SavedPlaceEntity(label = label, address = address, lat = lat, lon = lon, isFavorite = true))
    }

    suspend fun toggleFavorite(place: SavedPlaceEntity) {
        dao.upsert(place.copy(isFavorite = !place.isFavorite))
    }

    suspend fun delete(place: SavedPlaceEntity) = dao.delete(place.id)
}
