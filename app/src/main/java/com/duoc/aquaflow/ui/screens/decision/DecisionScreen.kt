package com.duoc.aquaflow.ui.screens.decision

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
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
import com.duoc.aquaflow.model.Usuario
import com.duoc.aquaflow.ui.components.AlertBanner
import com.duoc.aquaflow.ui.components.AquaScreen
import com.duoc.aquaflow.ui.components.DangerButton
import com.duoc.aquaflow.ui.components.DatoFila
import com.duoc.aquaflow.ui.components.EstadoFaenaChip
import com.duoc.aquaflow.ui.components.FotosFila
import com.duoc.aquaflow.ui.components.LoadingBox
import com.duoc.aquaflow.ui.components.LocalAppContainer
import com.duoc.aquaflow.ui.components.MensajeSnackbar
import com.duoc.aquaflow.ui.components.MotivoDialog
import com.duoc.aquaflow.ui.components.NoDisponible
import com.duoc.aquaflow.ui.components.PinDialog
import com.duoc.aquaflow.ui.components.PrimaryButton
import com.duoc.aquaflow.ui.components.ScreenColumn
import com.duoc.aquaflow.ui.components.SectionCard
import com.duoc.aquaflow.ui.components.StatusChip
import com.duoc.aquaflow.ui.components.Tone
import com.duoc.aquaflow.ui.navigation.Routes
import com.duoc.aquaflow.ui.theme.AquaOnSurfaceVariant
import com.duoc.aquaflow.viewmodel.DecisionViewModel
import com.duoc.aquaflow.viewmodel.vmFactory

private enum class Dialogo { NINGUNO, FIRMAR, AUTORIZAR, RECHAZAR }

/** Resumen consolidado de la faena y decisión: firma del supervisor, autorización o rechazo de la jefatura. */
@Composable
fun DecisionScreen(usuario: Usuario, faenaId: Long, onVolver: () -> Unit, onNavegar: (String) -> Unit) {
    val container = LocalAppContainer.current
    val vm: DecisionViewModel = viewModel(
        key = "decision-${usuario.id}-$faenaId",
        factory = vmFactory {
            DecisionViewModel(usuario, faenaId, container.faenaRepository, container.authRepository, container.reglas)
        }
    )
    val state by vm.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var dialogo by rememberSaveable { mutableStateOf(Dialogo.NINGUNO) }
    MensajeSnackbar(state.mensaje, snackbar, vm::consumirMensaje)

    val detalle = state.detalle
    AquaScreen(
        title = "Resumen y decisión",
        subtitle = detalle?.faena?.codigo ?: "Faena",
        onBack = onVolver,
        snackbarHostState = snackbar
    ) { padding ->
        when {
            state.cargando -> LoadingBox(Modifier.padding(padding))
            !state.disponible || detalle == null -> NoDisponible(onVolver, Modifier.padding(padding))
            else -> ScreenColumn(padding) {
                val faena = detalle.faena
                val evaluacion = state.evaluacion

                SectionCard(title = "Faena", trailing = { EstadoFaenaChip(faena.estado) }) {
                    DatoFila("Centro de cultivo", faena.centroNombre)
                    DatoFila("Buzo asignado", faena.buzoNombre)
                    DatoFila("Supervisor", faena.supervisorNombre)
                    DatoFila("Fecha y hora", Fechas.fechaHora(faena.fechaInicio))
                }

                if (evaluacion != null) {
                    SectionCard(title = "Auditoría integral consolidada") {
                        FilaAuditoria("Riesgos (AST)", evaluacion.ast) { onNavegar(Routes.ast(faenaId)) }
                        FilaAuditoria("Equipos (DPR-24)", evaluacion.checklist) { onNavegar(Routes.dpr24(faenaId)) }
                        FilaAuditoria("Fisiología del buzo", evaluacion.salud) { onNavegar(Routes.salud(faenaId)) }
                    }

                    if (detalle.evidencias.isNotEmpty()) {
                        SectionCard(title = "Registro fotográfico (${detalle.evidencias.size})") {
                            FotosFila(detalle.evidencias, editable = false, onEliminar = {})
                        }
                    }

                    if (evaluacion.bloqueos.isNotEmpty()) {
                        AlertBanner("Condiciones de seguridad no cumplidas", evaluacion.bloqueos, Tone.DANGER)
                    }
                    if (evaluacion.advertencias.isNotEmpty()) {
                        AlertBanner("Avisos para revisión", evaluacion.advertencias, Tone.WARNING)
                    }
                    if (evaluacion.faltantes.isNotEmpty() && faena.estado == EstadoFaena.PRECHEQUEO) {
                        AlertBanner("Falta completar antes de firmar", evaluacion.faltantes, Tone.INFO, icono = Icons.Filled.Edit)
                    }
                }

                SectionCard(title = "Firmas de responsabilidad") {
                    FilaFirma(
                        rol = "Supervisor de Buceo",
                        nombre = faena.supervisorNombre,
                        firmada = faena.supervisorFirmaAt != null,
                        detalle = faena.supervisorFirmaAt?.let { Fechas.fechaHora(it) }
                    )
                    val decidida = faena.estado == EstadoFaena.AUTORIZADA || faena.estado == EstadoFaena.CERRADA
                    FilaFirma(
                        rol = "Jefe de Centro / Prevención",
                        nombre = faena.decisionPorNombre ?: "Pendiente de asignar",
                        firmada = decidida,
                        detalle = when (faena.estado) {
                            EstadoFaena.RECHAZADA -> "Rechazada: ${faena.decisionMotivo.orEmpty()}"
                            else -> faena.decisionAt?.let { Fechas.fechaHora(it) }
                        }
                    )
                }

                if (state.puedeEnviar) {
                    PrimaryButton(
                        texto = "Firmar y enviar a revisión",
                        onClick = { dialogo = Dialogo.FIRMAR },
                        icono = Icons.Filled.CheckCircle
                    )
                }
                if (state.puedeDecidir) {
                    if (!state.puedeAutorizar) {
                        AlertBanner(
                            titulo = "No se puede autorizar",
                            mensajes = (evaluacion?.bloqueos.orEmpty() + evaluacion?.faltantes.orEmpty()).ifEmpty { listOf("Revisa la información de la faena.") },
                            tono = Tone.DANGER,
                            icono = Icons.Filled.Info
                        )
                    }
                    PrimaryButton(
                        texto = "Autorizar faena de buceo",
                        onClick = { dialogo = Dialogo.AUTORIZAR },
                        enabled = state.puedeAutorizar,
                        icono = Icons.Filled.CheckCircle
                    )
                    DangerButton(
                        texto = "Rechazar faena (registrar motivo)",
                        onClick = { dialogo = Dialogo.RECHAZAR },
                        icono = Icons.Filled.Cancel
                    )
                }

                Bitacora(detalle.eventos)
            }
        }
    }

    fun cerrarDialogo() {
        dialogo = Dialogo.NINGUNO
        vm.limpiarErrorDialogo()
    }

    when (dialogo) {
        Dialogo.FIRMAR -> PinDialog(
            titulo = "Firma del supervisor",
            descripcion = "Ingresa tu PIN para firmar el pre-chequeo y enviarlo a revisión.",
            textoConfirmar = "Firmar y enviar",
            error = state.errorDialogo,
            cargando = state.operando,
            onConfirmar = { pin -> vm.firmarYEnviar(pin) { cerrarDialogo() } },
            onCerrar = { cerrarDialogo() }
        )
        Dialogo.AUTORIZAR -> PinDialog(
            titulo = "Autorizar faena",
            descripcion = "Ingresa tu PIN de seguridad para autorizar la inmersión. La decisión queda registrada.",
            textoConfirmar = "Autorizar",
            error = state.errorDialogo,
            cargando = state.operando,
            onConfirmar = { pin -> vm.autorizar(pin) { cerrarDialogo() } },
            onCerrar = { cerrarDialogo() }
        )
        Dialogo.RECHAZAR -> MotivoDialog(
            titulo = "Rechazar faena",
            descripcion = "La faena quedará rechazada y no podrá reabrirse. Describe el motivo.",
            textoConfirmar = "Rechazar",
            error = state.errorDialogo,
            cargando = state.operando,
            onConfirmar = { motivo -> vm.rechazar(motivo) { cerrarDialogo() } },
            onCerrar = { cerrarDialogo() }
        )
        Dialogo.NINGUNO -> Unit
    }
}

