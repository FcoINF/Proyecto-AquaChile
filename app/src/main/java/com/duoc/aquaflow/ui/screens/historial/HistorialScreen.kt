package com.duoc.aquaflow.ui.screens.historial

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.duoc.aquaflow.domain.CategoriaHistorial
import com.duoc.aquaflow.domain.HistorialFiltro
import com.duoc.aquaflow.model.EstadoFaena
import com.duoc.aquaflow.model.HistorialItem
import com.duoc.aquaflow.model.Rol
import com.duoc.aquaflow.model.ResultadoFaena
import com.duoc.aquaflow.ui.navigation.MainBottomBar
import com.duoc.aquaflow.ui.navigation.Tab
import com.duoc.aquaflow.ui.theme.AquaFlowTheme
import com.duoc.aquaflow.viewmodel.HistorialUiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

private val HistorialAzulNoche = Color(0xFF182B3A)
private val HistorialAzul = Color(0xFF0755B8)
private val HistorialFondo = Color(0xFFF5F7FC)
private val HistorialTexto = Color(0xFF172033)
private val HistorialSecundario = Color(0xFF64748B)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistorialScreen(
    state: HistorialUiState,
    tabs: List<Tab>,
    nombreUsuario: String,
    rol: Rol,
    onTexto: (String) -> Unit,
    onFecha: (String) -> Unit = {},
    onCategoria: (CategoriaHistorial) -> Unit,
    onLimpiar: () -> Unit,
    onReintentar: () -> Unit,
    onDetalle: (String) -> Unit,
    onTab: (Tab) -> Unit,
    onSalir: () -> Unit
) {
    val filtro = state.filtro
    var mostrarCalendario by remember { mutableStateOf(false) }
    var mostrarPerfil by remember { mutableStateOf(false) }
    if (mostrarCalendario) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = filtro.fecha.aMillisUtc())
        DatePickerDialog(
            onDismissRequest = { mostrarCalendario = false },
            confirmButton = {
                TextButton(
                    enabled = datePickerState.selectedDateMillis != null,
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis -> onFecha(millis.aFechaUtc()) }
                        mostrarCalendario = false
                    }
                ) { Text("Aplicar") }
            },
            dismissButton = {
                TextButton(onClick = { mostrarCalendario = false }) { Text("Cancelar") }
            }
        ) {
            DatePicker(state = datePickerState, showModeToggle = false)
        }
    }
    if (mostrarPerfil) {
        AlertDialog(
            onDismissRequest = { mostrarPerfil = false },
            title = { Text("Perfil") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(nombreUsuario, style = MaterialTheme.typography.titleSmall)
                    Text(if (rol == Rol.JEFE) "Jefe" else rol.texto, style = MaterialTheme.typography.bodySmall, color = HistorialSecundario)
                }
            },
            confirmButton = {
                TextButton(onClick = onSalir) {
                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                    Spacer(Modifier.size(6.dp))
                    Text("Cerrar sesión")
                }
            },
            dismissButton = { TextButton(onClick = { mostrarPerfil = false }) { Text("Cancelar") } }
        )
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Historial de Faenas", fontWeight = FontWeight.SemiBold) },
                actions = {
                    Text(if (rol == Rol.JEFE) "Jefe" else rol.texto, style = MaterialTheme.typography.labelMedium, color = Color.White)
                    Spacer(Modifier.size(10.dp))
                    IconButton(onClick = { mostrarPerfil = true }, modifier = Modifier.padding(end = 8.dp).size(38.dp)) {
                        Surface(shape = CircleShape, color = Color.White, border = BorderStroke(1.dp, HistorialAzul), modifier = Modifier.size(34.dp)) {}
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = HistorialAzulNoche,
                    titleContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        },
        bottomBar = { MainBottomBar(Tab.HISTORIAL, tabs, onTab) },
        containerColor = HistorialFondo
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 19.dp, vertical = 13.dp),
            verticalArrangement = Arrangement.spacedBy(11.dp)
        ) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    IndicadorCard("TOTAL FAENAS", "${state.total}", "reg", "faenas registradas", Icons.AutoMirrored.Filled.DirectionsRun, Modifier.weight(1f), Color(0xFF0755B8))
                    val horas = state.tiempoFondoMinutos / 60
                    val minutos = state.tiempoFondoMinutos % 60
                    IndicadorCard("TIEMPO FONDO", "${horas}h ${minutos}m", "", "acumulado", Icons.Default.Timer, Modifier.weight(1f), Color(0xFF42658C))
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    IndicadorCard("INCIDENTES / DECO", "${state.alertas}", "alertas", "según evaluación AST y salud", Icons.Default.Shield, Modifier.weight(1f), Color(0xFF059669))
                    IndicadorCard("BUZOS ACTIVOS", "${state.buzosActivos}", "en turno", "inmersiones en ejecución", Icons.Default.Group, Modifier.weight(1f), Color(0xFF42658C))
                }
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    OutlinedTextField(
                        value = filtro.texto,
                        onValueChange = onTexto,
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        placeholder = { Text("Buscar por ID faena, buzo, jaula…", fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF42658C)) },
                        trailingIcon = {
                            IconButton(onClick = { mostrarCalendario = true }) {
                                Icon(Icons.Default.CalendarMonth, contentDescription = "Filtrar por fecha", tint = HistorialAzul)
                            }
                        },
                        shape = RoundedCornerShape(11.dp),
                        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = Color.White,
                            focusedContainerColor = Color.White
                        )
                    )
                    if (filtro.fecha.isNotBlank()) {
                        Surface(color = Color(0xFFE6F0FF), shape = RoundedCornerShape(7.dp)) {
                            Row(Modifier.padding(start = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text("Fecha: ${filtro.fecha}", color = HistorialAzulNoche, style = MaterialTheme.typography.labelSmall)
                                IconButton(onClick = { onFecha("") }, modifier = Modifier.size(32.dp)) {
                                    Icon(Icons.Default.Close, contentDescription = "Quitar filtro de fecha", tint = HistorialAzul, modifier = Modifier.size(15.dp))
                                }
                            }
                        }
                    }
                }
            }
            item {
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    CategoriaHistorial.entries.forEach { categoria ->
                        val cantidad = when (categoria) {
                            CategoriaHistorial.TODAS -> state.total
                            CategoriaHistorial.APROBADAS -> state.aprobadas
                            CategoriaHistorial.OBSERVADAS -> state.observadas
                            CategoriaHistorial.EN_CURSO -> state.enCurso
                            CategoriaHistorial.RECHAZADAS -> state.rechazadas
                        }
                        FilterChip(
                            selected = filtro.categoria == categoria,
                            onClick = { onCategoria(categoria) },
                            label = { Text("${categoria.etiqueta.uppercase()} ($cantidad)", fontSize = 9.sp, fontWeight = FontWeight.Bold) },
                            shape = RoundedCornerShape(10.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = HistorialAzulNoche,
                                selectedLabelColor = Color.White,
                                containerColor = Color.White,
                                labelColor = Color(0xFF334155)
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = filtro.categoria == categoria,
                                borderColor = Color(0xFFE5EAF0),
                                selectedBorderColor = HistorialAzulNoche
                            )
                        )
                    }
                }
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("BITÁCORA OFICIAL DE INMERSIONES", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color(0xFF42658C), fontSize = 9.sp)
                    if (filtro.texto.isNotBlank() || filtro.fecha.isNotBlank() || filtro.categoria != CategoriaHistorial.TODAS) {
                        TextButton(onClick = onLimpiar, contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)) {
                            Text("Limpiar", fontSize = 10.sp)
                        }
                    }
                }
            }

            when {
                state.cargando -> item {
                    Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
                        Text("Cargando historial…", Modifier.padding(18.dp), color = HistorialSecundario)
                    }
                }
                state.error != null -> item {
                    Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(state.error, color = MaterialTheme.colorScheme.error)
                            OutlinedButton(onClick = onReintentar) { Text("Reintentar") }
                        }
                    }
                }
                state.faenas.isEmpty() -> item {
                    Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(14.dp)) {
                        Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.AutoMirrored.Filled.List, contentDescription = null, tint = HistorialAzul, modifier = Modifier.size(28.dp))
                            Text(if (state.total == 0) "Aún no hay faenas" else "Sin resultados", fontWeight = FontWeight.Bold, color = HistorialTexto)
                            Text(if (state.total == 0) "Las faenas registradas aparecerán aquí." else "Prueba con otros filtros.", color = HistorialSecundario, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                else -> items(state.faenas, key = { it.id }) { item ->
                    FaenaHistorialCard(item, onDetalle = { onDetalle(item.id) })
                }
            }
        }
    }
}

