package com.valencia.geobus.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.valencia.geobus.ui.screens.components.CatalogSyncCard
import com.valencia.geobus.ui.screens.components.MonitoredStopsList
import com.valencia.geobus.ui.screens.components.PermissionCard
import com.valencia.geobus.ui.screens.components.StopSearchSection
import com.valencia.geobus.ui.theme.EmtRed
import com.valencia.geobus.ui.theme.OneUiBgDark
import com.valencia.geobus.ui.theme.OneUiCardDark
import com.valencia.geobus.ui.theme.OneUiGreen
import com.valencia.geobus.ui.theme.OneUiSurfaceVariant
import com.valencia.geobus.ui.theme.OneUiTextPrimary
import com.valencia.geobus.ui.theme.OneUiTextSecondary
import com.valencia.geobus.ui.theme.OneUiTextTertiary
import com.valencia.geobus.ui.theme.OneUiYellow
import com.valencia.geobus.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val monitoredStops by viewModel.monitoredStops.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.statusMessage) {
        uiState.statusMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearStatusMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(EmtRed, RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.DirectionsBus,
                                contentDescription = null,
                                tint = OneUiTextPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "València GeoBus",
                                color = OneUiTextPrimary,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "One UI 8.5 · Galaxy S24 Ultra",
                                color = OneUiTextTertiary,
                                fontSize = 11.sp
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = OneUiBgDark
                )
            )
        },
        containerColor = OneUiBgDark
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Tarjeta de Estado y Guía de Permisos de Ubicación
            PermissionCard()

            // 2. Tarjeta de Catálogo y Botón de Actualizar Paradas
            CatalogSyncCard(
                catalogCount = uiState.catalogCount,
                lastSyncText = uiState.lastSyncText,
                isSyncing = uiState.isSyncingCatalog,
                onSyncClick = { viewModel.syncCatalog() }
            )

            // 3. Buscador y Selector de Paradas
            StopSearchSection(
                searchQuery = uiState.searchQuery,
                searchResults = uiState.searchResults,
                selectedStop = uiState.selectedStop,
                selectedLines = uiState.selectedLines,
                onQueryChange = viewModel::onSearchQueryChange,
                onStopSelected = viewModel::selectStop,
                onLineToggled = viewModel::toggleLineSelection,
                onAddStopClick = viewModel::saveMonitoredStop,
                onClearSelection = viewModel::clearSelection
            )

            // 4. Lista de Paradas Guardadas por el Usuario
            MonitoredStopsList(
                stops = monitoredStops,
                onToggleEnabled = viewModel::toggleStopEnabled,
                onDeleteStop = viewModel::removeMonitoredStop
            )

            // 5. Botón de Prueba en Directo (GPS + API de Tiempos Reales)
            LiveTestSection(
                isTesting = uiState.isTestingLive,
                testResults = uiState.testResults,
                onTestClick = viewModel::testLiveProximity
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun LiveTestSection(
    isTesting: Boolean,
    testResults: List<com.valencia.geobus.data.model.LineArrivals>?,
    onTestClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(OneUiCardDark, RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.GpsFixed,
                contentDescription = null,
                tint = OneUiGreen
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Simulador de Lectura del Widget",
                    color = OneUiTextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Prueba el cálculo GPS y la obtención de los 2 próximos autobuses.",
                    color = OneUiTextSecondary,
                    fontSize = 12.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = onTestClick,
            enabled = !isTesting,
            colors = ButtonDefaults.buttonColors(containerColor = OneUiGreen),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isTesting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = OneUiBgDark
                )
            } else {
                Text(
                    text = "Probar Refresco Ahora",
                    color = OneUiBgDark,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        if (testResults != null) {
            Spacer(modifier = Modifier.height(12.dp))
            if (testResults.isEmpty()) {
                Text(
                    text = "Estás a más de 500m de todas tus paradas configuradas (Widget en reposo).",
                    color = OneUiTextTertiary,
                    fontSize = 12.sp
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    testResults.forEach { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(OneUiSurfaceVariant, RoundedCornerShape(12.dp))
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(width = 38.dp, height = 26.dp)
                                    .background(EmtRed, RoundedCornerShape(6.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = item.lineId,
                                    color = OneUiTextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.stopName,
                                    color = OneUiTextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                                Text(
                                    text = "${item.stopId} · a ${item.distanceMeters}m",
                                    color = OneUiTextSecondary,
                                    fontSize = 10.sp
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = item.firstBusText,
                                    color = if (item.firstBus?.isImminent == true) OneUiGreen else OneUiTextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                if (item.secondBus != null) {
                                    Text(
                                        text = item.secondBusFormatted,
                                        color = OneUiTextTertiary,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
