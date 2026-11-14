package com.duoc.aquaflow.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.duoc.aquaflow.model.ChecklistTareas
import com.duoc.aquaflow.ui.components.CircularCheckbox
import com.duoc.aquaflow.viewmodel.TareasViewModel

private val AzulTareas = Color(0xFF0755B8)
private val AzulNocheTareas = Color(0xFF182B3A)
private val FondoTareas = Color(0xFFF3F6F9)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TareasScreen(
    id: String,
    inicial: ChecklistTareas?,
    ocupado: Boolean,
    errorExterno: String?,
    onGuardar: (ChecklistTareas) -> Unit,
    onBorrador: (ChecklistTareas) -> Unit,
    onVolver: () -> Unit
) {
    val vm: TareasViewModel = viewModel(key = "tareas-$id")
    LaunchedEffect(id) { vm.cargar(inicial) }
    val items by vm.items.collectAsState()
    var error by remember { mutableStateOf<String?>(null) }

    Scaffold(
        containerColor = FondoTareas,
        topBar = {
            TopAppBar(
                title = { Text("Checklist de tareas") },
                navigationIcon = {
                    IconButton(onClick = onVolver, enabled = !ocupado) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AzulNocheTareas,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        bottomBar = {
            Column(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        val tareas = vm.validar()
                        if (tareas == null) error = "Registra y verifica al menos una tarea; completa todas las tareas agregadas."
                        else onGuardar(tareas)
                    },
                    enabled = !ocupado,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AzulTareas)
                ) { Text("Guardar tareas y revisar") }
                OutlinedButton(
                    onClick = { onBorrador(vm.borrador()) },
                    enabled = !ocupado,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    border = BorderStroke(1.dp, AzulTareas)
                ) { Text("Guardar borrador") }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { ProgresoTareas() }
            item {
                Text(
                    "Registra las tareas previstas y confirma su preparación antes de solicitar autorización.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            itemsIndexed(items) { indice, tarea ->
                Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = tarea.descripcion,
                            onValueChange = { vm.cambiar(indice, it); error = null },
                            label = { Text("Tarea ${indice + 1}") },
                            enabled = !ocupado,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularCheckbox(
                                checked = tarea.verificada,
                                onCheckedChange = { vm.verificar(indice, it); error = null },
                                contentDescription = "Preparación verificada de la tarea ${indice + 1}",
                                enabled = !ocupado
                            )
                            Text("Preparación verificada", modifier = Modifier.weight(1f))
                            IconButton(onClick = { vm.quitar(indice) }, enabled = !ocupado) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = "Quitar tarea ${indice + 1}")
                            }
                        }
                    }
                }
            }
            item {
                OutlinedButton(
                    onClick = vm::agregar,
                    enabled = !ocupado && items.size < 50,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Agregar tarea")
                }
            }
            (errorExterno ?: error)?.let { mensaje ->
                item { Text(mensaje, color = MaterialTheme.colorScheme.error) }
            }
        }
    }
}

@Composable
private fun ProgresoTareas() {
    Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(color = AzulTareas, shape = CircleShape) {
                    Text("5", color = Color.White, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                }
                Spacer(Modifier.width(8.dp))
                Text("FLUJO OPERATIVO DE FAENA", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelLarge)
                Text("Paso 5 de 5", style = MaterialTheme.typography.labelMedium)
            }
            LinearProgressIndicator(progress = { 1f }, modifier = Modifier.fillMaxWidth(), color = AzulTareas)
            Text("5. TAREAS DE LA FAENA", style = MaterialTheme.typography.labelLarge)
        }
    }
}
