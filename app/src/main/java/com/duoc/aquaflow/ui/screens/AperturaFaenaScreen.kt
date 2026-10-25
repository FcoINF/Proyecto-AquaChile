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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
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
    BackHandler { if (!ui.ocupado) confirmarSalida = true }
    var confirmarRecuperacion by rememberSaveable { mutableStateOf(false) }
    val apertura = ui.apertura

    val centrosCultivo = listOf(
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
        topBar = {
            TopAppBar(
                title = { Text("Apertura de faena") },
                navigationIcon = {
                    TextButton(enabled = !ui.ocupado, onClick = { confirmarSalida = true }) { Text("Volver") }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).imePadding()
                .verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Preparar una operación", style = MaterialTheme.typography.headlineSmall)
                    Text(
                        "Completa los datos y confirma que el equipo esté listo para salir.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                WeatherConditionsCard(
                    condicionesListas = apertura.condicionesListas,
                    onCondicionesListasChange = {
                        onCambiar(apertura.copy(condicionesListas = it))
                    }
                )

                AquaSectionCard(
                    title = "Datos Operativos de Faena",
                    subtitle = "Requerido"
                ) {
                    DropdownField(
                        label = "Centro de cultivo",
                        selectedValue = apertura.centroCultivo,
                        options = centrosCultivo,
                        onSelected = { onCambiar(apertura.copy(centroCultivo = it)) }
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OperationalValueField(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Filled.Place,
                            label = "Fecha de faena",
                            value = apertura.fecha
                        )
                        OperationalValueField(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Filled.AccessTime,
                            label = "Hora de faena",
                            value = apertura.hora
                        )
                    }
                }

                AquaSectionCard(
                    title = "Buzo Técnico Asignado",
                    subtitle = "Selecciona el buzo de esta operación"
                ) {
                    DropdownField(
                        label = "Buzo asignado",
                        selectedValue = apertura.buzoAsignado,
                        options = buzosAsignados,
                        onSelected = { onCambiar(apertura.copy(buzoAsignado = it)) }
                    )
                }

                AquaSectionCard(
                    title = "Condiciones iniciales",
                    subtitle = "Confirma cada condición antes de continuar"
                ) {
                    CondicionSiNoRow(
                        texto = "Embarcación o plataforma de apoyo lista",
                        marcada = apertura.embarcacionLista,
                        testTag = "embarcacion",
                        onCambiar = { onCambiar(apertura.copy(embarcacionLista = it)) }
                    )
                    CondicionSiNoRow(
                        texto = "Comunicaciones y respuesta de emergencia operativas",
                        marcada = apertura.comunicacionesOperativas,
                        testTag = "comunicaciones",
                        onCambiar = { onCambiar(apertura.copy(comunicacionesOperativas = it)) }
                    )
                }

                ui.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                ui.mensaje?.let { Text(it, color = MaterialTheme.colorScheme.tertiary) }
                if (ui.ocupado) Text("Procesando apertura…")

                Button(
                    onClick = onContinuar,
                    enabled = !ui.ocupado && apertura.id.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (ui.checklist == null) "Continuar al checklist" else "Revisar checklist")
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TextButton(
                        onClick = onGuardar,
                        enabled = !ui.ocupado && apertura.id.isNotBlank(),
                        modifier = Modifier.weight(1f)
                    ) { Text("Guardar borrador") }
                    TextButton(
                        onClick = { confirmarRecuperacion = true },
                        enabled = !ui.ocupado && apertura.id.isNotBlank(),
                        modifier = Modifier.weight(1f)
                    ) { Text("Recuperar última") }
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
                    Text("Condiciones Climáticas", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "Mediciones referenciales",
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
                        color = AquaVerde
                    ) {
                        Text(
                            "APTA",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onTertiary
                        )
                    }
                }
            }

            Text(
                "Valores referenciales de presentación; verificar en terreno antes de operar.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

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

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

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
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = AquaGrisSuave,
            border = CardDefaults.outlinedCardBorder()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                androidx.compose.material3.Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = AquaAzulMedio,
                    modifier = Modifier.size(18.dp)
                )
                Text(value, style = MaterialTheme.typography.bodyLarge)
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
    marcada: Boolean,
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
                seleccionada = marcada,
                positiva = true,
                testTag = "$testTag-si",
                modifier = Modifier.weight(1f),
                onClick = { onCambiar(true) }
            )
            CondicionChoice(
                texto = "No",
                seleccionada = !marcada,
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
    onSelected: (String) -> Unit
) {
    var expanded by rememberSaveable { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = selectedValue,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
        )
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
