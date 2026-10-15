package com.duoc.aquaflow.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.duoc.aquaflow.ui.theme.AquaDanger
import com.duoc.aquaflow.ui.theme.AquaNavy
import com.duoc.aquaflow.ui.theme.AquaOnSurfaceVariant
import com.duoc.aquaflow.ui.theme.AquaOrange
import com.duoc.aquaflow.ui.theme.AquaOutline
import com.duoc.aquaflow.ui.theme.AquaOutlineVariant

private val AlturaBoton = 56.dp

@Composable
fun PrimaryButton(
    texto: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icono: ImageVector? = null,
    cargando: Boolean = false
) {
    Button(
        onClick = onClick,
        enabled = enabled && !cargando,
        modifier = modifier.fillMaxWidth().heightIn(min = AlturaBoton),
        shape = MaterialTheme.shapes.medium,
        colors = ButtonDefaults.buttonColors(
            containerColor = AquaOrange,
            contentColor = AquaNavy,
            disabledContainerColor = AquaOutlineVariant,
            disabledContentColor = AquaOnSurfaceVariant
        )
    ) {
        BotonContenido(texto, icono, cargando)
    }
}

@Composable
fun SecondaryButton(
    texto: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icono: ImageVector? = null
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth().heightIn(min = AlturaBoton),
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.5.dp, AquaOutline),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = AquaNavy)
    ) {
        BotonContenido(texto, icono, false)
    }
}

@Composable
fun DangerButton(
    texto: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icono: ImageVector? = null
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth().heightIn(min = AlturaBoton),
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.5.dp, AquaDanger),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = AquaDanger)
    ) {
        BotonContenido(texto, icono, false)
    }
}

@Composable
private fun BotonContenido(texto: String, icono: ImageVector?, cargando: Boolean) {
    if (cargando) {
        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = AquaNavy)
        Spacer(Modifier.width(8.dp))
    } else if (icono != null) {
        Icon(icono, contentDescription = null, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
    }
    Text(texto, style = MaterialTheme.typography.labelLarge)
}
