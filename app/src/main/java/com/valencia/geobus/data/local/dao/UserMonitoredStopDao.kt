package com.valencia.geobus.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.valencia.geobus.data.local.entity.UserMonitoredStopEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserMonitoredStopDao {

    @Query("SELECT * FROM user_monitored_stops ORDER BY order_index ASC, stop_id ASC")
    fun getAllMonitoredStopsFlow(): Flow<List<UserMonitoredStopEntity>>

    @Query("SELECT * FROM user_monitored_stops WHERE is_enabled = 1 ORDER BY order_index ASC")
    suspend fun getEnabledMonitoredStops(): List<UserMonitoredStopEntity>

    @Query("SELECT * FROM user_monitored_stops WHERE stop_id = :stopId LIMIT 1")
    suspend fun getMonitoredStopById(stopId: Int): UserMonitoredStopEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(stop: UserMonitoredStopEntity)

    @Update
    suspend fun update(stop: UserMonitoredStopEntity)

    @Query("UPDATE user_monitored_stops SET is_enabled = :isEnabled WHERE stop_id = :stopId")
    suspend fun updateEnabled(stopId: Int, isEnabled: Boolean)

    @Query("DELETE FROM user_monitored_stops WHERE stop_id = :stopId")
    suspend fun deleteByStopId(stopId: Int)

    @Delete
    suspend fun delete(stop: UserMonitoredStopEntity)

    @Query("SELECT COUNT(*) FROM user_monitored_stops")
    suspend fun getMonitoredStopsCount(): Int
}
