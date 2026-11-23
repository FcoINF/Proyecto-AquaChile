package com.duoc.aquaflow.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role as SemanticsRole
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.duoc.aquaflow.model.FaenaGuardada
import com.duoc.aquaflow.model.Rol
import com.duoc.aquaflow.model.Usuario
import com.duoc.aquaflow.ui.components.AquaSectionCard
import com.duoc.aquaflow.ui.navigation.MainBottomBar
import com.duoc.aquaflow.ui.navigation.Tab
import com.duoc.aquaflow.ui.theme.AquaAzul
import com.duoc.aquaflow.ui.theme.AquaAzulMedio
import com.duoc.aquaflow.ui.theme.AquaFlowTheme
import com.duoc.aquaflow.ui.theme.AquaGrisSuave

private val AzulAccion = Color(0xFF0755B8)
private val TextoSecundario = Color(0xFF738096)
private val VerdeEstado = Color(0xFF009F74)

@Composable
fun DashboardScreen(
    usuario: Usuario,
    tabs: List<Tab>,
    faenas: List<FaenaGuardada> = emptyList(),
    onNuevaFaena: () -> Unit,
    onTab: (Tab) -> Unit,
    onSalir: () -> Unit,
    cargando: Boolean = false,
    error: String? = null,
    onReintentar: () -> Unit = {},
    onVerFaena: (String) -> Unit = {},
    onAbrirBorradores: () -> Unit = {},
    onVerHistorial: () -> Unit = { onTab(Tab.HISTORIAL) },
    onAperturasRecibidas: (() -> Unit)? = null,
    envios: List<com.duoc.aquaflow.data.local.EnvioFaena> = emptyList(),
    onReintentarEnvio: (String) -> Unit = {},
    pronostico: com.duoc.aquaflow.viewmodel.PronosticoUbicacionViewModel? = null
) {
    val puedeCrear = usuario.rol == Rol.SUPERVISOR
    val borradores = faenas.filter { it.decision == null }
    val nombre = usuario.nombre.substringBefore(" ")
    val tabsDashboard = tabs.filter { it != Tab.ENCUESTA }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = AquaGrisSuave,
        topBar = {
            Column(Modifier.fillMaxWidth().background(AquaAzul).statusBarsPadding()) {
                CabeceraDashboard(usuario = usuario, onSalir = onSalir)
                pronostico?.let { com.duoc.aquaflow.ui.components.BarraPronosticoUbicacion(it) }
            }
        },
        bottomBar = { MainBottomBar(Tab.INICIO, tabsDashboard, onTab) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 30.dp, vertical = 22.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth().then(if (!puedeCrear) Modifier.heightIn(min = 198.dp) else Modifier),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = AquaAzul)
            ) {
                Box {
                    if (puedeCrear) {
                        Canvas(modifier = Modifier.fillMaxWidth().height(112.dp)) {
                            val centerX = size.width * 0.86f
                            val centerY = size.height * 0.5f
                            val baseRadius = size.height * 0.44f
                            listOf(0.75f, 1.12f, 1.5f).forEachIndexed { index, factor ->
                                drawCircle(
                                    color = Color.White.copy(alpha = 0.08f - index * 0.015f),
                                    radius = baseRadius * factor,
                                    center = androidx.compose.ui.geometry.Offset(centerX, centerY),
                                    style = Stroke(width = 2.dp.toPx())
                                )
                            }
                        }
                        Column(
                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Text("PANEL OPERATIVO", style = MaterialTheme.typography.labelMedium, color = Color(0xFF83B9FF))
                            Text("Hola, $nombre", style = MaterialTheme.typography.headlineSmall, color = Color.White)
                            Text("Gestiona la preparación y seguridad de cada faena.", style = MaterialTheme.typography.bodyMedium, color = Color(0xFFE0E7EF))
                        }
                    } else {
                        Box(Modifier.fillMaxWidth()) {
                            Surface(
                                modifier = Modifier.align(Alignment.BottomEnd).size(width = 150.dp, height = 150.dp),
                                color = Color.White.copy(alpha = 0.06f),
                                shape = RoundedCornerShape(topStart = 24.dp)
                            ) {}
                            Column(Modifier.padding(horizontal = 20.dp, vertical = 20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Surface(color = AzulAccion, shape = RoundedCornerShape(5.dp)) {
                                    Text("♙ PANEL OPERATIVO • CONTROL DE GESTIÓN", Modifier.padding(horizontal = 8.dp, vertical = 5.dp), color = Color.White, style = MaterialTheme.typography.labelSmall)
                                }
                                Text("Hola, Jefe!", style = MaterialTheme.typography.headlineLarge, color = Color.White, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                                Text(
                                    "Supervisa faenas en tiempo real, audita pre-chequeos normativos DPR-24 / DS 752 y valida las operaciones pendientes de tu autorización formal.",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = Color(0xFF9AA8B7)
                                )
                            }
                        }
                    }
                }
            }

            if (puedeCrear) {
                AquaSectionCard(
                    title = "Comenzar una faena",
                    subtitle = "Registra al equipo, las condiciones y los controles previos."
                ) {
                    Button(
                        onClick = onNuevaFaena,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AzulAccion)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Filled.CheckCircle, contentDescription = null)
                            Text("Nueva faena y checklist AST", modifier = Modifier.weight(1f))
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                        }
                    }
                }
            }

            if (onAperturasRecibidas != null) {
                OutlinedButton(onClick = onAperturasRecibidas, modifier = Modifier.fillMaxWidth()) {
                    Text(if (puedeCrear) "Mis expedientes en el servidor" else "Aperturas recibidas")
                }
            }
            if (envios.isNotEmpty()) {
                Text("Sincronización de expedientes", style = MaterialTheme.typography.titleMedium)
                envios.sortedBy { it.estado == "SINCRONIZADO" }.forEach { envio ->
                    val nombreFaena = faenas.firstOrNull { it.resumen.apertura.id == envio.faena }
                        ?.resumen?.apertura?.centroCultivo ?: envio.faena
                    com.duoc.aquaflow.ui.components.AquaSectionCard(title = nombreFaena) {
                        com.duoc.aquaflow.ui.components.EstadoSincronizacion(envio)
                        if (envio.estado == "ERROR") {
                            OutlinedButton(onClick = { onReintentarEnvio(envio.faena) }) { Text("Reintentar envío") }
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

            if (puedeCrear) UltimaFaenaCard(faenas.firstOrNull(), onVerHistorial)

            if (puedeCrear) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("MÓDULOS DE TERRENO", style = MaterialTheme.typography.labelLarge, color = AquaAzul)
                    ModuloTerreno(
                        titulo = "Borradores por completar",
                        descripcion = "Continúa con las faenas que guardaste.",
                        detalle = if (cargando || error != null) null else "${borradores.size} pendiente${if (borradores.size == 1) "" else "s"}",
                        onClick = onAbrirBorradores
                    )
                }
            }
        }
    }
}

