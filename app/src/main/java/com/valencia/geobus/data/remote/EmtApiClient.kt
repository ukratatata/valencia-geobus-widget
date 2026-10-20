package com.valencia.geobus.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Cliente HTTP ligero basado en OkHttp optimizado para Android 16.
 * Diseñado para consultas rápidas sin sobrecarga de memoria.
 */
class EmtApiClient(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()
) {

    companion object {
        private const val USER_AGENT = "ValènciaGeoBus/1.0 (Android 16; OneUI 8.5; SM-S928B)"
        
        // Endpoint oficial de Geoportal Valencia con las 1.155 paradas en GeoJSON WGS84
        private const val CATALOG_URL =
            "https://geoportal.valencia.es/server/rest/services/OPENDATA/Trafico/MapServer/226/query?where=1=1&outFields=id_parada,denominacion,lineas,suprimida&outSR=4326&f=geojson"

        // Endpoint en tiempo real de EMT Valencia
        private const val REALTIME_URL_BASE =
            "https://www.emtvalencia.es/ciudadano/modules/mod_tiempo/busca_parada.php"
    }

    /**
     * Descarga el catálogo completo de paradas desde el servidor OpenData/Geoportal.
     */
    suspend fun fetchCatalogGeoJson(): String = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(CATALOG_URL)
            .header("User-Agent", USER_AGENT)
            .header("Accept", "application/json")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("Error HTTP ${response.code} descargando catálogo de paradas")
            }
            response.body?.string() ?: throw IOException("Cuerpo de respuesta vacío en catálogo")
        }
    }

    /**
     * Obtiene el payload HTML de tiempos reales para una parada específica de EMT Valencia.
     */
    suspend fun fetchStopRealTimeHtml(stopId: Int): String = withContext(Dispatchers.IO) {
        val url = "$REALTIME_URL_BASE?parada=$stopId&adaptados=0&idioma=es"
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", USER_AGENT)
            .header("Referer", "https://www.emtvalencia.es/")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("Error HTTP ${response.code} consultando parada $stopId")
            }
            response.body?.string() ?: ""
        }
    }
}
