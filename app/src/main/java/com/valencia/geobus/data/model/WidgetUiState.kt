package com.valencia.geobus.data.model

import java.io.Serializable

/**
 * Estados posibles del Widget en Jetpack Glance (PreferencesGlanceStateDefinition).
 */
sealed interface WidgetUiState : Serializable {

    data object Loading : WidgetUiState

    data class Success(
        val arrivals: List<LineArrivals>,
        val lastUpdatedText: String
    ) : WidgetUiState

    data class Inactive(
        val closestStopName: String? = null,
        val closestDistanceMeters: Int? = null
    ) : WidgetUiState

    data object NoStopsConfigured : WidgetUiState

    data object PermissionRequired : WidgetUiState

    data class Error(
        val message: String
    ) : WidgetUiState
}
