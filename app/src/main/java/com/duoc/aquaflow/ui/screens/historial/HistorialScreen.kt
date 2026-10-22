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
import androidx.compose.material.icons.filled.History
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.duoc.aquaflow.model.EstadoFaena
import com.duoc.aquaflow.model.HistorialItem
import com.duoc.aquaflow.model.Usuario
import com.duoc.aquaflow.ui.components.EmptyState
import com.duoc.aquaflow.ui.components.EstadoChip
import com.duoc.aquaflow.ui.components.SemaforoSalud
import com.duoc.aquaflow.ui.navigation.MainBottomBar
import com.duoc.aquaflow.ui.navigation.Tab
import com.duoc.aquaflow.ui.navigation.tabsPara
import com.duoc.aquaflow.viewmodel.HistorialUiState
import com.duoc.aquaflow.viewmodel.HistorialViewModel

/**
 * Historial de faenas adaptado al diseño simple del proyecto actual.
 * Los datos son de ejemplo en memoria (ver [com.duoc.aquaflow.data.HistorialDemo]);
 * cuando exista Room se reemplaza la fuente sin cambiar esta pantalla.
 */
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
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ResumenCard(state)

            OutlinedTextField(
                value = filtro.texto,
                onValueChange = vm::onTexto,
                label = { Text("Buscar por código, buzo, centro o supervisor") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Text("Estado", style = MaterialTheme.typography.labelLarge)
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = filtro.estado == null,
                    onClick = { vm.onEstado(null) },
                    label = { Text("Todos") }
                )
                EstadoFaena.entries.forEach { estado ->
                    FilterChip(
                        selected = filtro.estado == estado,
                        onClick = { vm.onEstado(estado) },
                        label = { Text(estado.etiqueta) }
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("${state.faenas.size} de ${state.total} faena(s)", style = MaterialTheme.typography.bodyMedium)
                if (hayFiltros) {
                    OutlinedButton(onClick = vm::limpiar) { Text("Limpiar filtros") }
                }
            }

            if (state.faenas.isEmpty()) {
                EmptyState(
                    titulo = if (state.total == 0) "Aún no hay faenas" else "Sin resultados",
                    texto = if (state.total == 0) "Las faenas que registres aparecerán aquí." else "Prueba con otros filtros.",
                    icono = Icons.Filled.History
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
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Resumen", style = MaterialTheme.typography.titleMedium)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                ResumenDato("Total", state.total.toString())
                ResumenDato("Autorizadas", state.autorizadas.toString())
                ResumenDato("Rechazadas", state.rechazadas.toString())
                ResumenDato("Cerradas", state.cerradas.toString())
            }
        }
    }
}

@Composable
private fun ResumenDato(etiqueta: String, valor: String) {
    Column {
        Text(etiqueta, style = MaterialTheme.typography.labelSmall)
        Text(valor, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun FaenaHistorialCard(item: HistorialItem) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    item.codigo,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                EstadoChip(item.estado)
            }
            Text("${item.buzoAsignado} • ${item.centroCultivo}", style = MaterialTheme.typography.bodyLarge)
            Text("Supervisor: ${item.supervisorACargo}", style = MaterialTheme.typography.bodyMedium)
            Text("Fecha: ${item.fecha} ${item.hora}", style = MaterialTheme.typography.bodyMedium)
            item.encuesta?.let { e ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Salud: ${e.sistolica}/${e.diastolica} mmHg · Sat. ${e.saturacion}%")
                    SemaforoSalud(e.nivel)
                }
            }
        }
    }
}
