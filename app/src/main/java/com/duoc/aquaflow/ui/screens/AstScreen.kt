package com.duoc.aquaflow.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.duoc.aquaflow.model.*
import com.duoc.aquaflow.ui.components.AquaSectionCard
import com.duoc.aquaflow.ui.components.Guardando
import com.duoc.aquaflow.viewmodel.AstViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AstScreen(
    ocupado: Boolean = false,
    errorExterno: String? = null,
    vm: AstViewModel,
    centro: String,
    fecha: String,
    onGuardar: (FormularioAst) -> Unit,
    onGuardarBorrador: (FormularioAst) -> Unit,
    onVolver: () -> Unit
) {
    val ui by vm.ui.collectAsState()
    val a = ui.antecedentes
    LaunchedEffect(centro, fecha) {
        vm.antecedentes(vm.ui.value.antecedentes.let {
            it.copy(fechaInicio = it.fechaInicio.ifBlank { fecha }, ubicacion = it.ubicacion.ifBlank { centro })
        })
    }
    Guardando(ocupado)
    Scaffold(topBar = {
        TopAppBar(title = { Text("Formulario AST") }, navigationIcon = {
            TextButton(onClick = onVolver, enabled = !ocupado) { Text("Volver") }
        })
    }) { padding ->
        Column(Modifier.padding(padding).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Planificación y ejecución de trabajos críticos", style = MaterialTheme.typography.titleMedium)
            Text("Análisis Seguro de Trabajo (AST)", style = MaterialTheme.typography.headlineSmall)
            Text("Código: SSO_P_03_REG_01 · Fecha: 18-03-2026 · Revisión: 01 · Versión: 03",
                style = MaterialTheme.typography.bodySmall)
            AquaSectionCard(title = "1. Antecedentes generales") {
                CampoAst("Fecha de inicio del trabajo", a.fechaInicio, ocupado) { vm.antecedentes(a.copy(fechaInicio = it)) }
                CampoAst("Turno de inicio", a.turnoInicio, ocupado) { vm.antecedentes(a.copy(turnoInicio = it)) }
                CampoAst("Fecha de cierre del trabajo", a.fechaCierre, ocupado) { vm.antecedentes(a.copy(fechaCierre = it)) }
                CampoAst("Turno de cierre", a.turnoCierre, ocupado) { vm.antecedentes(a.copy(turnoCierre = it)) }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(ui.requiereNuevoAst, { vm.alternarNuevoAst() }, enabled = !ocupado)
                    Text("Existen cambios: requiere un nuevo AST", Modifier.weight(1f))
                }
                CampoAst("Descripción del trabajo a ejecutar", a.descripcion, ocupado) { vm.antecedentes(a.copy(descripcion = it)) }
                CampoAst("Ubicación específica del trabajo", a.ubicacion, ocupado) { vm.antecedentes(a.copy(ubicacion = it)) }
                Text("Ejecución del trabajo", style = MaterialTheme.typography.titleSmall)
                listOf("Ejecución propia", "Ejecución empresa prestadora de servicio").forEach { opcion ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(a.ejecucion == opcion, { vm.antecedentes(a.copy(ejecucion = opcion)) }, enabled = !ocupado)
                        Text(opcion, Modifier.weight(1f))
                    }
                }
                if (a.ejecucion == "Ejecución empresa prestadora de servicio") {
                    CampoAst("Nombre de la empresa prestadora de servicio", a.empresa, ocupado) { vm.antecedentes(a.copy(empresa = it)) }
                    CampoAst("RUT empresa prestadora de servicio", a.rutEmpresa, ocupado) { vm.antecedentes(a.copy(rutEmpresa = it)) }
                }
            }
            AquaSectionCard(title = "2. Tipología del trabajo", subtitle = "Marca las que correspondan") {
                val seleccionadas = ui.tipologia.split("\n").filter { it.isNotBlank() }
                (TIPOLOGIAS_AST + seleccionadas.filter { it !in TIPOLOGIAS_AST }).forEach { tipo ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(tipo in seleccionadas, { marcada ->
                            vm.elegirTipologia((if (marcada) seleccionadas + tipo else seleccionadas - tipo).joinToString("\n"))
                        }, enabled = !ocupado)
                        Text(tipo, Modifier.weight(1f))
                    }
                }
                if ("Otro" in seleccionadas) {
                    CampoAst("Otro (describa)", a.otraTipologia, ocupado) { vm.antecedentes(a.copy(otraTipologia = it)) }
                }
            }
            AquaSectionCard(title = "3. Chequeo inicial") {
                PREGUNTAS_AST.forEachIndexed { i, pregunta ->
                    Column {
                        Text(pregunta.texto)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(ui.respuestas.getOrNull(i) == true, { vm.responder(i, true) }, enabled = !ocupado)
                            Text("Sí")
                            RadioButton(ui.respuestas.getOrNull(i) == false, { vm.responder(i, false) }, enabled = !ocupado)
                            Text("No")
                        }
                    }
                }
                Text("Si una respuesta es NO, repórtelo a su jefatura directa y NO inicie sus labores hasta que todas sus respuestas sean SÍ.")
            }
            (errorExterno ?: ui.error)?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Button(onClick = { vm.guardar()?.let(onGuardar) }, enabled = !ocupado, modifier = Modifier.fillMaxWidth()) { Text("Guardar AST") }
            OutlinedButton(onClick = { onGuardarBorrador(vm.borrador()) }, enabled = !ocupado, modifier = Modifier.align(Alignment.End)) { Text("Guardar borrador") }
        }
    }
}

@Composable
internal fun CampoAst(titulo: String, valor: String, ocupado: Boolean, cambiar: (String) -> Unit) {
    OutlinedTextField(value = valor, onValueChange = cambiar, label = { Text(titulo) },
        enabled = !ocupado, modifier = Modifier.fillMaxWidth())
}

@Composable
internal fun FechasRevisionAst(fechas: List<String>, ocupado: Boolean, cambiar: (List<String>) -> Unit) {
    Text("Verificaciones del supervisor ejecutante · Fechas de revisión", style = MaterialTheme.typography.titleSmall)
    repeat(4) { i ->
        CampoAst("Revisión ${i + 1} (dd/mm/aaaa)", fechas.getOrElse(i) { "" }, ocupado) { valor ->
            cambiar(List(4) { indice -> if (indice == i) valor else fechas.getOrElse(indice) { "" } })
        }
    }
}
