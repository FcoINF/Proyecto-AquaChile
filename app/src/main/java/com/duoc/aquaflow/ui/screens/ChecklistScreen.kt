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
    onVolver: () -> Unit,
    centroCultivo: String = "Centro sin asignar - Jaula 24"
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
        onVolver = onVolver,
        centroCultivo = centroCultivo
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
    onVolver: () -> Unit,
    centroCultivo: String = "Centro sin asignar - Jaula 24"
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                        Text("Apertura de Faena", style = MaterialTheme.typography.titleMedium)
                        Text(centroCultivo.ifBlank { "Centro sin asignar - Jaula 24" }, style = MaterialTheme.typography.labelSmall)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onVolver, enabled = !ocupado) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF182B3A),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        containerColor = Color(0xFFF3F6F9)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            LazyColumn(modifier = Modifier.weight(1f)) {
                item { ProgresoChecklist() }
                item {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Surface(color = Color(0xFFE6F0FF), shape = RoundedCornerShape(9.dp)) {
                            Icon(Icons.Default.Shield, contentDescription = null, modifier = Modifier.padding(7.dp).size(18.dp), tint = Color(0xFF0755B8))
                        }
                        Text(
                            "EQUIPOS CRÍTICOS DE BUCEO",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = Color(0xFF334155))
                        )
                    }
                }
                items(items) { item ->
                    ItemChecklist(
                        item = item,
                        onEstadoCambiado = { nuevoEstado ->
                            onEstadoCambiado(item.id, nuevoEstado)
                        }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(4.dp))
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

            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Button(
                    enabled = !ocupado,
                    onClick = onGuardarClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 42.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0755B8))
                ) {
                    Text(
                        text = "Continuar al Checklist",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    )
                }
                OutlinedButton(
                    enabled = !ocupado,
                    onClick = onGuardarBorradorClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 38.dp),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF0755B8)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF0755B8))
                ) {
                    Text("Guardar Borrador")
                }
            }
        }
    }
}

@Composable
private fun ProgresoChecklist() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                Surface(color = Color(0xFF0755B8), shape = CircleShape) {
                    Text("2", modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp), color = Color.White, style = MaterialTheme.typography.labelSmall)
                }
                Text("FLUJO OPERATIVO DE FAENA", style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f))
                Text("Paso 2 de 4", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            LinearProgressIndicator(progress = 0.5f, modifier = Modifier.fillMaxWidth(), color = Color(0xFF0755B8), trackColor = Color(0xFFE5EAF0))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("2. INSPECCIÓN DE EQUIPOS", style = MaterialTheme.typography.labelSmall, color = Color(0xFF334155))
                Text("Sig: Encuesta Salud", style = MaterialTheme.typography.labelSmall, color = Color(0xFF8A9BB3))
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
                    categoria = "EQUIPO 01 • SUMINISTRO PRIMARIO",
                    badge = "220 BAR"
                ),
                ItemEquipamiento(
                    id = "2",
                    titulo = "Traje Seco, Arnés y Casco KMB",
                    descripcion = "Prueba de estanqueidad de cremallera estanca, diafragma buconasal y fonía bidireccional limpia.",
                    categoria = "EQUIPO 02 • PERSONAL",
                    badge = "BUZO #1"
                ),
                ItemEquipamiento(
                    id = "3",
                    titulo = "Botellón de Rescate (Bail-out)",
                    descripcion = "Presión 210 BAR verificada en manómetro análogo sumergible. Conmutación manual activa.",
                    categoria = "EQUIPO 03 • RESPALDO RESCATE",
                    badge = "210 BAR"
                ),
                ItemEquipamiento(
                    id = "4",
                    titulo = "Manguera Umbilical y Conexiones",
                    descripcion = "Roco superficial en manga protectora externa 15m. Conexiones neumáticas y audio 100% funcionales.",
                    categoria = "EQUIPO 04 • ALIMENTACIÓN",
                    badge = "OBSERVACIÓN"
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
