package com.duoc.aquaflow.ui.screens.faena

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.duoc.aquaflow.domain.Fechas
import com.duoc.aquaflow.domain.rules.EvaluacionSeccion
import com.duoc.aquaflow.model.EstadoFaena
import com.duoc.aquaflow.model.EventoFaena
import com.duoc.aquaflow.model.FaenaDetalle
import com.duoc.aquaflow.model.Usuario
import com.duoc.aquaflow.ui.components.AlertBanner
import com.duoc.aquaflow.ui.components.AquaScreen
import com.duoc.aquaflow.ui.components.DatoFila
import com.duoc.aquaflow.ui.components.DangerButton
import com.duoc.aquaflow.ui.components.EstadoFaenaChip
import com.duoc.aquaflow.ui.components.LoadingBox
import com.duoc.aquaflow.ui.components.LocalAppContainer
import com.duoc.aquaflow.ui.components.MensajeSnackbar
import com.duoc.aquaflow.ui.components.MotivoDialog
import com.duoc.aquaflow.ui.components.NoDisponible
import com.duoc.aquaflow.ui.components.ScreenColumn
import com.duoc.aquaflow.ui.components.SectionCard
import com.duoc.aquaflow.ui.components.StatusChip
import com.duoc.aquaflow.ui.components.Tone
import com.duoc.aquaflow.ui.navigation.Routes
import com.duoc.aquaflow.ui.theme.AquaOnSurfaceVariant
import com.duoc.aquaflow.ui.theme.AquaOrange
import com.duoc.aquaflow.viewmodel.FaenaHubViewModel
import com.duoc.aquaflow.viewmodel.vmFactory

/** Centro de una faena: avance de formularios, acceso a cada paso y bitácora. */
@Composable
fun FaenaScreen(usuario: Usuario, faenaId: Long, onVolver: () -> Unit, onNavegar: (String) -> Unit) {
    val container = LocalAppContainer.current
    val vm: FaenaHubViewModel = viewModel(
        key = "hub-${usuario.id}-$faenaId",
        factory = vmFactory { FaenaHubViewModel(usuario, faenaId, container.faenaRepository, container.reglas) }
    )
    val state by vm.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var cancelando by rememberSaveable { mutableStateOf(false) }
    MensajeSnackbar(state.mensaje, snackbar, vm::consumirMensaje)

    val detalle = state.detalle
    AquaScreen(
        title = detalle?.faena?.codigo ?: "Faena",
        subtitle = "Pre-chequeo de faena",
        onBack = onVolver,
        snackbarHostState = snackbar
    ) { padding ->
        when {
            state.cargando -> LoadingBox(Modifier.padding(padding))
            !state.disponible || detalle == null -> NoDisponible(onVolver, Modifier.padding(padding))
            else -> ScreenColumn(padding) {
                val evaluacion = state.evaluacion
                val faena = detalle.faena

                ResumenFaena(detalle)

                val motivo = faena.decisionMotivo
                if ((faena.estado == EstadoFaena.RECHAZADA || faena.estado == EstadoFaena.CANCELADA) && motivo != null) {
                    AlertBanner(
                        titulo = "${faena.estado.etiqueta} por ${faena.decisionPorNombre ?: "—"}",
                        mensajes = listOf(motivo),
                        tono = Tone.DANGER,
                        icono = Icons.Filled.Block
                    )
                }

                if (evaluacion != null) {
                    val completas = listOf(evaluacion.checklist, evaluacion.ast, evaluacion.salud).count { it.completa }
                    SectionCard(title = "Avance del pre-chequeo") {
                        LinearProgressIndicator(progress = { completas / 3f }, modifier = Modifier.fillMaxWidth(), color = AquaOrange)
                        Text("$completas de 3 formularios completos", style = MaterialTheme.typography.bodyMedium)
                    }

                    Paso(1, "Apertura de faena", "Centro, fecha y buzo asignado", "Listo", Tone.SUCCESS, true) {}
                    Paso(
                        2, "Equipos de soporte vital (DPR-24)", "Checklist de equipos y evidencias fotográficas",
                        estadoSeccion(evaluacion.checklist).first, estadoSeccion(evaluacion.checklist).second, true
                    ) { onNavegar(Routes.dpr24(faenaId)) }
                    Paso(
                        3, "Análisis seguro de trabajo (AST)", "Condiciones previas, riesgos y controles",
                        estadoSeccion(evaluacion.ast).first, estadoSeccion(evaluacion.ast).second, true
                    ) { onNavegar(Routes.ast(faenaId)) }
                    Paso(
                        4, "Encuesta de salud y signos", "Declaración DS 752 y signos vitales",
                        estadoSeccion(evaluacion.salud).first, estadoSeccion(evaluacion.salud).second, true
                    ) { onNavegar(Routes.salud(faenaId)) }
                    Paso(
                        5, "Resumen y decisión", "Auditoría consolidada, firmas y autorización",
                        faena.estado.etiqueta, faena.estado.tonoPaso(), true
                    ) { onNavegar(Routes.decision(faenaId)) }

                    val registroHabilitado = faena.estado == EstadoFaena.AUTORIZADA || faena.estado == EstadoFaena.CERRADA
                    Paso(
                        6, "Registro posterior y cierre", "Parámetros de inmersión y chequeo médico de salida",
                        if (detalle.registro != null) "Completo" else if (registroHabilitado) "Pendiente" else "Bloqueado hasta autorizar",
                        if (detalle.registro != null) Tone.SUCCESS else if (registroHabilitado) Tone.WARNING else Tone.NEUTRAL,
                        registroHabilitado
                    ) { onNavegar(Routes.registro(faenaId)) }
                }

                Bitacora(detalle.eventos)

                if (state.puedeCancelar) {
                    DangerButton(texto = "Cancelar faena", onClick = { cancelando = true })
                }
            }
        }
    }

    if (cancelando) {
        MotivoDialog(
            titulo = "Cancelar faena",
            descripcion = "La faena quedará cancelada y el buzo quedará disponible. Esta acción no se puede deshacer.",
            textoConfirmar = "Cancelar faena",
            error = state.errorCancelar,
            cargando = false,
            onConfirmar = { motivoTexto ->
                vm.cancelar(motivoTexto) {
                    cancelando = false
                    onVolver()
                }
            },
            onCerrar = {
                cancelando = false
                vm.limpiarErrorCancelar()
            }
        )
    }
}

