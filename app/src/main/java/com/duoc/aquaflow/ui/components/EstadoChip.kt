package com.duoc.aquaflow.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.duoc.aquaflow.model.EstadoFaena

/** Etiqueta de color con el estado de una faena. */
@Composable
fun EstadoChip(estado: EstadoFaena) {
    val fondo = when (estado) {
        EstadoFaena.PRECHEQUEO -> MaterialTheme.colorScheme.secondaryContainer
        EstadoFaena.AUTORIZADA -> MaterialTheme.colorScheme.primaryContainer
        EstadoFaena.RECHAZADA -> MaterialTheme.colorScheme.errorContainer
        EstadoFaena.CERRADA -> MaterialTheme.colorScheme.surfaceVariant
    }
    val contenido = when (estado) {
        EstadoFaena.PRECHEQUEO -> MaterialTheme.colorScheme.onSecondaryContainer
        EstadoFaena.AUTORIZADA -> MaterialTheme.colorScheme.onPrimaryContainer
        EstadoFaena.RECHAZADA -> MaterialTheme.colorScheme.onErrorContainer
        EstadoFaena.CERRADA -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(color = fondo, contentColor = contenido, shape = RoundedCornerShape(50)) {
        Text(
            estado.etiqueta,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
        )
    }
}
