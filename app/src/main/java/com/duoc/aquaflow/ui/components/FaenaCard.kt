package com.duoc.aquaflow.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import com.duoc.aquaflow.domain.Fechas
import com.duoc.aquaflow.model.Faena
import com.duoc.aquaflow.ui.theme.AquaOnSurfaceVariant
import com.duoc.aquaflow.ui.theme.AquaOrangeDark

/** Resumen de una faena para listas (inicio e historial). */
@Composable
fun FaenaCard(faena: Faena, onClick: () -> Unit, modifier: Modifier = Modifier) {
    SectionCard(modifier = modifier, onClick = onClick) {
        Row(
            modifier = Modifier,
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = faena.codigo,
                style = MaterialTheme.typography.labelLarge,
                color = AquaOrangeDark,
                modifier = Modifier.weight(1f)
            )
            EstadoFaenaChip(faena.estado)
        }
        Text(faena.buzoNombre, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(
            text = "${faena.centroNombre} · ${Fechas.fechaHora(faena.fechaInicio)}",
            style = MaterialTheme.typography.bodyMedium,
            color = AquaOnSurfaceVariant
        )
        Text(
            text = "Supervisor: ${faena.supervisorNombre}",
            style = MaterialTheme.typography.bodySmall,
            color = AquaOnSurfaceVariant
        )
    }
}
