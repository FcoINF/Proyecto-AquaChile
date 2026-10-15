package com.duoc.aquaflow.ui.screens.salud

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.duoc.aquaflow.domain.catalog.Catalogos
import com.duoc.aquaflow.model.Usuario
import com.duoc.aquaflow.ui.components.AlertBanner
import com.duoc.aquaflow.ui.components.AquaScreen
import com.duoc.aquaflow.ui.components.AquaTextField
import com.duoc.aquaflow.ui.components.LoadingBox
import com.duoc.aquaflow.ui.components.LocalAppContainer
import com.duoc.aquaflow.ui.components.MensajeSnackbar
import com.duoc.aquaflow.ui.components.NoDisponible
import com.duoc.aquaflow.ui.components.PrimaryButton
import com.duoc.aquaflow.ui.components.ScreenColumn
import com.duoc.aquaflow.ui.components.SectionCard
import com.duoc.aquaflow.ui.components.SiNoSelector
import com.duoc.aquaflow.ui.components.StatusChip
import com.duoc.aquaflow.ui.components.Tone
import com.duoc.aquaflow.ui.components.rememberSalidaProtegida
import com.duoc.aquaflow.ui.theme.AquaOnSurfaceVariant
import com.duoc.aquaflow.viewmodel.SaludViewModel
import com.duoc.aquaflow.viewmodel.vmFactory

@Composable
fun SaludScreen(usuario: Usuario, faenaId: Long, onVolver: () -> Unit) {
    val container = LocalAppContainer.current
    val vm: SaludViewModel = viewModel(
        key = "salud-${usuario.id}-$faenaId",
        factory = vmFactory { SaludViewModel(usuario, faenaId, container.faenaRepository, container.reglas) }
    )
    val state by vm.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    MensajeSnackbar(state.mensaje, snackbar, vm::consumirMensaje)
    val volver = rememberSalidaProtegida(state.sinGuardar, onVolver)

    AquaScreen(
        title = "Encuesta de salud y signos",
        subtitle = if (state.codigo.isBlank()) "Condición del buzo" else "${state.codigo} · Condición del buzo",
        onBack = volver,
        snackbarHostState = snackbar
    ) { padding ->
        when {
            state.cargando -> LoadingBox(Modifier.padding(padding))
            !state.disponible -> NoDisponible(onVolver, Modifier.padding(padding))
            else -> ScreenColumn(padding) {
                val form = state.form
                val editable = state.editable

                if (!editable) {
                    AlertBanner(
                        titulo = "Solo lectura",
                        mensajes = listOf("Este formulario ya no admite cambios o no tienes permiso para editarlo."),
                        tono = Tone.INFO,
                        icono = Icons.Filled.Info
                    )
                }

                SectionCard(title = "Buzo evaluado") {
                    Text(state.buzoNombre, style = MaterialTheme.typography.titleMedium)
                }

                SectionCard(
                    title = "Signos vitales",
                    trailing = { StatusChip("Rangos provisionales", tono = Tone.NEUTRAL) }
                ) {
                    Text(
                        "Registra las mediciones tomadas al buzo. Los rangos de referencia son provisionales y solo generan avisos: la decisión de aptitud es siempre de las personas responsables.",
                        style = MaterialTheme.typography.bodySmall,
                        color = AquaOnSurfaceVariant
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        AquaTextField(
                            valor = form.sistolica,
                            onValueChange = vm::onSistolica,
                            etiqueta = "Sistólica",
                            sufijo = "mmHg",
                            keyboardType = KeyboardType.Number,
                            enabled = editable,
                            error = state.errores["sistolica"],
                            modifier = Modifier.weight(1f)
                        )
                        AquaTextField(
                            valor = form.diastolica,
                            onValueChange = vm::onDiastolica,
                            etiqueta = "Diastólica",
                            sufijo = "mmHg",
                            keyboardType = KeyboardType.Number,
                            enabled = editable,
                            error = state.errores["diastolica"],
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        AquaTextField(
                            valor = form.saturacion,
                            onValueChange = vm::onSaturacion,
                            etiqueta = "SpO2",
                            sufijo = "%",
                            keyboardType = KeyboardType.Number,
                            enabled = editable,
                            error = state.errores["spo2"],
                            modifier = Modifier.weight(1f)
                        )
                        AquaTextField(
                            valor = form.temperatura,
                            onValueChange = vm::onTemperatura,
                            etiqueta = "Temperatura axilar",
                            sufijo = "°C",
                            keyboardType = KeyboardType.Decimal,
                            enabled = editable,
                            error = state.errores["temperatura"],
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                SectionCard(title = "Declaración DS 752 Armada de Chile") {
                    Text(
                        "Evaluación obligatoria de aptitud psicofísica previa al buceo.",
                        style = MaterialTheme.typography.bodySmall,
                        color = AquaOnSurfaceVariant
                    )
                    Catalogos.declaracionSalud.forEachIndexed { indice, pregunta ->
                        Text("${indice + 1}. ${pregunta.texto}", style = MaterialTheme.typography.bodyLarge)
                        SiNoSelector(
                            valor = form.declaraciones[pregunta.id],
                            onChange = { vm.onDeclaracion(pregunta.id, it) },
                            etiquetaSi = pregunta.etiquetaSi,
                            etiquetaNo = pregunta.etiquetaNo,
                            enabled = editable
                        )
                    }
                }

                SectionCard(title = "Observaciones") {
                    AquaTextField(
                        valor = form.observaciones,
                        onValueChange = vm::onObservaciones,
                        etiqueta = "Observaciones (opcional)",
                        enabled = editable,
                        error = state.errores["observaciones"],
                        singleLine = false,
                        minLines = 2
                    )
                }

                val evaluacion = state.evaluacion
                if (evaluacion.bloqueos.isNotEmpty()) AlertBanner("Buzo NO APTO según su declaración", evaluacion.bloqueos, Tone.DANGER)
                if (evaluacion.advertencias.isNotEmpty()) AlertBanner("Avisos para revisión", evaluacion.advertencias, Tone.WARNING)
                if (evaluacion.faltantes.isNotEmpty()) AlertBanner("Falta completar", evaluacion.faltantes, Tone.INFO, icono = Icons.Filled.Edit)

                if (editable) {
                    PrimaryButton(
                        texto = if (state.sinGuardar) "Guardar encuesta" else "Encuesta guardada",
                        onClick = vm::guardar,
                        enabled = state.sinGuardar,
                        cargando = state.guardando
                    )
                }
            }
        }
    }
}
