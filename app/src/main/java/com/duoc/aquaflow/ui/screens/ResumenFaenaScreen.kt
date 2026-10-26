package com.duoc.aquaflow.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.duoc.aquaflow.domain.ReglasSecciones
import com.duoc.aquaflow.model.EstadoSeccion
import com.duoc.aquaflow.domain.ReglasDecision
import com.duoc.aquaflow.domain.Reglas
import com.duoc.aquaflow.model.Decision
import com.duoc.aquaflow.model.EstadoEquipo
import com.duoc.aquaflow.model.PreChequeo
import com.duoc.aquaflow.model.RegistroDecision
import com.duoc.aquaflow.model.ResumenFaena
import com.duoc.aquaflow.model.Rol
import com.duoc.aquaflow.ui.components.AquaSectionCard
import com.duoc.aquaflow.ui.components.SemaforoAst
import com.duoc.aquaflow.ui.components.SemaforoFaena
import com.duoc.aquaflow.ui.components.SemaforoSalud
import com.duoc.aquaflow.ui.theme.AquaFlowTheme
import com.duoc.aquaflow.viewmodel.ResumenFaenaViewModel

// Muestra todo lo recolectado y, si el usuario es de la jefatura, permite autorizar o rechazar.
// No decide nada: valida el ViewModel y calculan las reglas del dominio.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResumenFaenaScreen(
    vm: ResumenFaenaViewModel,
    resumen: ResumenFaena,
    decision: RegistroDecision?,
    rol: Rol,
    nombreUsuario: String,
    onDecidir: (RegistroDecision) -> Unit,
    onVolver: () -> Unit,
    onFinalizar: () -> Unit,
    textoFinalizar: String = "Ir al inicio",
    // Error que no nace en esta pantalla (por ejemplo, al guardar la decisión en el dispositivo)
    cierre: com.duoc.aquaflow.model.RegistroCierre? = null,
    onCerrar: ((com.duoc.aquaflow.model.RegistroCierre) -> Unit)? = null,
    ocupado: Boolean = false,
    errorExterno: String? = null
) {
    val ui by vm.ui.collectAsState()
    val resultado = ReglasDecision.resultado(resumen)
    val faltantes = ReglasDecision.faltantes(resumen)
    val apertura = resumen.apertura
    val estados = ReglasSecciones.estados(resumen)

    com.duoc.aquaflow.ui.components.Guardando(ocupado)
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Resumen de faena") },
                navigationIcon = { TextButton(onClick = onVolver) { Text("Volver") } }
            )
        }
    ) { padding ->
        Column(
            Modifier.padding(padding).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Revisión de la operación", style = MaterialTheme.typography.headlineSmall)
                Text("Confirma los datos y resuelve la autorización.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            // Semáforo global: solo aparece cuando están todas las partes
            if (resultado != null) {
                AquaSectionCard(title = "Estado de la faena") { SemaforoFaena(resultado) }
            } else {
                Card(colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                    Text(
                        "Resumen incompleto · Falta: ${faltantes.joinToString()}",
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }

            Seccion("Apertura") {
                Text("Estado: ${estados.getValue("Apertura").texto}")
                Text(apertura.centroCultivo.ifBlank { "Centro sin definir" })
                Text("${apertura.fecha} ${apertura.hora}".trim())
                Text("Buzo: ${apertura.buzoAsignado.ifBlank { "sin asignar" }}")
                Text("Supervisor: ${apertura.supervisorACargo.ifBlank { "sin asignar" }}")
            }

            Seccion("Checklist de equipos") {
                Text("Estado: ${estados.getValue("Checklist").texto}")
                val items = resumen.checklist?.items.orEmpty()
                if (resumen.checklist != null && items.isEmpty()) Text("Sin equipos registrados")
                items.forEach {
                    Text("${it.titulo} · ${it.estado.texto()}")
                    if (it.notas.isNotBlank()) Text("Nota · ${it.notas}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                resumen.checklist?.let {
                    Text("Fotos de evidencia: ${it.fotosEvidencia.size}")
                    com.duoc.aquaflow.ui.components.EvidenciasGaleria(it.fotosEvidencia)
                }
            }

            Seccion("AST") {
                Text("Estado: ${estados.getValue("AST").texto}")
                val ast = resumen.ast
                if (ast != null) {
                    Text(ast.tipologia)
                    Text("Pasos: ${ast.pasos.size} · Riesgos críticos: ${ast.pasos.count { it.critico }}")
                    if (ast.requiereNuevoAst) Text("Requiere un nuevo AST")
                    if (estados.getValue("AST") == EstadoSeccion.COMPLETA) SemaforoAst(ast.resultado)
                }
            }

            Seccion("Salud (ficticia)") {
                Text("Estado: ${estados.getValue("Encuesta de salud").texto}")
                val e = resumen.encuesta
                if (e != null) {
                    Text("Presión ${e.sistolica}/${e.diastolica} · Saturación ${e.saturacion}%")
                    e.alertasEncuesta.forEach { Text("• $it", color = MaterialTheme.colorScheme.error) }
                    if (estados.getValue("Encuesta de salud") == EstadoSeccion.COMPLETA) SemaforoSalud(Reglas.nivelSalud(e.sistolica, e.diastolica, e.saturacion, e.alertasEncuesta.size))
                }
            }

            Seccion("Checklist de tareas") {
                Text("Estado: ${estados.getValue("Checklist de tareas").texto}")
                resumen.tareas?.items?.forEach { Text("${if (it.verificada) "✓" else "Pendiente"} · ${it.descripcion}") }
            }

            errorExterno?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Seccion("Decisión") {
                when {
                    // Ya decidida: se muestra el registro y se cierra el formulario
                    decision != null -> {
                        Text("${decision.decision.texto} por ${decision.decididoPor}")
                        if (decision.motivo.isNotEmpty()) Text("Motivo: ${decision.motivo}")
                        Button(onClick = onFinalizar, modifier = Modifier.fillMaxWidth()) { Text(textoFinalizar) }
                    }
                    // El supervisor no decide: su parte termina aquí, con la faena guardada para la jefatura
                    !ReglasDecision.puedeDecidir(rol) -> {
                        Text("Solo la jefatura autoriza o rechaza la faena. Ya quedó guardada en el dispositivo para su revisión.")
                        Button(onClick = onFinalizar, modifier = Modifier.fillMaxWidth()) { Text(textoFinalizar) }
                    }
                    else -> {
                        OutlinedTextField(
                            value = ui.motivo,
                            onValueChange = vm::cambiarMotivo,
                            label = { Text("Motivo (obligatorio al rechazar)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        ui.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                enabled = resultado != null && !ocupado,
                                onClick = { vm.decidir(Decision.AUTORIZADA, resumen, rol, nombreUsuario)?.let(onDecidir) },
                                modifier = Modifier.weight(1f)
                            ) { Text("Autorizar") }
                            OutlinedButton(
                                enabled = resultado != null && !ocupado,
                                onClick = { vm.decidir(Decision.RECHAZADA, resumen, rol, nombreUsuario)?.let(onDecidir) },
                                modifier = Modifier.weight(1f)
                            ) { Text("Rechazar") }
                        }
                    }
                }
            }
            if (cierre != null) {
                Seccion("Faena cerrada") {
                    Text("Tiempo de inmersión: ${cierre.duracionMinutos} minutos")
                    Text("Observaciones: ${cierre.observaciones}")
                    Text("Revisión de salida: ${if (cierre.revisionSalida) "realizada" else "pendiente"}")
                    Text("Cerrada por ${cierre.cerradoPor}")
                    Text(java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date(cierre.cerradoEn)))
                }
            }

        }
    }
}

// Tarjeta con título para cada parte del resumen
@Composable
private fun Seccion(titulo: String, contenido: @Composable () -> Unit) {
    AquaSectionCard(title = titulo) { contenido() }
}

private fun EstadoEquipo.texto() = when (this) {
    EstadoEquipo.CUMPLE -> "Cumple"
    EstadoEquipo.NO_CUMPLE -> "No cumple"
    EstadoEquipo.OBSERVADO -> "Observado"
    EstadoEquipo.PENDIENTE -> "Pendiente"
}

@Preview(showBackground = true)
@Composable
fun ResumenFaenaScreenPreview() {
    AquaFlowTheme {
        ResumenFaenaScreen(
            vm = ResumenFaenaViewModel(),
            resumen = ResumenFaena(
                apertura = PreChequeo(
                    centroCultivo = "Centro Punta de Tralca",
                    fecha = "20/10/2026",
                    hora = "09:30",
                    buzoAsignado = "Pedro Soto",
                    supervisorACargo = "Carlos Supervisor"
                ),
                checklist = null,
                ast = null,
                encuesta = null
            ),
            decision = null,
            rol = Rol.JEFE,
            nombreUsuario = "Jefe Operaciones",
            onDecidir = {},
            onVolver = {},
            onFinalizar = {}
        )
    }
}