@Composable
private fun CabeceraDashboard(
    usuario: Usuario,
    onSalir: () -> Unit
) {
    var mostrarMenuPerfil by remember { mutableStateOf(false) }
    val rol = if (usuario.rol == Rol.JEFE) "Jefe" else usuario.rol.texto

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text("AquaChile", style = MaterialTheme.typography.titleLarge, color = Color.White)
            Text("Puerto Montt - Operaciones", style = MaterialTheme.typography.bodySmall, color = Color(0xFFD6E0E6))
        }
        Box {
            Surface(
                onClick = { mostrarMenuPerfil = true },
                modifier = Modifier.semantics {
                    contentDescription = "Perfil de ${usuario.nombre}, $rol"
                    role = SemanticsRole.Button
                },
                shape = CircleShape,
                color = AquaAzulMedio,
                contentColor = Color.White,
                border = BorderStroke(1.dp, Color(0xFF92A8B7))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(rol, style = MaterialTheme.typography.labelMedium)
                    Icon(Icons.Default.ExpandMore, contentDescription = null, modifier = Modifier.size(14.dp))
                }
            }
            DropdownMenu(expanded = mostrarMenuPerfil, onDismissRequest = { mostrarMenuPerfil = false }) {
                DropdownMenuItem(
                    text = { Text("Cerrar sesión") },
                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null) },
                    onClick = {
                        mostrarMenuPerfil = false
                        onSalir()
                    }
                )
            }
        }
    }
}

