package com.valencia.geobus.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.valencia.geobus.data.local.dao.UserMonitoredStopDao
import com.valencia.geobus.data.local.entity.UserMonitoredStopEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

sealed interface ProximityResult {
    data class StopsInRange(
        val stopsWithDistance: List<Pair<UserMonitoredStopEntity, Int>>,
        val userLat: Double,
        val userLon: Double
    ) : ProximityResult

    data class OutOfRange(
        val closestStop: UserMonitoredStopEntity?,
        val closestDistanceMeters: Int?,
        val userLat: Double,
        val userLon: Double
    ) : ProximityResult

    data object NoStopsConfigured : ProximityResult
    data object PermissionDenied : ProximityResult
    data class LocationUnavailable(val reason: String) : ProximityResult
}

/**
 * Motor de proximidad GPS para evaluar qué paradas configuradas por el usuario
 * están dentro del radio de activación (<= 500m) en el Samsung Galaxy S24 Ultra.
 */
class ProximityEvaluator(
    private val context: Context,
    private val monitoredDao: UserMonitoredStopDao,
    private val fusedClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)
) {
    companion object {
        private const val TAG = "ProximityEvaluator"
        private const val GPS_TIMEOUT_MS = 6000L
    }

    /**
     * Comprueba si la app tiene permisos de ubicación concedidos.
     */
    fun hasLocationPermission(): Boolean {
        val fineGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarseGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        return fineGranted || coarseGranted
    }

    /**
     * Evalúa la posición actual del usuario respecto a todas las paradas configuradas.
     */
    @SuppressLint("MissingPermission")
    suspend fun evaluateProximity(): ProximityResult = withContext(Dispatchers.IO) {
        if (!hasLocationPermission()) {
            Log.w(TAG, "Permiso de ubicación no concedido")
            return@withContext ProximityResult.PermissionDenied
        }

        val monitoredStops = monitoredDao.getEnabledMonitoredStops()
        if (monitoredStops.isEmpty()) {
            Log.i(TAG, "No hay paradas configuradas en Room")
            return@withContext ProximityResult.NoStopsConfigured
        }

        // Obtener ubicación GPS de alta precisión con timeout defensivo
        val location = fetchCurrentLocation()
            ?: return@withContext ProximityResult.LocationUnavailable("No se pudo obtener la señal GPS")

        val userLat = location.latitude
        val userLon = location.longitude

        val distanceResults = FloatArray(1)
        val stopsWithDistance = mutableListOf<Pair<UserMonitoredStopEntity, Int>>()
        var closestStop: UserMonitoredStopEntity? = null
        var minDistance = Int.MAX_VALUE

        for (stop in monitoredStops) {
            Location.distanceBetween(
                userLat,
                userLon,
                stop.latitude,
                stop.longitude,
                distanceResults
            )
            val distanceMeters = distanceResults[0].toInt()

            if (distanceMeters < minDistance) {
                minDistance = distanceMeters
                closestStop = stop
            }

            if (distanceMeters <= stop.radiusMeters) {
                stopsWithDistance.add(Pair(stop, distanceMeters))
            }
        }

        // Ordenar paradas en rango por distancia de menor a mayor
        stopsWithDistance.sortBy { it.second }

        if (stopsWithDistance.isNotEmpty()) {
            Log.i(TAG, "${stopsWithDistance.size} paradas dentro del radio (<=500m)")
            ProximityResult.StopsInRange(stopsWithDistance, userLat, userLon)
        } else {
            Log.i(TAG, "Fuera de rango. Parada más cercana: ${closestStop?.stopName} a ${minDistance}m")
            ProximityResult.OutOfRange(
                closestStop = closestStop,
                closestDistanceMeters = if (minDistance != Int.MAX_VALUE) minDistance else null,
                userLat = userLat,
                userLon = userLon
            )
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun fetchCurrentLocation(): Location? {
        return try {
            val tokenSource = CancellationTokenSource()
            withTimeoutOrNull(GPS_TIMEOUT_MS) {
                fusedClient.getCurrentLocation(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    tokenSource.token
                ).await()
            } ?: fusedClient.lastLocation.await()
        } catch (e: Exception) {
            Log.e(TAG, "Error consultando FusedLocationProviderClient: ${e.message}")
            try {
                fusedClient.lastLocation.await()
            } catch (_: Exception) {
                null
            }
        }
    }
}
