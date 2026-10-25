package com.duoc.aquaflow.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.duoc.aquaflow.domain.Reglas
import com.duoc.aquaflow.model.EncuestaSalud
import com.duoc.aquaflow.model.PREGUNTAS_SALUD
import com.duoc.aquaflow.ui.components.AquaSectionCard
import com.duoc.aquaflow.ui.components.ParametroSaludStepper
import com.duoc.aquaflow.ui.components.SemaforoSalud
import com.duoc.aquaflow.ui.navigation.MainBottomBar
import com.duoc.aquaflow.ui.navigation.Tab
import com.duoc.aquaflow.ui.theme.AquaFlowTheme
import com.duoc.aquaflow.viewmodel.EncuestaSaludViewModel

// Solo dibuja y avisa los clics: la validación la hace el ViewModel y el semáforo, Reglas.
// `onGuardar` entrega la encuesta validada al flujo del pre-chequeo.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EncuestaSaludScreen(
    ocupado: Boolean = false,
    errorExterno: String? = null,
    vm: EncuestaSaludViewModel,
    buzo: String,
    tabs: List<Tab>,
    onGuardar: (EncuestaSalud) -> Unit,
    onGuardarBorrador: (EncuestaSalud) -> Unit,
    onVolver: () -> Unit,
    onTab: (Tab) -> Unit
) {
    // collectAsState convierte el StateFlow del ViewModel en estado de Compose
    val ui by vm.ui.collectAsState()

    com.duoc.aquaflow.ui.components.Guardando(ocupado)
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Encuesta de salud") },
                navigationIcon = { TextButton(onClick = onVolver, enabled = !ocupado) { Text("Volver") } }
            )
        },
        bottomBar = { MainBottomBar(Tab.ENCUESTA, tabs, onTab) }
    ) { padding ->
        Column(
            Modifier.padding(padding).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Control preventivo", style = MaterialTheme.typography.headlineSmall)
                Text("Completa los parámetros y la encuesta antes de continuar.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Buzo asignado", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(buzo, style = MaterialTheme.typography.titleMedium)
                    Text("Registro de apoyo académico; no constituye un diagnóstico médico.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            AquaSectionCard(title = "Presión arterial", subtitle = "Valores medidos · mmHg") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ParametroSaludStepper(
                        valor = ui.sistolica,
                        alCambiar = vm::cambiarSistolica,
                        etiqueta = "Sistólica",
                        rango = Reglas.RANGO_SISTOLICA,
                        unidad = "mmHg",
                        fueraDeRango = ui.sistolicaFuera,
                        modifier = Modifier.weight(1f)
                    )
                    ParametroSaludStepper(
                        valor = ui.diastolica,
                        alCambiar = vm::cambiarDiastolica,
                        etiqueta = "Diastólica",
                        rango = Reglas.RANGO_DIASTOLICA,
                        unidad = "mmHg",
                        fueraDeRango = ui.diastolicaFuera,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            AquaSectionCard(title = "SPO2", subtitle = "Saturación de oxígeno") {
                ParametroSaludStepper(
                    valor = ui.saturacion,
                    alCambiar = vm::cambiarSaturacion,
                    etiqueta = "Saturación O₂",
                    rango = Reglas.RANGO_SATURACION,
                    unidad = "%",
                    fueraDeRango = ui.saturacionFuera
                )
            }

            AquaSectionCard(
                title = "Encuesta preventiva",
                subtitle = "Sí agrega una alerta; No indica que la condición no está presente."
            ) {
                PREGUNTAS_SALUD.forEachIndexed { indice, pregunta ->
                    if (indice > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "${indice + 1}. $pregunta",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.selectableGroup()
                        ) {
                            RespuestaSaludChip(
                                texto = "Sí",
                                seleccionada = ui.respuestas[pregunta] == true,
                                respuestaAfirmativa = true,
                                tag = "pregunta-$indice-si",
                                modifier = Modifier.weight(1f),
                                onClick = { vm.responderPregunta(pregunta, respuestaAfirmativa = true) }
                            )
                            RespuestaSaludChip(
                                texto = "No",
                                seleccionada = ui.respuestas[pregunta] == false,
                                respuestaAfirmativa = false,
                                tag = "pregunta-$indice-no",
                                modifier = Modifier.weight(1f),
                                onClick = { vm.responderPregunta(pregunta, respuestaAfirmativa = false) }
                            )
                        }
                    }
                }
            }

            // Semáforo en vivo: aparece cuando ya hay presión y saturación
            ui.salud?.let {
                AquaSectionCard(title = "Resultado de la evaluación") { SemaforoSalud(it) }
            }

            // Mensaje de validación devuelto por el ViewModel (si lo hay)
            (errorExterno ?: ui.error)?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            androidx.compose.material3.OutlinedButton(
                enabled = !ocupado,
                onClick = { onGuardarBorrador(vm.borrador()) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) { Text("Guardar borrador") }
            Button(
                // Si la validación falla, guardar() deja el mensaje en ui.error y devuelve null
                enabled = !ocupado,
                onClick = { vm.guardar()?.let(onGuardar) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) { Text("Guardar") }
        }
    }
}

@Composable
private fun RespuestaSaludChip(
    texto: String,
    seleccionada: Boolean,
    respuestaAfirmativa: Boolean,
    tag: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val colorSeleccionado = if (respuestaAfirmativa) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.tertiary
    }
    val colorContenidoSeleccionado = if (respuestaAfirmativa) {
        MaterialTheme.colorScheme.onError
    } else {
        MaterialTheme.colorScheme.onTertiary
    }

    Surface(
        modifier = modifier
            .heightIn(min = 56.dp)
            .selectable(
                selected = seleccionada,
                onClick = onClick,
                role = Role.RadioButton
            )
            .testTag(tag),
        shape = RoundedCornerShape(12.dp),
        color = if (seleccionada) colorSeleccionado else MaterialTheme.colorScheme.surfaceVariant,
        contentColor = if (seleccionada) {
            colorContenidoSeleccionado
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (respuestaAfirmativa) Icons.Filled.CheckCircle else Icons.Filled.Cancel,
                contentDescription = if (respuestaAfirmativa) "Check positivo de Si" else "X de No",
                modifier = Modifier.size(20.dp)
            )
            Text(texto, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun EncuestaSaludScreenPreview() {
    AquaFlowTheme {
        EncuestaSaludScreen(
            vm = EncuestaSaludViewModel(),
            buzo = "Pedro Soto",
            tabs = listOf(Tab.INICIO, Tab.ENCUESTA, Tab.HISTORIAL),
            onGuardar = {},
            onGuardarBorrador = {},
            onVolver = {},
            onTab = {}
        )
    }
}