private fun estadoSeccion(seccion: EvaluacionSeccion): Pair<String, Tone> = when {
    seccion.faltantes.isNotEmpty() -> "Pendiente" to Tone.WARNING
    seccion.bloqueos.isNotEmpty() -> "Condición no cumplida" to Tone.DANGER
    seccion.advertencias.isNotEmpty() -> "Completo con avisos" to Tone.INFO
    else -> "Completo" to Tone.SUCCESS
}

private fun EstadoFaena.tonoPaso(): Tone = when (this) {
    EstadoFaena.AUTORIZADA, EstadoFaena.CERRADA -> Tone.SUCCESS
    EstadoFaena.PENDIENTE_AUTORIZACION -> Tone.WARNING
    EstadoFaena.RECHAZADA -> Tone.DANGER
    EstadoFaena.CANCELADA -> Tone.NEUTRAL
    EstadoFaena.PRECHEQUEO -> Tone.INFO
}

@Composable
private fun ResumenFaena(detalle: FaenaDetalle) {
    val faena = detalle.faena
    SectionCard(
        title = "Datos de la faena",
        trailing = { EstadoFaenaChip(faena.estado) }
    ) {
        DatoFila("Centro de cultivo", faena.centroNombre)
        DatoFila("Buzo asignado", faena.buzoNombre)
        DatoFila("Supervisor", faena.supervisorNombre)
        DatoFila("Fecha y hora", Fechas.fechaHora(faena.fechaInicio))
    }
}

@Composable
private fun Paso(
    numero: Int,
    titulo: String,
    detalle: String,
    estadoTexto: String,
    tono: Tone,
    habilitado: Boolean,
    onClick: () -> Unit
) {
    SectionCard(onClick = if (habilitado && numero > 1) onClick else null) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatusChip(numero.toString(), tono = Tone.BRAND)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(titulo, style = MaterialTheme.typography.titleSmall)
                Text(detalle, style = MaterialTheme.typography.bodySmall, color = AquaOnSurfaceVariant)
            }
            StatusChip(estadoTexto, tono = tono)
        }
    }
}

@Composable
private fun Bitacora(eventos: List<EventoFaena>) {
    SectionCard(title = "Bitácora de la faena") {
        if (eventos.isEmpty()) {
            Text("Sin eventos registrados.", style = MaterialTheme.typography.bodyMedium, color = AquaOnSurfaceVariant)
        }
        eventos.asReversed().forEach { evento ->
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    "${Fechas.fechaHora(evento.at)} · ${evento.tipo.etiqueta}",
                    style = MaterialTheme.typography.bodyMedium
                )
                val detalle = if (evento.detalle.isBlank()) evento.actorNombre else "${evento.actorNombre} — ${evento.detalle}"
                Text(detalle, style = MaterialTheme.typography.bodySmall, color = AquaOnSurfaceVariant)
            }
        }
    }
}
