package com.valencia.geobus.data.repository

import android.content.Context
import android.util.Log
import com.google.gson.JsonParser
import com.valencia.geobus.data.local.dao.EmtCatalogDao
import com.valencia.geobus.data.local.dao.UserMonitoredStopDao
import com.valencia.geobus.data.local.entity.EmtStopCatalogEntity
import com.valencia.geobus.data.remote.EmtApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * Repositorio encargado de sincronizar el catálogo oficial de paradas de la EMT
 * desde Geoportal València hacia la base de datos Room, y de propagar actualizaciones
 * de ubicación (por si las paradas han cambiado de sitio) a las paradas del usuario.
 */
class EmtCatalogSyncRepository(
    private val catalogDao: EmtCatalogDao,
    private val monitoredDao: UserMonitoredStopDao,
    private val apiClient: EmtApiClient = EmtApiClient(),
    context: Context? = null
) {
    private val prefs = context?.getSharedPreferences("emt_catalog_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val TAG = "EmtCatalogSync"
        private const val PREF_LAST_SYNC = "last_catalog_sync_timestamp"
    }

    fun getLastSyncTimestamp(): Long {
        return prefs?.getLong(PREF_LAST_SYNC, 0L) ?: 0L
    }

    private fun saveLastSyncTimestamp(timestamp: Long) {
        prefs?.edit()?.putLong(PREF_LAST_SYNC, timestamp)?.apply()
    }

    /**
     * Sincroniza el catálogo completo de paradas desde Geoportal Valencia hacia Room.
     * Si las coordenadas de alguna parada monitorizada por el usuario han variado,
     * las actualiza automáticamente para garantizar precisión en el radio GPS.
     */
    suspend fun syncCatalog(force: Boolean = false): Result<Int> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Iniciando sincronización del catálogo oficial de paradas...")
            val geoJsonString = apiClient.fetchCatalogGeoJson()

            val root = JsonParser.parseString(geoJsonString).asJsonObject
            val features = root.getAsJsonArray("features") ?: return@withContext Result.failure(
                IllegalStateException("Respuesta GeoJSON sin colección de features")
            )

            val parsedEntities = mutableListOf<EmtStopCatalogEntity>()
            val now = System.currentTimeMillis()

            for (element in features) {
                try {
                    val feature = element.asJsonObject
                    val properties = feature.getAsJsonObject("properties") ?: continue
                    val geometry = feature.getAsJsonObject("geometry") ?: continue
                    val coords = geometry.getAsJsonArray("coordinates") ?: continue

                    val stopId = properties.get("id_parada")?.asInt ?: continue
                    val rawName = properties.get("denominacion")?.asString ?: "Parada $stopId"
                    val rawLines = properties.get("lineas")?.asString ?: ""
                    val isSuppressed = properties.get("suprimida")?.asInt == 1

                    // GeoJSON estándar: [longitud, latitud]
                    val longitude = coords.get(0).asDouble
                    val latitude = coords.get(1).asDouble

                    parsedEntities.add(
                        EmtStopCatalogEntity(
                            stopId = stopId,
                            stopName = cleanStopName(rawName),
                            latitude = latitude,
                            longitude = longitude,
                            lines = rawLines,
                            isSuppressed = isSuppressed,
                            lastSyncTimestamp = now
                        )
                    )
                } catch (e: Exception) {
                    Log.w(TAG, "Error parseando parada individual en GeoJSON: ${e.message}")
                }
            }

            if (parsedEntities.isNotEmpty()) {
                catalogDao.insertAll(parsedEntities)
                saveLastSyncTimestamp(now)
                Log.d(TAG, "Catálogo sincronizado: ${parsedEntities.size} paradas guardadas en Room.")

                // Actualizar paradas del usuario si cambiaron de coordenadas o nombre
                updateMonitoredStopsIfMoved(parsedEntities)
            }

            Result.success(parsedEntities.size)
        } catch (e: Exception) {
            Log.e(TAG, "Error durante la sincronización del catálogo: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Comprueba si las paradas guardadas por el usuario cambiaron de posición o nombre
     * y las sincroniza con los nuevos datos oficiales.
     */
    private suspend fun updateMonitoredStopsIfMoved(catalog: List<EmtStopCatalogEntity>) {
        try {
            val catalogMap = catalog.associateBy { it.stopId }
            val monitoredStops = monitoredDao.getEnabledMonitoredStops()

            for (monitored in monitoredStops) {
                val updatedOfficial = catalogMap[monitored.stopId]
                if (updatedOfficial != null) {
                    val latDiff = Math.abs(updatedOfficial.latitude - monitored.latitude)
                    val lonDiff = Math.abs(updatedOfficial.longitude - monitored.longitude)
                    val nameChanged = updatedOfficial.stopName != monitored.stopName

                    // Si se ha movido más de 1 metro (~0.00001 deg) o cambió de nombre
                    if (latDiff > 0.00001 || lonDiff > 0.00001 || nameChanged) {
                        Log.i(TAG, "Parada ${monitored.stopId} ha cambiado de ubicación/nombre. Actualizando...")
                        monitoredDao.update(
                            monitored.copy(
                                stopName = updatedOfficial.stopName,
                                latitude = updatedOfficial.latitude,
                                longitude = updatedOfficial.longitude
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error comprobando cambios de posición en paradas del usuario: ${e.message}")
        }
    }

    private fun cleanStopName(rawName: String): String {
        // Limpia sufijos redundantes como " (1694)" al final del nombre
        return rawName.replace(Regex("\\s*\\(\\d+\\)\\s*$"), "").trim()
    }

    fun searchStops(query: String): Flow<List<EmtStopCatalogEntity>> {
        return catalogDao.searchStops(query.trim())
    }

    suspend fun getCatalogCount(): Int {
        return catalogDao.getCatalogCount()
    }
}
