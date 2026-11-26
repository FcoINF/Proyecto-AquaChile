package com.duoc.aquaflow.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val ITEMS_FOTO_OBLIGATORIOS = listOf(
    "1. Maleta de Suministro de Aire",
    "2. Filtros del Compresor",
    "3. Aletas y Equipo Personal",
    "4. Dispositivo Census"
)

@Composable
fun SeccionFotos(
    cantidadFotos: Int,
    totalEquipos: Int = ITEMS_FOTO_OBLIGATORIOS.size
) {
    val suficiente = cantidadFotos >= totalEquipos
    val colorProgreso = if (suficiente) Color(0xFF059669) else Color(0xFFD97706)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        "Evidencias Fotográficas Obligatorias (4 Ítems)",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        "$cantidadFotos de $totalEquipos equipos con fotografía",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = colorProgreso
                        )
                    )
                }
            }

            Surface(
                color = if (suficiente) Color(0xFFF0FDF4) else Color(0xFFFFFBEB),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = if (suficiente) Icons.Default.CheckCircle else Icons.Default.Warning,
                            contentDescription = null,
                            tint = if (suficiente) Color(0xFF059669) else Color(0xFFD97706),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = if (suficiente)
                                "Cada equipo tiene su evidencia fotográfica"
                            else
                                "Captura la evidencia desde la tarjeta de cada equipo:",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (suficiente) Color(0xFF065F46) else Color(0xFF92400E)
                            )
                        )
                    }

                    ITEMS_FOTO_OBLIGATORIOS.forEachIndexed { idx, itemNombre ->
                        val capturado = idx < cantidadFotos
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(start = 22.dp)
                        ) {
                            Text(
                                text = if (capturado) "✓ $itemNombre (Capturada)" else "• $itemNombre (Pendiente)",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 11.sp,
                                    color = if (capturado) Color(0xFF059669) else Color(0xFF78350F),
                                    fontWeight = if (capturado) FontWeight.Medium else FontWeight.Normal
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}
