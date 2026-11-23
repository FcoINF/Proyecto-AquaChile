package com.duoc.aquaflow.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PendingActions
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Waves
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.duoc.aquaflow.domain.GrupoFaena
import com.duoc.aquaflow.domain.ReglasDecision
import com.duoc.aquaflow.domain.ReglasPanel
import com.duoc.aquaflow.domain.ReglasSecciones
import com.duoc.aquaflow.model.EstadoFaena
import com.duoc.aquaflow.model.FaenaGuardada
import com.duoc.aquaflow.model.ResultadoFaena

private val AzulJefatura = Color(0xFF0755B8)
private val AzulNocheJefatura = Color(0xFF1B2B40)
private val TextoJefatura = Color(0xFF142233)

@Composable
fun PanelAdministrador(faenas: List<FaenaGuardada>, onVerFaena: (String) -> Unit) {
    val grupos = ReglasPanel.agrupar(faenas)
    var seleccionado by rememberSaveable { mutableStateOf(GrupoFaena.ABIERTAS) }
    val seccion = when (seleccionado) {
        GrupoFaena.ABIERTAS -> "Faenas Abiertas"
        GrupoFaena.EN_PROCESO -> "Faenas Autorizadas"
        GrupoFaena.RECHAZADAS -> "Faenas Rechazadas"
        GrupoFaena.CERRADAS -> "Faenas Cerradas"
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Estado de las faenas", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = TextoJefatura)
        Text("Selecciona una tarjeta para consultar y auditar sus faenas en terreno.", style = MaterialTheme.typography.bodyMedium, color = Color(0xFF4B5563))

        GrupoFaena.entries.chunked(2).forEach { fila ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(11.dp),
                modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)
            ) {
                fila.forEach { grupo ->
                    val elegido = seleccionado == grupo
                    val contador = grupos.getValue(grupo).size
                    TarjetaResumenGrupo(
                        grupo = grupo,
                        contador = contador,
                        elegido = elegido,
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        onClick = { seleccionado = grupo }
                    )
                }
            }
        }

        Column(Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(seccion, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = TextoJefatura)
            Text(
                when (seleccionado) {
                    GrupoFaena.ABIERTAS -> "Completas por supervisor, a la espera de visado normativo DS 752."
                    GrupoFaena.EN_PROCESO -> "Faenas autorizadas que están en agua o en proceso."
                    GrupoFaena.RECHAZADAS -> "Faenas rechazadas por condiciones o controles operativos."
                    GrupoFaena.CERRADAS -> "Bitácora histórica de faenas finalizadas."
                },
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF4B5563)
            )
        }

        val visibles = grupos.getValue(seleccionado)
        if (visibles.isEmpty()) {
            Surface(color = Color.White, shape = RoundedCornerShape(12.dp)) {
                Text("No hay faenas en esta categoría.", Modifier.fillMaxWidth().padding(16.dp), color = Color(0xFF64748B))
            }
        } else {
            visibles.forEach { faena ->
                TarjetaFaenaJefatura(faena, seleccionado, onVerFaena)
            }
        }
    }
}

