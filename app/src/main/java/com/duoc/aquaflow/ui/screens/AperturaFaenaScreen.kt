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
    onContinuar: () -> Unit,
    onSalir: () -> Unit
) {
    var confirmarSalida by rememberSaveable { mutableStateOf(false) }
    BackHandler { confirmarSalida = true }
    val apertura = ui.apertura

    if (confirmarSalida) {
        AlertDialog(
            onDismissRequest = { confirmarSalida = false },
            title = { Text("¿Descartar esta faena?") },
            text = { Text("El borrador todavía no se guarda en el dispositivo.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmarSalida = false
                    onSalir()
                }) { Text("Descartar") }
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
                    TextButton(onClick = { confirmarSalida = true }) { Text("Volver") }
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
            Text("Todos los campos son obligatorios.")
            OutlinedTextField(
                value = apertura.centroCultivo,
                onValueChange = { onCambiar(apertura.copy(centroCultivo = it)) },
                label = { Text("Centro de cultivo") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = apertura.fecha,
                onValueChange = { onCambiar(apertura.copy(fecha = it)) },
                label = { Text("Fecha (dd/MM/aaaa)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = apertura.hora,
                onValueChange = { onCambiar(apertura.copy(hora = it)) },
                label = { Text("Hora (HH:mm)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = apertura.buzoAsignado,
                onValueChange = { onCambiar(apertura.copy(buzoAsignado = it)) },
                label = { Text("Buzo asignado") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = apertura.supervisorACargo,
                onValueChange = {},
                readOnly = true,
                label = { Text("Supervisor a cargo") },
                supportingText = { Text("Corresponde al usuario que inició sesión.") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            ui.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            ui.encuesta?.let {
                Text("Encuesta registrada en este borrador.")
                SemaforoSalud(it.nivel)
                Text("Si modificas la apertura, tendrás que completar la encuesta otra vez.")
            }
            Text(
                "Avance académico: por ahora se conecta con Salud. " +
                    "Checklist y AST se integrarán en los siguientes pasos.",
                style = MaterialTheme.typography.bodySmall
            )
            Button(onClick = onContinuar, modifier = Modifier.fillMaxWidth()) {
                Text(if (ui.encuesta == null) "Continuar a Salud" else "Revisar encuesta")
            }
        }
    }
}
