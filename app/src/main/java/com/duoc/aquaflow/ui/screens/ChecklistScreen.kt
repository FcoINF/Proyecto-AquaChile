package com.duoc.aquaflow.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.duoc.aquaflow.model.EstadoEquipo
import com.duoc.aquaflow.model.ItemEquipamiento
import com.duoc.aquaflow.ui.components.ItemChecklist
import com.duoc.aquaflow.ui.components.SeccionFotos

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChecklistScreen(
    onContinuarClick: () -> Unit
) {
    // Lista inicial de prueba para la revisión DPR-24
    var equipos by remember {
        mutableStateOf(
            listOf(
                ItemEquipamiento("1", "Compressor y Filtros Carbón", "Test de CO/CO2 con tubo colorimétrico OK"),
                ItemEquipamiento("2", "Traje Seco, Arnés y Casco KMB", "Prueba de estanqueidad de cremallera estanca"),
                ItemEquipamiento("3", "Botellón de Rescate (Bail-out)", "Presión 210 BAR verificada en manómetro"),
                ItemEquipamiento("4", "Manguera Umbilical y Conexiones", "Roce superficial en manga protectora externa")
            )
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Checklist DPR-24 (Equipos)") },
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
                    SeccionFotos(cantidadFotos = 0, onTomarFotoClick = { /* Acción de fotos */ })
                }
            }

            Button(
                onClick = onContinuarClick,
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