package com.duoc.aquaflow.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.duoc.aquaflow.model.EstadoEquipo
import com.duoc.aquaflow.model.ItemEquipamiento
import com.duoc.aquaflow.ui.theme.AquaAzulAccion

@Composable
fun ItemChecklist(
    item: ItemEquipamiento,
    onEstadoCambiado: (EstadoEquipo) -> Unit
) {
    val categoriaTexto = item.categoria.ifBlank {
        when (item.id) {
            "1" -> "EQUIPO 01 • SUMINISTRO PRIMARIO"
            "2" -> "EQUIPO 02 • PERSONAL"
            "3" -> "EQUIPO 03 • RESPALDO RESCATE"
            else -> "EQUIPO 0${item.id} • DISTRIBUCIÓN Y LÍNEA"
        }
    }

    val badgeTexto = item.badge.ifBlank {
        when (item.id) {
            "1" -> "220 BAR"
            "2" -> "BUZO #1"
            "3" -> "210 BAR"
            else -> "SISTEMA OK"
        }
    }

    val iconVector: ImageVector = when {
        item.id == "1" || item.titulo.contains("Compresor", ignoreCase = true) -> Icons.Default.Air
        item.id == "2" || item.titulo.contains("Traje", ignoreCase = true) -> Icons.AutoMirrored.Filled.DirectionsRun
        item.id == "3" || item.titulo.contains("Botellón", ignoreCase = true) || item.titulo.contains("Rescate", ignoreCase = true) -> Icons.Default.Shield
        else -> Icons.Default.Build
    }
    val observado = item.estado == EstadoEquipo.OBSERVADO
    val estadoColor = when (item.estado) {
        EstadoEquipo.CUMPLE -> Color(0xFF059669)
        EstadoEquipo.NO_CUMPLE -> Color(0xFFDC2626)
        EstadoEquipo.OBSERVADO -> Color(0xFFF59E0B)
        EstadoEquipo.PENDIENTE -> Color(0xFFE2E8F0)
    }
    val estadoSuave = when (item.estado) {
        EstadoEquipo.CUMPLE -> Color(0xFFF0FDF4)
        EstadoEquipo.NO_CUMPLE -> Color(0xFFFEF2F2)
        EstadoEquipo.OBSERVADO -> Color(0xFFFFFBEB)
        EstadoEquipo.PENDIENTE -> Color.White
    }
    val observacionBadge = badgeTexto.equals("OBSERVACIÓN", ignoreCase = true)

    Card(
        modifier = Modifier
            .testTag("equipo-${item.id}")
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = estadoSuave),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, estadoColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(11.dp)
        ) {
            // Header Row: Icon | Category + Title + Description | Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                // Icon Box
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(
                            if (observado) Color(0xFFFEF3C7) else Color(0xFFEBF3FC),
                            shape = RoundedCornerShape(10.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = iconVector,
                        contentDescription = null,
                        tint = if (observado) Color(0xFFD97706) else Color(0xFF2B527C),
                        modifier = Modifier.size(19.dp)
                    )
                }

                // Middle Text Info
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 8.dp, end = 5.dp)
                ) {
                    Text(
                        text = categoriaTexto,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 8.sp,
                            letterSpacing = 0.5.sp,
                            color = AquaAzulAccion
                        )
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = item.titulo,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                            color = Color(0xFF0F172A)
                        )
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    if (item.descripcion.isNotBlank()) {
                        Text(
                            text = item.descripcion,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 10.sp,
                                lineHeight = 13.sp,
                                color = Color(0xFF64748B)
                            )
                        )
                    }
                }

                // Right Badge
                if (badgeTexto.isNotBlank() && item.id != "4") {
                    Box(
                        modifier = Modifier
                            .background(
                                if (observacionBadge) Color(0xFFFEF3C7) else Color(0xFFEEF2F6),
                                shape = RoundedCornerShape(6.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = badgeTexto,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 8.sp,
                            color = if (observacionBadge) Color(0xFFB45309) else Color(0xFF475569)
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Bottom Segmented Control for Estados
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF1F5F9), shape = RoundedCornerShape(12.dp))
                    .padding(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    EstadoSegmentedButton(
                        texto = "Cumple",
                        seleccionado = item.estado == EstadoEquipo.CUMPLE,
                        colorActivo = Color(0xFF059669),
                        icon = Icons.Default.CheckCircle,
                        modifier = Modifier.weight(1f)
                    ) { onEstadoCambiado(EstadoEquipo.CUMPLE) }

                    EstadoSegmentedButton(
                        texto = "No Cumple",
                        seleccionado = item.estado == EstadoEquipo.NO_CUMPLE,
                        colorActivo = Color(0xFFDC2626),
                        icon = Icons.Default.Cancel,
                        modifier = Modifier.weight(1f)
                    ) { onEstadoCambiado(EstadoEquipo.NO_CUMPLE) }

                    EstadoSegmentedButton(
                        texto = "Observado",
                        seleccionado = item.estado == EstadoEquipo.OBSERVADO,
                        colorActivo = Color(0xFFD97706),
                        icon = Icons.Default.Warning,
                        modifier = Modifier.weight(1f)
                    ) { onEstadoCambiado(EstadoEquipo.OBSERVADO) }
                }
            }
        }
    }
}

@Composable
fun EstadoSegmentedButton(
    texto: String,
    seleccionado: Boolean,
    colorActivo: Color,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(30.dp),
        shape = RoundedCornerShape(8.dp),
        color = if (seleccionado) colorActivo else Color.Transparent,
        contentColor = if (seleccionado) Color.White else Color(0xFF334155)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 2.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(12.dp),
                tint = if (seleccionado) Color.White else Color(0xFF475569)
            )
            Spacer(modifier = Modifier.width(2.dp))
            Text(
                text = texto,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = if (seleccionado) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 9.sp
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun EstadoButton(
    texto: String,
    seleccionado: Boolean,
    colorActivo: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val icon = when (texto) {
        "Cumple" -> Icons.Default.CheckCircle
        "No Cumple" -> Icons.Default.Cancel
        else -> Icons.Default.Warning
    }
    EstadoSegmentedButton(
        texto = texto,
        seleccionado = seleccionado,
        colorActivo = colorActivo,
        icon = icon,
        modifier = modifier,
        onClick = onClick
    )
}
