package com.duoc.aquaflow.ui.screens.historial

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.duoc.aquaflow.model.EstadoFaena
import com.duoc.aquaflow.model.HistorialItem
import com.duoc.aquaflow.model.Rol
import com.duoc.aquaflow.model.Usuario
import com.duoc.aquaflow.ui.components.EmptyState
import com.duoc.aquaflow.ui.components.EstadoChip
import com.duoc.aquaflow.ui.navigation.MainBottomBar
import com.duoc.aquaflow.ui.navigation.Tab
import com.duoc.aquaflow.ui.navigation.tabsPara
import com.duoc.aquaflow.ui.theme.AquaFlowTheme
import com.duoc.aquaflow.viewmodel.HistorialUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistorialScreen(
    state: HistorialUiState,
    tabs: List<Tab>,
    onTexto: (String) -> Unit,
    onEstado: (EstadoFaena?) -> Unit,
    onLimpiar: () -> Unit,
    onReintentar: () -> Unit,
    onDetalle: (String) -> Unit,
    onTab: (Tab) -> Unit
) {
    val filtro = state.filtro
    Scaffold(
        topBar = { TopAppBar(title = { Text("Historial de faenas") }) },
        bottomBar = { MainBottomBar(Tab.HISTORIAL, tabs, onTab) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text("Aperturas guardadas", style = MaterialTheme.typography.titleLarge)
                Text("Por ahora se consultan los datos de apertura. Las decisiones y el cierre se incorporarán después.")
            }
            item {
                OutlinedTextField(
                    value = filtro.texto,
                    onValueChange = onTexto,
                    label = { Text("Buscar por código, buzo, centro o supervisor") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                Text("Estado", style = MaterialTheme.typography.labelLarge)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(selected = filtro.estado == null, onClick = { onEstado(null) }, label = { Text("Todos") })
                    EstadoFaena.entries.forEach { estado ->
                        FilterChip(selected = filtro.estado == estado,
                            onClick = { onEstado(estado) }, label = { Text(estado.etiqueta) })
                    }
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
            when {
                state.cargando -> item { Text("Cargando aperturas…") }
                state.error != null -> item {
                    Text(state.error, color = MaterialTheme.colorScheme.error)
                    Button(onClick = onReintentar) { Text("Reintentar") }
                }
                else -> {
                    item { Text("${state.faenas.size} de ${state.total} apertura(s)") }
                    if (state.faenas.isEmpty()) {
                        item {
                            EmptyState(
                                titulo = if (state.total == 0) "Aún no hay aperturas" else "Sin resultados",
                                texto = if (state.total == 0) "Las aperturas guardadas aparecerán aquí." else "Prueba con otros filtros.",
                                icono = Icons.Filled.History
                            )
                        }
                    }
                    items(state.faenas, key = { it.id }) { item ->
                        FaenaHistorialCard(item, onDetalle = { onDetalle(item.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun FaenaHistorialCard(item: HistorialItem, onDetalle: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(item.centroCultivo.ifBlank { "Centro por completar" }, style = MaterialTheme.typography.titleMedium)
            EstadoChip(item.estado)
            Text("Código: ${item.codigo}", style = MaterialTheme.typography.bodySmall)
            Text("Buzo: ${item.buzoAsignado.ifBlank { "Por completar" }}")
            Text("Supervisor: ${item.supervisorACargo}")
            Text("Fecha: ${item.fecha.ifBlank { "Por completar" }} ${item.hora}")
            OutlinedButton(onClick = onDetalle) { Text("Ver detalle") }
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
