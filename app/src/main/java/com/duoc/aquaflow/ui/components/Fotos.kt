package com.duoc.aquaflow.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
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
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0B1723)) // Color secundario AquaFlow
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = "Evidencias Fotográficas", color = Color.White, style = MaterialTheme.typography.titleMedium)
                Text(text = "$cantidadFotos adjuntas", color = Color.LightGray, style = MaterialTheme.typography.bodySmall)
            }
            IconButton(onClick = onTomarFotoClick) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Tomar Foto",
                    tint = Color(0xFFF95716) // Color principal
                )
            }
        }
    }
}