package com.duoc.aquaflow.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.unit.dp
import com.duoc.aquaflow.domain.GrupoFaena
import com.duoc.aquaflow.domain.ReglasPanel
import com.duoc.aquaflow.model.FaenaGuardada

@Composable
fun PanelAdministrador(faenas: List<FaenaGuardada>, onVerFaena: (String) -> Unit) {
    val grupos = remember(faenas) { ReglasPanel.agrupar(faenas) }
    var seleccionado by rememberSaveable { mutableStateOf(GrupoFaena.ABIERTAS) }
    Text("Estado de las faenas", style = MaterialTheme.typography.titleLarge)
    Text("Selecciona una tarjeta para consultar sus faenas.", style = MaterialTheme.typography.bodyMedium)
    GrupoFaena.entries.chunked(2).forEach { fila ->
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            fila.forEach { grupo ->
                Card(
                    onClick = { seleccionado = grupo },
                    modifier = Modifier.weight(1f).semantics { selected = seleccionado == grupo },
                    colors = CardDefaults.cardColors(containerColor =
                        if (seleccionado == grupo) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("${grupos.getValue(grupo).size}", style = MaterialTheme.typography.headlineLarge)
                        Text(grupo.titulo, style = MaterialTheme.typography.titleSmall)
                        if (seleccionado == grupo) Text("Mostrando", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
    AquaSectionCard(
        title = seleccionado.titulo,
        subtitle = if (seleccionado == GrupoFaena.ABIERTAS) "Completas y pendientes de decisión" else null
    ) {
        val visibles = grupos.getValue(seleccionado)
        if (visibles.isEmpty()) Text("No hay faenas en esta categoría.")
        visibles.forEach { faena ->
            val apertura = faena.resumen.apertura
            OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(apertura.centroCultivo, style = MaterialTheme.typography.titleMedium)
                    Text("Código · ${apertura.id.take(8).uppercase()}", style = MaterialTheme.typography.bodySmall)
                    Text("${apertura.fecha} · ${apertura.hora}")
                    Text("Supervisor · ${apertura.supervisorACargo}")
                    Text(if (seleccionado == GrupoFaena.ABIERTAS) "Pendiente de decisión" else faena.estado.etiqueta)
                    faena.cierre?.let { Text("Cerrada por · ${it.cerradoPor.ifBlank { apertura.supervisorACargo }}") }
                    OutlinedButton(onClick = { onVerFaena(apertura.id) }, modifier = Modifier.fillMaxWidth()) {
                        Text("Ver faena")
                    }
                }
            }
        }
    }
}
