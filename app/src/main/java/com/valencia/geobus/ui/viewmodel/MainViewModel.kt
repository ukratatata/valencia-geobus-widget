package com.valencia.geobus.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.valencia.geobus.GeoBusApplication
import com.valencia.geobus.data.local.AppDatabase
import com.valencia.geobus.data.local.entity.EmtStopCatalogEntity
import com.valencia.geobus.data.local.entity.UserMonitoredStopEntity
import com.valencia.geobus.data.model.LineArrivals
import com.valencia.geobus.data.repository.EmtCatalogSyncRepository
import com.valencia.geobus.data.repository.EmtRealTimeRepository
import com.valencia.geobus.location.ProximityEvaluator
import com.valencia.geobus.location.ProximityResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class MainUiState(
    val searchQuery: String = "",
    val searchResults: List<EmtStopCatalogEntity> = emptyList(),
    val selectedStop: EmtStopCatalogEntity? = null,
    val selectedLines: Set<String> = emptySet(),
    val isSyncingCatalog: Boolean = false,
    val catalogCount: Int = 0,
    val lastSyncText: String = "",
    val statusMessage: String? = null,
    val isTestingLive: Boolean = false,
    val testResults: List<LineArrivals>? = null
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = (application as GeoBusApplication).database
    private val catalogDao = db.emtCatalogDao()
    private val monitoredDao = db.userMonitoredStopDao()
    private val syncRepository = EmtCatalogSyncRepository(catalogDao, monitoredDao, context = application)
    private val realTimeRepository = EmtRealTimeRepository()
    private val proximityEvaluator = ProximityEvaluator(application, monitoredDao)

    val monitoredStops: StateFlow<List<UserMonitoredStopEntity>> =
        monitoredDao.getAllMonitoredStopsFlow()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private val _searchQueryFlow = MutableStateFlow("")

    init {
        loadCatalogStatus()

        viewModelScope.launch {
            _searchQueryFlow
                .debounce(250)
                .distinctUntilChanged()
                .flatMapLatest { query ->
                    if (query.length < 2) {
                        flowOf(emptyList())
                    } else {
                        catalogDao.searchStops(query)
                    }
                }
                .collect { results ->
                    _uiState.value = _uiState.value.copy(searchResults = results)
                }
        }
    }

    private fun loadCatalogStatus() {
        viewModelScope.launch {
            val count = catalogDao.getCatalogCount()
            val lastSync = syncRepository.getLastSyncTimestamp()
            val lastSyncText = if (lastSync > 0) {
                val diffMinutes = (System.currentTimeMillis() - lastSync) / (1000 * 60)
                when {
                    diffMinutes < 60 -> "Hace $diffMinutes min"
                    diffMinutes < 1440 -> "Hace ${diffMinutes / 60} h"
                    else -> "Hace ${diffMinutes / 1440} días"
                }
            } else {
                "Nunca sincronizado"
            }

            _uiState.value = _uiState.value.copy(
                catalogCount = count,
                lastSyncText = lastSyncText
            )

            // Si el catálogo está vacío en la primera apertura, sincronizar automáticamente
            if (count == 0) {
                syncCatalog()
            }
        }
    }

    /**
     * Sincroniza el catálogo completo de paradas desde Geoportal Valencia (1.155 paradas)
     * y actualiza las ubicaciones de paradas existentes si han variado.
     */
    fun syncCatalog() {
        if (_uiState.value.isSyncingCatalog) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isSyncingCatalog = true,
                statusMessage = "Sincronizando paradas oficiales de València..."
            )

            val result = syncRepository.syncCatalog(force = true)
            if (result.isSuccess) {
                val total = result.getOrDefault(0)
                _uiState.value = _uiState.value.copy(
                    isSyncingCatalog = false,
                    catalogCount = total,
                    lastSyncText = "Hace un momento",
                    statusMessage = "✓ $total paradas oficiales actualizadas con éxito"
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isSyncingCatalog = false,
                    statusMessage = "Error al sincronizar catálogo: ${result.exceptionOrNull()?.message}"
                )
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        _searchQueryFlow.value = query
    }

    fun selectStop(stop: EmtStopCatalogEntity) {
        val lines = stop.lines.split(",").map { it.trim() }.filter { it.isNotBlank() }
        _uiState.value = _uiState.value.copy(
            selectedStop = stop,
            selectedLines = lines.toSet(), // Por defecto todas seleccionadas para conveniencia
            searchResults = emptyList(),
            searchQuery = "${stop.stopId} - ${stop.stopName}"
        )
    }

    fun toggleLineSelection(line: String) {
        val current = _uiState.value.selectedLines.toMutableSet()
        if (current.contains(line)) {
            current.remove(line)
        } else {
            current.add(line)
        }
        _uiState.value = _uiState.value.copy(selectedLines = current)
    }

    fun clearSelection() {
        _uiState.value = _uiState.value.copy(
            selectedStop = null,
            selectedLines = emptySet(),
            searchQuery = "",
            searchResults = emptyList()
        )
    }

    fun saveMonitoredStop() {
        val stop = _uiState.value.selectedStop ?: return
        val lines = _uiState.value.selectedLines.toList()
        if (lines.isEmpty()) {
            _uiState.value = _uiState.value.copy(statusMessage = "Selecciona al menos una línea")
            return
        }

        viewModelScope.launch {
            monitoredDao.insertOrUpdate(
                UserMonitoredStopEntity(
                    stopId = stop.stopId,
                    stopName = stop.stopName,
                    latitude = stop.latitude,
                    longitude = stop.longitude,
                    monitoredLines = lines,
                    radiusMeters = 500,
                    isEnabled = true
                )
            )
            clearSelection()
            _uiState.value = _uiState.value.copy(
                statusMessage = "✓ Parada ${stop.stopId} añadida a tus zonas"
            )
        }
    }

    fun removeMonitoredStop(stopId: Int) {
        viewModelScope.launch {
            monitoredDao.deleteByStopId(stopId)
            _uiState.value = _uiState.value.copy(
                statusMessage = "Parada $stopId eliminada"
            )
        }
    }

    fun toggleStopEnabled(stopId: Int, isEnabled: Boolean) {
        viewModelScope.launch {
            monitoredDao.updateEnabled(stopId, isEnabled)
        }
    }

    /**
     * Prueba el motor de proximidad y lectura de tiempos en directo desde la app
     */
    fun testLiveProximity() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isTestingLive = true,
                statusMessage = "Comprobando ubicación y tiempos..."
            )

            when (val proximity = proximityEvaluator.evaluateProximity()) {
                is ProximityResult.PermissionDenied -> {
                    _uiState.value = _uiState.value.copy(
                        isTestingLive = false,
                        statusMessage = "Permiso de ubicación denegado. Concédelo arriba."
                    )
                }

                is ProximityResult.NoStopsConfigured -> {
                    _uiState.value = _uiState.value.copy(
                        isTestingLive = false,
                        statusMessage = "Añade primero una parada para probar."
                    )
                }

                is ProximityResult.OutOfRange -> {
                    _uiState.value = _uiState.value.copy(
                        isTestingLive = false,
                        testResults = emptyList(),
                        statusMessage = "Estás a >500m. Parada más cercana: ${proximity.closestStop?.stopName} a ${proximity.closestDistanceMeters}m"
                    )
                }

                is ProximityResult.LocationUnavailable -> {
                    _uiState.value = _uiState.value.copy(
                        isTestingLive = false,
                        statusMessage = "GPS no disponible: ${proximity.reason}"
                    )
                }

                is ProximityResult.StopsInRange -> {
                    val arrivals = mutableListOf<LineArrivals>()
                    for ((stop, dist) in proximity.stopsWithDistance) {
                        val result = realTimeRepository.getArrivalsForStop(
                            stopId = stop.stopId,
                            stopName = stop.stopName,
                            targetLines = stop.monitoredLines,
                            distanceMeters = dist
                        )
                        arrivals.addAll(result)
                    }
                    _uiState.value = _uiState.value.copy(
                        isTestingLive = false,
                        testResults = arrivals,
                        statusMessage = "✓ ${arrivals.size} líneas activas encontradas"
                    )
                }
            }
        }
    }

    fun clearStatusMessage() {
        _uiState.value = _uiState.value.copy(statusMessage = null)
    }
}
