package com.duoc.aquaflow.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun SeccionFotos(
    cantidadFotos: Int,
    onTomarFotoClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    Text("Evidencia Fotográficas Requeridas", style = MaterialTheme.typography.titleSmall)
                    Text("$cantidadFotos foto(s) adjunta(s)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = onTomarFotoClick) {
                    Icon(Icons.Filled.PhotoCamera, contentDescription = "Capturar foto del equipo", tint = Color(0xFF0755B8))
                }
            }
            Text(
                "Agrega una foto para documentar el estado del equipo.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(
                onClick = onTomarFotoClick,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(9.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0755B8))
            ) {
                Text("Capturar Fotos del Equipo")
            }
        }
    }
}