@Composable
private fun UltimaFaenaCard(faena: FaenaGuardada?, onVerHistorial: () -> Unit) {
    val apertura = faena?.resumen?.apertura
    val buzo = apertura?.buzoAsignado?.takeIf(String::isNotBlank) ?: "Jorge Cárdenas"
    val codigo = apertura?.id?.takeIf(String::isNotBlank)?.let { "ID: F-${it.take(8).uppercase()}" } ?: "ID: F-2026-1025"
    val estado = when (faena?.estado) {
        com.duoc.aquaflow.model.EstadoFaena.AUTORIZADA -> "Aprobado"
        com.duoc.aquaflow.model.EstadoFaena.CERRADA -> "Finalizada"
        com.duoc.aquaflow.model.EstadoFaena.EN_EJECUCION -> "En ejecución"
        com.duoc.aquaflow.model.EstadoFaena.RECHAZADA -> "Rechazada"
        com.duoc.aquaflow.model.EstadoFaena.PRECHEQUEO -> "En revisión"
        null -> "Aprobado"
    }
    val estadoColor = if (estado == "Aprobado" || estado == "Finalizada") VerdeEstado else AzulAccion
    val estadoFondo = if (estado == "Aprobado" || estado == "Finalizada") Color(0xFFE7FBF3) else Color(0xFFE6F0FF)
    val detalle = if (faena == null) "Finalizada hoy 10:42 hrs - Jaula 12"
    else "${apertura?.fecha.orEmpty()} · ${apertura?.hora.orEmpty()} - ${apertura?.centroCultivo?.ifBlank { "Faena" }}"
    val mediciones = faena?.cierre?.mediciones

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                Text("●", color = VerdeEstado, style = MaterialTheme.typography.labelMedium)
                Text("ÚLTIMA FAENA REGISTRADA", style = MaterialTheme.typography.labelMedium, color = Color(0xFF667A97), modifier = Modifier.weight(1f))
                Surface(color = AquaGrisSuave, shape = RoundedCornerShape(5.dp)) {
                    Text(codigo, modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall, color = AquaAzul)
                }
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFE1E5E9)))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(color = Color(0xFFE6F0FF), shape = RoundedCornerShape(10.dp)) {
                    Text("〰", modifier = Modifier.padding(horizontal = 9.dp, vertical = 7.dp), color = AzulAccion, style = MaterialTheme.typography.titleMedium)
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    Text(buzo, style = MaterialTheme.typography.titleSmall, color = AquaAzul)
                    Text("Buzo Comercial Intermedio", style = MaterialTheme.typography.bodySmall, color = TextoSecundario)
                    Text("◷ $detalle", style = MaterialTheme.typography.bodySmall, color = Color(0xFFA0A7B1))
                }
                Surface(
                    color = estadoFondo,
                    shape = RoundedCornerShape(50),
                    border = BorderStroke(1.dp, estadoColor.copy(alpha = 0.35f))
                ) {
                    Text("✿ $estado", modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall, color = estadoColor)
                }
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFE1E5E9)))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MetricaFaena("TIEMPO FONDO", mediciones?.tiempoFondoMinutos?.toString() ?: "—", "MIN", Modifier.weight(1f))
                MetricaFaena("PROF. MÁX", mediciones?.profundidadMaximaMetros?.toString() ?: "—", "M", Modifier.weight(1f))
            }
            TextButton(onClick = onVerHistorial, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text("Ver Bitácora de Inmersión Completa →", color = AzulAccion, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun MetricaFaena(label: String, value: String, unit: String, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, color = AquaGrisSuave, shape = RoundedCornerShape(12.dp)) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = Color(0xFF8A9BB3), maxLines = 1)
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(value, style = MaterialTheme.typography.titleLarge, color = AquaAzul)
                Text(unit, style = MaterialTheme.typography.labelSmall, color = Color(0xFF6F7D90), modifier = Modifier.padding(bottom = 3.dp))
            }
        }
    }
}

@Composable
private fun ModuloTerreno(
    titulo: String,
    descripcion: String,
    detalle: String? = null,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = SemanticsRole.Button, onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("✓", modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp), color = AquaAzul)
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(titulo, style = MaterialTheme.typography.titleSmall)
                Text(descripcion, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (detalle != null) {
                Surface(color = AzulAccion, shape = RoundedCornerShape(50)) {
                    Text(detalle, modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp), style = MaterialTheme.typography.labelSmall, color = Color.White)
                }
            } else {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun DatoSalud(titulo: String, valor: String, unidad: String, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, color = AquaGrisSuave, shape = RoundedCornerShape(12.dp)) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(titulo, style = MaterialTheme.typography.labelSmall, color = TextoSecundario)
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(valor, style = MaterialTheme.typography.titleLarge, color = AquaAzul)
                Text(unidad, style = MaterialTheme.typography.labelSmall, color = TextoSecundario, modifier = Modifier.padding(bottom = 3.dp))
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
            onNuevaFaena = {},
            onTab = {},
            onSalir = {}
        )
    }
}
