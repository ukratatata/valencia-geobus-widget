package com.valencia.geobus.data.model

/**
 * Representa una estimación individual de llegada para un autobús en una parada.
 * Incluye soporte para avisos de desvío e incidencias de línea.
 */
data class BusArrivalDto(
    val lineId: String,
    val destination: String,
    val timeText: String,
    val minutes: Int? = null,
    val isImminent: Boolean = false,
    val isDeviated: Boolean = false,
    val notice: String? = null
) {
    companion object {
        fun create(
            lineId: String,
            destination: String,
            rawTime: String,
            additionalNotice: String? = null
        ): BusArrivalDto {
            val cleanTime = rawTime.trim()
            val lower = cleanTime.lowercase()
            val noticeLower = additionalNotice?.lowercase() ?: ""

            val isDeviated = lower.contains("desvi") ||
                    lower.contains("alterad") ||
                    noticeLower.contains("desvi") ||
                    noticeLower.contains("obras") ||
                    lower.contains("sin servicio")

            val minutes = when {
                lower.contains("próximo") || lower.contains("proximo") || lower.contains("en parada") -> 0
                isDeviated -> null
                else -> {
                    val digits = cleanTime.filter { it.isDigit() }
                    digits.toIntOrNull()
                }
            }

            val isImminent = when {
                isDeviated -> false
                minutes != null && minutes <= 3 -> true
                lower.contains("próximo") || lower.contains("proximo") || lower.contains("en parada") -> true
                else -> false
            }

            val finalNotice = when {
                isDeviated && additionalNotice == null -> "Línea con desvío"
                else -> additionalNotice
            }

            return BusArrivalDto(
                lineId = lineId,
                destination = destination,
                timeText = if (isDeviated && cleanTime.isBlank()) "Desviada" else cleanTime,
                minutes = minutes,
                isImminent = isImminent,
                isDeviated = isDeviated,
                notice = finalNotice
            )
        }
    }
}
