package com.duoc.aquaflow.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.duoc.aquaflow.domain.Reglas
import com.duoc.aquaflow.model.EncuestaSalud
import com.duoc.aquaflow.model.PREGUNTAS_SALUD
import com.duoc.aquaflow.ui.components.CampoSalud
import com.duoc.aquaflow.ui.components.AquaSectionCard
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
    vm: EncuestaSaludViewModel,
    buzo: String,
    tabs: List<Tab>,
    onGuardar: (EncuestaSalud) -> Unit,
    onVolver: () -> Unit,
    onTab: (Tab) -> Unit
) {
    // collectAsState convierte el StateFlow del ViewModel en estado de Compose
    val ui by vm.ui.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Encuesta de salud") },
                navigationIcon = { TextButton(onClick = onVolver) { Text("Volver") } }
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

            AquaSectionCard(title = "Parámetros de salud", subtitle = "Ingresa los valores medidos") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CampoSalud(
                        ui.sistolica, vm::cambiarSistolica, "Sistólica",
                        "${Reglas.RANGO_SISTOLICA.first}-${Reglas.RANGO_SISTOLICA.last} mmHg",
                        ui.sistolicaFuera, Modifier.weight(1f)
                    )
                    CampoSalud(
                        ui.diastolica, vm::cambiarDiastolica, "Diastólica",
                        "${Reglas.RANGO_DIASTOLICA.first}-${Reglas.RANGO_DIASTOLICA.last} mmHg",
                        ui.diastolicaFuera, Modifier.weight(1f)
                    )
                }
                CampoSalud(
                    ui.saturacion, vm::cambiarSaturacion, "Saturación O₂ (%)",
                    "${Reglas.RANGO_SATURACION.first}-${Reglas.RANGO_SATURACION.last} %",
                    ui.saturacionFuera
                )
            }

            AquaSectionCard(title = "Encuesta preventiva", subtitle = "Marca Sí cuando la condición esté presente") {
                // Cada "Sí" marcado entra al conjunto de alertas (se agrega o se quita al tocar)
                PREGUNTAS_SALUD.forEach { pregunta ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = pregunta in ui.alertas, onCheckedChange = { vm.alternarAlerta(pregunta) })
                        Text(pregunta, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            // Semáforo en vivo: aparece cuando ya hay presión y saturación
            ui.salud?.let {
                AquaSectionCard(title = "Resultado de la evaluación") { SemaforoSalud(it) }
            }

            // Mensaje de validación devuelto por el ViewModel (si lo hay)
            ui.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Button(
                // Si la validación falla, guardar() deja el mensaje en ui.error y devuelve null
                onClick = { vm.guardar()?.let(onGuardar) },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Guardar") }
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
            onVolver = {},
            onTab = {}
        )
    }
}
