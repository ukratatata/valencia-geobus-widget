package com.valencia.geobus.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.valencia.geobus.data.local.entity.EmtStopCatalogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EmtCatalogDao {

    @Query("SELECT * FROM emt_stop_catalog WHERE is_suppressed = 0 ORDER BY stop_id ASC")
    fun getAllStops(): Flow<List<EmtStopCatalogEntity>>

    @Query("SELECT * FROM emt_stop_catalog WHERE stop_id = :stopId LIMIT 1")
    suspend fun getStopById(stopId: Int): EmtStopCatalogEntity?

    @Query("""
        SELECT * FROM emt_stop_catalog 
        WHERE is_suppressed = 0 
          AND (CAST(stop_id AS TEXT) LIKE '%' || :query || '%' OR stop_name LIKE '%' || :query || '%')
        ORDER BY 
          CASE WHEN CAST(stop_id AS TEXT) = :query THEN 0 
               WHEN CAST(stop_id AS TEXT) LIKE :query || '%' THEN 1 
               ELSE 2 END,
          stop_id ASC
        LIMIT 50
    """)
    fun searchStops(query: String): Flow<List<EmtStopCatalogEntity>>

    @Query("SELECT COUNT(*) FROM emt_stop_catalog")
    suspend fun getCatalogCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(stops: List<EmtStopCatalogEntity>)

    @Query("DELETE FROM emt_stop_catalog")
    suspend fun clearCatalog()
}
