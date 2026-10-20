package com.duoc.aquaflow.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.duoc.aquaflow.model.Rol
import com.duoc.aquaflow.model.Usuario

@Composable
fun DashboardScreen(
    usuario: Usuario,
    onNuevaFaena: () -> Unit,
    onSalir: () -> Unit
) {
    val puedeCrear = usuario.rol == Rol.SUPERVISOR
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        floatingActionButton = {
            if (puedeCrear) {
                ExtendedFloatingActionButton(onClick = onNuevaFaena) { Text("Nueva faena") }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("AquaFlow", style = MaterialTheme.typography.headlineMedium)
            Text("Bienvenido: ${usuario.nombre}")
            Text("Rol activo: ${usuario.rol.texto}")
            Text(
                if (puedeCrear) "Inicia una faena para registrar su apertura."
                else "La revisión y el historial se incorporarán en una próxima etapa."
            )
            Button(onClick = onSalir) { Text("Cerrar sesión") }
        }
    }
}
