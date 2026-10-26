package com.duoc.aquaflow.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.duoc.aquaflow.model.ChecklistTareas
import com.duoc.aquaflow.viewmodel.TareasViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TareasScreen(id: String, inicial: ChecklistTareas?, ocupado: Boolean, errorExterno: String?,
    onGuardar: (ChecklistTareas) -> Unit, onBorrador: (ChecklistTareas) -> Unit, onVolver: () -> Unit) {
    val vm: TareasViewModel = viewModel(key = "tareas-$id")
    LaunchedEffect(id) { vm.cargar(inicial) }
    val items by vm.items.collectAsState()
    var error by remember { mutableStateOf<String?>(null) }
    Scaffold(topBar = { TopAppBar(title = { Text("Checklist de tareas") }, navigationIcon = {
        TextButton(onClick = onVolver, enabled = !ocupado) { Text("Volver") }
    }) }) { padding ->
        Column(Modifier.padding(padding).padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Identificación y control de peligros", style = MaterialTheme.typography.titleLarge)
            Text("Registra los pasos, sus peligros, las medidas de control y las fechas de revisión del supervisor ejecutante.")
            items.forEachIndexed { i, tarea ->
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    OutlinedTextField(tarea.descripcion, { vm.cambiar(i, it); error = null },
                        label = { Text("Paso ${i + 1}") }, enabled = !ocupado, modifier = Modifier.fillMaxWidth())
                    CampoAst("Peligros", tarea.peligro, ocupado) { vm.detalle(i, tarea.copy(peligro = it)); error = null }
                    CampoAst("Medidas de control", tarea.control, ocupado) { vm.detalle(i, tarea.copy(control = it)); error = null }
                    FechasRevisionAst(tarea.fechasRevision, ocupado) { vm.detalle(i, tarea.copy(fechasRevision = it)); error = null }
                    Row(Modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Checkbox(tarea.verificada, { vm.verificar(i, it); error = null }, enabled = !ocupado)
                        Text("Preparación verificada", modifier = Modifier.weight(1f))
                        TextButton(onClick = { vm.quitar(i) }, enabled = !ocupado) { Text("Quitar") }
                    }
                }
            }
            OutlinedButton(onClick = vm::agregar, enabled = !ocupado && items.size < 50) { Text("Agregar tarea") }
            (errorExterno ?: error)?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(modifier = Modifier.fillMaxWidth(), onClick = {
                    val tareas = vm.validar()
                    if (tareas == null) error = "Registra y verifica al menos una tarea; completa todas las tareas agregadas."
                    else onGuardar(tareas)
                }, enabled = !ocupado) { Text("Guardar tareas y revisar") }
                OutlinedButton(onClick = { onBorrador(vm.borrador()) }, enabled = !ocupado,
                    modifier = Modifier.align(androidx.compose.ui.Alignment.End)) { Text("Guardar borrador") }
            }
        }
    }
}
