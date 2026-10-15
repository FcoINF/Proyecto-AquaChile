package com.duoc.aquaflow.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType

// Campo numérico de la encuesta: teclado numérico, rango aceptado como ayuda y borde rojo si el valor no cabe.
@Composable
fun CampoSalud(
    valor: String,
    alCambiar: (String) -> Unit,
    etiqueta: String,
    rango: String,
    fueraDeRango: Boolean,
    modifier: Modifier = Modifier.fillMaxWidth()
) {
    OutlinedTextField(
        value = valor,
        onValueChange = alCambiar,
        label = { Text(etiqueta) },
        supportingText = { Text(if (fueraDeRango) "Fuera de rango ($rango)" else rango) },
        isError = fueraDeRango,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = modifier
    )
}