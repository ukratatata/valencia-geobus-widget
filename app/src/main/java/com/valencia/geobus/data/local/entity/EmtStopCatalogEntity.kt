package com.valencia.geobus.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entidad Room que almacena el catálogo completo descargado de paradas oficiales
 * de la EMT de Valencia desde OpenData / Geoportal.
 */
@Entity(
    tableName = "emt_stop_catalog",
    indices = [
        Index(value = ["stop_id"], unique = true),
        Index(value = ["stop_name"]),
        Index(value = ["lines"])
    ]
)
data class EmtStopCatalogEntity(
    @PrimaryKey
    @ColumnInfo(name = "stop_id")
    val stopId: Int,

    @ColumnInfo(name = "stop_name")
    val stopName: String,

    @ColumnInfo(name = "latitude")
    val latitude: Double,

    @ColumnInfo(name = "longitude")
    val longitude: Double,

    @ColumnInfo(name = "lines")
    val lines: String,

    @ColumnInfo(name = "is_suppressed")
    val isSuppressed: Boolean = false,

    @ColumnInfo(name = "last_sync_timestamp")
    val lastSyncTimestamp: Long = System.currentTimeMillis()
)
