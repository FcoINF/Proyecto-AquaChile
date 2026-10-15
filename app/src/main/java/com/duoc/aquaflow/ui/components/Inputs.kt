package com.duoc.aquaflow.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.duoc.aquaflow.model.EstadoEquipo
import com.duoc.aquaflow.ui.theme.AquaOnSurfaceVariant
import com.duoc.aquaflow.ui.theme.AquaOutline
import com.duoc.aquaflow.ui.theme.AquaOutlineVariant
import com.duoc.aquaflow.ui.theme.AquaSurface

@Composable
fun AquaTextField(
    valor: String,
    onValueChange: (String) -> Unit,
    etiqueta: String,
    modifier: Modifier = Modifier,
    error: String? = null,
    ayuda: String? = null,
    enabled: Boolean = true,
    singleLine: Boolean = true,
    minLines: Int = 1,
    sufijo: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    visualTransformation: VisualTransformation = VisualTransformation.None
) {
    OutlinedTextField(
        value = valor,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        enabled = enabled,
        label = { Text(etiqueta) },
        isError = error != null,
        singleLine = singleLine,
        minLines = minLines,
        shape = MaterialTheme.shapes.medium,
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboardType,
            capitalization = if (keyboardType == KeyboardType.Text) KeyboardCapitalization.Sentences else KeyboardCapitalization.None
        ),
        visualTransformation = visualTransformation,
        suffix = if (sufijo != null) ({ Text(sufijo) }) else null,
        supportingText = if (error != null || ayuda != null) ({ Text(error ?: ayuda.orEmpty()) }) else null
    )
}

/** Botón de opción grande (ícono arriba, texto abajo) para decisiones rápidas en terreno. */
@Composable
fun OpcionBoton(
    texto: String,
    seleccionado: Boolean,
    tono: Tone,
    icono: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val forma = MaterialTheme.shapes.medium
    val fondo = if (seleccionado) tono.fondo else AquaSurface
    val borde = if (seleccionado) BorderStroke(2.dp, tono.contenido) else BorderStroke(1.dp, AquaOutline)
    val color = if (seleccionado) tono.contenido else AquaOnSurfaceVariant
    Column(
        modifier = modifier
            .heightIn(min = 60.dp)
            .clip(forma)
            .background(fondo)
            .border(borde, forma)
            .selectable(selected = seleccionado, enabled = enabled, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(icono, contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
        Text(
            text = texto,
            color = color,
            style = MaterialTheme.typography.labelMedium,
            textAlign = TextAlign.Center,
            maxLines = 2
        )
    }
}

@Composable
fun SiNoSelector(
    valor: Boolean?,
    onChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    etiquetaSi: String = "SÍ",
    etiquetaNo: String = "NO",
    enabled: Boolean = true
) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        OpcionBoton(
            texto = etiquetaSi,
            seleccionado = valor == true,
            tono = Tone.SUCCESS,
            icono = Icons.Filled.CheckCircle,
            onClick = { onChange(true) },
            modifier = Modifier.weight(1f),
            enabled = enabled
        )
        OpcionBoton(
            texto = etiquetaNo,
            seleccionado = valor == false,
            tono = Tone.DANGER,
            icono = Icons.Filled.Cancel,
            onClick = { onChange(false) },
            modifier = Modifier.weight(1f),
            enabled = enabled
        )
    }
}

@Composable
fun EstadoEquipoSelector(
    valor: EstadoEquipo?,
    onChange: (EstadoEquipo) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OpcionBoton(
            texto = EstadoEquipo.CUMPLE.etiqueta,
            seleccionado = valor == EstadoEquipo.CUMPLE,
            tono = Tone.SUCCESS,
            icono = Icons.Filled.CheckCircle,
            onClick = { onChange(EstadoEquipo.CUMPLE) },
            modifier = Modifier.weight(1f),
            enabled = enabled
        )
        OpcionBoton(
            texto = EstadoEquipo.NO_CUMPLE.etiqueta,
            seleccionado = valor == EstadoEquipo.NO_CUMPLE,
            tono = Tone.DANGER,
            icono = Icons.Filled.Cancel,
            onClick = { onChange(EstadoEquipo.NO_CUMPLE) },
            modifier = Modifier.weight(1f),
            enabled = enabled
        )
        OpcionBoton(
            texto = EstadoEquipo.OBSERVADO.etiqueta,
            seleccionado = valor == EstadoEquipo.OBSERVADO,
            tono = Tone.WARNING,
            icono = Icons.Filled.Warning,
            onClick = { onChange(EstadoEquipo.OBSERVADO) },
            modifier = Modifier.weight(1f),
            enabled = enabled
        )
    }
}

/** Fila seleccionable tipo radio, con título, detalle y una advertencia opcional. */
@Composable
fun OpcionCard(
    titulo: String,
    seleccionado: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    detalle: String? = null,
    advertencia: String? = null,
    enabled: Boolean = true
) {
    val forma = MaterialTheme.shapes.medium
    val borde = if (seleccionado) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else BorderStroke(1.dp, AquaOutlineVariant)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(forma)
            .background(if (seleccionado) MaterialTheme.colorScheme.primaryContainer else AquaSurface)
            .border(borde, forma)
            .selectable(selected = seleccionado, enabled = enabled, role = Role.RadioButton, onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = seleccionado, onClick = null, enabled = enabled)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(titulo, style = MaterialTheme.typography.titleSmall)
            if (detalle != null) {
                Text(detalle, style = MaterialTheme.typography.bodySmall, color = AquaOnSurfaceVariant)
            }
            if (advertencia != null) {
                Text(advertencia, style = MaterialTheme.typography.labelMedium, color = Tone.DANGER.contenido)
            }
        }
    }
}

/** Línea "etiqueta: valor" para fichas de solo lectura. */
@Composable
fun DatoFila(etiqueta: String, valor: String, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(etiqueta, style = MaterialTheme.typography.bodyMedium, color = AquaOnSurfaceVariant, modifier = Modifier.weight(0.45f))
        Text(
            valor,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(0.55f)
        )
    }
}

/** Chip de filtro: seleccionado se muestra con relleno y borde de marca. */
@Composable
fun ChipSeleccionable(
    texto: String,
    seleccionado: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val forma = androidx.compose.foundation.shape.RoundedCornerShape(50)
    Box(
        modifier = modifier
            .heightIn(min = 40.dp)
            .clip(forma)
            .background(if (seleccionado) MaterialTheme.colorScheme.primaryContainer else AquaSurface)
            .border(
                if (seleccionado) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else BorderStroke(1.dp, AquaOutline),
                forma
            )
            .selectable(selected = seleccionado, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(texto, style = MaterialTheme.typography.labelLarge)
    }
}
