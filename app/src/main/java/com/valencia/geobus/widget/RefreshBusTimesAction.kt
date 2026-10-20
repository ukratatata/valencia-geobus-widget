package com.valencia.geobus.widget

import android.content.Context
import android.util.Log
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import com.google.gson.Gson
import com.valencia.geobus.GeoBusApplication
import com.valencia.geobus.data.local.AppDatabase
import com.valencia.geobus.data.model.LineArrivals
import com.valencia.geobus.data.repository.EmtRealTimeRepository
import com.valencia.geobus.location.ProximityEvaluator
import com.valencia.geobus.location.ProximityResult
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * ActionCallback ejecutado al pulsar el botón de refresco en la cabecera del widget.
 * Obtiene la ubicación GPS actual bajo demanda y consulta en paralelo los tiempos
 * reales de las paradas que se encuentren a <= 500m del usuario.
 */
class RefreshBusTimesAction : ActionCallback {

    companion object {
        private const val TAG = "RefreshBusAction"
        private val gson = Gson()
    }

    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        Log.d(TAG, "Botón de actualización pulsado en el Widget.")

        // 1. Mostrar estado de carga inmediato
        setWidgetState(context, glanceId) { prefs ->
            prefs[GeoBusWidgetKeys.KEY_STATE] = GeoBusWidgetKeys.STATE_LOADING
        }

        try {
            val db = AppDatabase.getInstance(context)
            val monitoredDao = db.userMonitoredStopDao()
            val proximityEvaluator = ProximityEvaluator(context, monitoredDao)
            val realTimeRepo = EmtRealTimeRepository()

            when (val proximityResult = proximityEvaluator.evaluateProximity()) {
                is ProximityResult.PermissionDenied -> {
                    setWidgetState(context, glanceId) { prefs ->
                        prefs[GeoBusWidgetKeys.KEY_STATE] = GeoBusWidgetKeys.STATE_PERMISSION_REQUIRED
                    }
                }

                is ProximityResult.NoStopsConfigured -> {
                    setWidgetState(context, glanceId) { prefs ->
                        prefs[GeoBusWidgetKeys.KEY_STATE] = GeoBusWidgetKeys.STATE_NO_STOPS
                    }
                }

                is ProximityResult.OutOfRange -> {
                    // Cero consultas HTTP a la API cuando está fuera de rango (>500m)
                    setWidgetState(context, glanceId) { prefs ->
                        prefs[GeoBusWidgetKeys.KEY_STATE] = GeoBusWidgetKeys.STATE_INACTIVE
                        prefs[GeoBusWidgetKeys.KEY_CLOSEST_STOP_NAME] =
                            proximityResult.closestStop?.stopName ?: ""
                        if (proximityResult.closestDistanceMeters != null) {
                            prefs[GeoBusWidgetKeys.KEY_CLOSEST_DISTANCE] =
                                proximityResult.closestDistanceMeters
                        }
                    }
                }

                is ProximityResult.LocationUnavailable -> {
                    setWidgetState(context, glanceId) { prefs ->
                        prefs[GeoBusWidgetKeys.KEY_STATE] = GeoBusWidgetKeys.STATE_ERROR
                        prefs[GeoBusWidgetKeys.KEY_ERROR_MESSAGE] = proximityResult.reason
                    }
                }

                is ProximityResult.StopsInRange -> {
                    // Consultar en paralelo los tiempos para todas las paradas en rango (<=500m)
                    val allArrivals: List<LineArrivals> = coroutineScope {
                        proximityResult.stopsWithDistance.map { (stop, distance) ->
                            async {
                                realTimeRepo.getArrivalsForStop(
                                    stopId = stop.stopId,
                                    stopName = stop.stopName,
                                    targetLines = stop.monitoredLines,
                                    distanceMeters = distance
                                )
                            }
                        }.awaitAll().flatten()
                    }

                    val timeFormatter = SimpleDateFormat("HH:mm", Locale.getDefault())
                    val currentTimeString = timeFormatter.format(Date())
                    val jsonArrivals = gson.toJson(allArrivals)

                    setWidgetState(context, glanceId) { prefs ->
                        prefs[GeoBusWidgetKeys.KEY_STATE] = GeoBusWidgetKeys.STATE_SUCCESS
                        prefs[GeoBusWidgetKeys.KEY_ARRIVALS_JSON] = jsonArrivals
                        prefs[GeoBusWidgetKeys.KEY_LAST_UPDATED] = currentTimeString
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error actualizando widget: ${e.message}", e)
            setWidgetState(context, glanceId) { prefs ->
                prefs[GeoBusWidgetKeys.KEY_STATE] = GeoBusWidgetKeys.STATE_ERROR
                prefs[GeoBusWidgetKeys.KEY_ERROR_MESSAGE] = e.localizedMessage ?: "Error de conexión"
            }
        }
    }

    private suspend fun setWidgetState(
        context: Context,
        glanceId: GlanceId,
        updateBlock: (androidx.datastore.preferences.core.MutablePreferences) -> Unit
    ) {
        updateAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId) { prefs ->
            prefs.toMutablePreferences().apply(updateBlock)
        }
        GeoBusGlanceWidget().update(context, glanceId)
    }
}
