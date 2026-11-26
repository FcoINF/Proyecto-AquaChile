package com.duoc.aquaflow.ui.screens.historial

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.duoc.aquaflow.domain.CategoriaHistorial
import com.duoc.aquaflow.domain.HistorialFiltro
import com.duoc.aquaflow.model.EstadoFaena
import com.duoc.aquaflow.model.HistorialItem
import com.duoc.aquaflow.model.ResultadoFaena
import com.duoc.aquaflow.model.Rol
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

private data class EstiloTarjetaFaena(
    val etiqueta: String,
    val color: Color,
    val suave: Color,
    val mensajeOperativo: String?,
    val textoBoton: String,
    val iconoBoton: ImageVector,
    val botonPrimario: Boolean
)

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
    onCierresRecibidos: (() -> Unit)? = null,
    onSalir: () -> Unit
) {
    val filtro = state.filtro
    var mostrarCalendario by remember { mutableStateOf(false) }
    var mostrarPerfil by remember { mutableStateOf(false) }

    val fuenteOperaciones = state.todasFaenas.ifEmpty { state.faenas }
    val operacionesActivas = remember(fuenteOperaciones) {
        fuenteOperaciones.filter {
            it.estado == EstadoFaena.AUTORIZADA || it.estado == EstadoFaena.EN_EJECUCION
        }
    }
    val autorizadasPendientes = remember(fuenteOperaciones) {
        fuenteOperaciones.count { it.estado == EstadoFaena.AUTORIZADA }
    }

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
                    val colorAlertas = if (state.alertasVelocidad > 0) Color(0xFFDC2626) else Color(0xFF059669)
                    IndicadorCard("ALERTAS DE VELOCIDAD", "${state.alertasVelocidad}", "alertas", "ascenso y descenso", Icons.Default.Shield, Modifier.weight(1f), colorAlertas)
                    val pieActivos = when {
                        autorizadasPendientes > 0 -> "$autorizadasPendientes autorizada${if (autorizadasPendientes == 1) "" else "s"} por iniciar"
                        else -> "inmersiones en ejecución"
                    }
                    val colorActivos = if (autorizadasPendientes > 0) Color(0xFF059669) else Color(0xFF42658C)
                    IndicadorCard("BUZOS ACTIVOS", "${state.buzosActivos}", "en turno", pieActivos, Icons.Default.Group, Modifier.weight(1f), colorActivos)
                }
            }

            if (operacionesActivas.isNotEmpty()) {
                item {
                    SeccionOperacionesActivas(
                        operaciones = operacionesActivas,
                        rol = rol,
                        onDetalle = onDetalle
                    )
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    OutlinedTextField(
                        value = filtro.texto,
                        onValueChange = onTexto,
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        placeholder = { Text("Buscar por buzo o jaula…", fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF42658C)) },
                        trailingIcon = {
                            IconButton(onClick = { mostrarCalendario = true }) {
                                Icon(Icons.Default.CalendarMonth, contentDescription = "Filtrar por fecha", tint = HistorialAzul)
                            }
                        },
                        shape = RoundedCornerShape(11.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = Color.White,
                            focusedContainerColor = Color.White
                        )
                    )
                    if (filtro.fecha.isNotBlank()) {
                        Surface(color = Color(0xFFE6F0FF), shape = RoundedCornerShape(7.dp)) {
                            Row(Modifier.padding(start = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text("Fecha: ${filtro.fecha}", color = HistorialAzulNoche, style = MaterialTheme.typography.labelSmall)
                                IconButton(onClick = { onFecha("") }, modifier = Modifier.size(48.dp)) {
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

            if (onCierresRecibidos != null) item {
                OutlinedButton(onClick = onCierresRecibidos, modifier = Modifier.fillMaxWidth()) {
                    Text("Cierres recibidos y alertas")
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
                    FaenaHistorialCard(item = item, rol = rol, onDetalle = { onDetalle(item.id) })
                }
            }
        }
    }
}

@Composable
private fun SeccionOperacionesActivas(
    operaciones: List<HistorialItem>,
    rol: Rol,
    onDetalle: (String) -> Unit
) {
    val esSupervisor = rol == Rol.SUPERVISOR
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.5.dp, Color(0xFF93C5FD)),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(color = Color(0xFFE6F0FF), shape = CircleShape) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = HistorialAzul,
                        modifier = Modifier.padding(5.dp).size(15.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        if (esSupervisor) "OPERACIONES ACTIVAS / POR INICIAR" else "OPERACIONES EN SEGUIMIENTO",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = HistorialAzulNoche,
                        fontSize = 10.sp
                    )
                    Text(
                        if (esSupervisor) "Faenas autorizadas o en curso listas para gestionar"
                        else "Inmersiones autorizadas o actualmente en ejecución",
                        style = MaterialTheme.typography.bodySmall,
                        color = HistorialSecundario,
                        fontSize = 10.sp
                    )
                }
                Surface(color = HistorialAzul, shape = CircleShape) {
                    Text(
                        operaciones.size.toString(),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            operaciones.forEach { item ->
                val autorizada = item.estado == EstadoFaena.AUTORIZADA
                val acento = if (autorizada) Color(0xFF059669) else HistorialAzul
                val fondoSuave = if (autorizada) Color(0xFFE5F8F1) else Color(0xFFE6F0FF)
                val badge = if (autorizada) "AUTORIZADA" else "EN CURSO"
                val aviso = if (autorizada) {
                    "Autorizada por Jefatura · Lista para iniciar inmersión"
                } else {
                    "Inmersión en curso · Pendiente de cierre"
                }
                val textoBoton = when {
                    autorizada && esSupervisor -> "INICIAR INMERSIÓN"
                    !autorizada && esSupervisor -> "FINALIZAR Y CERRAR FAENA"
                    else -> "VER SEGUIMIENTO ACTIVO"
                }

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFFAFCFF),
                    border = BorderStroke(1.dp, acento.copy(alpha = 0.45f))
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    item.centroCultivo.ifBlank { "Centro sin asignar" },
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = HistorialTexto,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    "Buzo: ${item.buzoAsignado.ifBlank { "Por asignar" }} · ${item.fecha.ifBlank { "S/F" }} ${item.hora}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = HistorialSecundario,
                                    fontSize = 10.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Surface(color = fondoSuave, shape = CircleShape) {
                                Text(
                                    badge,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    color = acento,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 8.sp
                                )
                            }
                        }

                        Surface(color = fondoSuave, shape = RoundedCornerShape(7.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = if (autorizada) Icons.Default.CheckCircle else Icons.Default.Schedule,
                                    contentDescription = null,
                                    tint = acento,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    aviso,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = acento,
                                    fontSize = 10.sp
                                )
                            }
                        }

                        Button(
                            onClick = { onDetalle(item.id) },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 42.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = acento, contentColor = Color.White)
                        ) {
                            Text(textoBoton, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            Spacer(Modifier.width(6.dp))
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(15.dp))
                        }
                    }
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
private fun IndicadorCard(titulo: String, valor: String, sufijo: String, pie: String, icono: ImageVector, modifier: Modifier = Modifier, color: Color) {
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
private fun FaenaHistorialCard(item: HistorialItem, rol: Rol, onDetalle: () -> Unit) {
    val estilo = estiloEstado(item, rol)
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.2.dp, estilo.color.copy(alpha = 0.6f)),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Row(Modifier.fillMaxWidth()) {
            Box(Modifier.width(4.dp).heightIn(min = 138.dp).background(estilo.color))
            Column(Modifier.weight(1f).padding(10.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            Surface(color = Color(0xFFE6F0FF), shape = RoundedCornerShape(4.dp)) {
                                Text(item.fecha.ifBlank { "S/F" }, Modifier.padding(horizontal = 5.dp, vertical = 2.dp), color = Color(0xFF334155), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                            }
                            if (item.hora.isNotBlank()) {
                                Text(item.hora, color = HistorialSecundario, fontSize = 8.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                        Text("⌖ ${item.centroCultivo.ifBlank { "Centro sin asignar" }}", style = MaterialTheme.typography.bodySmall, color = Color(0xFF334155), fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Surface(color = estilo.suave, shape = CircleShape) {
                        Text(estilo.etiqueta.uppercase(), Modifier.padding(horizontal = 8.dp, vertical = 4.dp), color = estilo.color, fontWeight = FontWeight.Bold, fontSize = 8.sp)
                    }
                }
                if (estilo.mensajeOperativo != null) {
                    Surface(color = estilo.suave, shape = RoundedCornerShape(7.dp)) {
                        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (item.estado == EstadoFaena.PRECHEQUEO && (item.resultado == ResultadoFaena.OBSERVADO || item.resultado == ResultadoFaena.REQUIERE_REVISION)) Icons.Default.WarningAmber else estilo.iconoBoton,
                                contentDescription = null,
                                tint = estilo.color,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                estilo.mensajeOperativo,
                                color = HistorialTexto,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 9.sp
                            )
                        }
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
                item.alertasVelocidad.forEach { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 10.sp) }

                if (estilo.botonPrimario) {
                    Button(
                        onClick = onDetalle,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 46.dp),
                        contentPadding = PaddingValues(6.dp),
                        shape = RoundedCornerShape(7.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = estilo.color, contentColor = Color.White)
                    ) {
                        Icon(estilo.iconoBoton, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.size(6.dp))
                        Text(estilo.textoBoton.uppercase(), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                } else {
                    OutlinedButton(
                        onClick = onDetalle,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 46.dp),
                        contentPadding = PaddingValues(4.dp),
                        shape = RoundedCornerShape(7.dp),
                        border = BorderStroke(0.dp, Color.Transparent),
                        colors = ButtonDefaults.outlinedButtonColors(containerColor = estilo.suave, contentColor = if (item.estado == EstadoFaena.CERRADA) HistorialAzulNoche else estilo.color)
                    ) {
                        Icon(estilo.iconoBoton, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.size(5.dp))
                        Text(estilo.textoBoton.uppercase(), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
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

private fun estiloEstado(item: HistorialItem, rol: Rol): EstiloTarjetaFaena {
    val esSupervisor = rol == Rol.SUPERVISOR
    return when {
        item.estado == EstadoFaena.RECHAZADA -> EstiloTarjetaFaena(
            etiqueta = "Rechazada",
            color = Color(0xFFDC2626),
            suave = Color(0xFFFEF2F2),
            mensajeOperativo = "Operación rechazada por Jefatura.",
            textoBoton = "Revisar rechazo",
            iconoBoton = Icons.Default.Cancel,
            botonPrimario = false
        )
        item.estado == EstadoFaena.AUTORIZADA -> EstiloTarjetaFaena(
            etiqueta = "Autorizada",
            color = Color(0xFF059669),
            suave = Color(0xFFE5F8F1),
            mensajeOperativo = "Aprobada por Jefatura · Lista para iniciar inmersión.",
            textoBoton = if (esSupervisor) "Iniciar inmersión" else "Ver detalle",
            iconoBoton = Icons.Default.PlayArrow,
            botonPrimario = esSupervisor
        )
        item.estado == EstadoFaena.EN_EJECUCION -> EstiloTarjetaFaena(
            etiqueta = "En curso",
            color = Color(0xFF0755B8),
            suave = Color(0xFFE6F0FF),
            mensajeOperativo = "Inmersión en ejecución · Pendiente de cierre.",
            textoBoton = if (esSupervisor) "Finalizar y cerrar faena" else "Ver seguimiento",
            iconoBoton = Icons.Default.CheckCircle,
            botonPrimario = esSupervisor
        )
        item.estado == EstadoFaena.CERRADA -> EstiloTarjetaFaena(
            etiqueta = "Finalizada",
            color = Color(0xFF0F766E),
            suave = Color(0xFFE5EEFF),
            mensajeOperativo = null,
            textoBoton = "Ver bitácora",
            iconoBoton = Icons.Default.Visibility,
            botonPrimario = false
        )
        item.resultado == ResultadoFaena.REQUIERE_REVISION -> EstiloTarjetaFaena(
            etiqueta = "Requiere revisión",
            color = Color(0xFFDC2626),
            suave = Color(0xFFFEF2F2),
            mensajeOperativo = "Requiere revisión antes de autorizar.",
            textoBoton = "Revisar solicitud",
            iconoBoton = Icons.Default.Schedule,
            botonPrimario = false
        )
        item.resultado == ResultadoFaena.OBSERVADO -> EstiloTarjetaFaena(
            etiqueta = "Observada",
            color = Color(0xFFD97706),
            suave = Color(0xFFFFF7E6),
            mensajeOperativo = "Faena con observaciones para revisión.",
            textoBoton = "Revisar observaciones",
            iconoBoton = Icons.Default.Schedule,
            botonPrimario = false
        )
        else -> EstiloTarjetaFaena(
            etiqueta = "En revisión",
            color = Color(0xFF42658C),
            suave = Color(0xFFEAF1FF),
            mensajeOperativo = "Pendiente de autorización de Jefatura.",
            textoBoton = "Ver solicitud",
            iconoBoton = Icons.Default.Schedule,
            botonPrimario = false
        )
    }
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