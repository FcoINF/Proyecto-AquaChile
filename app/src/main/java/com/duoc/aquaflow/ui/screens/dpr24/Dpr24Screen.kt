package com.duoc.aquaflow.ui.screens.dpr24

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.duoc.aquaflow.domain.catalog.Catalogos
import com.duoc.aquaflow.model.EstadoEquipo
import com.duoc.aquaflow.model.Evidencia
import com.duoc.aquaflow.model.TipoEvidencia
import com.duoc.aquaflow.model.Usuario
import com.duoc.aquaflow.ui.components.AccionesEvidencia
import com.duoc.aquaflow.ui.components.AlertBanner
import com.duoc.aquaflow.ui.components.AquaScreen
import com.duoc.aquaflow.ui.components.AquaTextField
import com.duoc.aquaflow.ui.components.EstadoEquipoSelector
import com.duoc.aquaflow.ui.components.FotosFila
import com.duoc.aquaflow.ui.components.LoadingBox
import com.duoc.aquaflow.ui.components.LocalAppContainer
import com.duoc.aquaflow.ui.components.MensajeSnackbar
import com.duoc.aquaflow.ui.components.NoDisponible
import com.duoc.aquaflow.ui.components.PrimaryButton
import com.duoc.aquaflow.ui.components.ScreenColumn
import com.duoc.aquaflow.ui.components.SectionCard
import com.duoc.aquaflow.ui.components.StatusChip
import com.duoc.aquaflow.ui.components.Tone
import com.duoc.aquaflow.ui.components.rememberEvidenciaLauncher
import com.duoc.aquaflow.ui.components.rememberSalidaProtegida
import com.duoc.aquaflow.ui.theme.AquaOnSurfaceVariant
import com.duoc.aquaflow.ui.theme.AquaOrange
import com.duoc.aquaflow.viewmodel.Dpr24ViewModel
import com.duoc.aquaflow.viewmodel.vmFactory

