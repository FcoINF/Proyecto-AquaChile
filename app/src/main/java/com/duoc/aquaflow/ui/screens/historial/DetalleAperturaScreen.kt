package com.duoc.aquaflow.ui.screens.historial

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.duoc.aquaflow.model.EstadoFaena
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
            modifier = Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when {
                state.cargando -> Text("Cargando apertura…")
                state.error != null -> {
                    Text(state.error, color = MaterialTheme.colorScheme.error)
                    Button(onClick = onReintentar) { Text("Reintentar") }
                }
                state.apertura == null -> Text("Esta apertura no está disponible para tu usuario.")
                else -> {
                    val apertura = state.apertura
                    EstadoChip(EstadoFaena.PRECHEQUEO)
                    DatoApertura("Código", apertura.id)
                    DatoApertura("Centro de cultivo", apertura.centroCultivo)
                    DatoApertura("Fecha", apertura.fecha)
                    DatoApertura("Hora", apertura.hora)
                    DatoApertura("Buzo asignado", apertura.buzoAsignado)
                    DatoApertura("Supervisor a cargo", apertura.supervisorACargo)
                    Text("Se muestran los datos de apertura guardados. Checklist, AST, salud, decisión y cierre todavía no forman parte de este registro.",
                        style = MaterialTheme.typography.bodySmall)
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
