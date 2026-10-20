package com.valencia.geobus.ui.screens.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.valencia.geobus.data.local.entity.UserMonitoredStopEntity
import com.valencia.geobus.ui.theme.EmtRed
import com.valencia.geobus.ui.theme.OneUiBgDark
import com.valencia.geobus.ui.theme.OneUiCardDark
import com.valencia.geobus.ui.theme.OneUiGreen
import com.valencia.geobus.ui.theme.OneUiSurfaceVariant
import com.valencia.geobus.ui.theme.OneUiTextPrimary
import com.valencia.geobus.ui.theme.OneUiTextSecondary
import com.valencia.geobus.ui.theme.OneUiTextTertiary

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MonitoredStopsList(
    stops: List<UserMonitoredStopEntity>,
    onToggleEnabled: (stopId: Int, isEnabled: Boolean) -> Unit,
    onDeleteStop: (stopId: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(OneUiCardDark, RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Tus Paradas Guardadas",
                color = OneUiTextPrimary,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )

            Text(
                text = "${stops.size} activas",
                color = OneUiGreen,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (stops.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(OneUiSurfaceVariant, RoundedCornerShape(14.dp))
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No tienes paradas configuradas aún.\nBusca arriba una parada para añadirla.",
                    color = OneUiTextTertiary,
                    fontSize = 13.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                stops.forEach { stop ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(OneUiSurfaceVariant, RoundedCornerShape(14.dp))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Badge con número de parada
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(EmtRed, RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stop.stopId.toString(),
                                color = OneUiTextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stop.stopName,
                                color = OneUiTextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                stop.monitoredLines.forEach { line ->
                                    Box(
                                        modifier = Modifier
                                            .background(OneUiBgDark, RoundedCornerShape(4.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = line,
                                            color = OneUiGreen,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        Switch(
                            checked = stop.isEnabled,
                            onCheckedChange = { onToggleEnabled(stop.stopId, it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = OneUiTextPrimary,
                                checkedTrackColor = OneUiGreen,
                                uncheckedThumbColor = OneUiTextTertiary,
                                uncheckedTrackColor = OneUiBgDark
                            )
                        )

                        IconButton(onClick = { onDeleteStop(stop.stopId) }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Eliminar",
                                tint = OneUiTextTertiary
                            )
                        }
                    }
                }
            }
        }
    }
}
