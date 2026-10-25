package com.duoc.aquaflow.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.duoc.aquaflow.domain.ReglasAst
import com.duoc.aquaflow.model.FormularioAst
import com.duoc.aquaflow.model.PREGUNTAS_AST
import com.duoc.aquaflow.model.TIPOLOGIAS_AST
import com.duoc.aquaflow.ui.components.AquaSectionCard
import com.duoc.aquaflow.ui.theme.AquaAzul
import com.duoc.aquaflow.ui.theme.AquaBlanco
import com.duoc.aquaflow.ui.theme.AquaFlowTheme
import com.duoc.aquaflow.viewmodel.AstViewModel

// Solo dibuja y avisa los clics: la validación y el semáforo los resuelven el ViewModel y ReglasAst.
// El centro y la fecha llegan de la apertura de la faena; `onGuardar` entrega el AST validado.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AstScreen(
    vm: AstViewModel,
    centro: String,
    fecha: String,
    onGuardar: (FormularioAst) -> Unit,
    onVolver: () -> Unit
) {
    val ui by vm.ui.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Formulario AST") },
                navigationIcon = { TextButton(onClick = onVolver) { Text("Volver") } }
            )
        }
    ) { padding ->
        Column(
            Modifier.padding(padding).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Análisis Seguro de Trabajo", style = MaterialTheme.typography.headlineSmall)
                Text("$centro · $fecha", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            // 1. Antecedentes generales (ubicación y fecha vienen de la apertura)
            AquaSectionCard(title = "1. Antecedentes generales", subtitle = "Selecciona la tipología de la tarea") {
                TIPOLOGIAS_AST.forEach { tipo ->
                    Row(
                        Modifier.fillMaxWidth().clickable { vm.elegirTipologia(tipo) },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = ui.tipologia == tipo, onClick = { vm.elegirTipologia(tipo) })
                        Text(tipo, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = ui.requiereNuevoAst, onCheckedChange = { vm.alternarNuevoAst() })
                    Text("Requiere un nuevo AST (cambiaron los registros)", style = MaterialTheme.typography.bodyMedium)
                }
            }

            // 2. Chequeo inicial utilizando el botón Switch con ícono de verificación.
            AquaSectionCard(title = "2. Chequeo inicial", subtitle = "Verifica cada condición antes de continuar") {
                PREGUNTAS_AST.forEachIndexed { i, pregunta ->
                    val respuesta = ui.respuestas[i]
                    val estaRespuesta = respuesta == true

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { vm.responder(i, respuesta != true) },
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = pregunta.texto,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (pregunta.critica) {
                                    Text(
                                        text = "Pregunta crítica",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = when (respuesta) {
                                        null -> "Sin responder"
                                        true -> "Sí"
                                        false -> "No"
                                    },
                                    style = MaterialTheme.typography.labelMedium,
                                    color = when (respuesta) {
                                        true -> AquaAzul
                                        false -> MaterialTheme.colorScheme.error
                                        null -> MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                                Switch(
                                    checked = estaRespuesta,
                                    onCheckedChange = { vm.responder(i, it) },
                                    thumbContent = if (estaRespuesta) {
                                        {
                                            Icon(
                                                imageVector = Icons.Filled.Check,
                                                contentDescription = null,
                                                modifier = Modifier.size(SwitchDefaults.IconSize)
                                            )
                                        }
                                    } else null,
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = AquaBlanco,
                                        checkedTrackColor = AquaAzul,
                                        checkedIconColor = AquaAzul,
                                        uncheckedThumbColor = Color(0xFF6C7A86),
                                        uncheckedTrackColor = Color(0xFFE2E8ED),
                                        uncheckedBorderColor = Color(0xFF6C7A86)
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // 3. Pasos de la tarea con su peligro y medida de control
            AquaSectionCard(title = "3. Pasos, peligros y controles", subtitle = "Define el control para cada paso de la tarea") {
                ui.pasos.forEachIndexed { i, paso ->
                    Card(
                        Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Paso ${i + 1}", style = MaterialTheme.typography.titleSmall)
                            OutlinedTextField(
                                value = paso.tarea,
                                onValueChange = { vm.cambiarPaso(i, paso.copy(tarea = it)) },
                                label = { Text("Tarea") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = paso.peligro,
                                onValueChange = { vm.cambiarPaso(i, paso.copy(peligro = it)) },
                                label = { Text("Peligro") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = paso.control,
                                onValueChange = { vm.cambiarPaso(i, paso.copy(control = it)) },
                                label = { Text("Medida de control") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = paso.critico, onCheckedChange = { vm.cambiarPaso(i, paso.copy(critico = it)) })
                                Text("Riesgo crítico", style = MaterialTheme.typography.bodyMedium)
                            }
                            if (ui.pasos.size > 1) TextButton(onClick = { vm.quitarPaso(i) }) { Text("Quitar paso") }
                        }
                    }
                }
                if (ui.pasos.size < ReglasAst.MAX_PASOS) {
                    OutlinedButton(onClick = vm::agregarPaso, modifier = Modifier.fillMaxWidth()) { Text("Agregar paso") }
                }
            }

            // El resultado sigue calculándose y se incluye al guardar.
            ui.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Button(
                onClick = { vm.guardar()?.let(onGuardar) },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Guardar AST") }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AstScreenPreview() {
    AquaFlowTheme {
        AstScreen(
            vm = AstViewModel(),
            centro = "Centro Punta de Tralca",
            fecha = "20/10/2026",
            onGuardar = {},
            onVolver = {}
        )
    }
}
