package com.duoc.aquaflow.ui.screens.historial

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.duoc.aquaflow.model.EstadoFaena
import com.duoc.aquaflow.model.HistorialItem
import com.duoc.aquaflow.model.Rol
import com.duoc.aquaflow.model.Usuario
import com.duoc.aquaflow.ui.components.AquaSectionCard
import com.duoc.aquaflow.ui.components.EmptyState
import com.duoc.aquaflow.ui.components.EstadoChip
import com.duoc.aquaflow.ui.components.SemaforoSalud
import com.duoc.aquaflow.ui.navigation.MainBottomBar
import com.duoc.aquaflow.ui.navigation.Tab
import com.duoc.aquaflow.ui.navigation.tabsPara
import com.duoc.aquaflow.ui.theme.AquaFlowTheme
import com.duoc.aquaflow.ui.theme.AquaNaranja
import com.duoc.aquaflow.viewmodel.HistorialUiState
import com.duoc.aquaflow.viewmodel.HistorialViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistorialScreen(
    usuario: Usuario,
    onTab: (Tab) -> Unit,
    tabs: List<Tab> = tabsPara(usuario.rol)
) {
    val vm: HistorialViewModel = viewModel()
    val state by vm.state.collectAsState()
    val filtro = state.filtro
    val hayFiltros = filtro.texto.isNotBlank() || filtro.estado != null

    Scaffold(
        topBar = { TopAppBar(title = { Text("Historial de faenas") }) },
        bottomBar = { MainBottomBar(Tab.HISTORIAL, tabs, onTab) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Registro de inmersiones", style = MaterialTheme.typography.headlineSmall)
                Text(
                    "Consulta el avance y los controles realizados en cada faena.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            ResumenCard(state)

            AquaSectionCard(title = "Buscar y filtrar") {
                OutlinedTextField(
                    value = filtro.texto,
                    onValueChange = vm::onTexto,
                    label = { Text("Código, buzo, centro o supervisor") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Estado de la faena", style = MaterialTheme.typography.labelLarge)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = filtro.estado == null,
                        onClick = { vm.onEstado(null) },
                        label = { Text("Todas") }
                    )
                    EstadoFaena.entries.forEach { estado ->
                        FilterChip(
                            selected = filtro.estado == estado,
                            onClick = { vm.onEstado(estado) },
                            label = { Text(estado.etiqueta) }
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "${state.faenas.size} de ${state.total} faenas",
                    style = MaterialTheme.typography.titleSmall
                )
                if (hayFiltros) {
                    OutlinedButton(onClick = vm::limpiar) { Text("Limpiar filtros") }
                }
            }

            if (state.faenas.isEmpty()) {
                EmptyState(
                    titulo = if (state.total == 0) "Aún no hay faenas" else "Sin resultados",
                    texto = if (state.total == 0) "Las faenas que registres aparecerán aquí." else "Prueba con otros filtros.",
                    icono = Icons.AutoMirrored.Filled.List
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    state.faenas.forEach { item -> FaenaHistorialCard(item) }
                }
            }
        }
    }
}

@Composable
private fun ResumenCard(state: HistorialUiState) {
    AquaSectionCard(title = "Resumen de operaciones") {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ResumenDato("Total", state.total.toString(), Modifier.weight(1f))
            ResumenDato("Autorizadas", state.autorizadas.toString(), Modifier.weight(1f), AquaNaranja)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ResumenDato("Rechazadas", state.rechazadas.toString(), Modifier.weight(1f))
            ResumenDato("Cerradas", state.cerradas.toString(), Modifier.weight(1f))
        }
    }
}

@Composable
private fun ResumenDato(etiqueta: String, valor: String, modifier: Modifier = Modifier, acento: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.primary) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(etiqueta, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(valor, style = MaterialTheme.typography.headlineSmall, color = acento)
        }
    }
}

@Composable
private fun FaenaHistorialCard(item: HistorialItem) {
    AquaSectionCard(title = item.codigo) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(item.buzoAsignado, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
            EstadoChip(item.estado)
        }
        Text(item.centroCultivo, style = MaterialTheme.typography.bodyMedium)
        Text("Supervisor · ${item.supervisorACargo}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("${item.fecha} · ${item.hora}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        item.encuesta?.let { encuesta ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("${encuesta.sistolica}/${encuesta.diastolica} mmHg · O₂ ${encuesta.saturacion}%", style = MaterialTheme.typography.bodySmall)
                SemaforoSalud(encuesta.nivel)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HistorialScreenPreview() {
    AquaFlowTheme {
        HistorialScreen(
            usuario = Usuario("Carlos Supervisor", "1234", Rol.SUPERVISOR),
            onTab = {}
        )
    }
}
