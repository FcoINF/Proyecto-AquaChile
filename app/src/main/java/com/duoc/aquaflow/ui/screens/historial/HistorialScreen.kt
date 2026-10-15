package com.duoc.aquaflow.ui.screens.historial

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.duoc.aquaflow.domain.Fechas
import com.duoc.aquaflow.domain.rules.RangoFecha
import com.duoc.aquaflow.model.EstadoFaena
import com.duoc.aquaflow.model.Faena
import com.duoc.aquaflow.model.RegistroPosterior
import com.duoc.aquaflow.model.Rol
import com.duoc.aquaflow.model.Usuario
import com.duoc.aquaflow.ui.components.AquaScreen
import com.duoc.aquaflow.ui.components.AquaTextField
import com.duoc.aquaflow.ui.components.ChipSeleccionable
import com.duoc.aquaflow.ui.components.EmptyState
import com.duoc.aquaflow.ui.components.EstadoFaenaChip
import com.duoc.aquaflow.ui.components.LocalAppContainer
import com.duoc.aquaflow.ui.components.ScreenColumn
import com.duoc.aquaflow.ui.components.SecondaryButton
import com.duoc.aquaflow.ui.components.SectionCard
import com.duoc.aquaflow.ui.navigation.MainBottomBar
import com.duoc.aquaflow.ui.navigation.Tab
import com.duoc.aquaflow.ui.theme.AquaBackground
import com.duoc.aquaflow.ui.theme.AquaGreen
import com.duoc.aquaflow.ui.theme.AquaGreenBg
import com.duoc.aquaflow.ui.theme.AquaGreenDark
import com.duoc.aquaflow.ui.theme.AquaNavy
import com.duoc.aquaflow.ui.theme.AquaNavyLight
import com.duoc.aquaflow.ui.theme.AquaOnSurfaceVariant
import com.duoc.aquaflow.ui.theme.AquaOrangeDark
import com.duoc.aquaflow.ui.theme.AquaTextGrey
import com.duoc.aquaflow.viewmodel.HistorialViewModel
import com.duoc.aquaflow.viewmodel.vmFactory

/**
 * Historial de faenas con datos reales de Room.
 *
 * Mantiene los filtros y la navegación existentes y enriquece la vista con el
 * lenguaje visual del prototipo Proyecto-AquaChile (resumen consolidado,
 * telemetría del registro posterior y filas con PROFUNDIDAD / DURACIÓN / FECHA),
 * pero todo cableado a [RegistroPosterior] real. Sin registros solo se muestra
 * "—" y el bloque de telemetría se oculta: nunca se inventan números.
 */
