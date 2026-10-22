package com.duoc.aquaflow.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.duoc.aquaflow.model.PreChequeo
import com.duoc.aquaflow.ui.components.SemaforoSalud
import com.duoc.aquaflow.viewmodel.FaenaUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AperturaFaenaScreen(
    ui: FaenaUiState,
    onCambiar: (PreChequeo) -> Unit,
    onGuardar: () -> Unit,
    onRecuperar: () -> Unit,
    onContinuar: () -> Unit,
    onSalir: () -> Unit
) {
    var confirmarSalida by rememberSaveable { mutableStateOf(false) }
    BackHandler { if (!ui.ocupado) confirmarSalida = true }
    var confirmarRecuperacion by rememberSaveable { mutableStateOf(false) }
    val apertura = ui.apertura

    if (confirmarRecuperacion) {
        AlertDialog(
            onDismissRequest = { confirmarRecuperacion = false },
            title = { Text("¿Recuperar la última apertura?") },
            text = { Text("Reemplazará los datos en pantalla por tu última apertura guardada. Guarda primero si quieres conservar los cambios actuales.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmarRecuperacion = false
                    onRecuperar()
                }) { Text("Recuperar") }
            },
            dismissButton = {
                TextButton(onClick = { confirmarRecuperacion = false }) { Text("Cancelar") }
            }
        )
    }

    if (confirmarSalida) {
        AlertDialog(
            onDismissRequest = { confirmarSalida = false },
            title = { Text("¿Salir de la apertura?") },
            text = { Text("Los cambios no guardados se perderán. La apertura guardada se conserva.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmarSalida = false
                    onSalir()
                }) { Text("Salir") }
            },
            dismissButton = {
                TextButton(onClick = { confirmarSalida = false }) { Text("Seguir editando") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Apertura de faena") },
                navigationIcon = {
                    TextButton(enabled = !ui.ocupado, onClick = { confirmarSalida = true }) { Text("Volver") }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).imePadding()
                .verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Datos de la faena", style = MaterialTheme.typography.titleLarge)
            Text("Completa todos los campos para continuar. Puedes guardar un borrador incompleto.")
            OutlinedTextField(
                value = apertura.centroCultivo,
                onValueChange = { onCambiar(apertura.copy(centroCultivo = it)) },
                label = { Text("Centro de cultivo") },
                enabled = !ui.ocupado,
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = apertura.fecha,
                onValueChange = { onCambiar(apertura.copy(fecha = it)) },
                label = { Text("Fecha (dd/MM/aaaa)") },
                enabled = !ui.ocupado,
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = apertura.hora,
                onValueChange = { onCambiar(apertura.copy(hora = it)) },
                label = { Text("Hora (HH:mm)") },
                enabled = !ui.ocupado,
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = apertura.buzoAsignado,
                onValueChange = { onCambiar(apertura.copy(buzoAsignado = it)) },
                label = { Text("Buzo asignado") },
                enabled = !ui.ocupado,
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = apertura.supervisorACargo,
                onValueChange = {},
                readOnly = true,
                label = { Text("Supervisor a cargo") },
                supportingText = { Text("Corresponde al usuario que inició sesión.") },
                enabled = !ui.ocupado,
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            ui.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            ui.encuesta?.let {
                Text("Encuesta registrada en este borrador.")
                SemaforoSalud(it.nivel)
                Text("Si modificas la apertura, tendrás que completar la encuesta otra vez.")
            }
            Text("Por ahora se guarda la apertura. Salud y AST deberán completarse nuevamente al reiniciar.",
                style = MaterialTheme.typography.bodySmall)
            ui.mensaje?.let { Text(it, color = MaterialTheme.colorScheme.onSurface) }
            if (ui.ocupado) Text("Procesando apertura…")
            Button(onClick = onGuardar, enabled = !ui.ocupado && apertura.id.isNotBlank(),
                modifier = Modifier.fillMaxWidth()) { Text("Guardar borrador") }
            TextButton(onClick = { confirmarRecuperacion = true },
                enabled = !ui.ocupado && apertura.id.isNotBlank(), modifier = Modifier.fillMaxWidth()) {
                Text("Recuperar última apertura")
            }
            Button(onClick = onContinuar, enabled = !ui.ocupado && apertura.id.isNotBlank(), modifier = Modifier.fillMaxWidth()) {
                Text(if (ui.encuesta == null) "Continuar a Salud" else "Revisar encuesta")
            }
        }
    }
}
