package com.duoc.aquaflow.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.duoc.aquaflow.model.EncuestaSalud
import com.duoc.aquaflow.model.Rol
import com.duoc.aquaflow.model.Usuario
import com.duoc.aquaflow.ui.components.AquaSectionCard
import com.duoc.aquaflow.ui.components.SemaforoSalud
import com.duoc.aquaflow.ui.navigation.MainBottomBar
import com.duoc.aquaflow.ui.navigation.Tab
import com.duoc.aquaflow.ui.theme.AquaAzul
import com.duoc.aquaflow.ui.theme.AquaFlowTheme
import com.duoc.aquaflow.ui.theme.AquaNaranja

@Composable
fun DashboardScreen(
    usuario: Usuario,
    tabs: List<Tab>,
    ultimaEncuesta: EncuestaSalud?,
    onNuevaFaena: () -> Unit,
    onTab: (Tab) -> Unit,
    onSalir: () -> Unit
) {
    val puedeCrear = usuario.rol == Rol.SUPERVISOR
    val nombre = usuario.nombre.substringBefore(" ")

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = { MainBottomBar(Tab.INICIO, tabs, onTab) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("AquaChile", style = MaterialTheme.typography.titleLarge, color = AquaAzul)
                    Text("AquaFlow · Operaciones", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        usuario.rol.texto,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = AquaAzul
                    )
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = AquaAzul)
            ) {
                Column(
                    modifier = Modifier.padding(22.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("PANEL OPERATIVO", style = MaterialTheme.typography.labelMedium, color = AquaNaranja)
                    Text("Hola, $nombre", style = MaterialTheme.typography.headlineMedium, color = Color.White)
                    Text(
                        if (puedeCrear) "Gestiona la preparación y seguridad de cada faena."
                        else "Revisa las faenas guardadas y resuelve su autorización desde Historial.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFD6E0E6)
                    )
                }
            }

            if (puedeCrear) {
                AquaSectionCard(
                    title = "Comenzar una faena",
                    subtitle = "Registra al equipo, las condiciones y los controles previos."
                ) {
                    Button(onClick = onNuevaFaena, modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Nueva faena")
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                        }
                    }
                }
            } else {
                AquaSectionCard(
                    title = "Seguimiento operativo",
                    subtitle = "Abre una faena desde el historial para revisarla y autorizarla o rechazarla."
                ) {
                    Text(
                        "Usa las pestañas inferiores para revisar las operaciones disponibles.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            ultimaEncuesta?.let { encuesta ->
                AquaSectionCard(
                    title = "Último control de salud",
                    subtitle = "Último control guardado durante esta sesión"
                ) {
                    Text("Presión arterial", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${encuesta.sistolica}/${encuesta.diastolica} mmHg", style = MaterialTheme.typography.headlineSmall)
                    Text("Saturación de oxígeno · ${encuesta.saturacion}%", style = MaterialTheme.typography.bodyMedium)
                    SemaforoSalud(encuesta.nivel)
                }
            }

            OutlinedButton(onClick = onSalir, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                Text("Cerrar sesión", modifier = Modifier.padding(start = 8.dp))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DashboardScreenPreview() {
    AquaFlowTheme {
        DashboardScreen(
            usuario = Usuario("Carlos Supervisor", "1234", Rol.SUPERVISOR),
            tabs = listOf(Tab.INICIO, Tab.HISTORIAL),
            ultimaEncuesta = null,
            onNuevaFaena = {},
            onTab = {},
            onSalir = {}
        )
    }
}
