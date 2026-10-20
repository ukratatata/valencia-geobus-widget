package com.valencia.geobus.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entidad Room que almacena las paradas y líneas configuradas por el usuario
 * para su monitorización por proximidad GPS (radio por defecto: 500m).
 */
@Entity(
    tableName = "user_monitored_stops",
    indices = [
        Index(value = ["stop_id"], unique = true)
    ]
)
data class UserMonitoredStopEntity(
    @PrimaryKey
    @ColumnInfo(name = "stop_id")
    val stopId: Int,

    @ColumnInfo(name = "stop_name")
    val stopName: String,

    @ColumnInfo(name = "latitude")
    val latitude: Double,

    @ColumnInfo(name = "longitude")
    val longitude: Double,

    /**
     * Lista de líneas seleccionadas por el usuario para esta parada (ej. ["C3", "98"]).
     * Se serializa/deserializa mediante [com.valencia.geobus.data.local.Converters].
     */
    @ColumnInfo(name = "monitored_lines")
    val monitoredLines: List<String>,

    @ColumnInfo(name = "radius_meters")
    val radiusMeters: Int = 500,

    @ColumnInfo(name = "is_enabled")
    val isEnabled: Boolean = true,

    @ColumnInfo(name = "order_index")
    val orderIndex: Int = 0
)