@Composable
fun HistorialScreen(usuario: Usuario, onAbrirFaena: (Faena) -> Unit, onTab: (Tab) -> Unit) {
    val container = LocalAppContainer.current
    val vm: HistorialViewModel = viewModel(
        key = "historial-${usuario.id}",
        factory = vmFactory { HistorialViewModel(usuario, container.faenaRepository) }
    )
    val state by vm.state.collectAsStateWithLifecycle()
    val filtro = state.filtro
    val hayFiltros = filtro.texto.isNotBlank() || filtro.estado != null || filtro.rango != RangoFecha.TODOS

    AquaScreen(
        title = "Historial de faenas",
        subtitle = if (usuario.rol == Rol.ADMINISTRADOR) "Todas las faenas" else "Mis faenas",
        bottomBar = { MainBottomBar(Tab.HISTORIAL, usuario.rol, onTab) }
    ) { padding ->
        ScreenColumn(padding) {
            ResumenConsolidado(total = state.total, registros = state.registros.values.toList())

            if (state.registros.isNotEmpty()) {
                TelemetriaSection(registros = state.registros.values.toList())
            }

            AquaTextField(
                valor = filtro.texto,
                onValueChange = vm::onTexto,
                etiqueta = "Buscar por código, buzo, centro o supervisor"
            )

            Text("Estado", style = MaterialTheme.typography.labelLarge)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ChipSeleccionable("Todos", filtro.estado == null, { vm.onEstado(null) })
                EstadoFaena.values().forEach { estado ->
                    ChipSeleccionable(estado.etiqueta, filtro.estado == estado, { vm.onEstado(estado) })
                }
            }

            Text("Fecha de la faena", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RangoFecha.values().forEach { rango ->
                    ChipSeleccionable(rango.etiqueta, filtro.rango == rango, { vm.onRango(rango) })
                }
            }

            Text(
                "${state.faenas.size} de ${state.total} faena(s)",
                style = MaterialTheme.typography.bodyMedium,
                color = AquaOnSurfaceVariant
            )
            if (hayFiltros) SecondaryButton("Limpiar filtros", onClick = vm::limpiar)

            if (state.faenas.isEmpty() && !state.cargando) {
                EmptyState(
                    titulo = if (state.total == 0) "Aún no hay faenas" else "Sin resultados",
                    texto = if (state.total == 0) "Las faenas que registres aparecerán aquí." else "Prueba con otros filtros.",
                    icono = Icons.Filled.History
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    state.faenas.forEach { faena ->
                        FaenaHistorialRow(
                            faena = faena,
                            registro = state.registros[faena.id],
                            onClick = { onAbrirFaena(faena) }
                        )
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Resumen consolidado (estilo HISTORIAL3 del prototipo, con conteos reales)
// ---------------------------------------------------------------------------

@Composable
private fun ResumenConsolidado(total: Int, registros: List<RegistroPosterior>) {
    val conRegistro = registros.size
    val profMax = registros.maxOfOrNull { it.profundidadMaxM }
    val tiempoTotal = registros.sumOf { it.tiempoInmersionMin }
    SectionCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "HISTORIAL LOCAL\nCONSOLIDADO",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = AquaNavy,
                lineHeight = 15.sp,
                modifier = Modifier.weight(1f)
            )
            Surface(shape = RoundedCornerShape(6.dp), color = AquaBackground) {
                Text(
                    "ROOM DB: $total\nFAENAS",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = AquaTextGrey,
                    textAlign = TextAlign.Center,
                    lineHeight = 11.sp
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            FaenaDato("CERRADAS", if (conRegistro == 0) "—" else "$conRegistro", Modifier.weight(1f))
            FaenaDato("PROF. MÁX", profMax?.let { "$it m" } ?: "—", Modifier.weight(1f))
            FaenaDato("T. TOTAL", if (conRegistro == 0) "—" else "$tiempoTotal min", Modifier.weight(1f))
        }
    }
}

// ---------------------------------------------------------------------------
// Telemetría (estilo HISTORIAL1 del prototipo, con registros posteriores reales)
// ---------------------------------------------------------------------------

@Composable
private fun TelemetriaSection(registros: List<RegistroPosterior>) {
    val profMax = registros.maxOf { it.profundidadMaxM }
    val tiempoTotal = registros.sumOf { it.tiempoInmersionMin }
    val presionMax = registros.maxOf { it.presionBancoBar }
    val ultimo = registros.maxBy { it.registradoAt }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    "TELEMETRÍA Y CURVA DE\nDESCOMPRESIÓN",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = AquaNavy,
                    lineHeight = 15.sp,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    "REGISTRO\nPOSTERIOR",
                    fontSize = 10.sp,
                    color = AquaTextGrey,
                    textAlign = TextAlign.End,
                    lineHeight = 12.sp
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TelemetriaMiniCard(
                    titulo = "PROF.\nMÁXIMA",
                    valor = "$profMax",
                    unidad = "M",
                    pie = "< 30m plan",
                    modifier = Modifier.weight(1f)
                )
                TelemetriaMiniCard(
                    titulo = "T. DE FONDO\nTOTAL",
                    valor = "$tiempoTotal",
                    unidad = "MIN",
                    pie = "${registros.size} faena(s)",
                    destacado = true,
                    modifier = Modifier.weight(1f)
                )
                TelemetriaMiniCard(
                    titulo = "PRESIÓN\nMÁX.",
                    valor = "$presionMax",
                    unidad = "BAR",
                    pie = "> 50 BAR",
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            DiveProfileChart(
                profundidadM = ultimo.profundidadMaxM,
                tiempoMin = ultimo.tiempoInmersionMin
            )
        }
    }
}

@Composable
private fun TelemetriaMiniCard(
    titulo: String,
    valor: String,
    unidad: String,
    pie: String,
    destacado: Boolean = false,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = AquaBackground
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                titulo,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = AquaTextGrey,
                textAlign = TextAlign.Center,
                lineHeight = 11.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(valor, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = AquaNavy)
                Spacer(modifier = Modifier.width(3.dp))
                Text(unidad, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AquaNavy)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                (if (destacado) "✓ " else "✓ ") + pie,
                fontSize = 9.sp,
                color = AquaGreenDark,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

/**
 * Curva de descenso/ascenso con la forma del prototipo pero rotulada con el
 * registro posterior real más reciente.
 */
@Composable
private fun DiveProfileChart(profundidadM: Double, tiempoMin: Int) {
    val tasa = if (tiempoMin > 0) profundidadM / (tiempoMin / 2.0) else 0.0
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = AquaNavy),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF29B6F6))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "Perfil de Descenso & Ascenso",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Surface(shape = RoundedCornerShape(6.dp), color = AquaNavyLight) {
                    Text(
                        "Tasa: ${"%.1f".format(tasa)} m/min",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        color = AquaGreen,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .padding(top = 10.dp)
            ) {
                val w = size.width
                val h = size.height - 24f
                val pts = listOf(
                    Offset(0.02f * w, 0.05f * h),
                    Offset(0.06f * w, 0.30f * h),
                    Offset(0.18f * w, 0.97f * h),
                    Offset(0.52f * w, 0.97f * h),
                    Offset(0.66f * w, 0.55f * h),
                    Offset(0.74f * w, 0.38f * h),
                    Offset(0.84f * w, 0.38f * h),
                    Offset(0.98f * w, 0.05f * h)
                )
                for (i in 0..3) {
                    val y = (h / 3) * i
                    drawLine(Color.White.copy(alpha = 0.12f), Offset(0f, y), Offset(w, y), strokeWidth = 1f)
                }
                val path = Path().apply {
                    moveTo(pts[0].x, pts[0].y)
                    pts.drop(1).forEach { lineTo(it.x, it.y) }
                }
                drawPath(path, Color(0xFF29B6F6), style = Stroke(width = 5f, cap = StrokeCap.Round))
                pts.forEachIndexed { index, p ->
                    val c = when (index) {
                        0 -> Color(0xFFF95716)
                        pts.lastIndex -> AquaGreen
                        else -> Color(0xFF29B6F6)
                    }
                    drawCircle(c, radius = 9f, center = p)
                    drawCircle(Color.White, radius = 3.5f, center = p)
                }
            }
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = Color(0xFF12395B),
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text(
                    "Fondo: $profundidadM m @ $tiempoMin'",
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Fila de faena (estilo HISTORIAL3 del prototipo, con datos reales)
// ---------------------------------------------------------------------------

@Composable
private fun FaenaHistorialRow(faena: Faena, registro: RegistroPosterior?, onClick: () -> Unit) {
    SectionCard(onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = faena.codigo.ifBlank { "Faena #${faena.id}" },
                style = MaterialTheme.typography.labelLarge,
                color = AquaOrangeDark,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            EstadoFaenaChip(faena.estado)
        }
        Text(
            "${faena.buzoNombre} • ${faena.centroNombre}",
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = "Supervisor: ${faena.supervisorNombre}",
            style = MaterialTheme.typography.bodySmall,
            color = AquaOnSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            FaenaDato("PROFUNDIDAD", registro?.let { "${it.profundidadMaxM} m" } ?: "—", Modifier.weight(1f))
            FaenaDato("DURACIÓN", registro?.let { "${it.tiempoInmersionMin} min" } ?: "—", Modifier.weight(1f))
            FaenaDato("FECHA", Fechas.fecha(faena.fechaInicio), Modifier.weight(1f))
        }
        if (faena.estado == EstadoFaena.CERRADA && registro == null) {
            Text(
                "Sin registro posterior",
                fontSize = 11.sp,
                color = AquaTextGrey
            )
        }
    }
}

@Composable
private fun FaenaDato(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.Start) {
        Text(label, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = AquaTextGrey)
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = AquaNavy)
    }
}
