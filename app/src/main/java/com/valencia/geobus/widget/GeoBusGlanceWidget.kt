package com.valencia.geobus.widget

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.valencia.geobus.R
import com.valencia.geobus.data.model.LineArrivals
import com.valencia.geobus.ui.MainActivity

/**
 * Widget nativo en Jetpack Glance diseñado siguiendo las directrices estrictas
 * de Samsung One UI 8.5 para el Samsung Galaxy S24 Ultra.
 */
class GeoBusGlanceWidget : GlanceAppWidget() {

    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition

    // Colores oficiales One UI 8.5 & EMT Valencia
    private val colorBgDark = Color(0xFF17171A)
    private val colorCardDark = Color(0xFF242429)
    private val colorEmtRed = Color(0xFFE30613)
    private val colorGreenImminent = Color(0xFF30D158)
    private val colorYellowLater = Color(0xFFFFD60A)
    private val colorTextPrimary = Color(0xFFFFFFFF)
    private val colorTextSecondary = Color(0xFF9E9EA6)
    private val colorTextTertiary = Color(0xFF8E8E93)

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val prefs = currentState<Preferences>()
            val state = prefs[GeoBusWidgetKeys.KEY_STATE] ?: GeoBusWidgetKeys.STATE_INACTIVE
            val lastUpdated = prefs[GeoBusWidgetKeys.KEY_LAST_UPDATED] ?: ""
            val jsonArrivals = prefs[GeoBusWidgetKeys.KEY_ARRIVALS_JSON] ?: "[]"
            val closestStopName = prefs[GeoBusWidgetKeys.KEY_CLOSEST_STOP_NAME]
            val closestDistance = prefs[GeoBusWidgetKeys.KEY_CLOSEST_DISTANCE]
            val errorMessage = prefs[GeoBusWidgetKeys.KEY_ERROR_MESSAGE] ?: "Error"

            val arrivals: List<LineArrivals> = try {
                val listType = object : TypeToken<List<LineArrivals>>() {}.type
                Gson().fromJson(jsonArrivals, listType) ?: emptyList()
            } catch (_: Exception) {
                emptyList()
            }

