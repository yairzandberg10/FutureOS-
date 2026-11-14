package com.future.navigation.data.gtfs

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [StopEntity::class, RouteEntity::class, TripEntity::class, StopTimeEntity::class, CalendarEntity::class, SavedPlaceEntity::class],
    version = 1,
    exportSchema = false
)
abstract class GtfsDatabase : RoomDatabase() {
    abstract fun gtfsDao(): GtfsDao
    abstract fun savedPlaceDao(): SavedPlaceDao

    companion object {
        @Volatile
        private var instance: GtfsDatabase? = null

        fun getInstance(context: Context): GtfsDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    GtfsDatabase::class.java,
                    "navigation.db"
                ).build().also { instance = it }
            }
        }
    }
}
