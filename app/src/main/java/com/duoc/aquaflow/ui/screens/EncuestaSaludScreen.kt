package com.duoc.aquaflow.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.duoc.aquaflow.domain.Reglas
import com.duoc.aquaflow.model.EncuestaSalud
import com.duoc.aquaflow.model.NivelSalud
import com.duoc.aquaflow.model.PREGUNTAS_SALUD
import com.duoc.aquaflow.ui.theme.AquaFlowTheme
import com.duoc.aquaflow.viewmodel.EncuestaSaludViewModel

private val AzulNoche = Color(0xFF182B3A)
private val AzulAccion = Color(0xFF0755B8)
private val GrisFondo = Color(0xFFF3F6F9)
private val GrisInactivo = Color(0xFFF0F3F7)
private val GrisTexto = Color(0xFF64748B)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EncuestaSaludScreen(
    ocupado: Boolean = false,
    errorExterno: String? = null,
    vm: EncuestaSaludViewModel,
    buzo: String,
    onGuardar: (EncuestaSalud) -> Unit,
    onGuardarBorrador: (EncuestaSalud) -> Unit,
    onVolver: () -> Unit,
    centroCultivo: String = "Centro sin asignar - Jaula 24"
) {
    val ui by vm.ui.collectAsState()

    com.duoc.aquaflow.ui.components.Guardando(ocupado)
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Apertura de Faena", style = MaterialTheme.typography.titleMedium)
                        Text(centroCultivo.ifBlank { "Centro sin asignar - Jaula 24" }, style = MaterialTheme.typography.labelSmall)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onVolver, enabled = !ocupado) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AzulNoche,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        containerColor = GrisFondo,
        bottomBar = {
            Column(
                Modifier.fillMaxWidth().padding(horizontal = 28.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Button(
                    enabled = !ocupado,
                    onClick = { vm.guardar()?.let(onGuardar) },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 42.dp),
                    shape = RoundedCornerShape(9.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AzulAccion)
                ) { Text("Continuar al Checklist", fontWeight = FontWeight.Bold) }
                OutlinedButton(
                    enabled = !ocupado,
                    onClick = { onGuardarBorrador(vm.borrador()) },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 36.dp),
                    shape = RoundedCornerShape(9.dp),
                    border = BorderStroke(1.dp, AzulAccion),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AzulAccion)
                ) { Text("Guardar Borrador") }
            }
        }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ProgresoSalud()
            BuzoAsignadoCard(buzo)

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                Icon(Icons.Default.FavoriteBorder, contentDescription = null, tint = Color(0xFFEF476F), modifier = Modifier.size(18.dp))
                Text("PRESIÓN ARTERIAL", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Color(0xFF334155))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MedicionSaludCard(
                    titulo = "SISTÓLICA", valor = ui.sistolica, unidad = "mmHg",
                    rango = Reglas.RANGO_SISTOLICA, modifier = Modifier.weight(1f),
                    onCambiar = vm::cambiarSistolica
                )
                MedicionSaludCard(
                    titulo = "DIASTÓLICA", valor = ui.diastolica, unidad = "mmHg",
                    rango = Reglas.RANGO_DIASTOLICA, modifier = Modifier.weight(1f),
                    onCambiar = vm::cambiarDiastolica
                )
            }
            MedicionSaludCard(
                titulo = "SPO2", valor = ui.saturacion, unidad = "%",
                rango = Reglas.RANGO_SATURACION, saturacion = true,
                onCambiar = vm::cambiarSaturacion
            )

            EncuestaPreventivaCard(
                respuestas = ui.respuestas,
                onResponder = vm::responderPregunta
            )

            ResultadoSaludCard(ui.salud)
            (errorExterno ?: ui.error)?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.height(2.dp))
        }
    }
}

