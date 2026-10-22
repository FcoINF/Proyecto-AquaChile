package com.duoc.aquaflow.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.duoc.aquaflow.model.EstadoEquipo
import com.duoc.aquaflow.model.ItemEquipamiento

@Composable
fun ItemChecklist(
    item: ItemEquipamiento,
    onEstadoCambiado: (EstadoEquipo) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFFFF))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = item.titulo, style = MaterialTheme.typography.titleMedium)
            Text(text = item.descripcion, style = MaterialTheme.typography.bodyMedium, color = Color.Gray)

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                EstadoButton(
                    texto = "Cumple",
                    seleccionado = item.estado == EstadoEquipo.CUMPLE,
                    colorActivo = Color(0xFF4CAF50)
                ) { onEstadoCambiado(EstadoEquipo.CUMPLE) }

                EstadoButton(
                    texto = "No Cumple",
                    seleccionado = item.estado == EstadoEquipo.NO_CUMPLE,
                    colorActivo = Color.Red
                ) { onEstadoCambiado(EstadoEquipo.NO_CUMPLE) }

                EstadoButton(
                    texto = "Observado",
                    seleccionado = item.estado == EstadoEquipo.OBSERVADO,
                    colorActivo = Color(0xFFF95716) // Color principal AquaFlow
                ) { onEstadoCambiado(EstadoEquipo.OBSERVADO) }
            }
        }
    }
}

@Composable
fun EstadoButton(
    texto: String,
    seleccionado: Boolean,
    colorActivo: Color,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (seleccionado) colorActivo else Color(0xFFE0E0E0),
            contentColor = if (seleccionado) Color.White else Color.Black
        )
    ) {
        Text(text = texto)
    }
}