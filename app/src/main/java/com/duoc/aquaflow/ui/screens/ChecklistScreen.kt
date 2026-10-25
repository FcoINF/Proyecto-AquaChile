package com.duoc.aquaflow.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.duoc.aquaflow.model.ChecklistFaena
import com.duoc.aquaflow.model.EstadoEquipo
import com.duoc.aquaflow.model.ItemEquipamiento
import com.duoc.aquaflow.ui.components.ItemChecklist
import com.duoc.aquaflow.ui.components.SeccionFotos
import com.duoc.aquaflow.ui.theme.AquaFlowTheme

@Composable
fun ChecklistScreen(
    ocupado: Boolean = false,
    errorExterno: String? = null,
    faenaId: String,
    checklistInicial: ChecklistFaena?,
    onGuardar: (ChecklistFaena) -> Unit,
    onGuardarBorrador: (ChecklistFaena) -> Unit,
    onVolver: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val vm: com.duoc.aquaflow.viewmodel.ChecklistViewModel = androidx.lifecycle.viewmodel.compose.viewModel(key = "checklist-$faenaId")
    LaunchedEffect(faenaId) { vm.cargar(faenaId, checklistInicial) }
    val ui by vm.ui.collectAsState()

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { resultado ->
        val uri = vm.ui.value.pendiente?.let(android.net.Uri::parse)
        val guardada = resultado && uri != null && com.duoc.aquaflow.data.local.EvidenciasFotos.existe(context, uri)
        if (!guardada && uri != null) com.duoc.aquaflow.data.local.EvidenciasFotos.descartar(context, uri)
        vm.terminarFoto(guardada)
    }

    com.duoc.aquaflow.ui.components.Guardando(ocupado)

    ChecklistScreenContent(
        ocupado = ocupado,
        error = errorExterno ?: ui.error,
        items = ui.items,
        fotos = ui.fotos,
        onEstadoCambiado = vm::cambiarEstado,
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
        },
        onQuitarFoto = vm::quitarReferencia,
        onGuardarClick = {
            vm.guardar()?.let(onGuardar)
        },
        onGuardarBorradorClick = {
            onGuardarBorrador(vm.borrador())
        },
        onVolver = onVolver
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChecklistScreenContent(
    ocupado: Boolean,
    error: String?,
    items: List<ItemEquipamiento>,
    fotos: List<String>,
    onEstadoCambiado: (String, EstadoEquipo) -> Unit,
    onTomarFotoClick: () -> Unit,
    onQuitarFoto: (String) -> Unit,
    onGuardarClick: () -> Unit,
    onGuardarBorradorClick: () -> Unit,
    onVolver: () -> Unit
) {
    val observacionesCount = items.count { it.estado == EstadoEquipo.NO_CUMPLE || it.estado == EstadoEquipo.OBSERVADO }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(end = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFF2B527C),
                                modifier = Modifier.size(28.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Shield,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            Text(
                                text = "EQUIPOS CRÍTICOS DE BUCEO",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF1E293B),
                                    fontSize = 14.sp
                                )
                            )
                        }
                        Text(
                            text = "Protocolo Armada",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color(0xFF64748B),
                                fontSize = 12.sp
                            )
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onVolver, enabled = !ocupado) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = Color(0xFF1E293B)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFF3F6F9))
            )
        },
        containerColor = Color(0xFFF3F6F9)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(
                text = "Inspección de equipos",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )
            )
            Text(
                text = "Revisa cada elemento y registra su condición.",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = Color(0xFF64748B)
                )
            )

            Spacer(modifier = Modifier.height(6.dp))

            if (observacionesCount > 0) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp, bottom = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
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
                items(items) { item ->
                    ItemChecklist(
                        item = item,
                        onEstadoCambiado = { nuevoEstado ->
                            onEstadoCambiado(item.id, nuevoEstado)
                        }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    SeccionFotos(
                        cantidadFotos = fotos.size,
                        onTomarFotoClick = onTomarFotoClick
                    )
                    com.duoc.aquaflow.ui.components.EvidenciasGaleria(fotos, onQuitarFoto)
                }
            }

            error?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }

            OutlinedButton(
                enabled = !ocupado,
                onClick = onGuardarBorradorClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Guardar borrador")
            }

            Button(
                enabled = !ocupado,
                onClick = onGuardarClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 8.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669))
            ) {
                Text(
                    text = "Guardar y continuar",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ChecklistScreenPreview() {
    AquaFlowTheme {
        ChecklistScreenContent(
            ocupado = false,
            error = null,
            items = listOf(
                ItemEquipamiento(
                    id = "1",
                    titulo = "Compresor, Purgas y Filtros Carbón",
                    descripcion = "Test de CO/CO2 con tubo colorimétrico OK. Purgas automáticas drenadas y banco presurizado.",
                    estado = EstadoEquipo.CUMPLE,
                    categoria = "EQUIPO 01 • SUMINISTRO PRIMARIO",
                    badge = "220 BAR"
                ),
                ItemEquipamiento(
                    id = "2",
                    titulo = "Traje Seco, Arnés y Casco KMB",
                    descripcion = "Prueba de estanqueidad de cremallera estanca, diafragma buconasal y fonía bidireccional limpia.",
                    estado = EstadoEquipo.CUMPLE,
                    categoria = "EQUIPO 02 • PERSONAL",
                    badge = "BUZO #1"
                ),
                ItemEquipamiento(
                    id = "3",
                    titulo = "Botellón de Rescate (Bail-out)",
                    descripcion = "Presión 210 BAR verificada en manómetro análogo sumergible. Conmutación manual activa.",
                    estado = EstadoEquipo.CUMPLE,
                    categoria = "EQUIPO 03 • RESPALDO RESCATE",
                    badge = "210 BAR"
                )
            ),
            fotos = emptyList(),
            onEstadoCambiado = { _, _ -> },
            onTomarFotoClick = {},
            onQuitarFoto = {},
            onGuardarClick = {},
            onGuardarBorradorClick = {},
            onVolver = {}
        )
    }
}
