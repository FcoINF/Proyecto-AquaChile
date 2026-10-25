package com.duoc.aquaflow.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.window.DialogProperties

@Composable
fun Guardando(ocupado: Boolean) {
    BackHandler(enabled = ocupado) { }
    if (ocupado) AlertDialog(
        onDismissRequest = {}, confirmButton = {},
        title = { Text("Guardando en el dispositivo") },
        text = { CircularProgressIndicator() },
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
    )
}
