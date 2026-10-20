package com.valencia.geobus.ui.screens.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.valencia.geobus.data.local.entity.EmtStopCatalogEntity
import com.valencia.geobus.ui.theme.EmtRed
import com.valencia.geobus.ui.theme.OneUiBgDark
import com.valencia.geobus.ui.theme.OneUiCardDark
import com.valencia.geobus.ui.theme.OneUiCardStroke
import com.valencia.geobus.ui.theme.OneUiSurfaceVariant
import com.valencia.geobus.ui.theme.OneUiTextPrimary
import com.valencia.geobus.ui.theme.OneUiTextSecondary
import com.valencia.geobus.ui.theme.OneUiTextTertiary

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StopSearchSection(
    searchQuery: String,
    searchResults: List<EmtStopCatalogEntity>,
    selectedStop: EmtStopCatalogEntity?,
    selectedLines: Set<String>,
    onQueryChange: (String) -> Unit,
    onStopSelected: (EmtStopCatalogEntity) -> Unit,
    onLineToggled: (String) -> Unit,
    onAddStopClick: () -> Unit,
    onClearSelection: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(OneUiCardDark, RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Text(
            text = "Buscar y Añadir Parada",
            color = OneUiTextPrimary,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Campo de Búsqueda One UI
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text(
                    text = "Número (ej. 1694) o calle (ej. Primat Reig)",
                    color = OneUiTextTertiary,
                    fontSize = 13.sp
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = OneUiTextSecondary
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = onClearSelection) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Limpiar",
                            tint = OneUiTextSecondary
                        )
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = EmtRed,
                unfocusedBorderColor = OneUiCardStroke,
                focusedContainerColor = OneUiBgDark,
                unfocusedContainerColor = OneUiBgDark,
                focusedTextColor = OneUiTextPrimary,
                unfocusedTextColor = OneUiTextPrimary
            )
        )

        // Resultados desplegables de búsqueda rápida
        if (searchResults.isNotEmpty() && selectedStop == null) {
            Spacer(modifier = Modifier.height(8.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(OneUiSurfaceVariant, RoundedCornerShape(14.dp))
                    .padding(vertical = 4.dp)
            ) {
                searchResults.take(6).forEach { stop ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onStopSelected(stop) }
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
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
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1
                            )
                            Text(
                                text = "Líneas: ${stop.lines}",
                                color = OneUiTextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }

        // Parada Seleccionada y Selector de Líneas
        if (selectedStop != null) {
            Spacer(modifier = Modifier.height(14.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(OneUiSurfaceVariant, RoundedCornerShape(14.dp))
                    .padding(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Place,
                        contentDescription = null,
                        tint = EmtRed
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "${selectedStop.stopId} · ${selectedStop.stopName}",
                            color = OneUiTextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Coordenadas: %.5f, %.5f".format(selectedStop.latitude, selectedStop.longitude),
                            color = OneUiTextTertiary,
                            fontSize = 11.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Selecciona qué líneas quieres monitorizar:",
                    color = OneUiTextSecondary,
                    fontSize = 12.sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                val lines = selectedStop.lines.split(",").map { it.trim() }.filter { it.isNotBlank() }
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    lines.forEach { line ->
                        val isSelected = selectedLines.contains(line)
                        FilterChip(
                            selected = isSelected,
                            onClick = { onLineToggled(line) },
                            label = {
                                Text(
                                    text = line,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = EmtRed,
                                selectedLabelColor = OneUiTextPrimary,
                                containerColor = OneUiBgDark,
                                labelColor = OneUiTextSecondary
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = onAddStopClick,
                    enabled = selectedLines.isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(containerColor = EmtRed),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = OneUiTextPrimary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Añadir a Mis Paradas Activas (Radio: 500m)",
                        color = OneUiTextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
