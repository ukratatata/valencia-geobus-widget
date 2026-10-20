package com.valencia.geobus.widget

import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver

/**
 * BroadcastReceiver que gestiona el ciclo de vida del Widget en la pantalla de inicio
 * de Samsung One UI 8.5.
 */
class GeoBusWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = GeoBusGlanceWidget()
}
