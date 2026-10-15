package com.duoc.aquaflow.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.duoc.aquaflow.ui.theme.AquaDanger

/** Confirma una firma o autorización pidiendo el PIN del usuario (4 dígitos). */
@Composable
fun PinDialog(
    titulo: String,
    descripcion: String,
    textoConfirmar: String,
    error: String?,
    cargando: Boolean,
    onConfirmar: (String) -> Unit,
    onCerrar: () -> Unit
) {
    var pin by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = { if (!cargando) onCerrar() },
        title = { Text(titulo) },
        text = {
            Column {
                Text(descripcion, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = pin,
                    onValueChange = { nuevo -> pin = nuevo.filter { c -> c.isDigit() }.take(4) },
                    label = { Text("PIN de 4 dígitos") },
                    singleLine = true,
                    isError = error != null,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    supportingText = if (error != null) ({ Text(error) }) else null
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirmar(pin) }, enabled = pin.length == 4 && !cargando) {
                Text(textoConfirmar)
            }
        },
        dismissButton = {
            TextButton(onClick = onCerrar, enabled = !cargando) { Text("Cancelar") }
        }
    )
}

/** Pide un motivo escrito (rechazar o cancelar una faena). */
@Composable
fun MotivoDialog(
    titulo: String,
    descripcion: String,
    textoConfirmar: String,
    error: String?,
    cargando: Boolean,
    onConfirmar: (String) -> Unit,
    onCerrar: () -> Unit
) {
    var motivo by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = { if (!cargando) onCerrar() },
        title = { Text(titulo) },
        text = {
            Column {
                Text(descripcion, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = motivo,
                    onValueChange = { motivo = it.take(500) },
                    label = { Text("Motivo") },
                    minLines = 3,
                    isError = error != null,
                    supportingText = if (error != null) ({ Text(error) }) else null
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirmar(motivo) }, enabled = motivo.isNotBlank() && !cargando) {
                Text(textoConfirmar, color = AquaDanger)
            }
        },
        dismissButton = {
            TextButton(onClick = onCerrar, enabled = !cargando) { Text("Volver") }
        }
    )
}

@Composable
fun ConfirmDialog(
    titulo: String,
    texto: String,
    textoConfirmar: String,
    onConfirmar: () -> Unit,
    onCerrar: () -> Unit,
    textoCancelar: String = "Cancelar"
) {
    AlertDialog(
        onDismissRequest = onCerrar,
        title = { Text(titulo) },
        text = { Text(texto) },
        confirmButton = { TextButton(onClick = onConfirmar) { Text(textoConfirmar) } },
        dismissButton = { TextButton(onClick = onCerrar) { Text(textoCancelar) } }
    )
}

/**
 * Evita perder cambios sin guardar: devuelve la acción de "volver" que debe usarse tanto en el botón
 * del encabezado como (automáticamente) con el gesto/botón atrás del sistema.
 */
@Composable
fun rememberSalidaProtegida(sinGuardar: Boolean, onVolver: () -> Unit): () -> Unit {
    var confirmar by remember { mutableStateOf(false) }
    BackHandler(enabled = sinGuardar) { confirmar = true }
    if (confirmar) {
        ConfirmDialog(
            titulo = "Cambios sin guardar",
            texto = "Si sales ahora perderás los cambios que no guardaste.",
            textoConfirmar = "Salir sin guardar",
            textoCancelar = "Seguir editando",
            onConfirmar = {
                confirmar = false
                onVolver()
            },
            onCerrar = { confirmar = false }
        )
    }
    return { if (sinGuardar) confirmar = true else onVolver() }
}
