package com.duoc.aquaflow.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.duoc.aquaflow.model.EncuestaSalud
import com.duoc.aquaflow.model.Rol
import com.duoc.aquaflow.model.Usuario
import com.duoc.aquaflow.ui.components.SemaforoSalud

@Composable
fun DashboardScreen(
    usuario: Usuario,
    ultimaEncuesta: EncuestaSalud?,
    onNuevaEncuesta: () -> Unit,
    onSalir: () -> Unit
) {
    val puedeCrear = usuario.rol == Rol.SUPERVISOR

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        floatingActionButton = {
            if (puedeCrear) {
                ExtendedFloatingActionButton(onClick = onNuevaEncuesta) {
                    Text("Encuesta de salud")
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier.padding(innerPadding).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Bienvenido: ${usuario.nombre}")
            Text("Rol activo: ${usuario.rol}")
            Text(
                if (puedeCrear) "Permisos: Registro y consulta de pre-chequeos"
                else "Permisos: Solo consulta de historial y detalles"
            )

            ultimaEncuesta?.let { e ->
                Text("Última encuesta: ${e.sistolica}/${e.diastolica} mmHg · Sat. ${e.saturacion}%")
                Text("Alertas en la encuesta: ${e.alertasEncuesta.size}")
                SemaforoSalud(e.nivel)
            }

            Button(onClick = onSalir) { Text("Cerrar sesión") }
        }
    }
}