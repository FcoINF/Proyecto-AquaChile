package com.duoc.aquaflow.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.GregorianCalendar
import java.util.Locale
import java.util.TimeZone

private val FondoCampo = Color(0xFF0D1C25)
private val FondoCalendario = Color(0xFF233542)
private val BordeCalendario = Color(0xFF748994)
private val TextoCalendario = Color(0xFFDBE3E6)
private val TextoSecundarioCalendario = Color(0xFFB1BEC3)
private val FondoDiaSeleccionado = Color(0xFFA8B9BE)
private val ZonaCalendario = TimeZone.getTimeZone("UTC")
private val Espanol = Locale.forLanguageTag("es-CL")
private val DiasSemana = listOf("Lu", "Ma", "Mi", "Ju", "Vi", "Sá", "Do")

private class DiaCalendario(
    val numero: Int,
    val texto: String,
    val deshabilitado: Boolean
)

@Composable
fun CampoFecha(
    etiqueta: String,
    fecha: String,
    desplegado: Boolean,
    onAlternar: () -> Unit,
    onFechaSeleccionada: (String) -> Unit,
    habilitado: Boolean = true,
    soloDesdeHoy: Boolean = false
) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(etiqueta, style = MaterialTheme.typography.labelLarge)
        Surface(
            onClick = onAlternar,
            enabled = habilitado,
            modifier = Modifier.fillMaxWidth().semantics {
                contentDescription = etiqueta
                stateDescription = if (desplegado) "Calendario desplegado" else "Calendario cerrado"
            },
            shape = CircleShape,
            color = FondoCampo,
            border = BorderStroke(1.dp, BordeCalendario)
        ) {
            Row(
                Modifier.heightIn(min = 48.dp).padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    fecha.ifBlank { "Seleccionar fecha" },
                    Modifier.weight(1f),
                    color = if (fecha.isBlank()) TextoSecundarioCalendario else TextoCalendario,
                    style = MaterialTheme.typography.bodyMedium
                )
                Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = TextoCalendario, modifier = Modifier.size(19.dp))
            }
        }
        if (desplegado && habilitado) {
            val fechaSeleccionada = remember(fecha) {
                val formato = formatoFecha("dd/MM/yyyy")
                runCatching { formato.parse(fecha) }.getOrNull()?.takeIf { formato.format(it) == fecha }
            }
            CalendarioDesplegable(fechaSeleccionada, soloDesdeHoy, onFechaSeleccionada)
        }
    }
}

