package com.duoc.aquaflow.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.duoc.aquaflow.model.PreChequeo
import com.duoc.aquaflow.ui.components.AquaSectionCard
import com.duoc.aquaflow.ui.theme.AquaAzulMedio
import com.duoc.aquaflow.ui.theme.AquaFlowTheme
import com.duoc.aquaflow.ui.theme.AquaGrisSuave
import com.duoc.aquaflow.ui.theme.AquaVerde
import com.duoc.aquaflow.viewmodel.FaenaUiState

private const val TURNO_MAREA_PREDEFINIDO = "Mañana (08:30 - 14:30)"

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
    BackHandler { if (!ui.ocupado) confirmarSalida = true }
    var confirmarRecuperacion by rememberSaveable { mutableStateOf(false) }
    val apertura = ui.apertura

    val centrosCultivo = listOf(
        "Centro Huito - Balsa Jaula 04 (Calbuco)",
        "Centro Punta de Tralca",
        "Centro Caleta Abajo",
        "Centro Bahía Profunda",
        "Centro Estero Norte",
        "Centro Norte"
    )

    val buzosAsignados = listOf(
        "Juan Pérez",
        "Pedro Soto",
        "Luis Gómez",
        "Mateo Silva",
        "Alejandro Ruiz"
    )

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
                    enabled = !ui.ocupado && apertura.id.isNotBlank(),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0755B8))
                ) {
                    Text(if (ui.checklist == null) "Continuar al Checklist" else "Revisar checklist")
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
                    onSelected = { onCambiar(apertura.copy(centroCultivo = it)) }
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OperationalValueField(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Filled.Place,
                        label = "Fecha de Faena",
                        value = apertura.fecha
                    )
                    DropdownField(
                        modifier = Modifier.weight(1f),
                        label = "Turno de Marea",
                        selectedValue = TURNO_MAREA_PREDEFINIDO,
                        options = listOf(TURNO_MAREA_PREDEFINIDO),
                        onSelected = {}
                    )
                }
            }
            AquaSectionCard(title = "Buzo Técnico Asignado") {
                DropdownField(
                    label = "",
                    selectedValue = apertura.buzoAsignado,
                    placeholder = "Selecciona un buzo para la operación",
                    options = buzosAsignados,
                    onSelected = { onCambiar(apertura.copy(buzoAsignado = it)) }
                )
            }
            AquaSectionCard(title = "Condiciones Iniciales", subtitle = "Confirma cada condición antes de continuar") {
                CondicionSiNoRow(
                    texto = "Embarcación o plataforma de apoyo lista",
                    respuesta = apertura.embarcacionLista,
                    testTag = "embarcacion",
                    onCambiar = { onCambiar(apertura.copy(embarcacionLista = it)) }
                )
                CondicionSiNoRow(
                    texto = "Comunicaciones y respuesta de emergencia operativas",
                    respuesta = apertura.comunicacionesOperativas,
                    testTag = "comunicaciones",
                    onCambiar = { onCambiar(apertura.copy(comunicacionesOperativas = it)) }
                )
            }
        }
    }
}

@Composable
private fun ProgresoApertura() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(color = Color(0xFF0755B8), shape = CircleShape) {
                    Text("1", modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp), color = Color.White, style = MaterialTheme.typography.labelMedium)
                }
                Text("FLUJO OPERATIVO DE FAENA", style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f))
                Text("Paso 1 de 4", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            LinearProgressIndicator(progress = 0.25f, modifier = Modifier.fillMaxWidth(), color = Color(0xFF0755B8), trackColor = AquaGrisSuave)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("1. Apertura & Buceo", style = MaterialTheme.typography.labelSmall, color = AquaAzulMedio)
                Text("Sig: Inspección de Equipos", style = MaterialTheme.typography.labelSmall, color = Color(0xFF8A9BB3))
            }
        }
    }
}

@Composable
private fun WeatherConditionsCard(
    condicionesListas: Boolean,
    onCondicionesListasChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(36.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = AquaGrisSuave
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        androidx.compose.material3.Icon(
                            imageVector = Icons.Filled.Air,
                            contentDescription = null,
                            tint = AquaAzulMedio,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text("Condiciones en Pontón", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "Telemetría ambiental en tiempo real",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Surface(
                        modifier = Modifier.testTag("clima-apta"),
                        shape = RoundedCornerShape(50),
                        color = Color(0xFFE7FBF3),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF9CE6CA))
                    ) {
                        Text(
                            "APTA",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = AquaVerde
                        )
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                WeatherMetricCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Filled.Air,
                    title = "Viento Superficie",
                    value = "12",
                    detail = "NUDOS SO",
                    supportingLabel = "LÍMITE OPERATIVO",
                    supportingText = "< 20 kts"
                )
                WeatherMetricCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Filled.Waves,
                    title = "Estado Marea",
                    value = "Favorable",
                    detail = null,
                    supportingText = "Reposo marea alta (09:45)"
                )
            }
            CondicionRow(
                texto = "Confirmo la verificación meteorológica real en terreno",
                marcada = condicionesListas,
                onCambiar = onCondicionesListasChange,
                testTag = "weather-manual-confirmation"
            )
        }
    }
}

