package com.duoc.aquaflow.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Waves
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.duoc.aquaflow.model.PreChequeo
import com.duoc.aquaflow.ui.components.AquaSectionCard
import com.duoc.aquaflow.ui.components.CampoFecha
import com.duoc.aquaflow.ui.components.CircularCheckbox
import com.duoc.aquaflow.ui.theme.AquaAzulMedio
import com.duoc.aquaflow.ui.theme.AquaFlowTheme
import com.duoc.aquaflow.ui.theme.AquaGrisSuave
import com.duoc.aquaflow.ui.theme.AquaVerde
import com.duoc.aquaflow.viewmodel.FaenaUiState
import com.duoc.aquaflow.domain.Reglas

private const val TURNO_MAREA_PREDEFINIDO = "Mañana (08:30 - 14:30)"

private enum class CalendarioVigencia { EXAMEN, TEST_ESFUERZO }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AperturaFaenaScreen(
    ui: FaenaUiState,
    onCambiar: (PreChequeo) -> Unit,
    onGuardar: () -> Unit,
    onRecuperar: () -> Unit,
    onContinuar: () -> Unit,
    onSalir: () -> Unit
) {
    var confirmarSalida by rememberSaveable { mutableStateOf(false) }
    var mostrarMenuAcciones by rememberSaveable { mutableStateOf(false) }
    var calendarioActivo by rememberSaveable { mutableStateOf<CalendarioVigencia?>(null) }
    BackHandler {
        if (!ui.ocupado) {
            if (calendarioActivo != null) calendarioActivo = null else confirmarSalida = true
        }
    }
    var confirmarRecuperacion by rememberSaveable { mutableStateOf(false) }
    val apertura = ui.apertura

    val centrosCultivo = listOf(
        "Centro Demo Aurora - Jaula 04",
        "Centro Demo Nébula",
        "Centro Demo Fiordo Esmeralda",
        "Centro Demo Bahía Horizonte",
        "Centro Demo Estero Boreal",
        "Centro Demo Faro Sur"
    )

    val buzosAsignados = listOf(
        "Buzo Demo Alfa",
        "Buzo Demo Bravo",
        "Buzo Demo Charlie",
        "Buzo Demo Delta",
        "Buzo Demo Eco"
    )
    val centrosIds = centrosCultivo.mapIndexed { indice, nombre -> nombre to "CENTRO-${indice + 1}" }.toMap()
    val buzosIds = buzosAsignados.mapIndexed { indice, nombre -> nombre to "BUZO-${indice + 1}" }.toMap()
    val bloqueoEsfuerzo = Reglas.bloqueoTestEsfuerzo(apertura)

    if (confirmarRecuperacion) {
        AlertDialog(
            onDismissRequest = { confirmarRecuperacion = false },
            title = { Text("¿Recuperar la última apertura?") },
            text = { Text("Reemplazará los datos en pantalla por tu última apertura guardada. Guarda primero si quieres conservar los cambios actuales.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmarRecuperacion = false
                    onRecuperar()
                }) { Text("Recuperar") }
            },
            dismissButton = {
                TextButton(onClick = { confirmarRecuperacion = false }) { Text("Cancelar") }
            }
        )
    }

    if (confirmarSalida) {
        AlertDialog(
            onDismissRequest = { confirmarSalida = false },
            title = { Text("¿Salir de la apertura?") },
            text = { Text("Los cambios no guardados se perderán. La apertura guardada se conserva.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmarSalida = false
                    onSalir()
                }) { Text("Salir") }
            },
            dismissButton = {
                TextButton(onClick = { confirmarSalida = false }) { Text("Seguir editando") }
            }
        )
    }

    Scaffold(
        containerColor = AquaGrisSuave,
        topBar = {
            TopAppBar(
                title = {
                    Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                        Text("Apertura de Faena", style = MaterialTheme.typography.titleMedium)
                        Text(
                            apertura.centroCultivo.ifBlank { "Centro sin asignar - Jaula 24" },
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                },
                navigationIcon = {
                    androidx.compose.material3.IconButton(enabled = !ui.ocupado, onClick = { confirmarSalida = true }) {
                        androidx.compose.material3.Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    Box {
                        androidx.compose.material3.IconButton(onClick = { mostrarMenuAcciones = true }, enabled = !ui.ocupado) {
                            androidx.compose.material3.Icon(Icons.Filled.MoreVert, contentDescription = "Más opciones")
                        }
                        androidx.compose.material3.DropdownMenu(
                            expanded = mostrarMenuAcciones,
                            onDismissRequest = { mostrarMenuAcciones = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Recuperar última apertura") },
                                onClick = {
                                    mostrarMenuAcciones = false
                                    confirmarRecuperacion = true
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF182B3A),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier.fillMaxWidth().background(AquaGrisSuave).imePadding().padding(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ui.error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                ui.mensaje?.let { Text(it, color = MaterialTheme.colorScheme.tertiary, style = MaterialTheme.typography.bodySmall) }
                if (ui.ocupado) Text("Procesando apertura…", style = MaterialTheme.typography.bodySmall)
                Button(
                    onClick = onContinuar,
                    enabled = !ui.ocupado && apertura.id.isNotBlank() && bloqueoEsfuerzo == null,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0755B8))
                ) {
                    Text(if (ui.checklist == null) "Continuar al Checklist" else "Revisar checklist")
                }
                bloqueoEsfuerzo?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    TextButton(onClick = onContinuar, enabled = !ui.ocupado) {
                        Text("Registrar intento bloqueado")
                    }
                }
                androidx.compose.material3.OutlinedButton(
                    onClick = onGuardar,
                    enabled = !ui.ocupado && apertura.id.isNotBlank(),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 42.dp),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF0755B8)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF0755B8))
                ) { Text("Guardar Borrador") }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ProgresoApertura()
            WeatherConditionsCard(
                condicionesListas = apertura.condicionesListas,
                onCondicionesListasChange = { onCambiar(apertura.copy(condicionesListas = it)) }
            )
            AquaSectionCard(title = "Datos Operativos de Faena", subtitle = "Requerido") {
                DropdownField(
                    label = "Centro de Cultivo",
                    selectedValue = apertura.centroCultivo,
                    placeholder = "Sin asignar",
                    options = centrosCultivo,
                    onSelected = { onCambiar(apertura.copy(centroCultivo = it, centroId = centrosIds.getValue(it))) }
                )
                BoxWithConstraints {
                    if (maxWidth < 520.dp) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            OperationalValueField(
                                icon = Icons.Filled.Place,
                                label = "Fecha de Faena",
                                value = apertura.fecha
                            )
                            DropdownField(
                                label = "Turno de Marea",
                                selectedValue = TURNO_MAREA_PREDEFINIDO,
                                options = listOf(TURNO_MAREA_PREDEFINIDO),
                                onSelected = {}
                            )
                        }
                    } else {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OperationalValueField(
                                modifier = Modifier.weight(2f),
                                icon = Icons.Filled.Place,
                                label = "Fecha de Faena",
                                value = apertura.fecha
                            )
                            DropdownField(
                                modifier = Modifier.weight(3f),