@Composable
private fun CalendarioDesplegable(
    fechaSeleccionada: Date?,
    soloDesdeHoy: Boolean,
    onFechaSeleccionada: (String) -> Unit
) {
    val hoyUtc = remember {
        val local = GregorianCalendar()
        GregorianCalendar(ZonaCalendario).apply {
            clear()
            set(local.get(Calendar.YEAR), local.get(Calendar.MONTH), local.get(Calendar.DAY_OF_MONTH))
        }
    }
    val anioActual = hoyUtc.get(Calendar.YEAR)
    val mesActual = anioActual * 12 + hoyUtc.get(Calendar.MONTH)

    val fechaInicial = remember(fechaSeleccionada) {
        if (fechaSeleccionada == null) GregorianCalendar()
        else GregorianCalendar(ZonaCalendario).apply { time = fechaSeleccionada }
    }
    val anioInicial = fechaInicial.get(Calendar.YEAR)
    val anios = if (soloDesdeHoy) {
        anioActual..maxOf(2100, anioInicial)
    } else {
        minOf(1900, anioInicial)..maxOf(2100, anioInicial)
    }
    val mesMinimo = if (soloDesdeHoy) mesActual else anios.first * 12
    val mesInicial = maxOf(anioInicial * 12 + fechaInicial.get(Calendar.MONTH), mesMinimo)

    var mesVisible by rememberSaveable(soloDesdeHoy) {
        mutableIntStateOf(mesInicial)
    }
    var eligiendoAnio by rememberSaveable { mutableStateOf(false) }
    val anio = mesVisible / 12
    val mes = mesVisible % 12
    val primerDia = remember(mesVisible) {
        GregorianCalendar(ZonaCalendario).apply {
            clear()
            set(anio, mes, 1)
        }
    }
    val tituloMes = remember(mesVisible) { formatoFecha("MMMM 'de' yyyy").format(primerDia.time) }
    val fechaMarcada = remember(fechaSeleccionada) { fechaSeleccionada?.let(formatoFecha("dd/MM/yyyy")::format) }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = FondoCalendario,
        border = BorderStroke(1.dp, Color(0xFF344855))
    ) {
        Column(
            Modifier.padding(horizontal = 12.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                FlechaMes(
                    anterior = true,
                    habilitada = !eligiendoAnio && mesVisible > mesMinimo
                ) { mesVisible-- }
                Surface(
                    onClick = { eligiendoAnio = !eligiendoAnio },
                    modifier = Modifier.weight(1f).semantics {
                        contentDescription = if (eligiendoAnio) "Volver a $tituloMes" else "$tituloMes, elegir año"
                    },
                    color = Color.Transparent
                ) {
                    Box(Modifier.heightIn(min = 44.dp), contentAlignment = Alignment.Center) {
                        Text(tituloMes, color = TextoCalendario, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                FlechaMes(
                    anterior = false,
                    habilitada = !eligiendoAnio && mesVisible < anios.last * 12 + 11
                ) { mesVisible++ }
            }
            if (eligiendoAnio) {
                LazyColumn(
                    Modifier.fillMaxWidth().height(200.dp).selectableGroup(),
                    state = rememberLazyListState(initialFirstVisibleItemIndex = (anio - anios.first - 2).coerceAtLeast(0))
                ) {
                    items(anios.count()) { indice ->
                        val opcion = anios.first + indice
                        Box(
                            Modifier.fillMaxWidth().heightIn(min = 44.dp)
                                .selectable(selected = opcion == anio, role = Role.RadioButton) {
                                    mesVisible = maxOf(opcion * 12 + mes, mesMinimo)
                                    eligiendoAnio = false
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                opcion.toString(),
                                color = TextoCalendario,
                                fontWeight = if (opcion == anio) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            } else {
                DiasDelMes(
                    primerDia = primerDia,
                    fechaMarcada = fechaMarcada,
                    fechaMinima = if (soloDesdeHoy) hoyUtc.time else null,
                    onFechaSeleccionada = onFechaSeleccionada
                )
            }
        }
    }
}

@Composable
private fun FlechaMes(anterior: Boolean, habilitada: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        enabled = habilitada,
        modifier = Modifier.size(44.dp),
        shape = CircleShape,
        color = Color.Transparent,
        border = BorderStroke(1.dp, BordeCalendario)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                if (anterior) Icons.AutoMirrored.Filled.KeyboardArrowLeft else Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = if (anterior) "Mes anterior" else "Mes siguiente",
                tint = TextoCalendario.copy(alpha = if (habilitada) 1f else 0.4f)
            )
        }
    }
}

@Composable
private fun DiasDelMes(
    primerDia: Calendar,
    fechaMarcada: String?,
    fechaMinima: Date?,
    onFechaSeleccionada: (String) -> Unit
) {
    val dias = remember(primerDia.timeInMillis, fechaMinima) {
        val formato = formatoFecha("dd/MM/yyyy")
        val calendario = primerDia.clone() as Calendar
        (1..primerDia.getActualMaximum(Calendar.DAY_OF_MONTH)).map { dia ->
            calendario.set(Calendar.DAY_OF_MONTH, dia)
            DiaCalendario(
                numero = dia,
                texto = formato.format(calendario.time),
                deshabilitado = fechaMinima != null && calendario.time.before(fechaMinima)
            )
        }
    }
    val inicio = (primerDia.get(Calendar.DAY_OF_WEEK) + 5) % 7
    val semanas = (inicio + dias.size + 6) / 7
    val formaDia = RoundedCornerShape(10.dp)
    Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Row(Modifier.fillMaxWidth()) {
            DiasSemana.forEach { dia ->
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Text(dia, color = TextoSecundarioCalendario, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelMedium)
                }
            }
        }
        repeat(semanas) { semana ->
            Row(Modifier.fillMaxWidth()) {
                repeat(7) { columna ->
                    val indice = semana * 7 + columna - inicio
                    val diaInfo = dias.getOrNull(indice)
                    val seleccionado = diaInfo != null && diaInfo.texto == fechaMarcada
                    val celda = Modifier.weight(1f).heightIn(min = 42.dp)
                    Box(
                        modifier = if (diaInfo == null) celda else celda.padding(1.dp).clip(formaDia)
                            .selectable(
                                selected = seleccionado,
                                enabled = !diaInfo.deshabilitado,
                                role = Role.RadioButton
                            ) { onFechaSeleccionada(diaInfo.texto) }
                            .semantics { contentDescription = diaInfo.texto },
                        contentAlignment = Alignment.Center
                    ) {
                        if (diaInfo != null) {
                            Surface(shape = formaDia, color = if (seleccionado) FondoDiaSeleccionado else Color.Transparent) {
                                Box(Modifier.padding(horizontal = 10.dp, vertical = 5.dp), contentAlignment = Alignment.Center) {
                                    Text(
                                        diaInfo.numero.toString(),
                                        color = when {
                                            seleccionado -> FondoCampo
                                            diaInfo.deshabilitado -> TextoCalendario.copy(alpha = 0.3f)
                                            else -> TextoCalendario
                                        },
                                        fontWeight = if (seleccionado) FontWeight.Bold else FontWeight.Normal,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatoFecha(patron: String) = SimpleDateFormat(patron, Espanol).apply {
    timeZone = ZonaCalendario
    isLenient = false
}