@Composable
private fun WeatherMetricCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    title: String,
    value: String,
    detail: String?,
    supportingLabel: String? = null,
    supportingText: String
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(AquaGrisSuave)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            androidx.compose.material3.Icon(
                imageVector = icon,
                contentDescription = null,
                tint = AquaAzulMedio,
                modifier = Modifier.size(16.dp)
            )
            Text(
                title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                value,
                style = if (detail == null) MaterialTheme.typography.titleMedium else MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (detail != null) {
                Text(
                    detail,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

        }
        if (supportingLabel != null) {
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(supportingLabel)
                    }
                    append(" ")
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(supportingText)
                    }
                },
                modifier = Modifier.testTag("weather-operational-limit"),
                style = MaterialTheme.typography.bodySmall,
                color = AquaVerde
            )
        } else {
            Text(
                supportingText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun OperationalValueField(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    value: String
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = AquaAzulMedio)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            color = AquaGrisSuave,
            border = CardDefaults.outlinedCardBorder()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 11.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(value, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                if (icon == Icons.Filled.AccessTime) {
                    androidx.compose.material3.Icon(icon, contentDescription = null, tint = AquaAzulMedio, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
private fun CondicionRow(
    texto: String,
    marcada: Boolean,
    testTag: String? = null,
    onCambiar: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCambiar(!marcada) },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = marcada,
            onCheckedChange = onCambiar,
            colors = CheckboxDefaults.colors(
                checkedColor = AquaVerde,
                checkmarkColor = MaterialTheme.colorScheme.onTertiary
            ),
            modifier = if (testTag == null) Modifier else Modifier.testTag(testTag)
        )
        Text(texto, modifier = Modifier.padding(start = 8.dp), style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun CondicionSiNoRow(
    texto: String,
    respuesta: Boolean?,
    testTag: String,
    onCambiar: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(texto, style = MaterialTheme.typography.bodyMedium)
        Row(
            modifier = Modifier.fillMaxWidth().selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CondicionChoice(
                texto = "Sí",
                seleccionada = respuesta == true,
                positiva = true,
                testTag = "$testTag-si",
                modifier = Modifier.weight(1f),
                onClick = { onCambiar(true) }
            )
            CondicionChoice(
                texto = "No",
                seleccionada = respuesta == false,
                positiva = false,
                testTag = "$testTag-no",
                modifier = Modifier.weight(1f),
                onClick = { onCambiar(false) }
            )
        }
    }
}

@Composable
private fun CondicionChoice(
    texto: String,
    seleccionada: Boolean,
    positiva: Boolean,
    testTag: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val afirmativaSeleccionada = seleccionada && positiva
    val negativaSeleccionada = seleccionada && !positiva
    Surface(
        modifier = modifier
            .heightIn(min = 56.dp)
            .selectable(
                selected = seleccionada,
                onClick = onClick,
                role = Role.RadioButton
            )
            .testTag(testTag),
        shape = RoundedCornerShape(16.dp),
        color = when {
            afirmativaSeleccionada -> AquaVerde
            negativaSeleccionada -> MaterialTheme.colorScheme.error
            else -> MaterialTheme.colorScheme.surfaceVariant
        },
        contentColor = when {
            afirmativaSeleccionada -> MaterialTheme.colorScheme.onTertiary
            negativaSeleccionada -> MaterialTheme.colorScheme.onError
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        },
        border = if (seleccionada && !positiva) CardDefaults.outlinedCardBorder() else null
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            androidx.compose.material3.Icon(
                imageVector = if (positiva) Icons.Filled.CheckCircle else Icons.Filled.Cancel,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Text(texto, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropdownField(
    label: String,
    selectedValue: String,
    options: List<String>,
    placeholder: String = "Selecciona una opción",
    modifier: Modifier = Modifier.fillMaxWidth(),
    onSelected: (String) -> Unit
) {
    var expanded by rememberSaveable { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        Column {
            if (label.isNotBlank()) {
                Text(label, style = MaterialTheme.typography.labelSmall, color = AquaAzulMedio, modifier = Modifier.padding(bottom = 5.dp))
            }
            OutlinedTextField(
                value = selectedValue,
                onValueChange = {},
                readOnly = true,
                placeholder = { Text(placeholder, style = MaterialTheme.typography.bodySmall) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = AquaGrisSuave,
                    focusedContainerColor = AquaGrisSuave
                ),
                modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth().heightIn(min = 48.dp)
            )
        }
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AperturaFaenaScreenPreview() {
    AquaFlowTheme {
        AperturaFaenaScreen(
            ui = FaenaUiState(
                apertura = PreChequeo(
                    centroCultivo = "Centro Punta de Tralca",
                    fecha = "20/10/2026",
                    hora = "09:30",
                    buzoAsignado = "Pedro Soto",
                    supervisorACargo = "Carlos Supervisor",
                    condicionesListas = true,
                    embarcacionLista = true,
                    comunicacionesOperativas = true
                )
            ),
            onCambiar = {},
            onGuardar = {},
            onRecuperar = {},
            onContinuar = {},
            onSalir = {}
        )
    }
}
