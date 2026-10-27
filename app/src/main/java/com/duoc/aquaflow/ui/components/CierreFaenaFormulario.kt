package com.duoc.aquaflow.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.viewmodel.compose.viewModel
import com.duoc.aquaflow.model.RegistroCierre
import com.duoc.aquaflow.viewmodel.CierreFaenaViewModel

@Composable
fun CierreFaenaFormulario(id: String, ocupado: Boolean = false, onCerrar: (RegistroCierre) -> Unit) {
    val vm: CierreFaenaViewModel = viewModel(key = "cierre-$id")
    val ui by vm.ui.collectAsState()
    var confirmar by remember { mutableStateOf(false) }
    OutlinedTextField(ui.minutos, vm::minutos, label = { Text("Tiempo de inmersión (minutos)") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
    OutlinedTextField(ui.observaciones, vm::observaciones, label = { Text("Observaciones de salida") },
        modifier = Modifier.fillMaxWidth())
    Row {
        Checkbox(ui.revisionSalida, vm::revision)
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
    Button(enabled = !ocupado, onClick = { if (vm.validar() != null) confirmar = true }, modifier = Modifier.fillMaxWidth()) {
        Text("Cerrar faena")
    }
    if (confirmar) AlertDialog(
        onDismissRequest = { confirmar = false }, title = { Text("¿Confirmar cierre?") },
        text = { Text("El cierre quedará registrado con tu usuario y la fecha y hora del dispositivo. No se podrá sobrescribir.") },
        confirmButton = { TextButton(enabled = !ocupado, onClick = { confirmar = false; vm.validar()?.let(onCerrar) }) { Text("Confirmar cierre") } },
        dismissButton = { TextButton(onClick = { confirmar = false }) { Text("Volver") } }
    )
}
