package com.duoc.aquaflow.ui.screens.historial

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.duoc.aquaflow.ui.components.AquaSectionCard
import com.duoc.aquaflow.viewmodel.AperturasRecibidasUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AperturasRecibidasScreen(
    state: AperturasRecibidasUiState,
    onActualizar: () -> Unit,
    onVolver: () -> Unit
) {
    Scaffold(topBar = {
        TopAppBar(
            title = { Text("Aperturas recibidas") },
            navigationIcon = {
                IconButton(onClick = onVolver) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                }
            },
            actions = {
                TextButton(onClick = onActualizar, enabled = !state.cargando) { Text("Actualizar") }
            }
        )
    }) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when {
                state.cargando -> item { Text("Consultando el servidor…") }
                state.error != null -> item {
                    Text(state.error, color = MaterialTheme.colorScheme.error)
                }
                state.aperturas.isEmpty() -> item { Text("Aún no hay aperturas confirmadas.") }
            }
            items(state.aperturas, key = { it.id }) { apertura ->
                val datos = apertura.datos
                AquaSectionCard(
                    title = datos.centroCultivo,
                    subtitle = "Prechequeo pendiente · ${datos.fecha} ${datos.hora}"
                ) {
                    Text("Buzo · ${datos.buzoAsignado}")
                    Text("Supervisor · ${apertura.supervisorACargo}")
                    Text("ID · ${apertura.id}", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