private fun String.aMillisUtc(): Long? {
    if (isBlank()) return null
    return runCatching {
        SimpleDateFormat("dd/MM/yyyy", Locale.ROOT).apply {
        isLenient = false
        timeZone = TimeZone.getTimeZone("UTC")
        }.parse(this)?.time
    }.getOrNull()
}

private fun Long.aFechaUtc(): String = SimpleDateFormat("dd/MM/yyyy", Locale.ROOT).apply {
    timeZone = TimeZone.getTimeZone("UTC")
}.format(Date(this))

@Composable
private fun IndicadorCard(titulo: String, valor: String, sufijo: String, pie: String, icono: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier = Modifier, color: Color) {
    Card(
        modifier = modifier.height(82.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(1.dp),
        shape = RoundedCornerShape(11.dp)
    ) {
        Column(Modifier.fillMaxSize().padding(horizontal = 10.dp, vertical = 8.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(titulo, style = MaterialTheme.typography.labelSmall, color = Color(0xFF42658C), fontWeight = FontWeight.Bold, fontSize = 8.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Icon(icono, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
            }
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(valor, style = MaterialTheme.typography.headlineSmall, color = Color(0xFF102033), fontWeight = FontWeight.Bold, fontSize = 23.sp, maxLines = 1)
                if (sufijo.isNotBlank()) Text(sufijo, Modifier.padding(bottom = 3.dp), style = MaterialTheme.typography.labelSmall, color = color, fontWeight = FontWeight.Bold, fontSize = 9.sp)
            }
            Text(pie, style = MaterialTheme.typography.labelSmall, color = color, fontSize = 8.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun FaenaHistorialCard(item: HistorialItem, onDetalle: () -> Unit) {
    val (estado, color, suave) = estiloEstado(item)
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.55f)),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Row(Modifier.fillMaxWidth()) {
            androidx.compose.foundation.layout.Box(Modifier.width(4.dp).heightIn(min = 130.dp).background(color))
            Column(Modifier.weight(1f).padding(10.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            Text("Faena #${item.codigo}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = HistorialTexto)
                            Surface(color = Color(0xFFE6F0FF), shape = RoundedCornerShape(4.dp)) {
                                Text(item.fecha.ifBlank { "S/F" }, Modifier.padding(horizontal = 5.dp, vertical = 2.dp), color = Color(0xFF334155), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Text("⌖ ${item.centroCultivo.ifBlank { "Centro sin asignar" }}", style = MaterialTheme.typography.bodySmall, color = Color(0xFF334155), fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Surface(color = suave, shape = CircleShape) {
                        Text(estado.uppercase(), Modifier.padding(horizontal = 7.dp, vertical = 4.dp), color = color, fontWeight = FontWeight.Bold, fontSize = 8.sp)
                    }
                }
                Row(Modifier.fillMaxWidth().padding(1.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    DatoFaena("BUZO", item.buzoAsignado.ifBlank { "Por asignar" }, Modifier.weight(1f))
                    DatoFaena("SUPERVISOR", item.supervisorACargo.ifBlank { "Por asignar" }, Modifier.weight(1f))
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    MetricaFaena("TIEMPO FONDO", item.tiempoFondoMinutos?.let { "${it} min" } ?: "—", Modifier.weight(1f))
                    MetricaFaena("PRESIÓN", item.encuesta?.let { "${it.sistolica}/${it.diastolica}" } ?: "—", Modifier.weight(1f))
                    MetricaFaena("SPO₂", item.encuesta?.let { "${it.saturacion}%" } ?: "—", Modifier.weight(1f))
                }
                if (item.estado == EstadoFaena.PRECHEQUEO && (item.resultado == ResultadoFaena.OBSERVADO || item.resultado == ResultadoFaena.REQUIERE_REVISION)) {
                    Surface(color = Color(0xFFEAF1FF), shape = RoundedCornerShape(7.dp)) {
                        Row(Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.WarningAmber, contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
                            Text(
                                if (item.resultado == ResultadoFaena.OBSERVADO) "Faena con observaciones para revisión." else "Requiere revisión antes de autorizar.",
                                color = HistorialTexto,
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 9.sp
                            )
                        }
                    }
                }
                OutlinedButton(
                    onClick = onDetalle,
                    modifier = Modifier.fillMaxWidth().height(34.dp),
                    contentPadding = PaddingValues(4.dp),
                    shape = RoundedCornerShape(7.dp),
                    border = BorderStroke(0.dp, Color.Transparent),
                    colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(containerColor = Color(0xFFE5EEFF), contentColor = HistorialAzulNoche)
                ) {
                    Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.size(5.dp))
                    Text("VER BITÁCORA", fontWeight = FontWeight.Bold, fontSize = 9.sp)
                }
            }
        }
    }
}

@Composable
private fun DatoFaena(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier.padding(horizontal = 7.dp, vertical = 5.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color(0xFF42658C), fontWeight = FontWeight.Bold, fontSize = 7.sp)
        Text(value, style = MaterialTheme.typography.labelMedium, color = HistorialTexto, fontWeight = FontWeight.SemiBold, fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun MetricaFaena(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(modifier, color = Color(0xFFEAF1FF), shape = RoundedCornerShape(5.dp)) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 5.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = Color(0xFF42658C), fontWeight = FontWeight.Bold, fontSize = 7.sp, maxLines = 1)
            Text(value, style = MaterialTheme.typography.titleSmall, color = HistorialTexto, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1)
        }
    }
}

private fun estiloEstado(item: HistorialItem): Triple<String, Color, Color> = when {
    item.estado == EstadoFaena.RECHAZADA -> Triple("Rechazada", Color(0xFFDC2626), Color(0xFFFEF2F2))
    item.estado == EstadoFaena.AUTORIZADA || item.estado == EstadoFaena.CERRADA -> Triple("Aprobada", Color(0xFF059669), Color(0xFFE5F8F1))
    item.estado == EstadoFaena.EN_EJECUCION -> Triple("En ejecución", Color(0xFF0755B8), Color(0xFFE6F0FF))
    item.resultado == ResultadoFaena.REQUIERE_REVISION -> Triple("Requiere revisión", Color(0xFFDC2626), Color(0xFFFEF2F2))
    item.resultado == ResultadoFaena.OBSERVADO -> Triple("Observada", Color(0xFFF59E0B), Color(0xFFFFF7E6))
    else -> Triple("En pre-chequeo", Color(0xFF42658C), Color(0xFFEAF1FF))
}

@Preview(showBackground = true, widthDp = 380, heightDp = 850)
@Composable
fun HistorialScreenPreview() {
    AquaFlowTheme {
        HistorialScreen(
            state = HistorialUiState(cargando = false, filtro = HistorialFiltro()),
            tabs = listOf(Tab.INICIO, Tab.HISTORIAL),
            nombreUsuario = "Jefe",
            rol = Rol.JEFE,
            onTexto = {}, onCategoria = {}, onLimpiar = {}, onReintentar = {}, onDetalle = {}, onTab = {}, onSalir = {}
        )
    }
}
