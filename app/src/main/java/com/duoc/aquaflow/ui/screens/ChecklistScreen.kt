package com.duoc.aquaflow.ui.screens

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
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
    faenaId: String,
    checklistInicial: ChecklistFaena?,
    onGuardar: (ChecklistFaena) -> Unit,
    onVolver: () -> Unit
) {
    // Lista inicial de prueba para la revisión DPR-24
    var equipos by remember {
        mutableStateOf(
            checklistInicial?.items?.takeIf { it.isNotEmpty() } ?: listOf(
                ItemEquipamiento("1", "Compresor y Filtros Carbón", "Test de CO/CO2 con tubo colorimétrico OK"),
                ItemEquipamiento("2", "Traje Seco, Arnés y Casco KMB", "Prueba de estanqueidad de cremallera estanca"),
                ItemEquipamiento("3", "Botellón de Rescate (Bail-out)", "Presión 210 BAR verificada en manómetro"),
                ItemEquipamiento("4", "Manguera Umbilical y Conexiones", "Roce superficial en manga protectora externa")
            )
        )
    }

    var fotosBitmaps by remember { mutableStateOf<List<Bitmap>>(emptyList()) }
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            fotosBitmaps = fotosBitmaps + bitmap
        }
    }

    val observacionesCount = equipos.count { it.estado == EstadoEquipo.NO_CUMPLE || it.estado == EstadoEquipo.OBSERVADO }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Checklist DPR-24 (Equipos)") },
                navigationIcon = {
                    TextButton(onClick = onVolver) { Text("Volver") }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0B1723),
                    titleContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            if (observacionesCount > 0) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0))
                ) {
                    Text(
                        text = "⚠ Hay $observacionesCount equipo(s) con observaciones o no cumple. Se recomienda adjuntar evidencia fotográfica.",
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFE65100)
                    )
                }
            }

            LazyColumn(modifier = Modifier.weight(1f)) {
                items(equipos) { item ->
                    ItemChecklist(
                        item = item,
                        onEstadoCambiado = { nuevoEstado ->
                            equipos = equipos.map {
                                if (it.id == item.id) it.copy(estado = nuevoEstado) else it
                            }
                        }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    SeccionFotos(
                        cantidadFotos = fotosBitmaps.size,
                        onTomarFotoClick = {
                            try {
                                cameraLauncher.launch(null)
                            } catch (_: Exception) {
                                // Si no hay app de cámara disponible, simulamos una foto para pruebas
                                val dummyBitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
                                fotosBitmaps = fotosBitmaps + dummyBitmap
                            }
                        }
                    )

                    if (fotosBitmaps.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Evidencias fotográficas DPR-24:", style = MaterialTheme.typography.titleSmall)
                        Spacer(modifier = Modifier.height(6.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(fotosBitmaps) { bmp ->
                                Card(
                                    modifier = Modifier.size(80.dp),
                                    elevation = CardDefaults.cardElevation(4.dp)
                                ) {
                                    Image(
                                        bitmap = bmp.asImageBitmap(),
                                        contentDescription = "Evidencia DPR-24",
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Button(
                onClick = {
                    val checklist = ChecklistFaena(
                        idFaena = faenaId,
                        items = equipos,
                        fotosEvidencia = fotosBitmaps.mapIndexed { index, _ -> "dpr24_evidencia_$index.jpg" }
                    )
                    onGuardar(checklist)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF95716))
            ) {
                Text("Guardar y Continuar", color = Color.White)
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
