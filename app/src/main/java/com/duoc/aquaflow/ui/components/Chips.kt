package com.duoc.aquaflow.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.duoc.aquaflow.model.EstadoFaena
import com.duoc.aquaflow.ui.theme.AquaDanger
import com.duoc.aquaflow.ui.theme.AquaDangerContainer
import com.duoc.aquaflow.ui.theme.AquaInfo
import com.duoc.aquaflow.ui.theme.AquaInfoContainer
import com.duoc.aquaflow.ui.theme.AquaNavy
import com.duoc.aquaflow.ui.theme.AquaNavyContainer
import com.duoc.aquaflow.ui.theme.AquaOrangeContainer
import com.duoc.aquaflow.ui.theme.AquaOrangeDark
import com.duoc.aquaflow.ui.theme.AquaSuccess
import com.duoc.aquaflow.ui.theme.AquaSuccessContainer
import com.duoc.aquaflow.ui.theme.AquaWarning
import com.duoc.aquaflow.ui.theme.AquaWarningContainer

/** Tono semántico. El color nunca es el único portador de significado: siempre va con texto o ícono. */
enum class Tone(val fondo: Color, val contenido: Color) {
    NEUTRAL(AquaNavyContainer, AquaNavy),
    SUCCESS(AquaSuccessContainer, AquaSuccess),
    WARNING(AquaWarningContainer, AquaWarning),
    DANGER(AquaDangerContainer, AquaDanger),
    INFO(AquaInfoContainer, AquaInfo),
    BRAND(AquaOrangeContainer, AquaOrangeDark)
}

@Composable
fun StatusChip(
    texto: String,
    modifier: Modifier = Modifier,
    tono: Tone = Tone.NEUTRAL,
    icono: ImageVector? = null
) {
    Surface(color = tono.fondo, contentColor = tono.contenido, shape = RoundedCornerShape(50), modifier = modifier) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (icono != null) Icon(icono, contentDescription = null, modifier = Modifier.size(14.dp))
            Text(texto, style = MaterialTheme.typography.labelSmall)
        }
    }
}

fun EstadoFaena.tono(): Tone = when (this) {
    EstadoFaena.PRECHEQUEO -> Tone.INFO
    EstadoFaena.PENDIENTE_AUTORIZACION -> Tone.WARNING
    EstadoFaena.AUTORIZADA -> Tone.SUCCESS
    EstadoFaena.RECHAZADA -> Tone.DANGER
    EstadoFaena.CANCELADA -> Tone.NEUTRAL
    EstadoFaena.CERRADA -> Tone.NEUTRAL
}

fun EstadoFaena.icono(): ImageVector = when (this) {
    EstadoFaena.PRECHEQUEO -> Icons.Filled.Edit
    EstadoFaena.PENDIENTE_AUTORIZACION -> Icons.Filled.Info
    EstadoFaena.AUTORIZADA -> Icons.Filled.CheckCircle
    EstadoFaena.RECHAZADA -> Icons.Filled.Cancel
    EstadoFaena.CANCELADA -> Icons.Filled.Block
    EstadoFaena.CERRADA -> Icons.Filled.Done
}

@Composable
fun EstadoFaenaChip(estado: EstadoFaena, modifier: Modifier = Modifier) {
    StatusChip(estado.etiqueta, modifier = modifier, tono = estado.tono(), icono = estado.icono())
}
