package com.future.navigation.data.places

import com.future.navigation.data.gtfs.SavedPlaceDao
import com.future.navigation.data.gtfs.SavedPlaceEntity
import kotlinx.coroutines.flow.Flow

/** עטיפה דקה מעל ה-DAO, כמו NoteRepository ב-notes/ - ללא לוגיקה נוספת. */
class SavedPlaceRepository(private val dao: SavedPlaceDao) {
    val allPlaces: Flow<List<SavedPlaceEntity>> = dao.allPlaces()
    val homePlace: Flow<SavedPlaceEntity?> = dao.homePlace()
    val workPlace: Flow<SavedPlaceEntity?> = dao.workPlace()

    // בית/עבודה: השורה הקודמת מנוקה ואחריה נמחקות שורות שלא משמשות לכלום (F27),
    // כך שלא נשארות שורות יתומות ב-DB.
    suspend fun setHome(label: String, address: String, lat: Double, lon: Double) {
        dao.clearHome()
        dao.deleteOrphans()
        dao.upsert(SavedPlaceEntity(label = label, address = address, lat = lat, lon = lon, isHome = true))
    }

    suspend fun setWork(label: String, address: String, lat: Double, lon: Double) {
        dao.clearWork()
        dao.deleteOrphans()
        dao.upsert(SavedPlaceEntity(label = label, address = address, lat = lat, lon = lon, isWork = true))
    }

    /** מועדף לכתובת שכבר קיימת לא נוסף שוב (F27: מניעת כפילויות). */
    suspend fun addFavorite(label: String, address: String, lat: Double, lon: Double) {
        if (dao.favoriteByAddress(address) != null) return
        dao.upsert(SavedPlaceEntity(label = label, address = address, lat = lat, lon = lon, isFavorite = true))
    }

    suspend fun toggleFavorite(place: SavedPlaceEntity) {
        // מועדף שהוסר ולא בשימוש אחר (בית/עבודה) נמחק, כדי שלא יישאר יתום ב-DB (F27).
        if (place.isFavorite && !place.isHome && !place.isWork) dao.delete(place.id)
        else dao.upsert(place.copy(isFavorite = !place.isFavorite))
    }

    suspend fun delete(place: SavedPlaceEntity) = dao.delete(place.id)
}
