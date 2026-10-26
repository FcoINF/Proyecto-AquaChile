package com.duoc.aquaflow.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.duoc.aquaflow.model.EncuestaSalud
import com.duoc.aquaflow.model.FaenaGuardada
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
    faenas: List<FaenaGuardada> = emptyList(),
    ultimaEncuesta: EncuestaSalud?,
    onNuevaFaena: () -> Unit,
    onEditarBorrador: (String) -> Unit = {},
    onTab: (Tab) -> Unit,
    onSalir: () -> Unit,
    cargando: Boolean = false,
    error: String? = null,
    onReintentar: () -> Unit = {},
    onVerFaena: (String) -> Unit = {}
) {
    val puedeCrear = usuario.rol == Rol.SUPERVISOR
    val borradores = faenas.filter { it.decision == null }
    val nombre = usuario.nombre.substringBefore(" ")
    var mostrarMenuPerfil by remember { mutableStateOf(false) }

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
                Box {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = usuario.rol.texto.lowercase(),
                            style = MaterialTheme.typography.labelMedium,
                            color = AquaAzul
                        )
                        IconButton(
                            onClick = { mostrarMenuPerfil = true },
                            modifier = Modifier
                                .size(48.dp)
                                .semantics {
                                    contentDescription = "Perfil de ${usuario.nombre}, ${usuario.rol.texto}"
                                }
                        ) {
                            Surface(
                                modifier = Modifier.size(36.dp),
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                border = BorderStroke(1.dp, AquaAzul)
                            ) {}
                        }
                    }
                    DropdownMenu(
                        expanded = mostrarMenuPerfil,
                        onDismissRequest = { mostrarMenuPerfil = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Cerrar sesión") },
                            onClick = {
                                mostrarMenuPerfil = false
                                onSalir()
                            },
                            leadingIcon = {
                                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                            }
                        )
                    }
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
                        else "Consulta las faenas por estado y revisa las pendientes de decisión.",
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
            }

            when {
                cargando -> Text("Cargando faenas…")
                error != null -> AquaSectionCard(title = "No se pudo actualizar el inicio") {
                    Text(error)
                    OutlinedButton(onClick = onReintentar) { Text("Reintentar") }
                }
                !puedeCrear -> com.duoc.aquaflow.ui.components.PanelAdministrador(faenas, onVerFaena)
            }

            if (puedeCrear && !cargando && error == null && borradores.isNotEmpty()) {
                AquaSectionCard(
                    title = "Borradores guardados",
                    subtitle = "${borradores.size} borrador(es) pendiente(s)"
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        borradores.forEach { faena ->
                            val resumen = faena.resumen
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = resumen.apertura.centroCultivo.ifBlank { "Centro por completar" },
                                            style = MaterialTheme.typography.titleSmall,
                                            color = AquaAzul
                                        )
                                        Surface(
                                            color = MaterialTheme.colorScheme.tertiaryContainer,
                                            shape = RoundedCornerShape(50)
                                        ) {
                                            Text(
                                                text = "Borrador",
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                style = MaterialTheme.typography.labelSmall
                                            )
                                        }
                                    }
                                    Text("Código · ${resumen.apertura.id.take(8).uppercase()}", style = MaterialTheme.typography.bodySmall)
                                    Text("Buzo · ${resumen.apertura.buzoAsignado.ifBlank { "Por completar" }}", style = MaterialTheme.typography.bodyMedium)
                                    Text("${resumen.apertura.fecha} · ${resumen.apertura.hora}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    OutlinedButton(
                                        onClick = { onEditarBorrador(resumen.apertura.id) },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text("Editar borrador")
                                    }
                                }
                            }
                        }
                    }
                }
            }

            ultimaEncuesta?.takeIf { puedeCrear }?.let { encuesta ->
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
            faenas = emptyList(),
            ultimaEncuesta = null,
            onNuevaFaena = {},
            onEditarBorrador = {},
            onTab = {},
            onSalir = {}
        )
    }
}