@Composable
fun Dpr24Screen(usuario: Usuario, faenaId: Long, onVolver: () -> Unit) {
    val container = LocalAppContainer.current
    val vm: Dpr24ViewModel = viewModel(
        key = "dpr24-${usuario.id}-$faenaId",
        factory = vmFactory {
            Dpr24ViewModel(usuario, faenaId, container.faenaRepository, container.evidenciaStorage, container.reglas)
        }
    )
    val state by vm.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    MensajeSnackbar(state.mensaje, snackbar, vm::consumirMensaje)

    val launcher = rememberEvidenciaLauncher(
        faenaId = faenaId,
        onEvidencia = { tipo, itemId, ruta -> vm.agregarEvidencia(tipo, itemId, ruta) },
        onError = vm::avisar
    )
    val volver = rememberSalidaProtegida(state.sinGuardar, onVolver)

    AquaScreen(
        title = "Equipos de soporte vital (DPR-24)",
        subtitle = if (state.codigo.isBlank()) "Checklist de equipos" else "${state.codigo} · Checklist de equipos",
        onBack = volver,
        snackbarHostState = snackbar
    ) { padding ->
        when {
            state.cargando -> LoadingBox(Modifier.padding(padding))
            !state.disponible -> NoDisponible(onVolver, Modifier.padding(padding))
            else -> ScreenColumn(padding) {
                if (!state.editable) {
                    AlertBanner(
                        titulo = "Solo lectura",
                        mensajes = listOf("Este formulario ya no admite cambios o no tienes permiso para editarlo."),
                        tono = Tone.INFO,
                        icono = Icons.Filled.Info
                    )
                }

                val revisados = Catalogos.equipos.count { state.items[it.id]?.estado != null }
                SectionCard(title = "Inspección técnica crítica") {
                    LinearProgressIndicator(
                        progress = { revisados / Catalogos.equipos.size.toFloat() },
                        modifier = Modifier.fillMaxWidth(),
                        color = AquaOrange
                    )
                    Text("$revisados de ${Catalogos.equipos.size} equipos revisados", style = MaterialTheme.typography.bodyMedium)
                }

                Catalogos.equipos.forEachIndexed { indice, equipo ->
                    val item = state.items[equipo.id]
                    val estadoItem = item?.estado
                    val fotos = state.evidencias.filter { it.tipo == TipoEvidencia.EQUIPO && it.itemId == equipo.id }
                    SectionCard(
                        title = "Equipo %02d · %s".format(indice + 1, equipo.categoria),
                        trailing = { if (estadoItem != null) StatusChip(estadoItem.etiqueta, tono = estadoItem.tono()) }
                    ) {
                        Text(equipo.nombre, style = MaterialTheme.typography.titleMedium)
                        Text(equipo.descripcion, style = MaterialTheme.typography.bodyMedium, color = AquaOnSurfaceVariant)
                        EstadoEquipoSelector(
                            valor = estadoItem,
                            onChange = { vm.onEstado(equipo.id, it) },
                            enabled = state.editable
                        )
                        if (item != null && (estadoItem == EstadoEquipo.NO_CUMPLE || estadoItem == EstadoEquipo.OBSERVADO)) {
                            AquaTextField(
                                valor = item.nota,
                                onValueChange = { vm.onNota(equipo.id, it) },
                                etiqueta = "Nota obligatoria",
                                error = state.errores[equipo.id],
                                ayuda = "Describe la falla u observación detectada",
                                enabled = state.editable,
                                singleLine = false,
                                minLines = 2
                            )
                        }
                        SeccionFotos(
                            fotos = fotos,
                            editable = state.editable,
                            onCamara = { launcher.capturar(TipoEvidencia.EQUIPO, equipo.id) },
                            onGaleria = { launcher.desdeGaleria(TipoEvidencia.EQUIPO, equipo.id) },
                            onEliminar = { vm.eliminarEvidencia(it.id) },
                            textoVacio = "Sin fotos de este equipo."
                        )
                    }
                }

                SectionCard(title = "Evidencias fotográficas del buzo y los filtros") {
                    Catalogos.slotsEvidencia.forEach { slot ->
                        val fotos = state.evidencias.filter { it.tipo == slot.tipo }
                        Text(slot.titulo, style = MaterialTheme.typography.titleSmall)
                        SeccionFotos(
                            fotos = fotos,
                            editable = state.editable,
                            onCamara = { launcher.capturar(slot.tipo, null) },
                            onGaleria = { launcher.desdeGaleria(slot.tipo, null) },
                            onEliminar = { vm.eliminarEvidencia(it.id) },
                            textoVacio = slot.descripcion
                        )
                    }
                }

                val evaluacion = state.evaluacion
                if (evaluacion.bloqueos.isNotEmpty()) {
                    AlertBanner("Condiciones no cumplidas", evaluacion.bloqueos, Tone.DANGER)
                }
                if (evaluacion.advertencias.isNotEmpty()) {
                    AlertBanner("Observaciones", evaluacion.advertencias, Tone.WARNING)
                }
                if (evaluacion.faltantes.isNotEmpty()) {
                    AlertBanner("Falta completar", evaluacion.faltantes, Tone.INFO, icono = Icons.Filled.Edit)
                }

                if (state.editable) {
                    PrimaryButton(
                        texto = if (state.sinGuardar) "Guardar checklist" else "Checklist guardado",
                        onClick = vm::guardar,
                        enabled = state.sinGuardar,
                        cargando = state.guardando
                    )
                }
            }
        }
    }
}

private fun EstadoEquipo.tono(): Tone = when (this) {
    EstadoEquipo.CUMPLE -> Tone.SUCCESS
    EstadoEquipo.NO_CUMPLE -> Tone.DANGER
    EstadoEquipo.OBSERVADO -> Tone.WARNING
}

@Composable
private fun SeccionFotos(
    fotos: List<Evidencia>,
    editable: Boolean,
    onCamara: () -> Unit,
    onGaleria: () -> Unit,
    onEliminar: (Evidencia) -> Unit,
    textoVacio: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (fotos.isEmpty()) {
            Text(textoVacio, style = MaterialTheme.typography.bodySmall, color = AquaOnSurfaceVariant)
        } else {
            FotosFila(fotos, editable, onEliminar)
        }
        if (editable) AccionesEvidencia(onCamara = onCamara, onGaleria = onGaleria)
    }
}
