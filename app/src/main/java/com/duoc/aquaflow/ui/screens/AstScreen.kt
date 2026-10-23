package com.duoc.aquaflow.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.duoc.aquaflow.domain.ReglasAst
import com.duoc.aquaflow.model.FormularioAst
import com.duoc.aquaflow.model.PREGUNTAS_AST
import com.duoc.aquaflow.model.TIPOLOGIAS_AST
import com.duoc.aquaflow.ui.components.SemaforoAst
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Análisis Seguro de Trabajo", style = MaterialTheme.typography.titleMedium)
            Text("$centro · $fecha")

            // 1. Antecedentes generales (ubicación y fecha vienen de la apertura)
            Text("1. Antecedentes generales", style = MaterialTheme.typography.titleMedium)
            Text("Tipología de la tarea")
            TIPOLOGIAS_AST.forEach { tipo ->
                Row(
                    Modifier.fillMaxWidth().clickable { vm.elegirTipologia(tipo) },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(selected = ui.tipologia == tipo, onClick = { vm.elegirTipologia(tipo) })
                    Text(tipo)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = ui.requiereNuevoAst, onCheckedChange = { vm.alternarNuevoAst() })
                Text("Requiere un nuevo AST (cambiaron los registros)")
            }

            // 2. Chequeo inicial Sí/No
            Text("2. Chequeo inicial", style = MaterialTheme.typography.titleMedium)
            PREGUNTAS_AST.forEachIndexed { i, pregunta ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text(pregunta.texto + if (pregunta.critica) " (crítica)" else "")
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = ui.respuestas[i] == true, onClick = { vm.responder(i, true) })
                            Text("Sí")
                            RadioButton(selected = ui.respuestas[i] == false, onClick = { vm.responder(i, false) })
                            Text("No")
                        }
                    }
                }
            }

            // 3. Pasos de la tarea con su peligro y medida de control
            Text("3. Pasos, peligros y controles", style = MaterialTheme.typography.titleMedium)
            ui.pasos.forEachIndexed { i, paso ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                            Text("Riesgo crítico")
                        }
                        if (ui.pasos.size > 1) TextButton(onClick = { vm.quitarPaso(i) }) { Text("Quitar paso") }
                    }
                }
            }
            if (ui.pasos.size < ReglasAst.MAX_PASOS) {
                OutlinedButton(onClick = vm::agregarPaso, modifier = Modifier.fillMaxWidth()) { Text("Agregar paso") }
            }

            // Semáforo en vivo y guardado
            ui.resultado?.let { SemaforoAst(it) }
            ui.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Button(
                // Si la validación falla, guardar() deja el mensaje en ui.error y devuelve null
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
