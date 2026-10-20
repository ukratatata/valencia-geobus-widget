package com.valencia.geobus.data.model

/**
 * Representa la información consolidada de una línea en una parada específica,
 * incluyendo los 2 próximos autobuses estimados, estado de desvío y distancia del usuario.
 */
data class LineArrivals(
    val stopId: Int,
    val stopName: String,
    val lineId: String,
    val distanceMeters: Int,
    val firstBus: BusArrivalDto?,
    val secondBus: BusArrivalDto?,
    val isStopDeviated: Boolean = false,
    val deviationNotice: String? = null
) {
    /**
     * Indica si la línea o parada tiene aviso de desvío.
     */
    val hasDeviation: Boolean
        get() = isStopDeviated || (firstBus?.isDeviated == true) || (secondBus?.isDeviated == true)

    /**
     * Devuelve una cadena legible para la fila secundaria (ej. "• 11 min" o vacía).
     */
    val secondBusFormatted: String
        get() = secondBus?.let { "• ${it.timeText}" } ?: ""

    /**
     * Devuelve el texto principal del primer autobús (ej. "2 min." o "Desviada").
     */
    val firstBusText: String
        get() = when {
            hasDeviation -> deviationNotice ?: firstBus?.timeText ?: "Desviada"
            firstBus != null -> firstBus.timeText
            else -> "Sin datos"
        }
}
