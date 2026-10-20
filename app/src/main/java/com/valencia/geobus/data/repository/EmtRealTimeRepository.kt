package com.valencia.geobus.data.repository

import android.util.Log
import com.valencia.geobus.data.model.BusArrivalDto
import com.valencia.geobus.data.model.LineArrivals
import com.valencia.geobus.data.remote.EmtApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.regex.Pattern

/**
 * Repositorio de tiempos en tiempo real para EMT Valencia.
 * Extrae los 2 próximos autobuses por línea y detecta posibles desvíos o incidencias.
 */
class EmtRealTimeRepository(
    private val apiClient: EmtApiClient = EmtApiClient()
) {
    companion object {
        private const val TAG = "EmtRealTimeRepo"

        // Regex validada en Fase 0 contra el endpoint real de la EMT
        private val ARRIVAL_PATTERN: Pattern = Pattern.compile(
            "title=\"([^\"]+)\"[^>]*>.*?<span class=\"llegadaHome\">\\s*(&nbsp;)*\\s*([^-<]+)-\\s*([^<]+)</span>",
            Pattern.DOTALL or Pattern.CASE_INSENSITIVE
        )

        // Patrón para detectar avisos de desvío general o paradas suprimidas en el HTML
        private val DEVIATION_PATTERN: Pattern = Pattern.compile(
            "(desv[ií]o|desviad[ao]|suprimid[ao]|sin\\s+servicio|obras)",
            Pattern.CASE_INSENSITIVE
        )
    }

    /**
     * Obtiene y parsea los tiempos reales para una parada y filtra por las líneas solicitadas.
     * Retorna exactamente los 2 próximos autobuses por línea y si hay desvíos reportados.
     */
    suspend fun getArrivalsForStop(
        stopId: Int,
        stopName: String,
        targetLines: List<String>,
        distanceMeters: Int
    ): List<LineArrivals> = withContext(Dispatchers.IO) {
        try {
            val html = apiClient.fetchStopRealTimeHtml(stopId)
            parseArrivals(
                stopId = stopId,
                stopName = stopName,
                html = html,
                targetLines = targetLines,
                distanceMeters = distanceMeters
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error obteniendo tiempos reales para parada $stopId: ${e.message}")
            // Retorna un elemento informativo de error/sin datos para las líneas solicitadas
            targetLines.map { lineId ->
                LineArrivals(
                    stopId = stopId,
                    stopName = stopName,
                    lineId = lineId,
                    distanceMeters = distanceMeters,
                    firstBus = BusArrivalDto.create(lineId, "Sin conexión", "Reintentar"),
                    secondBus = null,
                    isStopDeviated = false
                )
            }
        }
    }

    /**
     * Parsea el HTML recibido de busca_parada.php y extrae hasta 2 autobuses por línea.
     */
    fun parseArrivals(
        stopId: Int,
        stopName: String,
        html: String,
        targetLines: List<String>,
        distanceMeters: Int
    ): List<LineArrivals> {
        val matcher = ARRIVAL_PATTERN.matcher(html)
        val rawArrivalsByLine = mutableMapOf<String, MutableList<Pair<String, String>>>()

        while (matcher.find()) {
            val lineId = matcher.group(1)?.trim() ?: continue
            val destination = matcher.group(3)?.trim() ?: ""
            val timeText = matcher.group(4)?.trim() ?: ""

            val list = rawArrivalsByLine.getOrPut(lineId) { mutableListOf() }
            list.add(Pair(destination, timeText))
        }

        // Comprobar si hay avisos de desvío globales en el texto
        val hasGlobalDeviation = DEVIATION_PATTERN.matcher(html).find()
        val deviationNotice = if (hasGlobalDeviation) "Línea/Parada con desvío" else null

        // Filtrar por las líneas configuradas (o todas si no se especificaron)
        val linesToProcess = if (targetLines.isNotEmpty()) {
            targetLines
        } else {
            rawArrivalsByLine.keys.toList()
        }

        val resultList = mutableListOf<LineArrivals>()

        for (lineId in linesToProcess) {
            val arrivals = rawArrivalsByLine[lineId]

            if (arrivals.isNullOrEmpty()) {
                // No hay autobuses próximos reportados (ej. horario nocturno o línea desviada)
                resultList.add(
                    LineArrivals(
                        stopId = stopId,
                        stopName = stopName,
                        lineId = lineId,
                        distanceMeters = distanceMeters,
                        firstBus = BusArrivalDto.create(
                            lineId = lineId,
                            destination = "Sin estimación",
                            rawTime = if (hasGlobalDeviation) "Desviada" else "Sin servicio",
                            additionalNotice = deviationNotice
                        ),
                        secondBus = null,
                        isStopDeviated = hasGlobalDeviation,
                        deviationNotice = deviationNotice
                    )
                )
            } else {
                val firstRaw = arrivals[0]
                val firstBus = BusArrivalDto.create(
                    lineId = lineId,
                    destination = firstRaw.first,
                    rawTime = firstRaw.second,
                    additionalNotice = deviationNotice
                )

                val secondBus = if (arrivals.size > 1) {
                    val secondRaw = arrivals[1]
                    BusArrivalDto.create(
                        lineId = lineId,
                        destination = secondRaw.first,
                        rawTime = secondRaw.second,
                        additionalNotice = deviationNotice
                    )
                } else null

                resultList.add(
                    LineArrivals(
                        stopId = stopId,
                        stopName = stopName,
                        lineId = lineId,
                        distanceMeters = distanceMeters,
                        firstBus = firstBus,
                        secondBus = secondBus,
                        isStopDeviated = hasGlobalDeviation,
                        deviationNotice = deviationNotice
                    )
                )
            }
        }

        return resultList
    }
}
