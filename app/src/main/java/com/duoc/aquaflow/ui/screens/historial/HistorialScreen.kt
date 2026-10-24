package com.duoc.aquaflow.ui.screens.historial

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.Card
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.duoc.aquaflow.domain.HistorialFiltro
import com.duoc.aquaflow.model.EstadoFaena
import com.duoc.aquaflow.model.HistorialItem
import com.duoc.aquaflow.model.Rol
import com.duoc.aquaflow.ui.components.EmptyState
import com.duoc.aquaflow.ui.components.EstadoChip
import com.duoc.aquaflow.ui.components.SemaforoSalud
import com.duoc.aquaflow.ui.navigation.MainBottomBar
import com.duoc.aquaflow.ui.navigation.Tab
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
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Registro de inmersiones", style = MaterialTheme.typography.headlineSmall)
                    Text(
                        "Consulta las aperturas guardadas y su información operativa.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            item {
                AquaSectionCard(
                    title = "Buscar y filtrar",
                    subtitle = "${state.total} apertura(s) guardada(s)"
                ) {
                    OutlinedTextField(
                        value = filtro.texto,
                        onValueChange = onTexto,
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
                            onClick = { onEstado(null) },
                            label = { Text("Todas") }
                        )
                        EstadoFaena.entries.forEach { estado ->
                            FilterChip(
                                selected = filtro.estado == estado,
                                onClick = { onEstado(estado) },
                                label = { Text(estado.etiqueta) }
                            )
                        }
                    }
                }
            }

            if (filtro.texto.isNotBlank() || filtro.estado != null) {
                item { OutlinedButton(onClick = onLimpiar) { Text("Limpiar filtros") } }
            }
            when {
                state.cargando -> item {
                    AquaSectionCard(title = "Cargando historial") {
                        Text("Estamos consultando las aperturas guardadas.")
                    }
                }
                state.error != null -> item {
                    AquaSectionCard(title = "No se pudo cargar el historial") {
                        Text(state.error, color = MaterialTheme.colorScheme.error)
                        Button(onClick = onReintentar) { Text("Reintentar") }
                    }
                }
                else -> {
                    item {
                        Text(
                            "${state.faenas.size} de ${state.total} aperturas",
                            style = MaterialTheme.typography.titleSmall
                        )
                    }
                    if (state.faenas.isEmpty()) {
                        item {
                            EmptyState(
                                titulo = if (state.total == 0) "Aún no hay aperturas" else "Sin resultados",
                                texto = if (state.total == 0) "Las aperturas guardadas aparecerán aquí." else "Prueba con otros filtros.",
                                texto = if (state.total == 0) "Las aperturas que registres aparecerán aquí." else "Prueba con otros filtros.",
                                icono = Icons.AutoMirrored.Filled.List
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
    AquaSectionCard(title = item.centroCultivo.ifBlank { "Centro por completar" }) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Código · ${item.codigo}", style = MaterialTheme.typography.titleSmall)
            EstadoChip(item.estado)
        }
        Text("Buzo · ${item.buzoAsignado.ifBlank { "Por completar" }}")
        Text("Supervisor · ${item.supervisorACargo.ifBlank { "Por completar" }}")
        Text(
            "${item.fecha.ifBlank { "Fecha por completar" }} · ${item.hora}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        item.encuesta?.let { encuesta ->
            Text("Salud · ${encuesta.sistolica}/${encuesta.diastolica} mmHg · O₂ ${encuesta.saturacion}%")
            SemaforoSalud(encuesta.nivel)
        }
        OutlinedButton(onClick = onDetalle, modifier = Modifier.fillMaxWidth()) {
            Text("Ver detalle")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HistorialScreenPreview() {
    AquaFlowTheme {
        HistorialScreen(
            state = HistorialUiState(cargando = false),
            tabs = tabsPara(Rol.SUPERVISOR),
            onTexto = {}, onEstado = {}, onLimpiar = {}, onReintentar = {}, onDetalle = {},
            onTab = {}
        )
    }
}
