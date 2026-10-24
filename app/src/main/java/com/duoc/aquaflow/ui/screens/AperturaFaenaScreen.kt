package com.duoc.aquaflow.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.duoc.aquaflow.model.PreChequeo
import com.duoc.aquaflow.ui.components.AquaSectionCard
import com.duoc.aquaflow.ui.theme.AquaFlowTheme
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

    val centrosCultivo = listOf(
        "Centro Punta de Tralca",
        "Centro Caleta Abajo",
        "Centro Bahía Profunda",
        "Centro Estero Norte",
        "Centro Norte"
    )

    val buzosAsignados = listOf(
        "Juan Pérez",
        "Pedro Soto",
        "Luis Gómez",
        "Mateo Silva",
        "Alejandro Ruiz"
    )

    val supervisores = listOf(
        apertura.supervisorACargo.ifBlank { "Carlos Supervisor" },
        "Ana Torres",
        "Luis Supervisor",
        "María Albornoz"
    ).distinct()

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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Preparar una operación", style = MaterialTheme.typography.headlineSmall)
                    Text(
                        "Completa los datos y confirma que el equipo esté listo para salir.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                AquaSectionCard(
                    title = "Datos de la faena",
                    subtitle = "Centro, fecha, buzo y supervisor a cargo"
                ) {
                    DropdownField(
                        label = "Centro de cultivo",
                        selectedValue = apertura.centroCultivo,
                        options = centrosCultivo,
                        onSelected = { onCambiar(apertura.copy(centroCultivo = it)) }
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = apertura.fecha,
                            onValueChange = { onCambiar(apertura.copy(fecha = it)) },
                            label = { Text("Fecha") },
                            supportingText = { Text("dd/MM/aaaa") },
                            enabled = !ui.ocupado,
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = apertura.hora,
                            onValueChange = { onCambiar(apertura.copy(hora = it)) },
                            label = { Text("Hora") },
                            supportingText = { Text("HH:mm") },
                            enabled = !ui.ocupado,
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    DropdownField(
                        label = "Buzo asignado",
                        selectedValue = apertura.buzoAsignado,
                        options = buzosAsignados,
                        onSelected = { onCambiar(apertura.copy(buzoAsignado = it)) }
                    )
                    DropdownField(
                        label = "Supervisor a cargo",
                        selectedValue = apertura.supervisorACargo.ifBlank { supervisores.first() },
                        options = supervisores,
                        onSelected = { onCambiar(apertura.copy(supervisorACargo = it)) }
                    )
                }

                AquaSectionCard(
                    title = "Condiciones iniciales",
                    subtitle = "Confirma cada condición antes de continuar"
                ) {
                    CondicionRow("Mar y meteorología aptas", apertura.condicionesListas) {
                        onCambiar(apertura.copy(condicionesListas = it))
                    }
                    CondicionRow("Embarcación o plataforma de apoyo lista", apertura.embarcacionLista) {
                        onCambiar(apertura.copy(embarcacionLista = it))
                    }
                    CondicionRow("Comunicaciones y respuesta de emergencia operativas", apertura.comunicacionesOperativas) {
                        onCambiar(apertura.copy(comunicacionesOperativas = it))
                    }
                }

                ui.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                ui.mensaje?.let { Text(it, color = MaterialTheme.colorScheme.tertiary) }
                if (ui.ocupado) Text("Procesando apertura…")

                Button(
                    onClick = onContinuar,
                    enabled = !ui.ocupado && apertura.id.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (ui.checklist == null) "Continuar al checklist" else "Revisar checklist")
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TextButton(
                        onClick = onGuardar,
                        enabled = !ui.ocupado && apertura.id.isNotBlank(),
                        modifier = Modifier.weight(1f)
                    ) { Text("Guardar borrador") }
                    TextButton(
                        onClick = { confirmarRecuperacion = true },
                        enabled = !ui.ocupado && apertura.id.isNotBlank(),
                        modifier = Modifier.weight(1f)
                    ) { Text("Recuperar última") }
                }
            }
        }
    }

@Composable
private fun CondicionRow(
    texto: String,
    marcada: Boolean,
    onCambiar: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCambiar(!marcada) },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = marcada, onCheckedChange = onCambiar)
        Text(texto, modifier = Modifier.padding(start = 8.dp), style = MaterialTheme.typography.bodyMedium)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropdownField(
    label: String,
    selectedValue: String,
    options: List<String>,
    onSelected: (String) -> Unit
) {
    var expanded by rememberSaveable { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = selectedValue,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AperturaFaenaScreenPreview() {
    AquaFlowTheme {
        AperturaFaenaScreen(
            ui = FaenaUiState(
                apertura = PreChequeo(
                    centroCultivo = "Centro Punta de Tralca",
                    fecha = "20/10/2026",
                    hora = "09:30",
                    buzoAsignado = "Pedro Soto",
                    supervisorACargo = "Carlos Supervisor",
                    condicionesListas = true,
                    embarcacionLista = true,
                    comunicacionesOperativas = true
                )
            ),
            onCambiar = {},
            onGuardar = {},
            onRecuperar = {},
            onContinuar = {},
            onSalir = {}
        )
    }
}
