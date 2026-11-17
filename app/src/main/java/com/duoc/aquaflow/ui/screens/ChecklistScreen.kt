package com.duoc.aquaflow.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.duoc.aquaflow.data.local.EvidenciasFotos
import com.duoc.aquaflow.model.ChecklistFaena
import com.duoc.aquaflow.model.EstadoEquipo
import com.duoc.aquaflow.model.ItemEquipamiento
import com.duoc.aquaflow.ui.components.EvidenciasGaleria
import com.duoc.aquaflow.ui.components.Guardando
import com.duoc.aquaflow.ui.components.ItemChecklist
import com.duoc.aquaflow.ui.components.SeccionFotos
import com.duoc.aquaflow.ui.theme.AquaFlowTheme
import com.duoc.aquaflow.viewmodel.ChecklistViewModel
import com.duoc.aquaflow.domain.ReglasSecciones

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
    val context = LocalContext.current
    val vm: ChecklistViewModel = viewModel(key = "checklist-$faenaId")
    LaunchedEffect(faenaId) { vm.cargar(faenaId, checklistInicial) }
    val ui by vm.ui.collectAsState()

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { resultado ->
        val uri = vm.ui.value.pendiente?.let(Uri::parse)
        val guardada = resultado && uri != null && EvidenciasFotos.existe(context, uri)
        if (!guardada && uri != null) EvidenciasFotos.descartar(context, uri)
        vm.terminarFoto(guardada)
    }

    Guardando(ocupado)

    ChecklistScreenContent(
        ocupado = ocupado,
        error = errorExterno ?: ui.error,
        items = ui.items,
        fotos = ui.fotos,
        fotosPorEquipo = ui.fotosPorEquipo,
        onEstadoCambiado = vm::cambiarEstado,
        onTomarFotoClick = { itemId ->
            if (ui.pendiente == null) {
                try {
                    val uri = EvidenciasFotos.crear(context)
                    vm.prepararFoto(itemId, uri.toString())
                    cameraLauncher.launch(uri)
                } catch (_: Exception) {
                    vm.ui.value.pendiente?.let { EvidenciasFotos.descartar(context, Uri.parse(it)) }
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
    fotosPorEquipo: Map<String, String>,
    onEstadoCambiado: (String, EstadoEquipo) -> Unit,
    onTomarFotoClick: (String) -> Unit,
    onQuitarFoto: (String) -> Unit,
    onGuardarClick: () -> Unit,
    onGuardarBorradorClick: () -> Unit,
    onVolver: () -> Unit,
    centroCultivo: String = "Centro sin asignar - Jaula 24"
) {
    val checklistCompletable = ReglasSecciones.checklistHabilitaContinuidad(
        ChecklistFaena("validacion-ui", items, fotos, fotosPorEquipo = fotosPorEquipo)
    )
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
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 8.dp)
            ) {
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
                        fotoCapturada = !fotosPorEquipo[item.id].isNullOrBlank(),
                        onEstadoCambiado = { nuevoEstado ->
                            onEstadoCambiado(item.id, nuevoEstado)
                        },
                        onTomarFoto = { onTomarFotoClick(item.id) }
                    )
                }

                item {
                    val nombresPorFoto = items.mapNotNull { item ->
                        fotosPorEquipo[item.id]?.let { referencia -> referencia to item.titulo }
                    }.toMap()
                    val categoriasPorFoto = items.mapNotNull { item ->
                        fotosPorEquipo[item.id]?.let { referencia -> referencia to item.categoria }
                    }.toMap()
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        SeccionFotos(
                            cantidadFotos = fotosPorEquipo.size,
                            totalEquipos = items.size
                        )
                        EvidenciasGaleria(
                            fotos = fotos,
                            nombresPorFoto = nombresPorFoto,
                            categoriasPorFoto = categoriasPorFoto,
                            onQuitar = onQuitarFoto
                        )
                    }
                }
            }

            error?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
            if (items.any { it.estado == EstadoEquipo.NO_CUMPLE }) {
                Text(
                    "Faena bloqueada: uno o más equipos críticos están en rojo. La evidencia fotográfica no habilita la continuidad.",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Button(
                    enabled = !ocupado && checklistCompletable,
                    onClick = onGuardarClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0755B8))
                ) {
                    Text(
                        text = "Continuar a Salud",
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
                        .heightIn(min = 48.dp),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color(0xFF0755B8)),
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
                Text("Paso 2 de 5", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            LinearProgressIndicator(progress = { 0.4f }, modifier = Modifier.fillMaxWidth(), color = Color(0xFF0755B8), trackColor = Color(0xFFE5EAF0))
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
                    titulo = "Maleta de Suministro de Aire",
                    descripcion = "Verificación de manifold, válvulas de alivio, manómetro análogo y presurización de línea principal.",
                    categoria = "EQUIPO 01 • SUMINISTRO PRIMARIO",
                    badge = "FOTO OBLIGATORIA"
                ),
                ItemEquipamiento(
                    id = "2",
                    titulo = "Filtros del Compresor",
                    descripcion = "Test de CO/CO2 con tubo colorimétrico OK. Purgas automáticas drenadas y carbón activo.",
                    categoria = "EQUIPO 02 • COMPRESOR",
                    badge = "FOTO OBLIGATORIA"
                ),
                ItemEquipamiento(
                    id = "3",
                    titulo = "Aletas y Equipamiento Personal",
                    descripcion = "Prueba de tirantes de aletas, ensamble de hebillas y estanqueidad de traje seco/casco KMB.",
                    categoria = "EQUIPO 03 • INDIVIDUAL BUZO",
                    badge = "FOTO OBLIGATORIA"
                ),
                ItemEquipamiento(
                    id = "4",
                    titulo = "Dispositivo Census",
                    descripcion = "Consola de monitoreo de profundidad, velocidad de ascenso/descenso y telemetría conectada.",
                    categoria = "EQUIPO 04 • MONITOREO Y CONTROL",
                    badge = "FOTO OBLIGATORIA"
                )
            ),
            fotos = emptyList(),
            fotosPorEquipo = emptyMap(),
            onEstadoCambiado = { _, _ -> },
            onTomarFotoClick = {},
            onQuitarFoto = {},
            onGuardarClick = {},
            onGuardarBorradorClick = {},
            onVolver = {}
        )
    }
}
