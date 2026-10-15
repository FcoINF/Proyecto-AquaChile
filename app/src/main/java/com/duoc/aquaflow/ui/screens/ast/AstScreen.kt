package com.duoc.aquaflow.ui.screens.ast

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.duoc.aquaflow.domain.Fechas
import com.duoc.aquaflow.domain.catalog.Catalogos
import com.duoc.aquaflow.model.Usuario
import com.duoc.aquaflow.ui.components.AlertBanner
import com.duoc.aquaflow.ui.components.AquaScreen
import com.duoc.aquaflow.ui.components.AquaTextField
import com.duoc.aquaflow.ui.components.DatoFila
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
import com.duoc.aquaflow.ui.theme.AquaNavyContainer
import com.duoc.aquaflow.ui.theme.AquaOnSurfaceVariant
import com.duoc.aquaflow.viewmodel.AstViewModel
import com.duoc.aquaflow.viewmodel.vmFactory

@Composable
fun AstScreen(usuario: Usuario, faenaId: Long, onVolver: () -> Unit) {
    val container = LocalAppContainer.current
    val vm: AstViewModel = viewModel(
        key = "ast-${usuario.id}-$faenaId",
        factory = vmFactory { AstViewModel(usuario, faenaId, container.faenaRepository, container.reglas) }
    )
    val state by vm.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    MensajeSnackbar(state.mensaje, snackbar, vm::consumirMensaje)
    val volver = rememberSalidaProtegida(state.sinGuardar, onVolver)

    AquaScreen(
        title = "Análisis seguro de trabajo (AST)",
        subtitle = if (state.codigo.isBlank()) "Formulario AST" else "${state.codigo} · Formulario AST",
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

                SectionCard(title = "Antecedentes generales") {
                    DatoFila("Centro de cultivo", state.centro)
                    DatoFila("Fecha y hora", Fechas.fechaHora(state.fechaInicio))
                    AquaTextField(
                        valor = form.tipologia,
                        onValueChange = vm::onTipologia,
                        etiqueta = "Tipología de la tarea",
                        enabled = editable,
                        error = state.errores["tipologia"]
                    )
                    if (editable) {
                        Row(
                            Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Catalogos.tipologiasAst.forEach { sugerida ->
                                Text(
                                    text = sugerida,
                                    style = MaterialTheme.typography.labelMedium,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .background(AquaNavyContainer)
                                        .clickable { vm.onTipologia(sugerida) }
                                        .padding(horizontal = 12.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }
                    AquaTextField(
                        valor = form.tarea,
                        onValueChange = vm::onTarea,
                        etiqueta = "Descripción de la tarea",
                        enabled = editable,
                        error = state.errores["tarea"],
                        singleLine = false,
                        minLines = 2
                    )
                    AquaTextField(
                        valor = form.pasos,
                        onValueChange = vm::onPasos,
                        etiqueta = "Pasos de la tarea",
                        ayuda = "Un paso por línea",
                        enabled = editable,
                        error = state.errores["pasos"],
                        singleLine = false,
                        minLines = 4
                    )
                    AquaTextField(
                        valor = form.profundidad,
                        onValueChange = vm::onProfundidad,
                        etiqueta = "Profundidad máxima planificada (opcional)",
                        sufijo = "m",
                        keyboardType = KeyboardType.Decimal,
                        enabled = editable,
                        error = state.errores["profundidad"]
                    )
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Switch(checked = form.requiereNuevoAst, onCheckedChange = vm::onRequiereNuevoAst, enabled = editable)
                        Text("Se requiere un nuevo AST (cambiaron las condiciones)", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    }
                }

                SectionCard(
                    title = "Gating de seguridad previo",
                    trailing = { StatusChip("OBLIGATORIO", tono = Tone.WARNING) }
                ) {
                    Text(
                        "Responde con precisión. Toda condición \"NO\" suspende la inmersión según el protocolo marítimo.",
                        style = MaterialTheme.typography.bodySmall,
                        color = AquaOnSurfaceVariant
                    )
                    Catalogos.gatingAst.forEach { pregunta ->
                        Text(pregunta.categoria.uppercase(), style = MaterialTheme.typography.labelSmall, color = AquaOnSurfaceVariant)
                        Text(pregunta.texto, style = MaterialTheme.typography.bodyLarge)
                        SiNoSelector(
                            valor = form.gating[pregunta.id],
                            onChange = { vm.onGating(pregunta.id, it) },
                            etiquetaSi = pregunta.etiquetaSi,
                            etiquetaNo = pregunta.etiquetaNo,
                            enabled = editable
                        )
                    }
                }

                val verificados = Catalogos.riesgosAst.count { form.riesgos[it.id] == true }
                SectionCard(
                    title = "Riesgos críticos y control",
                    trailing = { StatusChip("$verificados/${Catalogos.riesgosAst.size} verificados", tono = if (verificados == Catalogos.riesgosAst.size) Tone.SUCCESS else Tone.NEUTRAL) }
                ) {
                    Catalogos.riesgosAst.forEachIndexed { indice, riesgo ->
                        val verificado = form.riesgos[riesgo.id] == true
                        Text("RIESGO CRÍTICO #${indice + 1} · SEVERIDAD ${riesgo.severidad}", style = MaterialTheme.typography.labelSmall, color = AquaOnSurfaceVariant)
                        Text(riesgo.titulo, style = MaterialTheme.typography.titleMedium)
                        Text("Medida de control requerida:", style = MaterialTheme.typography.labelMedium)
                        Text(riesgo.medidaControl, style = MaterialTheme.typography.bodyMedium)
                        Row(
                            modifier = Modifier.toggleable(
                                value = verificado,
                                enabled = editable,
                                role = Role.Checkbox,
                                onValueChange = { vm.onRiesgo(riesgo.id, it) }
                            ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(checked = verificado, onCheckedChange = null, enabled = editable)
                            Text("  Medida de control verificada", style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }

                val evaluacion = state.evaluacion
                if (evaluacion.bloqueos.isNotEmpty()) AlertBanner("Condiciones no cumplidas", evaluacion.bloqueos, Tone.DANGER)
                if (evaluacion.advertencias.isNotEmpty()) AlertBanner("Observaciones", evaluacion.advertencias, Tone.WARNING)
                if (evaluacion.faltantes.isNotEmpty()) AlertBanner("Falta completar", evaluacion.faltantes, Tone.INFO, icono = Icons.Filled.Edit)

                if (editable) {
                    PrimaryButton(
                        texto = if (state.sinGuardar) "Guardar formulario AST" else "Formulario guardado",
                        onClick = vm::guardar,
                        enabled = state.sinGuardar,
                        cargando = state.guardando
                    )
                }
            }
        }
    }
}
