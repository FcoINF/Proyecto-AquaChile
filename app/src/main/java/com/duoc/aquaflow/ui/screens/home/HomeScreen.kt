package com.duoc.aquaflow.ui.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.duoc.aquaflow.model.EstadoFaena
import com.duoc.aquaflow.model.Faena
import com.duoc.aquaflow.model.Rol
import com.duoc.aquaflow.model.Usuario
import com.duoc.aquaflow.ui.components.AquaScreen
import com.duoc.aquaflow.ui.components.BannerLocal
import com.duoc.aquaflow.ui.components.EstadoFaenaChip
import com.duoc.aquaflow.ui.components.FaenaCard
import com.duoc.aquaflow.ui.components.LocalAppContainer
import com.duoc.aquaflow.ui.components.PrimaryButton
import com.duoc.aquaflow.ui.components.ScreenColumn
import com.duoc.aquaflow.ui.components.SectionCard
import com.duoc.aquaflow.ui.components.StatusChip
import com.duoc.aquaflow.ui.components.Tone
import com.duoc.aquaflow.ui.navigation.MainBottomBar
import com.duoc.aquaflow.ui.navigation.Tab
import com.duoc.aquaflow.ui.theme.AquaNavy
import com.duoc.aquaflow.ui.theme.AquaOnSurfaceVariant
import com.duoc.aquaflow.ui.theme.AquaOrange
import com.duoc.aquaflow.viewmodel.HomeViewModel
import com.duoc.aquaflow.viewmodel.vmFactory

@Composable
fun HomeScreen(
    usuario: Usuario,
    onNuevaFaena: () -> Unit,
    onAbrirFaena: (Faena) -> Unit,
    onTab: (Tab) -> Unit
) {
    val container = LocalAppContainer.current
    val vm: HomeViewModel = viewModel(
        key = "home-${usuario.id}",
        factory = vmFactory { HomeViewModel(usuario, container.faenaRepository) }
    )
    val state by vm.state.collectAsStateWithLifecycle()
    val esSupervisor = usuario.rol == Rol.SUPERVISOR

    AquaScreen(
        title = usuario.nombre,
        subtitle = usuario.rol.etiqueta,
        bottomBar = { MainBottomBar(Tab.INICIO, usuario.rol, onTab) },
        floatingActionButton = {
            if (esSupervisor) {
                ExtendedFloatingActionButton(
                    onClick = onNuevaFaena,
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    text = { Text("Nueva faena") },
                    containerColor = AquaOrange,
                    contentColor = AquaNavy
                )
            }
        }
    ) { padding ->
        Column(Modifier.padding(padding)) {
            BannerLocal(state.pendientesSync)
            ScreenColumn(PaddingValues(0.dp)) {
                if (esSupervisor) {
                    TarjetaNuevaFaena(onNuevaFaena)
                    SeccionFaenas(
                        titulo = "Faenas en curso",
                        vacio = "No tienes faenas en curso. Inicia una nueva apertura.",
                        faenas = state.activas,
                        onAbrir = onAbrirFaena
                    )
                } else {
                    TarjetaPendientes(state.pendientesAutorizacion.size)
                    SeccionFaenas(
                        titulo = "Pendientes de autorización",
                        vacio = "No hay faenas esperando tu decisión.",
                        faenas = state.pendientesAutorizacion,
                        onAbrir = onAbrirFaena
                    )
                }
                ResumenPorEstado(state.totalPorEstado)
                state.ultima?.let { ultima ->
                    Text("Última faena registrada", style = MaterialTheme.typography.titleMedium)
                    FaenaCard(ultima, onClick = { onAbrirFaena(ultima) })
                }
                Spacer(Modifier.height(72.dp)) // espacio para que el botón flotante no tape contenido
            }
        }
    }
}

@Composable
private fun TarjetaNuevaFaena(onNuevaFaena: () -> Unit) {
    Surface(color = AquaNavy, contentColor = Color.White, shape = MaterialTheme.shapes.large, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            StatusChip("PROTOCOLO DPR-24", tono = Tone.BRAND)
            Text("Iniciar nueva apertura / pre-chequeo de faena", style = MaterialTheme.typography.titleLarge)
            Text(
                "Verificación de equipos de soporte vital, AST y condición de salud del buzo antes de descender.",
                style = MaterialTheme.typography.bodyMedium
            )
            PrimaryButton("Abrir faena y checklist", onClick = onNuevaFaena, icono = Icons.Filled.PlayArrow)
        }
    }
}

@Composable
private fun TarjetaPendientes(cantidad: Int) {
    Surface(color = AquaNavy, contentColor = Color.White, shape = MaterialTheme.shapes.large, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            StatusChip("AUTORIZACIÓN DE FAENAS", tono = Tone.BRAND)
            Text(
                if (cantidad == 0) "Sin faenas pendientes" else "$cantidad faena(s) esperan tu decisión",
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                "Revisa la auditoría consolidada, firma con tu PIN y autoriza o rechaza con motivo.",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun SeccionFaenas(titulo: String, vacio: String, faenas: List<Faena>, onAbrir: (Faena) -> Unit) {
    Text(titulo, style = MaterialTheme.typography.titleMedium)
    if (faenas.isEmpty()) {
        Text(vacio, style = MaterialTheme.typography.bodyMedium, color = AquaOnSurfaceVariant)
    } else {
        faenas.forEach { faena -> FaenaCard(faena, onClick = { onAbrir(faena) }) }
    }
}

@Composable
private fun ResumenPorEstado(totales: Map<EstadoFaena, Int>) {
    if (totales.isEmpty()) return
    SectionCard(title = "Resumen de faenas") {
        EstadoFaena.values().forEach { estado ->
            val cantidad = totales[estado] ?: 0
            if (cantidad > 0) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    EstadoFaenaChip(estado)
                    Text(cantidad.toString(), style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}
