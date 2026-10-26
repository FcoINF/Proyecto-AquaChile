package com.duoc.aquaflow.ui.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.duoc.aquaflow.model.AntecedentesAst

@Composable
fun AntecedentesAstDetalle(a: AntecedentesAst) {
    listOf(
        "Fecha de inicio" to a.fechaInicio, "Turno de inicio" to a.turnoInicio,
        "Fecha de cierre" to a.fechaCierre, "Turno de cierre" to a.turnoCierre,
        "Descripción del trabajo" to a.descripcion, "Ubicación específica" to a.ubicacion,
        "Ejecución" to a.ejecucion,
        "Empresa prestadora de servicio" to a.empresa, "RUT empresa" to a.rutEmpresa,
        "Otra tipología" to a.otraTipologia
    ).filter { it.second.isNotBlank() }.forEach { (etiqueta, valor) -> Text("$etiqueta: $valor") }
}
