package com.duoc.aquaflow.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.duoc.aquaflow.model.EstadoEquipo
import com.duoc.aquaflow.model.ItemEquipamiento
import com.duoc.aquaflow.ui.theme.AquaNaranja

@Composable
fun ItemChecklist(
    item: ItemEquipamiento,
    onEstadoCambiado: (EstadoEquipo) -> Unit
) {
    Card(
        modifier = Modifier
            .testTag("equipo-${item.id}")
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(text = item.titulo, style = MaterialTheme.typography.titleMedium)
            Text(text = item.descripcion, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                EstadoButton(
                    texto = "Cumple",
                    seleccionado = item.estado == EstadoEquipo.CUMPLE,
                    colorActivo = Color(0xFF078B68),
                    modifier = Modifier.weight(1f)
                ) { onEstadoCambiado(EstadoEquipo.CUMPLE) }

                EstadoButton(
                    texto = "No Cumple",
                    seleccionado = item.estado == EstadoEquipo.NO_CUMPLE,
                    colorActivo = Color(0xFFB42318),
                    modifier = Modifier.weight(1f)
                ) { onEstadoCambiado(EstadoEquipo.NO_CUMPLE) }

                EstadoButton(
                    texto = "Observado",
                    seleccionado = item.estado == EstadoEquipo.OBSERVADO,
                    colorActivo = AquaNaranja,
                    modifier = Modifier.weight(1f)
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
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    if (seleccionado) {
        Button(
            onClick = onClick,
            modifier = modifier,
            colors = ButtonDefaults.buttonColors(
                containerColor = colorActivo,
                contentColor = if (colorActivo == AquaNaranja) MaterialTheme.colorScheme.onPrimary else Color.White
            ),
            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 10.dp)
        ) {
            Text(text = texto, style = MaterialTheme.typography.labelSmall, maxLines = 1)
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            modifier = modifier,
            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 10.dp)
        ) {
            Text(text = texto, style = MaterialTheme.typography.labelSmall, maxLines = 1)
        }
    }
}