@Composable
private fun FilaAuditoria(titulo: String, seccion: EvaluacionSeccion, onClick: () -> Unit) {
    val (texto, tono) = when {
        seccion.faltantes.isNotEmpty() -> "Pendiente" to Tone.WARNING
        seccion.bloqueos.isNotEmpty() -> "No cumple" to Tone.DANGER
        seccion.advertencias.isNotEmpty() -> "OK con avisos" to Tone.INFO
        else -> "Aprobado" to Tone.SUCCESS
    }
    SectionCard(onClick = onClick) {
        Row(Modifier, horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(titulo, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
            StatusChip(texto, tono = tono)
        }
    }
}

@Composable
private fun FilaFirma(rol: String, nombre: String, firmada: Boolean, detalle: String?) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(rol, style = MaterialTheme.typography.labelMedium, color = AquaOnSurfaceVariant)
            Text(nombre, style = MaterialTheme.typography.titleSmall)
            if (!detalle.isNullOrBlank()) Text(detalle, style = MaterialTheme.typography.bodySmall, color = AquaOnSurfaceVariant)
        }
        StatusChip(if (firmada) "FIRMADO" else "PENDIENTE", tono = if (firmada) Tone.SUCCESS else Tone.WARNING)
    }
}

@Composable
private fun Bitacora(eventos: List<EventoFaena>) {
    SectionCard(title = "Bitácora") {
        if (eventos.isEmpty()) Text("Sin eventos registrados.", style = MaterialTheme.typography.bodyMedium)
        eventos.asReversed().forEach { evento ->
            Column {
                Text("${Fechas.fechaHora(evento.at)} · ${evento.tipo.etiqueta}", style = MaterialTheme.typography.bodyMedium)
                Text(evento.actorNombre, style = MaterialTheme.typography.bodySmall, color = AquaOnSurfaceVariant)
            }
        }
    }
}
