package com.duoc.aquaflow.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

// Campo numérico de la encuesta: teclado numérico, rango aceptado como ayuda y borde rojo si el valor no cabe.
@Composable
fun CampoSalud(
    valor: String,
    alCambiar: (String) -> Unit,
    etiqueta: String,
    rango: String,
    fueraDeRango: Boolean,
    modifier: Modifier = Modifier.fillMaxWidth(),
    textStyle: TextStyle = MaterialTheme.typography.bodyLarge
) {
    OutlinedTextField(
        value = valor,
        onValueChange = alCambiar,
        label = { Text(etiqueta) },
        supportingText = { Text(if (fueraDeRango) "Fuera de rango ($rango)" else rango) },
        isError = fueraDeRango,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        textStyle = textStyle,
        modifier = modifier
    )
}

@Composable
fun ParametroSaludStepper(
    valor: String,
    alCambiar: (String) -> Unit,
    etiqueta: String,
    rango: IntRange,
    unidad: String,
    fueraDeRango: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        CampoSalud(
            valor = valor,
            alCambiar = alCambiar,
            etiqueta = etiqueta,
            rango = "${rango.first}-${rango.last} $unidad",
            fueraDeRango = fueraDeRango,
            textStyle = MaterialTheme.typography.headlineSmall
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val numero = valor.toIntOrNull()
            IconButton(
                onClick = { numero?.minus(1)?.let { alCambiar(it.toString()) } },
                enabled = numero != null && numero - 1 in rango,
                modifier = Modifier.weight(1f).heightIn(min = 48.dp)
            ) {
                Icon(Icons.Filled.Remove, contentDescription = "Disminuir $etiqueta")
            }
            IconButton(
                onClick = { numero?.plus(1)?.let { alCambiar(it.toString()) } },
                enabled = numero != null && numero + 1 in rango,
                modifier = Modifier.weight(1f).heightIn(min = 48.dp)
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Aumentar $etiqueta")
            }
        }
    }
}