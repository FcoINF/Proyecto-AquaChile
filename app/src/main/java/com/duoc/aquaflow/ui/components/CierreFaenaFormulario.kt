package com.duoc.aquaflow.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.duoc.aquaflow.model.RegistroCierre
import com.duoc.aquaflow.model.OrigenMedicion
import com.duoc.aquaflow.viewmodel.CierreFaenaViewModel

@Composable
fun CierreFaenaFormulario(id: String, ocupado: Boolean = false, onCerrar: (RegistroCierre) -> Unit) {
    val vm: CierreFaenaViewModel = viewModel(key = "cierre-$id")
    val ui by vm.ui.collectAsState()
    var confirmar by remember { mutableStateOf<RegistroCierre?>(null) }
    OutlinedTextField(ui.minutos, vm::minutos, label = { Text("Tiempo de inmersión (minutos)") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
    OutlinedTextField(ui.fondo, vm::fondo, label = { Text("Tiempo de fondo (minutos)") }, enabled = !ocupado,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
    MedicionDecimal("Profundidad máxima (m)", ui.profundidad, vm::profundidad, ocupado)
    MedicionDecimal("Velocidad máxima de descenso (m/min)", ui.descenso, vm::descenso, ocupado)
    MedicionDecimal("Velocidad máxima de ascenso (m/min)", ui.ascenso, vm::ascenso, ocupado)
    Text("Origen de las mediciones", style = MaterialTheme.typography.titleSmall)
    OrigenMedicion.entries.forEach { origen ->
        FilterChip(
            selected = ui.origen == origen,
            onClick = { vm.origen(origen) },
            label = { Text(origen.texto) },
            enabled = !ocupado
        )
    }
    OutlinedTextField(ui.observaciones, vm::observaciones, label = { Text("Observaciones de salida") },
        modifier = Modifier.fillMaxWidth())
    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
        CircularCheckbox(
            checked = ui.revisionSalida,
            onCheckedChange = vm::revision,
            contentDescription = "Revisión de salida realizada",
            enabled = !ocupado
        )
        Text("Revisión de salida realizada")
    }
    Text("Salud posterior a la inmersión", style = MaterialTheme.typography.titleMedium)
    OutlinedTextField(ui.sistolica, vm::sistolica, label = { Text("Presión sistólica de salida") }, enabled = !ocupado,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
    OutlinedTextField(ui.diastolica, vm::diastolica, label = { Text("Presión diastólica de salida") }, enabled = !ocupado,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
    OutlinedTextField(ui.saturacion, vm::saturacion, label = { Text("Saturación de salida (%)") }, enabled = !ocupado,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())

    ui.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    Button(enabled = !ocupado, onClick = { confirmar = vm.validar() }, modifier = Modifier.fillMaxWidth()) {
        Text("Cerrar faena")
    }
    confirmar?.let { cierre ->
        AlertDialog(
            onDismissRequest = { confirmar = null }, title = { Text("¿Confirmar cierre?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("El cierre quedará registrado con tu usuario y la fecha y hora del dispositivo. No se podrá sobrescribir.")
                    cierre.mediciones?.alertas.orEmpty().forEach { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            },
            confirmButton = { TextButton(enabled = !ocupado, onClick = { confirmar = null; onCerrar(cierre) }) { Text("Confirmar cierre") } },
            dismissButton = { TextButton(onClick = { confirmar = null }) { Text("Volver") } }
        )
    }
}

@Composable
private fun MedicionDecimal(etiqueta: String, valor: String, actualizar: (String) -> Unit, ocupado: Boolean) {
    OutlinedTextField(valor, actualizar, label = { Text(etiqueta) }, enabled = !ocupado,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
}
