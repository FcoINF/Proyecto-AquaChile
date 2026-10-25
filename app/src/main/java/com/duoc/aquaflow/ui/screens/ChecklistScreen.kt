package com.duoc.aquaflow.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.duoc.aquaflow.model.ChecklistFaena
import com.duoc.aquaflow.model.EstadoEquipo
import com.duoc.aquaflow.model.ItemEquipamiento
import com.duoc.aquaflow.ui.components.ItemChecklist
import com.duoc.aquaflow.ui.components.SeccionFotos
import com.duoc.aquaflow.ui.theme.AquaFlowTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChecklistScreen(
    ocupado: Boolean = false,
    errorExterno: String? = null,
    faenaId: String,
    checklistInicial: ChecklistFaena?,
    onGuardar: (ChecklistFaena) -> Unit,
    onVolver: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val vm: com.duoc.aquaflow.viewmodel.ChecklistViewModel = androidx.lifecycle.viewmodel.compose.viewModel(key = "checklist-$faenaId")
    LaunchedEffect(faenaId) { vm.cargar(faenaId, checklistInicial) }
    val ui by vm.ui.collectAsState()
    val equipos = ui.items
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { resultado ->
        val uri = vm.ui.value.pendiente?.let(android.net.Uri::parse)
        val guardada = resultado && uri != null && com.duoc.aquaflow.data.local.EvidenciasFotos.existe(context, uri)
        if (!guardada && uri != null) com.duoc.aquaflow.data.local.EvidenciasFotos.descartar(context, uri)
        vm.terminarFoto(guardada)
    }

    val observacionesCount = equipos.count { it.estado == EstadoEquipo.NO_CUMPLE || it.estado == EstadoEquipo.OBSERVADO }

    com.duoc.aquaflow.ui.components.Guardando(ocupado)
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Checklist DPR-24 (Equipos)") },
                navigationIcon = { TextButton(onClick = onVolver, enabled = !ocupado) { Text("Volver") } }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Text("Inspección de equipos", style = MaterialTheme.typography.headlineSmall)
            Text(
                "Revisa cada elemento y registra su condición.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (observacionesCount > 0) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        Text(
                            text = "$observacionesCount equipo(s) requieren atención. Adjunta evidencia fotográfica cuando corresponda.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

            LazyColumn(modifier = Modifier.weight(1f)) {
                items(equipos) { item ->
                    ItemChecklist(
                        item = item,
                        onEstadoCambiado = { nuevoEstado ->
                            vm.cambiarEstado(item.id, nuevoEstado)
                        }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    SeccionFotos(
                        cantidadFotos = ui.fotos.size,
                        onTomarFotoClick = {
                            if (ui.pendiente == null) {
                                try {
                                    val uri = com.duoc.aquaflow.data.local.EvidenciasFotos.crear(context)
                                    vm.prepararFoto(uri.toString())
                                    cameraLauncher.launch(uri)
                                } catch (_: Exception) {
                                    vm.ui.value.pendiente?.let { com.duoc.aquaflow.data.local.EvidenciasFotos.descartar(context, android.net.Uri.parse(it)) }
                                    vm.errorCamara()
                                }
                            }
                        }
                    )
                    com.duoc.aquaflow.ui.components.EvidenciasGaleria(ui.fotos, vm::quitarReferencia)
                }
            }

            (errorExterno ?: ui.error)?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Button(
                enabled = !ocupado,
                onClick = {
                    vm.guardar()?.let(onGuardar)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
            ) {
                Text("Guardar y continuar")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ChecklistScreenPreview() {
    AquaFlowTheme {
        ChecklistScreen(
            faenaId = "1",
            checklistInicial = null,
            onGuardar = {},
            onVolver = {}
        )
    }
}
