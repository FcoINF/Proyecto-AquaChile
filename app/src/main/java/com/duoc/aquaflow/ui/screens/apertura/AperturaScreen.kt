package com.duoc.aquaflow.ui.screens.apertura

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.duoc.aquaflow.domain.Fechas
import com.duoc.aquaflow.model.Usuario
import com.duoc.aquaflow.ui.components.AlertBanner
import com.duoc.aquaflow.ui.components.AquaScreen
import com.duoc.aquaflow.ui.components.LocalAppContainer
import com.duoc.aquaflow.ui.components.OpcionCard
import com.duoc.aquaflow.ui.components.PrimaryButton
import com.duoc.aquaflow.ui.components.ScreenColumn
import com.duoc.aquaflow.ui.components.SecondaryButton
import com.duoc.aquaflow.ui.components.SectionCard
import com.duoc.aquaflow.ui.components.Tone
import com.duoc.aquaflow.viewmodel.AperturaViewModel
import com.duoc.aquaflow.viewmodel.vmFactory
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AperturaScreen(usuario: Usuario, onVolver: () -> Unit, onCreada: (Long) -> Unit) {
    val container = LocalAppContainer.current
    val vm: AperturaViewModel = viewModel(
        key = "apertura-${usuario.id}",
        factory = vmFactory { AperturaViewModel(usuario, container.faenaRepository, container.catalogoRepository) }
    )
    val state by vm.state.collectAsStateWithLifecycle()
    var mostrarFecha by rememberSaveable { mutableStateOf(false) }
    var mostrarHora by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(state.faenaCreadaId) {
        state.faenaCreadaId?.let { id ->
            vm.consumirCreada()
            onCreada(id)
        }
    }

    AquaScreen(title = "Apertura de faena", subtitle = "Paso 1 · Datos operativos", onBack = onVolver) { padding ->
        ScreenColumn(padding) {
            SectionCard(title = "Centro de cultivo") {
                state.centros.forEach { centro ->
                    OpcionCard(
                        titulo = centro.nombre,
                        detalle = centro.ubicacion,
                        seleccionado = state.centroId == centro.id,
                        onClick = { vm.onCentro(centro.id) }
                    )
                }
                state.errores["centro"]?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }

            SectionCard(title = "Fecha y hora de la faena") {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SecondaryButton(
                        texto = Fechas.fecha(state.fecha),
                        onClick = { mostrarFecha = true },
                        icono = Icons.Filled.DateRange,
                        modifier = Modifier.weight(1f)
                    )
                    SecondaryButton(
                        texto = "%02d:%02d".format(state.hora.hour, state.hora.minute),
                        onClick = { mostrarHora = true },
                        modifier = Modifier.weight(1f)
                    )
                }
                state.errores["fecha"]?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }

            SectionCard(title = "Buzo asignado") {
                state.buzos.forEach { buzo ->
                    val vigenteHasta = LocalDate.ofEpochDay(buzo.certificacionVigenteHastaEpochDay)
                    val vencida = vigenteHasta.isBefore(state.fecha)
                    OpcionCard(
                        titulo = buzo.nombre,
                        detalle = "${buzo.categoria} · Mat. ${buzo.matricula}\nCertificación vigente hasta ${Fechas.fecha(vigenteHasta)}",
                        advertencia = if (vencida) "Certificación vencida para la fecha seleccionada" else null,
                        seleccionado = state.buzoId == buzo.id,
                        enabled = !vencida,
                        onClick = { vm.onBuzo(buzo.id) }
                    )
                }
                state.errores["buzo"]?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }

            SectionCard(title = "Supervisor a cargo") {
                Text(usuario.nombre, style = MaterialTheme.typography.bodyLarge)
                Text(usuario.rol.etiqueta, style = MaterialTheme.typography.bodySmall)
            }

            state.errorGeneral?.let { AlertBanner("No se pudo abrir la faena", listOf(it), Tone.DANGER) }

            PrimaryButton(
                texto = "Abrir faena y continuar",
                onClick = vm::crear,
                enabled = state.centroId != null && state.buzoId != null,
                cargando = state.guardando
            )
        }
    }

    if (mostrarFecha) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = state.fecha.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { mostrarFecha = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { millis ->
                        vm.onFecha(Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate())
                    }
                    mostrarFecha = false
                }) { Text("Aceptar") }
            },
            dismissButton = { TextButton(onClick = { mostrarFecha = false }) { Text("Cancelar") } }
        ) {
            DatePicker(state = pickerState)
        }
    }

    if (mostrarHora) {
        val horaState = rememberTimePickerState(
            initialHour = state.hora.hour,
            initialMinute = state.hora.minute,
            is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { mostrarHora = false },
            confirmButton = {
                TextButton(onClick = {
                    vm.onHora(LocalTime.of(horaState.hour, horaState.minute))
                    mostrarHora = false
                }) { Text("Aceptar") }
            },
            dismissButton = { TextButton(onClick = { mostrarHora = false }) { Text("Cancelar") } },
            text = { TimePicker(state = horaState) }
        )
    }
}
