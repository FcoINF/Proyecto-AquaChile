package com.duoc.aquaflow.ui.screens.registro

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
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
import com.duoc.aquaflow.domain.Fechas
import com.duoc.aquaflow.model.EstadoFaena
import com.duoc.aquaflow.model.RegistroPosterior
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
import com.duoc.aquaflow.ui.components.Tone
import com.duoc.aquaflow.ui.theme.AquaOnSurfaceVariant
import com.duoc.aquaflow.viewmodel.RegistroViewModel
import com.duoc.aquaflow.viewmodel.formatearDecimal
import com.duoc.aquaflow.viewmodel.vmFactory

/** Registro posterior a la inmersión: parámetros, chequeo médico de salida y cierre de la faena. */
@Composable
fun RegistroScreen(usuario: Usuario, faenaId: Long, onVolver: () -> Unit) {
    val container = LocalAppContainer.current
    val vm: RegistroViewModel = viewModel(
        key = "registro-${usuario.id}-$faenaId",
        factory = vmFactory { RegistroViewModel(usuario, faenaId, container.faenaRepository) }
    )
    val state by vm.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    MensajeSnackbar(state.mensaje, snackbar, vm::consumirMensaje)

    AquaScreen(
        title = "Cierre y registro posterior",
        subtitle = if (state.codigo.isBlank()) "Registro posterior" else "${state.codigo} · Registro posterior",
        onBack = onVolver,
        snackbarHostState = snackbar
    ) { padding ->
        when {
            state.cargando -> LoadingBox(Modifier.padding(padding))
            !state.disponible -> NoDisponible(onVolver, Modifier.padding(padding))
            else -> ScreenColumn(padding) {
                SectionCard(title = "Buzo") {
                    Text(state.buzoNombre, style = MaterialTheme.typography.titleMedium)
                }

                val registro = state.registro
                if (registro != null) {
                    ResumenRegistro(registro)
                } else if (state.estado != EstadoFaena.AUTORIZADA) {
                    AlertBanner(
                        titulo = "Registro no disponible",
                        mensajes = listOf("El registro posterior se habilita cuando la faena está autorizada."),
                        tono = Tone.INFO,
                        icono = Icons.Filled.Info
                    )
                } else if (!state.editable) {
                    AlertBanner(
                        titulo = "Solo lectura",
                        mensajes = listOf("Solo el supervisor responsable puede registrar los datos posteriores."),
                        tono = Tone.INFO,
                        icono = Icons.Filled.Info
                    )
                } else {
                    val form = state.form
                    SectionCard(title = "Parámetros de la inmersión") {
                        Text(
                            "Ingresa los datos descargados del instrumento de buceo (ingreso manual).",
                            style = MaterialTheme.typography.bodySmall,
                            color = AquaOnSurfaceVariant
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            AquaTextField(
                                valor = form.tiempo,
                                onValueChange = vm::onTiempo,
                                etiqueta = "Tiempo de fondo",
                                sufijo = "min",
                                keyboardType = KeyboardType.Number,
                                error = state.errores["tiempo"],
                                modifier = Modifier.weight(1f)
                            )
                            AquaTextField(
                                valor = form.profundidad,
                                onValueChange = vm::onProfundidad,
                                etiqueta = "Prof. máxima",
                                sufijo = "m",
                                keyboardType = KeyboardType.Decimal,
                                error = state.errores["profundidad"],
                                modifier = Modifier.weight(1f)
                            )
                        }
                        AquaTextField(
                            valor = form.presion,
                            onValueChange = vm::onPresion,
                            etiqueta = "Presión del banco",
                            sufijo = "bar",
                            keyboardType = KeyboardType.Number,
                            error = state.errores["presion"]
                        )
                    }

                    SectionCard(title = "Chequeo médico de salida") {
                        Text(
                            "Descarte de síntomas posteriores a la inmersión, previo al visado de la faena.",
                            style = MaterialTheme.typography.bodySmall,
                            color = AquaOnSurfaceVariant
                        )
                        Text("¿Sin dolor articular ni parestesia?", style = MaterialTheme.typography.bodyLarge)
                        SiNoSelector(valor = form.sinDolorArticular, onChange = vm::onSinDolor)
                        state.errores["sinDolor"]?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                        Text("¿Respiración normal, sin dolor torácico ni tos?", style = MaterialTheme.typography.bodyLarge)
                        SiNoSelector(valor = form.respiracionNormal, onChange = vm::onRespiracion)
                        state.errores["respiracion"]?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                        AquaTextField(
                            valor = form.observaciones,
                            onValueChange = vm::onObservaciones,
                            etiqueta = "Observaciones (opcional)",
                            error = state.errores["observaciones"],
                            singleLine = false,
                            minLines = 2
                        )
                    }

                    if (form.sinDolorArticular == false || form.respiracionNormal == false) {
                        AlertBanner(
                            titulo = "Síntomas posteriores informados",
                            mensajes = listOf("Registra el detalle en observaciones y avisa a la jefatura de inmediato."),
                            tono = Tone.DANGER
                        )
                    }

                    PrimaryButton(
                        texto = "Registrar y cerrar faena",
                        onClick = vm::guardar,
                        icono = Icons.Filled.CheckCircle,
                        cargando = state.guardando
                    )
                }
            }
        }
    }
}

@Composable
private fun ResumenRegistro(registro: RegistroPosterior) {
    SectionCard(title = "Registro posterior guardado") {
        DatoFila("Tiempo de fondo", "${registro.tiempoInmersionMin} min")
        DatoFila("Profundidad máxima", "${formatearDecimal(registro.profundidadMaxM)} m")
        DatoFila("Presión del banco", "${registro.presionBancoBar} bar")
        DatoFila("Sin dolor articular", if (registro.sinDolorArticular) "Sí" else "No")
        DatoFila("Respiración normal", if (registro.respiracionNormal) "Sí" else "No")
        if (registro.observaciones.isNotBlank()) DatoFila("Observaciones", registro.observaciones)
        DatoFila("Registrado", Fechas.fechaHora(registro.registradoAt))
    }
    if (!registro.sinDolorArticular || !registro.respiracionNormal) {
        AlertBanner(
            titulo = "Síntomas posteriores informados",
            mensajes = listOf("El chequeo de salida registró síntomas. Verifica el seguimiento con la jefatura."),
            tono = Tone.DANGER
        )
    }
}