@Composable
private fun ProgresoSalud() {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(1.dp),
        shape = RoundedCornerShape(13.dp)
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 11.dp, vertical = 9.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Surface(color = AzulAccion, shape = CircleShape) {
                    Text("3", color = Color.White, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall)
                }
                Text("FLUJO OPERATIVO DE FAENA", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                Text("Paso 3 de 4", style = MaterialTheme.typography.labelSmall, color = GrisTexto)
            }
            androidx.compose.material3.LinearProgressIndicator(
                progress = { 0.75f }, modifier = Modifier.fillMaxWidth(), color = AzulAccion,
                trackColor = Color(0xFFE5EAF0)
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("3. ENCUESTA SALUD", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color(0xFF334155))
                Text("Sig: Formulario AST", style = MaterialTheme.typography.labelSmall, color = Color(0xFF8A9BB3))
            }
        }
    }
}

@Composable
private fun BuzoAsignadoCard(buzo: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(1.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(Modifier.fillMaxWidth().padding(10.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                Surface(color = Color(0xFFE6F0FF), shape = RoundedCornerShape(8.dp)) {
                    Icon(Icons.Default.Shield, contentDescription = null, tint = AzulAccion, modifier = Modifier.padding(7.dp).size(20.dp))
                }
                Column {
                    Text("BUZO ASIGNADO", style = MaterialTheme.typography.labelSmall, color = AzulAccion, fontWeight = FontWeight.Bold)
                    Text(buzo.ifBlank { "Buzo por asignar" }, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Color(0xFF172033))
                    Text("Buzo técnico de esta faena", style = MaterialTheme.typography.bodySmall, color = GrisTexto)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                EtiquetaDatoBuzo("Buzo seleccionado", Color(0xFFE7F8F1), Color(0xFF047857), Modifier.weight(1f))
                EtiquetaDatoBuzo("Faena actual", Color(0xFFE6F0FF), AzulAccion, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun EtiquetaDatoBuzo(texto: String, fondo: Color, textoColor: Color, modifier: Modifier = Modifier) {
    Surface(modifier, color = fondo, shape = RoundedCornerShape(7.dp)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 7.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = textoColor, modifier = Modifier.size(12.dp))
            Spacer(Modifier.width(4.dp))
            Text(texto, color = textoColor, style = MaterialTheme.typography.labelSmall, fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun MedicionSaludCard(
    titulo: String,
    valor: String,
    unidad: String,
    rango: IntRange,
    modifier: Modifier = Modifier.fillMaxWidth(),
    saturacion: Boolean = false,
    onCambiar: (String) -> Unit
) {
    val numero = valor.toIntOrNull() ?: 0
    val dentroDeRango = numero != 0 && numero in rango
    val etiqueta = when {
        numero == 0 -> "PENDIENTE"
        dentroDeRango -> if (saturacion) "SATURACIÓN OXÍGENO" else if (titulo == "SISTÓLICA") "ÓPTIMO" else "NORMAL"
        else -> "REVISAR"
    }
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(1.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 9.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(titulo, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color(0xFF334155), fontSize = 9.sp)
                Surface(color = if (dentroDeRango) Color(0xFFE4FFF4) else GrisInactivo, shape = RoundedCornerShape(4.dp)) {
                    Text(etiqueta, modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp), color = if (dentroDeRango) Color(0xFF047857) else GrisTexto, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, fontSize = 7.sp)
                }
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.Center) {
                BasicTextField(
                    value = valor,
                    onValueChange = onCambiar,
                    modifier = Modifier.width(66.dp),
                    textStyle = TextStyle(
                        color = Color(0xFF1E293B),
                        fontSize = if (saturacion) 25.sp else 22.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
                Spacer(Modifier.width(5.dp))
                Text(unidad.uppercase(), modifier = Modifier.padding(bottom = 3.dp), style = MaterialTheme.typography.labelSmall, color = GrisTexto, fontWeight = FontWeight.Bold, fontSize = 8.sp)
            }
            Text("${rango.first}-${rango.last} $unidad", modifier = Modifier.align(Alignment.CenterHorizontally), style = MaterialTheme.typography.labelSmall, color = GrisTexto, fontSize = 8.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BotonMedicion(Icons.Default.Remove, "Disminuir $titulo", Modifier.weight(1f), enabled = numero > rango.first) {
                    onCambiar((numero - 1).toString())
                }
                BotonMedicion(Icons.Default.Add, "Aumentar $titulo", Modifier.weight(1f), enabled = numero < rango.last) {
                    onCambiar((if (numero == 0) rango.first else numero + 1).toString())
                }
            }
        }
    }
}

@Composable
private fun BotonMedicion(icon: androidx.compose.ui.graphics.vector.ImageVector, descripcion: String, modifier: Modifier, enabled: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = modifier.height(34.dp).clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        color = if (enabled) Color(0xFFF1F5F9) else Color(0xFFF7F8FA),
        shape = RoundedCornerShape(7.dp),
        border = BorderStroke(1.dp, Color(0xFFE0E7EF))
    ) {
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(icon, contentDescription = descripcion, tint = if (enabled) Color(0xFF334155) else Color(0xFFB8C0CC), modifier = Modifier.size(17.dp))
        }
    }
}

@Composable
private fun EncuestaPreventivaCard(respuestas: Map<String, Boolean>, onResponder: (String, Boolean) -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(1.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(Modifier.fillMaxWidth().padding(10.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(color = Color(0xFFE6F0FF), shape = RoundedCornerShape(6.dp)) {
                    Icon(Icons.Default.Shield, contentDescription = null, tint = AzulAccion, modifier = Modifier.padding(6.dp).size(16.dp))
                }
                Column {
                    Text("ENCUESTA PREVENTIVA", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color(0xFF172033))
                    Text("Evaluación obligatoria de aptitud psicofísica previa al buceo.", style = MaterialTheme.typography.labelSmall, color = GrisTexto, fontSize = 8.sp)
                }
            }
            PREGUNTAS_SALUD.forEachIndexed { index, pregunta ->
                if (index > 0) HorizontalDivider(color = Color(0xFFE9EDF2))
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text("${index + 1}. $pregunta", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = Color(0xFF172033), fontSize = 9.sp, lineHeight = 12.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        RespuestaSalud("Sí", seleccionada = respuestas[pregunta] == true, afirmativa = true, Modifier.weight(1f)) {
                            onResponder(pregunta, true)
                        }
                        RespuestaSalud("No", seleccionada = respuestas[pregunta] == false, afirmativa = false, Modifier.weight(1f)) {
                            onResponder(pregunta, false)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RespuestaSalud(texto: String, seleccionada: Boolean, afirmativa: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val colorActivo = if (afirmativa) Color(0xFF078B68) else AzulAccion
    Surface(
        modifier = modifier.height(32.dp).clickable(role = Role.RadioButton, onClick = onClick),
        color = if (seleccionada) colorActivo else GrisInactivo,
        contentColor = if (seleccionada) Color.White else Color(0xFF475569),
        shape = RoundedCornerShape(7.dp)
    ) {
        Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            Icon(if (afirmativa) Icons.Default.CheckCircle else Icons.Default.Cancel, contentDescription = null, modifier = Modifier.size(12.dp))
            Spacer(Modifier.width(4.dp))
            Text(texto, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, fontSize = 9.sp)
        }
    }
}

@Composable
private fun ResultadoSaludCard(nivel: NivelSalud?) {
    val fondo = when (nivel) {
        NivelSalud.NORMAL -> Color(0xFF07966D)
        NivelSalud.ATENCION -> AzulAccion
        NivelSalud.CRITICO -> Color(0xFFDC2626)
        null -> Color(0xFF94A3B8)
    }
    Card(colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(2.dp), shape = RoundedCornerShape(12.dp)) {
        Column(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text("RESULTADO DE LA EVALUACIÓN", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color(0xFF172033), fontSize = 9.sp)
            Surface(color = fondo, shape = CircleShape) {
                Text(
                    "SALUD: ${nivel?.texto?.uppercase() ?: "PENDIENTE"}",
                    modifier = Modifier.padding(horizontal = 15.dp, vertical = 6.dp),
                    color = Color.White,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    fontSize = 9.sp
                )
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 380, heightDp = 850)
@Composable
fun EncuestaSaludScreenPreview() {
    AquaFlowTheme {
        EncuestaSaludScreen(
            vm = EncuestaSaludViewModel(),
            buzo = "Juan Pérez",
            onGuardar = {},
            onGuardarBorrador = {},
            onVolver = {},
            centroCultivo = "Centro Huito - Balsa Jaula 04"
        )
    }
}
