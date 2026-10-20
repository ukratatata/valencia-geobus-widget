package com.valencia.geobus.widget

import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

/**
 * Claves de DataStore Preferences utilizadas por Jetpack Glance para persistir
 * el estado del widget sin requerir polling en segundo plano.
 */
object GeoBusWidgetKeys {
    val KEY_STATE = stringPreferencesKey("widget_state")
    val KEY_ARRIVALS_JSON = stringPreferencesKey("arrivals_json")
    val KEY_LAST_UPDATED = stringPreferencesKey("last_updated")
    val KEY_CLOSEST_STOP_NAME = stringPreferencesKey("closest_stop_name")
    val KEY_CLOSEST_DISTANCE = intPreferencesKey("closest_distance")
    val KEY_ERROR_MESSAGE = stringPreferencesKey("error_message")

    const val STATE_LOADING = "LOADING"
    const val STATE_SUCCESS = "SUCCESS"
    const val STATE_INACTIVE = "INACTIVE"
    const val STATE_NO_STOPS = "NO_STOPS"
    const val STATE_PERMISSION_REQUIRED = "PERMISSION_REQUIRED"
    const val STATE_ERROR = "ERROR"
}
