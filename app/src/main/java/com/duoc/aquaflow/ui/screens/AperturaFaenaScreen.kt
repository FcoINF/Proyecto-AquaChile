package com.duoc.aquaflow.ui.screens

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Waves
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.duoc.aquaflow.model.PreChequeo
import com.duoc.aquaflow.ui.components.AquaSectionCard
import com.duoc.aquaflow.ui.components.AquaOptionPicker
import com.duoc.aquaflow.ui.components.CampoFecha
import com.duoc.aquaflow.ui.theme.AquaAzulMedio
import com.duoc.aquaflow.ui.theme.AquaFlowTheme
import com.duoc.aquaflow.ui.theme.AquaGrisSuave
import com.duoc.aquaflow.ui.theme.AquaVerde
import com.duoc.aquaflow.viewmodel.FaenaUiState
import com.duoc.aquaflow.viewmodel.MeteorologiaUi
import com.duoc.aquaflow.model.CENTROS_MARITIMOS
import com.duoc.aquaflow.data.repository.ResultadoMeteorologia
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
    onSalir: () -> Unit,
    meteorologia: MeteorologiaUi = MeteorologiaUi(),
    onActualizarMeteorologia: () -> Unit = {},
    onEditarImc: ((String) -> Unit)? = null
) {
    val view = LocalView.current
    DisposableEffect(view) {
        val controller = (view.context as? Activity)?.window?.let { WindowCompat.getInsetsController(it, view) }
        val aparienciaAnterior = controller?.isAppearanceLightStatusBars
        controller?.isAppearanceLightStatusBars = false
        onDispose {
            if (aparienciaAnterior != null) controller.isAppearanceLightStatusBars = aparienciaAnterior
        }
    }
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

    val centrosIds = CENTROS_MARITIMOS.associate { it.nombre to it.id }
    val centroSeleccionado = CENTROS_MARITIMOS.firstOrNull { it.id == apertura.centroId }

    val buzosAsignados = listOf(
        "Buzo Demo Alfa",
        "Buzo Demo Bravo",
        "Buzo Demo Charlie",
        "Buzo Demo Delta",
        "Buzo Demo Eco"
    )
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
                        Text("Apertura de Faena", style = MaterialTheme.typography.titleMedium,
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            apertura.centroCultivo.ifBlank { "Centro sin asignar" },
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
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
                modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface)
            ) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Column(
                    modifier = Modifier.fillMaxWidth().navigationBarsPadding().imePadding()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ui.error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                    ui.mensaje?.let { Text(it, color = MaterialTheme.colorScheme.tertiary, style = MaterialTheme.typography.bodySmall) }
                    if (ui.ocupado) Text("Procesando apertura…", style = MaterialTheme.typography.bodySmall)
                    Button(
                        onClick = onContinuar,
                        enabled = !ui.ocupado && apertura.id.isNotBlank() && bloqueoEsfuerzo == null,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(if (ui.checklist == null) "Continuar al Checklist" else "Revisar checklist")
                    }
                    bloqueoEsfuerzo?.let {
                        TextButton(onClick = onContinuar, enabled = !ui.ocupado) {
                            Text("Registrar intento bloqueado")
                        }
                    }
                    androidx.compose.material3.OutlinedButton(
                        onClick = onGuardar,
                        enabled = !ui.ocupado && apertura.id.isNotBlank(),
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                        shape = MaterialTheme.shapes.small
                    ) { Text("Guardar Borrador") }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).consumeWindowInsets(padding).verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ProgresoApertura()
            AquaSectionCard(title = "Datos Operativos de Faena") {
                AquaOptionPicker(
                    label = "Centro de Cultivo",
                    title = "Selecciona un centro",
                    value = apertura.centroCultivo,
                    placeholder = "Selecciona un centro",
                    modifier = Modifier.testTag("centro-selector"),
                    enabled = !ui.ocupado,
                    options = centrosIds.keys.toList(),
                    onSelected = { nombre ->
                        val id = centrosIds.getValue(nombre)
                        onCambiar(apertura.copy(centroCultivo = nombre, centroId = id,
                            condicionesListas = apertura.condicionesListas && apertura.centroId == id))
                    }
                )
                BoxWithConstraints {
                    if (maxWidth < 520.dp) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            OperationalValueField(
                                icon = Icons.Filled.DateRange,
                                label = "Fecha de Faena",
                                value = apertura.fecha
                            )
                            OperationalValueField(
                                icon = Icons.Filled.AccessTime,
                                label = "Turno de Marea",
                                value = TURNO_MAREA_PREDEFINIDO
                            )
                        }
                    } else {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OperationalValueField(
                                modifier = Modifier.weight(2f),
                                icon = Icons.Filled.DateRange,
                                label = "Fecha de Faena",
                                value = apertura.fecha
                            )
                            OperationalValueField(
                                modifier = Modifier.weight(3f),
                                icon = Icons.Filled.AccessTime,
                                label = "Turno de Marea",
                                value = TURNO_MAREA_PREDEFINIDO
                            )
                        }
                    }
                }
            }
            WeatherConditionsCard(
                meteorologia = meteorologia.takeIf { it.centro == centroSeleccionado }
                    ?: MeteorologiaUi(centro = centroSeleccionado, cargando = centroSeleccionado != null),
                onActualizar = onActualizarMeteorologia,
                condicionesListas = apertura.condicionesListas,
                onCondicionesListasChange = { onCambiar(apertura.copy(condicionesListas = it)) }
            )
            AquaSectionCard(title = "Buzo Técnico Asignado") {
                AquaOptionPicker(
                    title = "Selecciona un buzo",
                    value = apertura.buzoAsignado,
                    placeholder = "Selecciona un buzo",
                    modifier = Modifier.testTag("buzo-selector"),
                    enabled = !ui.ocupado,
                    options = buzosAsignados,
                    onSelected = { nombre ->
                        val id = buzosIds.getValue(nombre)
                        val mismoBuzo = apertura.buzoAsignado == nombre &&
                            apertura.buzoId in listOf("", "LEGACY-BUZO", id)
                        if (mismoBuzo) {
                            if (apertura.buzoId != id) onCambiar(apertura.copy(buzoId = id))
                        } else {
                            onCambiar(apertura.copy(
                                buzoAsignado = nombre,
                                buzoId = id,
                                edadBuzo = 0,
                                imcBuzo = 0.0,
                                examenPreocupacionalVigente = null,
                                vigenciaExamenPreocupacional = "",
                                puntajeCalbuco = null,
                                testEsfuerzoVigente = null,
                                vigenciaTestEsfuerzo = ""
                            ))
                        }
                    }
                )
            }
            AquaSectionCard(title = "Ficha de salud del buzo", subtitle = "Obligatoria para autorizar la apertura") {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = apertura.edadBuzo.takeIf { it > 0 }?.toString().orEmpty(),
                        onValueChange = { valor ->
                            onCambiar(apertura.copy(edadBuzo = valor.filter(Char::isDigit).take(2).toIntOrNull() ?: 0))
                        },
                        label = { Text("Edad") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = ui.imcTexto ?: apertura.imcBuzo.takeIf { it > 0 }?.toString().orEmpty(),
                        onValueChange = { valor ->
                            if (onEditarImc != null) onEditarImc(valor)
                            else onCambiar(apertura.copy(imcBuzo = valor.replace(',', '.').toDoubleOrNull() ?: 0.0))
                        },
                        label = { Text("IMC") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                }
                CondicionSiNoRow(
                    texto = "Examen preocupacional vigente",
                    respuesta = apertura.examenPreocupacionalVigente,
                    testTag = "examen-preocupacional",
                    onCambiar = { vigente ->
                        if (!vigente && calendarioActivo == CalendarioVigencia.EXAMEN) calendarioActivo = null
                        onCambiar(apertura.copy(examenPreocupacionalVigente = vigente))
                    }
                )
                if (apertura.examenPreocupacionalVigente != false) {
                    CampoFecha(
                        etiqueta = "Vigencia examen",
                        fecha = apertura.vigenciaExamenPreocupacional,
                        desplegado = calendarioActivo == CalendarioVigencia.EXAMEN,
                        habilitado = !ui.ocupado,
                        soloDesdeHoy = true,
                        onAlternar = {
                            calendarioActivo = CalendarioVigencia.EXAMEN.takeUnless { it == calendarioActivo }
                        },
                        onFechaSeleccionada = {
                            calendarioActivo = null
                            onCambiar(apertura.copy(vigenciaExamenPreocupacional = it))
                        }
                    )
                }
                OutlinedTextField(
                    value = apertura.puntajeCalbuco?.toString().orEmpty(),
                    onValueChange = { valor ->
                        onCambiar(apertura.copy(puntajeCalbuco = valor.filter(Char::isDigit).take(3).toIntOrNull()))
                    },
                    label = { Text("Puntaje Calbuco") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                CondicionSiNoRow(
                    texto = "Test de esfuerzo vigente",
                    respuesta = apertura.testEsfuerzoVigente,
                    testTag = "test-esfuerzo",
                    onCambiar = { vigente ->
                        if (!vigente && calendarioActivo == CalendarioVigencia.TEST_ESFUERZO) calendarioActivo = null
                        onCambiar(apertura.copy(testEsfuerzoVigente = vigente))
                    }
                )
                if (apertura.testEsfuerzoVigente != false) {
                    CampoFecha(
                        etiqueta = "Vigencia test de esfuerzo",
                        fecha = apertura.vigenciaTestEsfuerzo,
                        desplegado = calendarioActivo == CalendarioVigencia.TEST_ESFUERZO,
                        habilitado = !ui.ocupado,
                        soloDesdeHoy = true,
                        onAlternar = {
                            calendarioActivo = CalendarioVigencia.TEST_ESFUERZO.takeUnless { it == calendarioActivo }
                        },
                        onFechaSeleccionada = {
                            calendarioActivo = null
                            onCambiar(apertura.copy(vigenciaTestEsfuerzo = it))
                        }
                    )
                }
                bloqueoEsfuerzo?.let {
                    Surface(color = MaterialTheme.colorScheme.errorContainer, shape = RoundedCornerShape(10.dp)) {
                        Text(
                            it,
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            AquaSectionCard(title = "Condiciones Iniciales", subtitle = "Confirma cada condición antes de continuar") {
                CondicionSiNoRow(
                    texto = "Embarcación o plataforma de apoyo lista",
                    respuesta = apertura.embarcacionLista,
                    testTag = "embarcacion",
                    onCambiar = { onCambiar(apertura.copy(embarcacionLista = it)) }
                )
                CondicionSiNoRow(
                    texto = "Buzo de emergencia disponible y apto",
                    respuesta = apertura.buzoEmergenciaDisponible,
                    testTag = "buzo-emergencia",
                    onCambiar = { onCambiar(apertura.copy(buzoEmergenciaDisponible = it)) }
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
                Text("Apertura y buceo", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                Text("Paso 1 de 5", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            }
            LinearProgressIndicator(progress = { 0.2f }, modifier = Modifier.fillMaxWidth(), trackColor = MaterialTheme.colorScheme.surfaceVariant)
            Text("Siguiente: Inspección de equipos", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WeatherConditionsCard(
    meteorologia: MeteorologiaUi,
    onActualizar: () -> Unit,
    condicionesListas: Boolean,
    onCondicionesListasChange: (Boolean) -> Unit
) {
    val colores = MaterialTheme.colorScheme
    val centro = meteorologia.centro
    val resultado = meteorologia.resultado
    val datos = (resultado as? ResultadoMeteorologia.Disponible)?.condiciones
    var mostrarDetalles by rememberSaveable(centro?.id) { mutableStateOf(false) }

    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.outlinedCardColors(containerColor = colores.surface)
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = MaterialTheme.shapes.small, color = colores.primaryContainer, contentColor = colores.onPrimaryContainer) {
                    Icon(Icons.Default.Waves, contentDescription = null, modifier = Modifier.padding(9.dp).size(20.dp))
                }
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("Meteorología marítima", style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
                    Text("Datos de referencia · Open-Meteo", style = MaterialTheme.typography.bodySmall, color = colores.onSurfaceVariant)
                }
            }

            if (centro == null) {
                Text("Selecciona un centro para consultar las condiciones.", style = MaterialTheme.typography.bodyMedium)
            } else {
                Text(centro.sector, style = MaterialTheme.typography.labelLarge)
                when {
                    meteorologia.cargando -> {
                        Text("Consultando condiciones marítimas…", style = MaterialTheme.typography.bodyMedium)
                        LinearProgressIndicator(Modifier.fillMaxWidth())
                    }
                    datos != null -> {
                        Surface(color = colores.background, shape = MaterialTheme.shapes.small) {
                            Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("Oleaje: ${medidaMaritima(datos.alturaOla, "m")}", style = MaterialTheme.typography.titleSmall)
                                Text("Período: ${medidaMaritima(datos.periodoOla, "s")}", style = MaterialTheme.typography.bodyMedium)
                                Text("Dirección: ${medidaMaritima(datos.direccionOla, "°")}", style = MaterialTheme.typography.bodyMedium)
                                Text("Temperatura superficial: ${medidaMaritima(datos.temperaturaSuperficial, "°C")}",
                                    style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                    resultado is ResultadoMeteorologia.SinConexion -> Text(
                        "Sin conexión. Puedes registrar la verificación en terreno.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    resultado is ResultadoMeteorologia.Error -> Text(
                        resultado.mensaje, color = colores.error, style = MaterialTheme.typography.bodyMedium
                    )
                }
                if (!meteorologia.cargando) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (datos != null) {
                            TextButton(onClick = { mostrarDetalles = !mostrarDetalles }) {
                                Text(if (mostrarDetalles) "Ocultar detalles" else "Ver detalles del modelo")
                            }
                        }
                        TextButton(onClick = onActualizar) {
                            Text(if (datos != null) "Actualizar" else "Reintentar consulta")
                        }
                    }
                    if (datos != null && mostrarDetalles) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(java.lang.String.format(java.util.Locale.ROOT, "Punto del modelo: %.2f°, %.2f°",
                                datos.latitudModelo, datos.longitudModelo), style = MaterialTheme.typography.bodySmall)
                            Text("Hora del modelo: ${fechaMaritima(datos.horaModelo)}", style = MaterialTheme.typography.bodySmall)
                            Text("Consulta: ${fechaMaritima(datos.consultadaEn)}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            HorizontalDivider(color = colores.outlineVariant)
            Row(
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)
                    .clip(MaterialTheme.shapes.small)
                    .background(if (condicionesListas) colores.tertiaryContainer else colores.background)
                    .toggleable(value = condicionesListas, role = Role.Checkbox, onValueChange = onCondicionesListasChange)
                    .testTag("weather-manual-confirmation")
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = condicionesListas,
                    onCheckedChange = null,
                    colors = CheckboxDefaults.colors(checkedColor = AquaVerde, uncheckedColor = colores.onSurfaceVariant)
                )
                Text("Verifiqué las condiciones en terreno", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

private fun medidaMaritima(valor: Double?, unidad: String): String =
    valor?.let { java.text.NumberFormat.getNumberInstance(java.util.Locale("es", "CL")).format(it) + " " + unidad }
        ?: "Sin datos"

private fun fechaMaritima(fecha: Long): String =
    java.text.SimpleDateFormat("dd/MM/yyyy HH:mm z", java.util.Locale("es", "CL")).apply {
        timeZone = java.util.TimeZone.getTimeZone("America/Santiago")
    }.format(java.util.Date(fecha))

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
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.small,
            color = AquaGrisSuave
        ) {
            Row(
                modifier = Modifier.heightIn(min = 48.dp).padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(icon, contentDescription = null, tint = AquaAzulMedio, modifier = Modifier.size(18.dp))
                Text(value, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            }
        }
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

@Preview(showBackground = true)
@Composable
fun AperturaFaenaScreenPreview() {
    AquaFlowTheme {
        AperturaFaenaScreen(
            ui = FaenaUiState(
                apertura = PreChequeo(
                    centroCultivo = "Centro Demo Nébula",
                    fecha = "20/10/2026",
                    hora = "09:30",
                    buzoAsignado = "Buzo Demo Bravo",
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