            // Contenedor Raíz One UI 8.5
            Box(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .cornerRadius(26.dp)
                    .background(colorBgDark)
                    .padding(16.dp)
            ) {
                Column(
                    modifier = GlanceModifier.fillMaxSize()
                ) {
                    // Cabecera del Widget
                    WidgetHeader(
                        lastUpdated = lastUpdated,
                        isLoading = state == GeoBusWidgetKeys.STATE_LOADING
                    )

                    Spacer(modifier = GlanceModifier.height(10.dp))

                    // Cuerpo dinámico según el estado
                    Box(
                        modifier = GlanceModifier
                            .fillMaxWidth()
                            .defaultWeight()
                    ) {
                        when (state) {
                            GeoBusWidgetKeys.STATE_LOADING -> {
                                LoadingStateView()
                            }

                            GeoBusWidgetKeys.STATE_SUCCESS -> {
                                if (arrivals.isNotEmpty()) {
                                    ArrivalsListView(arrivals)
                                } else {
                                    InactiveStateView(closestStopName, closestDistance)
                                }
                            }

                            GeoBusWidgetKeys.STATE_INACTIVE -> {
                                InactiveStateView(closestStopName, closestDistance)
                            }

                            GeoBusWidgetKeys.STATE_NO_STOPS -> {
                                InfoMessageView(
                                    title = "Sin paradas configuradas",
                                    subtitle = "Toca para abrir la app y añadir tus líneas y paradas habituales."
                                )
                            }

                            GeoBusWidgetKeys.STATE_PERMISSION_REQUIRED -> {
                                InfoMessageView(
                                    title = "Permiso de ubicación necesario",
                                    subtitle = "Activa 'Permitir todo el tiempo' en Ajustes para lecturas GPS en el widget."
                                )
                            }

                            GeoBusWidgetKeys.STATE_ERROR -> {
                                InfoMessageView(
                                    title = "Error de conexión",
                                    subtitle = errorMessage
                                )
                            }

                            else -> {
                                InactiveStateView(closestStopName, closestDistance)
                            }
                        }
                    }
                }
            }
        }
    }

    @androidx.compose.runtime.Composable
    private fun WidgetHeader(lastUpdated: String, isLoading: Boolean) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Título e indicador de hora
            Row(
                modifier = GlanceModifier.defaultWeight(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "EMT València",
                    style = TextStyle(
                        color = androidx.glance.unit.ColorProvider(colorTextTertiary),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                )

                if (lastUpdated.isNotBlank() && !isLoading) {
                    Text(
                        text = " • $lastUpdated",
                        style = TextStyle(
                            color = androidx.glance.unit.ColorProvider(colorTextTertiary),
                            fontSize = 11.sp
                        )
                    )
                }
            }

            // Botón de refresco One UI (32.dp con icono de actualización)
            Box(
                modifier = GlanceModifier
                    .size(32.dp)
                    .cornerRadius(16.dp)
                    .background(colorCardDark)
                    .clickable(actionRunCallback<RefreshBusTimesAction>()),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    provider = ImageProvider(R.drawable.ic_refresh),
                    contentDescription = "Actualizar",
                    modifier = GlanceModifier.size(16.dp),
                    colorFilter = ColorFilter.tint(
                        androidx.glance.unit.ColorProvider(
                            if (isLoading) colorGreenImminent else colorTextPrimary
                        )
                    )
                )
            }
        }
    }

    @androidx.compose.runtime.Composable
    private fun ArrivalsListView(arrivals: List<LineArrivals>) {
        LazyColumn(
            modifier = GlanceModifier.fillMaxSize()
        ) {
            items(arrivals) { item ->
                ArrivalRowItem(item)
                Spacer(modifier = GlanceModifier.height(6.dp))
            }
        }
    }

    @androidx.compose.runtime.Composable
    private fun ArrivalRowItem(item: LineArrivals) {
        val isImminent = item.firstBus?.isImminent == true
        val hasDeviation = item.hasDeviation

        val timeColor = when {
            hasDeviation -> colorYellowLater
            isImminent -> colorGreenImminent
            else -> colorTextPrimary
        }

        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .cornerRadius(16.dp)
                .background(colorCardDark)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Badge de Línea (Rojo EMT)
            Box(
                modifier = GlanceModifier
                    .size(width = 44.dp, height = 28.dp)
                    .cornerRadius(8.dp)
                    .background(colorEmtRed),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = item.lineId,
                    style = TextStyle(
                        color = androidx.glance.unit.ColorProvider(colorTextPrimary),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }

            Spacer(modifier = GlanceModifier.width(10.dp))

            // Información de Parada y Distancia
            Column(
                modifier = GlanceModifier.defaultWeight()
            ) {
                Text(
                    text = item.stopName,
                    style = TextStyle(
                        color = androidx.glance.unit.ColorProvider(colorTextPrimary),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    maxLines = 1
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${item.stopId} • a ${item.distanceMeters}m",
                        style = TextStyle(
                            color = androidx.glance.unit.ColorProvider(colorTextSecondary),
                            fontSize = 11.sp
                        )
                    )

                    if (hasDeviation) {
                        Text(
                            text = " • ⚠ Desvío",
                            style = TextStyle(
                                color = androidx.glance.unit.ColorProvider(colorYellowLater),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }

            Spacer(modifier = GlanceModifier.width(8.dp))

            // Tiempos de los 2 Próximos Autobuses
            Column(
                horizontalAlignment = Alignment.End
            ) {
                // Primer Autobús (Destacado)
                Text(
                    text = item.firstBusText,
                    style = TextStyle(
                        color = androidx.glance.unit.ColorProvider(timeColor),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                )

                // Segundo Autobús (Secundario debajo)
                if (item.secondBus != null) {
                    Text(
                        text = item.secondBusFormatted,
                        style = TextStyle(
                            color = androidx.glance.unit.ColorProvider(colorTextTertiary),
                            fontSize = 11.sp
                        )
                    )
                }
            }
        }
    }

    @androidx.compose.runtime.Composable
    private fun InactiveStateView(closestStopName: String?, closestDistance: Int?) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .cornerRadius(16.dp)
                .background(colorCardDark)
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Fuera de tus paradas (>500m)",
                style = TextStyle(
                    color = androidx.glance.unit.ColorProvider(colorTextPrimary),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            )

            Spacer(modifier = GlanceModifier.height(4.dp))

            Text(
                text = "Pulsa actualizar al acercarte a tus zonas.",
                style = TextStyle(
                    color = androidx.glance.unit.ColorProvider(colorTextTertiary),
                    fontSize = 11.sp
                )
            )

            if (!closestStopName.isNullOrBlank() && closestDistance != null) {
                Spacer(modifier = GlanceModifier.height(4.dp))
                Text(
                    text = "Más cercana: $closestStopName a ${closestDistance}m",
                    style = TextStyle(
                        color = androidx.glance.unit.ColorProvider(colorTextSecondary),
                        fontSize = 10.sp
                    ),
                    maxLines = 1
                )
            }
        }
    }

    @androidx.compose.runtime.Composable
    private fun LoadingStateView() {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .cornerRadius(16.dp)
                .background(colorCardDark)
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Localizando y consultando tiempos...",
                style = TextStyle(
                    color = androidx.glance.unit.ColorProvider(colorGreenImminent),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }

    @androidx.compose.runtime.Composable
    private fun InfoMessageView(title: String, subtitle: String) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .cornerRadius(16.dp)
                .background(colorCardDark)
                .padding(14.dp)
                .clickable(actionStartActivity<MainActivity>()),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = TextStyle(
                    color = androidx.glance.unit.ColorProvider(colorTextPrimary),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            )

            Spacer(modifier = GlanceModifier.height(4.dp))

            Text(
                text = subtitle,
                style = TextStyle(
                    color = androidx.glance.unit.ColorProvider(colorTextTertiary),
                    fontSize = 11.sp
                )
            )
        }
    }
}
