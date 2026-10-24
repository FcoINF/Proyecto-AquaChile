package com.duoc.aquaflow.ui.screens.historial

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.duoc.aquaflow.model.EstadoFaena
import com.duoc.aquaflow.ui.components.AquaSectionCard
import com.duoc.aquaflow.ui.components.EstadoChip
import com.duoc.aquaflow.viewmodel.DetalleAperturaUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetalleAperturaScreen(
    state: DetalleAperturaUiState,
    onVolver: () -> Unit,
    onReintentar: () -> Unit
) {
    Scaffold(topBar = {
        TopAppBar(title = { Text("Detalle de apertura") }, navigationIcon = {
            TextButton(onClick = onVolver) { Text("Volver") }
        })
    }) { padding ->
        Column(
            modifier = Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            when {
                state.cargando -> AquaSectionCard(title = "Cargando apertura") {
                    Text("Estamos consultando los datos guardados.")
                }
                state.error != null -> AquaSectionCard(title = "No se pudo cargar la apertura") {
                    Text(state.error, color = MaterialTheme.colorScheme.error)
                    Button(onClick = onReintentar) { Text("Reintentar") }
                }
                state.apertura == null -> AquaSectionCard(title = "Apertura no disponible") {
                    Text("Esta apertura no está disponible para tu usuario.")
                }
                else -> {
                    val apertura = state.apertura
                    AquaSectionCard(
                        title = apertura.centroCultivo.ifBlank { "Centro por completar" },
                        subtitle = "Apertura · ${apertura.id}"
                    ) {
                        EstadoChip(EstadoFaena.PRECHEQUEO)
                        DatoApertura("Fecha y hora", "${apertura.fecha} · ${apertura.hora}")
                        DatoApertura("Buzo asignado", apertura.buzoAsignado)
                        DatoApertura("Supervisor a cargo", apertura.supervisorACargo)
                    }
                    AquaSectionCard(title = "Alcance del registro") {
                        Text(
                            "Se muestran los datos de apertura guardados. Checklist, AST, salud, decisión y cierre todavía no forman parte de este registro.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DatoApertura(etiqueta: String, valor: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(etiqueta, style = MaterialTheme.typography.labelLarge)
        Text(valor.ifBlank { "Por completar" }, style = MaterialTheme.typography.bodyLarge)
    }
}
