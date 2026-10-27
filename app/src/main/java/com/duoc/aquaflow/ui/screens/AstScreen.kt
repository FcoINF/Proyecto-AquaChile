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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.duoc.aquaflow.model.FormularioAst
import com.duoc.aquaflow.model.PREGUNTAS_AST
import com.duoc.aquaflow.model.TIPOLOGIAS_AST
import com.duoc.aquaflow.ui.theme.AquaFlowTheme
import com.duoc.aquaflow.viewmodel.AstViewModel

private val AzulAst = Color(0xFF0755B8)
private val AzulNocheAst = Color(0xFF182B3A)
private val FondoAst = Color(0xFFF4F6FA)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AstScreen(
    ocupado: Boolean = false,
    errorExterno: String? = null,
    vm: AstViewModel,
    centro: String,
    fecha: String,
    onGuardar: (FormularioAst) -> Unit,
    onGuardarBorrador: (FormularioAst) -> Unit,
    onVolver: () -> Unit
) {
    val ui by vm.ui.collectAsState()
    com.duoc.aquaflow.ui.components.Guardando(ocupado)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                        Text("Apertura de Faena", style = MaterialTheme.typography.titleMedium)
                        Text(centro.ifBlank { "Centro sin asignar - Jaula 24" }, style = MaterialTheme.typography.labelSmall)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onVolver, enabled = !ocupado) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AzulNocheAst,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        containerColor = FondoAst,
        bottomBar = {
            Column(Modifier.fillMaxWidth().padding(horizontal = 40.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Button(
                    enabled = !ocupado,
                    onClick = { vm.guardar()?.let(onGuardar) },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 42.dp),
                    shape = RoundedCornerShape(9.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AzulAst)
                ) { Text("Finalizar Checklist", fontWeight = FontWeight.Bold) }
                OutlinedButton(
                    enabled = !ocupado,
                    onClick = { onGuardarBorrador(vm.borrador()) },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 36.dp),
                    shape = RoundedCornerShape(9.dp),
                    border = BorderStroke(1.dp, AzulAst),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AzulAst)
                ) { Text("Guardar Borrador") }
            }
        }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())
                .padding(horizontal = 19.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ProgresoAst()

            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(2.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(Modifier.fillMaxWidth().padding(horizontal = 13.dp, vertical = 11.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text("1. Antecedentes Generales", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Color(0xFF172033))
                    Text("Selecciona la tipología de la tarea", style = MaterialTheme.typography.bodySmall, color = Color(0xFF9299A3), fontSize = 9.sp)
                    Spacer(Modifier.height(1.dp))
                    TIPOLOGIAS_AST.forEach { tipo ->
                        Row(
                            modifier = Modifier.fillMaxWidth().height(28.dp).clickable(enabled = !ocupado) { vm.elegirTipologia(tipo) },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = ui.tipologia == tipo,
                                onClick = { if (!ocupado) vm.elegirTipologia(tipo) },
                                modifier = Modifier.padding(end = 2.dp),
                                colors = RadioButtonDefaults.colors(selectedColor = AzulAst)
                            )
                            Text(tipo, style = MaterialTheme.typography.labelMedium, color = Color(0xFF172033), fontWeight = FontWeight.Medium, fontSize = 10.sp)
                        }
                    }
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(2.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 11.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("2. Chequeo Inicial", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Color(0xFF172033))
                        Text("Verifica cada condición antes de continuar", style = MaterialTheme.typography.bodySmall, color = Color(0xFF9299A3), fontSize = 9.sp)
                    }
                    PREGUNTAS_AST.forEachIndexed { indice, pregunta ->
                        val respuesta = ui.respuestas.getOrNull(indice) == true
                        Card(
                            modifier = Modifier.fillMaxWidth().clickable(enabled = !ocupado) { vm.responder(indice, !respuesta) },
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF7F9FB)),
                            shape = RoundedCornerShape(9.dp),
                            border = BorderStroke(1.dp, Color(0xFFE1E7EF)),
                            elevation = CardDefaults.cardElevation(0.dp)
                        ) {
                            Row(
                                Modifier.fillMaxWidth().padding(start = 8.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    pregunta.texto,
                                    modifier = Modifier.weight(1f),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF172033),
                                    fontSize = 8.sp,
                                    lineHeight = 10.sp
                                )
                                Switch(
                                    checked = respuesta,
                                    onCheckedChange = { if (!ocupado) vm.responder(indice, it) },
                                    thumbContent = if (respuesta) {
                                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(SwitchDefaults.IconSize)) }
                                    } else null,
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = Color(0xFF07966D),
                                        checkedIconColor = Color(0xFF07966D),
                                        uncheckedThumbColor = Color(0xFF929292),
                                        uncheckedTrackColor = Color(0xFFF5F5F5),
                                        uncheckedBorderColor = Color(0xFFAAAAAA)
                                    )
                                )
                            }
                        }
                    }
                }
            }

            (errorExterno ?: ui.error)?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.height(1.dp))
        }
    }
}

@Composable
private fun ProgresoAst() {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(1.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Surface(color = AzulAst, shape = CircleShape) {
                    Text("4", color = Color.White, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall)
                }
                Text("FLUJO OPERATIVO DE FAENA", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, fontSize = 9.sp)
                Text("Paso 4 de 4", style = MaterialTheme.typography.labelSmall, color = Color(0xFF64748B), fontSize = 8.sp)
            }
            LinearProgressIndicator(progress = { 1f }, modifier = Modifier.fillMaxWidth(), color = AzulAst, trackColor = Color(0xFFE5EAF0))
            Text("4. FORMULARIO AST", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color(0xFF172033), fontSize = 8.sp)
        }
    }
}

@Preview(showBackground = true, widthDp = 286, heightDp = 844)
@Composable
fun AstScreenPreview() {
    AquaFlowTheme {
        AstScreen(
            vm = AstViewModel(),
            centro = "Centro Ang... - Jaula 24",
            fecha = "20/10/2026",
            onGuardar = {},
            onGuardarBorrador = {},
            onVolver = {}
        )
    }
}
