package com.valencia.geobus.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.valencia.geobus.data.local.dao.EmtCatalogDao
import com.valencia.geobus.data.local.dao.UserMonitoredStopDao
import com.valencia.geobus.data.local.entity.EmtStopCatalogEntity
import com.valencia.geobus.data.local.entity.UserMonitoredStopEntity

@Database(
    entities = [
        EmtStopCatalogEntity::class,
        UserMonitoredStopEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun emtCatalogDao(): EmtCatalogDao
    abstract fun userMonitoredStopDao(): UserMonitoredStopDao

    companion object {
        private const val DATABASE_NAME = "emt_geobus.db"

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