@Composable
private fun TarjetaResumenGrupo(
    grupo: GrupoFaena,
    contador: Int,
    elegido: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val colorFondo = if (elegido) AzulNocheJefatura else Color.White
    val colorTexto = if (elegido) Color.White else TextoJefatura
    val estilo = when (grupo) {
        GrupoFaena.ABIERTAS -> EstiloGrupo(
            "Abiertas", "Pendientes de decisión", "DECISIÓN", Icons.Default.PendingActions,
            Color(0xFF1D4ED8), Color(0xFFE8F0FF)
        )
        GrupoFaena.EN_PROCESO -> EstiloGrupo(
            "Autorizadas", "En agua / En proceso", "ACTIVAS", Icons.Default.Waves,
            Color(0xFF00674F), Color(0xFFDDF6EC)
        )
        GrupoFaena.RECHAZADAS -> EstiloGrupo(
            "Rechazadas", "Clima o equipos", "OBSERVADAS", Icons.Default.Cancel,
            Color(0xFFAE2436), Color(0xFFFDE8EA)
        )
        GrupoFaena.CERRADAS -> EstiloGrupo(
            "Cerradas", "Bitácora histórica", "HISTÓRICO", Icons.Default.Archive,
            Color(0xFF3E5875), Color(0xFFE9EFF8)
        )
    }
    val subColor = if (elegido) Color(0xFFD8E2EF) else Color(0xFF4B5563)

    Card(
        modifier = modifier.semantics { selected = elegido }.clickable(onClick = onClick),
        shape = RoundedCornerShape(11.dp),
        colors = CardDefaults.cardColors(containerColor = colorFondo),
        elevation = CardDefaults.cardElevation(if (elegido) 1.dp else 0.dp),
        border = if (!elegido) BorderStroke(1.dp, Color(0xFFE9EDF3)) else null
    ) {
        Column(
            Modifier.fillMaxWidth().fillMaxHeight().padding(horizontal = 11.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    contador.toString(),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = colorTexto
                )
                Icon(
                    estilo.icono,
                    contentDescription = null,
                    tint = if (elegido) estilo.fondoEtiqueta else estilo.acento,
                    modifier = Modifier.size(20.dp)
                )
            }
            Text(estilo.titulo, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = colorTexto)
            Text(estilo.descripcion, style = MaterialTheme.typography.labelSmall, color = subColor)
            Spacer(Modifier.weight(1f))
            Surface(color = estilo.fondoEtiqueta, shape = RoundedCornerShape(4.dp)) {
                Text(
                    estilo.etiqueta,
                    Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                    color = estilo.acento,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

private data class EstiloGrupo(
    val titulo: String,
    val descripcion: String,
    val etiqueta: String,
    val icono: ImageVector,
    val acento: Color,
    val fondoEtiqueta: Color
)

@Composable
private fun TarjetaFaenaJefatura(faena: FaenaGuardada, grupo: GrupoFaena, onVerFaena: (String) -> Unit) {
    val apertura = faena.resumen.apertura
    val resultado = ReglasDecision.evaluar(faena.resumen).resultado
    val colorBorde = when {
        grupo == GrupoFaena.RECHAZADAS || resultado == ResultadoFaena.REQUIERE_REVISION -> Color(0xFFDC2626)
        resultado == ResultadoFaena.OBSERVADO -> Color(0xFFF59E0B)
        grupo == GrupoFaena.EN_PROCESO || grupo == GrupoFaena.CERRADAS -> Color(0xFF059669)
        else -> Color(0xFF08A879)
    }
    val progreso = ReglasSecciones.estados(faena.resumen).values.count { it == com.duoc.aquaflow.model.EstadoSeccion.COMPLETA }
    val salud = faena.resumen.encuesta
    val nombreTarea = faena.resumen.ast?.tipologia?.takeIf(String::isNotBlank) ?: "Operación de buceo"
    val centro = apertura.centroCultivo.ifBlank { "Centro sin asignar" }
    val codigo = apertura.id.takeLast(9).uppercase()
    val fotos = faena.resumen.checklist?.fotosEvidencia?.size ?: 0
    val mediciones = faena.cierre?.mediciones

    Card(
        modifier = Modifier.fillMaxWidth().drawWithContent {
            drawContent()
            val inset = 10.dp.toPx()
            val ancho = 4.dp.toPx()
            drawRect(
                color = colorBorde,
                topLeft = androidx.compose.ui.geometry.Offset(0f, inset),
                size = androidx.compose.ui.geometry.Size(ancho, (size.height - inset * 2).coerceAtLeast(0f))
            )
        },
        shape = RoundedCornerShape(11.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, colorBorde.copy(alpha = 0.55f)),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(Modifier.fillMaxWidth().padding(start = 13.dp, end = 10.dp, top = 10.dp, bottom = 10.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
                        Surface(color = Color(0xFF182B3A), shape = RoundedCornerShape(4.dp)) {
                            Text("FAENA #$codigo", Modifier.padding(horizontal = 5.dp, vertical = 3.dp), color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                        Surface(color = Color(0xFFE6F0FF), shape = RoundedCornerShape(4.dp)) {
                            Text(centro.takeLast(24), Modifier.padding(horizontal = 5.dp, vertical = 3.dp), color = TextoJefatura, fontSize = 8.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                        }
                    }
                    Surface(color = colorBorde.copy(alpha = 0.12f), shape = RoundedCornerShape(5.dp)) {
                        Text("● Paso $progreso/5", Modifier.padding(horizontal = 5.dp, vertical = 4.dp), color = colorBorde, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Text(nombreTarea, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = TextoJefatura)

                Surface(color = Color(0xFFEEF3FF), shape = RoundedCornerShape(6.dp)) {
                    Column(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Column(Modifier.weight(1f)) {
                                Text("BUZO ESPECIALISTA", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4B5563))
                                Text(apertura.buzoAsignado.ifBlank { "Buzo por asignar" }, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextoJefatura, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            Column(Modifier.weight(1f)) {
                                Text("SUPERVISOR DE BUCEO", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4B5563))
                                Text(apertura.supervisorACargo.ifBlank { "Supervisor por asignar" }, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextoJefatura, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                        salud?.let {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Default.FavoriteBorder, contentDescription = null, tint = Color(0xFF059669), modifier = Modifier.size(13.dp))
                                Text("SpO₂ ${it.saturacion}% • PA ${it.sistolica}/${it.diastolica}", color = Color(0xFF059669), fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
                    ValorOperativo("Prof. Máx.", mediciones?.let { "${it.profundidadMaximaMetros} m" } ?: "—", Modifier.weight(1f))
                    ValorOperativo("Tiempo Fondo", mediciones?.let { "${it.tiempoFondoMinutos} min" } ?: "—", Modifier.weight(1f))
                    ValorOperativo("DPR-24", if (progreso == 5) "100%" else "$progreso/5", Modifier.weight(1f))
                }
                mediciones?.alertas.orEmpty().forEach { alerta ->
                    Text(alerta, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }

                if (fotos > 0 || resultado == ResultadoFaena.OBSERVADO) {
                    Surface(color = Color(0xFFEAF1FF), shape = RoundedCornerShape(6.dp)) {
                        Row(Modifier.fillMaxWidth().padding(7.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            Icon(Icons.Default.Checklist, contentDescription = null, tint = AzulJefatura, modifier = Modifier.size(15.dp))
                            Text(
                                buildString {
                                    if (fotos > 0) append("$fotos fotos de pre-chequeo adjuntas")
                                    if (fotos > 0 && resultado == ResultadoFaena.OBSERVADO) append(" • ")
                                    if (resultado == ResultadoFaena.OBSERVADO) append("Presenta observaciones para revisión")
                                },
                                fontSize = 8.sp,
                                color = TextoJefatura
                            )
                        }
                    }
                }

                if (grupo == GrupoFaena.ABIERTAS) {
                    Button(
                        onClick = { onVerFaena(apertura.id) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(7.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AzulJefatura)
                    ) {
                        Text("Revisar y Visar", fontWeight = FontWeight.Bold)
                    }
                } else {
                    OutlinedButton(
                        onClick = { onVerFaena(apertura.id) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(7.dp),
                        border = BorderStroke(0.dp, Color.Transparent),
                        colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(containerColor = Color(0xFFE6EEFF), contentColor = TextoJefatura)
                    ) {
                        Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.size(5.dp))
                        Text("Revisar Detalle de Operación", fontWeight = FontWeight.Bold, fontSize = 10.sp)
                    }
                }
        }
    }
}

@Composable
private fun ValorOperativo(titulo: String, valor: String, modifier: Modifier = Modifier) {
    Column(modifier.padding(vertical = 2.dp), verticalArrangement = Arrangement.spacedBy(1.dp)) {
        Text(titulo.uppercase(), fontSize = 7.sp, color = Color(0xFF42658C), fontWeight = FontWeight.Bold, maxLines = 1)
        Text(valor, fontSize = 10.sp, color = TextoJefatura, fontWeight = FontWeight.Bold)
    }
}
