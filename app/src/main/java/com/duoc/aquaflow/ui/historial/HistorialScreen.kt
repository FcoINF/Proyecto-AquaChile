package com.duoc.aquaflow.ui.historial

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.duoc.aquaflow.ui.theme.AquaBackground
import com.duoc.aquaflow.ui.theme.AquaBlueBg
import com.duoc.aquaflow.ui.theme.AquaDivider
import com.duoc.aquaflow.ui.theme.AquaGreen
import com.duoc.aquaflow.ui.theme.AquaGreenBg
import com.duoc.aquaflow.ui.theme.AquaGreenDark
import com.duoc.aquaflow.ui.theme.AquaNavy
import com.duoc.aquaflow.ui.theme.AquaNavyLight
import com.duoc.aquaflow.ui.theme.AquaOrange
import com.duoc.aquaflow.ui.theme.AquaTealBg
import com.duoc.aquaflow.ui.theme.AquaTextGrey
import com.duoc.aquaflow.ui.theme.AquaYellow
import com.duoc.aquaflow.ui.theme.AquaFlowTheme

// ---------------------------------------------------------------------------
// Modelos — orden HISTORIAL1 -> HISTORIAL3
// HISTORIAL1: Cierre y Registro Posterior + telemetría / curva descompresión
// HISTORIAL2: Chequeo médico Armada Art.302 + inicio historial local
// HISTORIAL3: Historial local consolidado + guardar en SQLite y cerrar faena
// ---------------------------------------------------------------------------

data class FaenaHistorial(
    val codigo: String,
    val buzo: String,
    val modulo: String,
    val estado: String,
    val profundidad: String,
    val duracion: String,
    val fecha: String
)

private val faenasDemo = listOf(
    FaenaHistorial(
        codigo = "Faena #104-0984",
        buzo = "Héctor Oliver Barria",
        modulo = "Módulo Sur",
        estado = "APROBADA",
        profundidad = "24.6 m",
        duracion = "38 min",
        fecha = "09/08/2026"
    ),
    FaenaHistorial(
        codigo = "Faena #104-0983",
        buzo = "Carlos Silva Méndez",
        modulo = "Módulo Norte",
        estado = "APROBADA",
        profundidad = "18.2 m",
        duracion = "42 min",
        fecha = "08/08/2026"
    )
)

// ---------------------------------------------------------------------------
// Pantalla completa scrollable
// ---------------------------------------------------------------------------

@Composable
fun HistorialScreen(
    onGuardarYCerrar: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = AquaBackground,
        bottomBar = { AquaBottomNav() }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            OfflineBanner()
            HeaderCierre()
            Column(modifier = Modifier.padding(horizontal = 12.dp)) {
                Spacer(modifier = Modifier.height(10.dp))
                // ---- HISTORIAL1 ----
                SuuntoCard()
                Spacer(modifier = Modifier.height(10.dp))
                DiverCard()
                Spacer(modifier = Modifier.height(10.dp))
                SurfaceStatusCard()
                Spacer(modifier = Modifier.height(10.dp))
                TelemetriaSection()
                Spacer(modifier = Modifier.height(10.dp))
                // ---- HISTORIAL2 ----
                MedicalCheckSection()
                Spacer(modifier = Modifier.height(10.dp))
                // ---- HISTORIAL3 ----
                HistorialLocalSection()
                Spacer(modifier = Modifier.height(12.dp))
                GuardarButton(onClick = onGuardarYCerrar)
                Spacer(modifier = Modifier.height(8.dp))
                FirmaDigitalLine()
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

// ---------------------------------------------------------------------------
// HISTORIAL1 — Banner offline + header + telemetría
// ---------------------------------------------------------------------------

@Composable
private fun OfflineBanner() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(AquaNavy)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(AquaYellow),
                contentAlignment = Alignment.Center
            ) {
                Text("◉", fontSize = 9.sp, color = AquaNavy)
            }
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "MODO OFFLINE ACTIVO • SQLITE ROOM DB",
                color = AquaYellow,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(AquaGreen)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "SYNC\nLISTA",
                color = AquaGreen,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 10.sp,
                textAlign = TextAlign.End
            )
        }
    }
}

@Composable
private fun HeaderCierre() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(AquaNavy)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            Text("☑", fontSize = 20.sp, color = AquaNavy)
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "AQUACHILE BUCEO • Pontón Salmones 12",
                color = Color.White.copy(alpha = 0.75f),
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "Cierre y Registro Posterior",
                color = Color.White,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color(0xFF3A4A5A)),
            contentAlignment = Alignment.Center
        ) {
            Text("🤿", fontSize = 20.sp)
        }
    }
}

@Composable
private fun SuuntoCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(AquaBlueBg)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(AquaNavy),
                contentAlignment = Alignment.Center
            ) {
                Text("◍", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("SUUNTO DIVELINK V3.2", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AquaNavy)
                Text("Telemetría descargada por NFC", fontSize = 12.sp, color = AquaNavy)
            }
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.White,
                border = null
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(AquaGreen)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("OK", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AquaNavy)
                }
            }
        }
    }
}

@Composable
private fun DiverCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row {
                Surface(shape = RoundedCornerShape(6.dp), color = AquaBlueBg) {
                    Text(
                        "MATRÍCULA #3829-B",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        fontSize = 10.sp, fontWeight = FontWeight.Bold, color = AquaNavy
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Surface(shape = RoundedCornerShape(6.dp), color = AquaBlueBg) {
                    Text(
                        "FAENA #104-0985",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        fontSize = 10.sp, fontWeight = FontWeight.Bold, color = AquaNavy
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFD9E4EA)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🤿", fontSize = 26.sp)
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Héctor Oliver Barria", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = AquaNavy)
                    Text(
                        "Módulo 04 • Jaula Red Lobera #12",
                        fontSize = 12.sp, color = AquaTextGrey
                    )
                }
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(AquaGreenBg),
                    contentAlignment = Alignment.Center
                ) {
                    Text("✓", color = AquaGreenDark, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }
        }
    }
}

@Composable
private fun SurfaceStatusCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = AquaTealBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("⚓", fontSize = 22.sp)
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "EN SUPERFICIE • INMERSIÓN FINALI...",
                    fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AquaGreenDark
                )
                Text(
                    "Intervalo seguro cumplido a las 16:42 hrs",
                    fontSize = 12.sp, color = AquaNavy
                )
            }
            Surface(shape = RoundedCornerShape(8.dp), color = AquaGreenDark) {
                Text(
                    "16:42",
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun TelemetriaSection() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Text("🌊", fontSize = 18.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "TELEMETRÍA Y CURVA DE\nDESCOMPRESIÓN",
                        fontSize = 13.sp, fontWeight = FontWeight.Bold, color = AquaNavy,
                        lineHeight = 15.sp
                    )
                }
                Text(
                    "TABLAS\nARMADA CH.",
                    fontSize = 10.sp, color = AquaTextGrey, textAlign = TextAlign.End,
                    lineHeight = 12.sp
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TelemetriaMiniCard(
                    titulo = "PROF.\nMÁXIMA",
                    valor = "22.4",
                    unidad = "M",
                    pie = "< 25m Máx",
                    modifier = Modifier.weight(1f)
                )
                TelemetriaMiniCard(
                    titulo = "T. DE FONDO",
                    valor = "35",
                    unidad = "MIN",
                    pie = "Sin Deco Ob.",
                    destacado = true,
                    modifier = Modifier.weight(1f)
                )
                TelemetriaMiniCard(
                    titulo = "PRESIÓN\nBANCO",
                    valor = "110",
                    unidad = "BAR",
                    pie = "> 50 BAR",
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            DiveProfileChart()
        }
    }
}

@Composable
private fun TelemetriaMiniCard(
    titulo: String,
    valor: String,
    unidad: String,
    pie: String,
    destacado: Boolean = false,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = AquaBackground
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(titulo, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = AquaTextGrey, textAlign = TextAlign.Center, lineHeight = 11.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(valor, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = AquaNavy)
                Spacer(modifier = Modifier.width(3.dp))
                Text(unidad, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AquaNavy)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                (if (destacado) "✓ " else "✓ ") + pie,
                fontSize = 9.sp,
                color = AquaGreenDark,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun DiveProfileChart() {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = AquaNavy),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF29B6F6)))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Perfil de Descenso & Ascenso", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Surface(shape = RoundedCornerShape(6.dp), color = AquaNavyLight) {
                    Text(
                        "Tasa: 8.8 m/min",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        color = AquaGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = Color(0xFF0E3B2E),
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text(
                    "Parada Seg: 3m / 3 min ✓",
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    color = Color(0xFF4ADE80), fontSize = 10.sp, fontWeight = FontWeight.Bold
                )
            }
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .padding(top = 4.dp)
            ) {
                val w = size.width
                val h = size.height - 24f
                // puntos normalizados (x: tiempo 0..42min, y: profundidad 0..22.4m)
                val pts = listOf(
                    Offset(0.02f * w, 0.05f * h),
                    Offset(0.06f * w, 0.30f * h),
                    Offset(0.18f * w, 0.97f * h),
                    Offset(0.52f * w, 0.97f * h),
                    Offset(0.66f * w, 0.55f * h),
                    Offset(0.74f * w, 0.38f * h),
                    Offset(0.84f * w, 0.38f * h),
                    Offset(0.98f * w, 0.05f * h)
                )
                // grilla
                for (i in 0..3) {
                    val y = (h / 3) * i
                    drawLine(Color.White.copy(alpha = 0.12f), Offset(0f, y), Offset(w, y), strokeWidth = 1f)
                }
                val path = Path().apply {
                    moveTo(pts[0].x, pts[0].y)
                    pts.drop(1).forEach { lineTo(it.x, it.y) }
                }
                drawPath(path, Color(0xFF29B6F6), style = Stroke(width = 5f, cap = StrokeCap.Round))
                pts.forEachIndexed { index, p ->
                    val c = when (index) {
                        0 -> Color(0xFFF95716)
                        pts.lastIndex -> AquaGreen
                        else -> Color(0xFF29B6F6)
                    }
                    drawCircle(c, radius = 9f, center = p)
                    drawCircle(Color.White, radius = 3.5f, center = p)
                }
            }
            Surface(shape = RoundedCornerShape(6.dp), color = Color(0xFF12395B), modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text(
                    "Fondo: 22.4m @ 35'",
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                    color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                listOf("0 min", "15 min", "30 min", "35 min", "42 min (Fin)").forEach {
                    Text(it, color = Color.White.copy(alpha = 0.6f), fontSize = 9.sp)
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// HISTORIAL2 — Chequeo médico
// ---------------------------------------------------------------------------

@Composable
private fun MedicalCheckSection() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(34.dp).clip(RoundedCornerShape(8.dp)).background(AquaNavy),
                    contentAlignment = Alignment.Center
                ) {
                    Text("✚", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("ARMADA DE CHILE • ART. 302", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = AquaOrange)
                    Text("Chequeo Médico de Sa...", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = AquaNavy)
                }
                Surface(shape = RoundedCornerShape(16.dp), color = AquaGreenBg) {
                    Text(
                        "APTO SALIDA",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        fontSize = 10.sp, fontWeight = FontWeight.Bold, color = AquaGreenDark
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                "Descarte obligatorio de Enfermedad por Descompresión post-inmersión. Verificación requerida previo al visado de faena.",
                fontSize = 12.sp, color = AquaTextGrey, lineHeight = 16.sp
            )
            Spacer(modifier = Modifier.height(10.dp))
            MedicalItem(
                titulo = "Sin dolor articular ni parestesia",
                desc = "Hombros, codos y rodillas sin dolor; sin hormigueo en extremidades."
            )
            HorizontalDivider(color = AquaDivider, thickness = 1.dp, modifier = Modifier.padding(vertical = 8.dp))
            MedicalItem(
                titulo = "Respiración eupneica & SpO2",
                desc = "Ventilación normal sin dolor torácico, tos ni sensación de ahogo.",
                badge = "98% SpO2"
            )
            HorizontalDivider(color = AquaDivider, thickness = 1.dp, modifier = Modifier.padding(vertical = 8.dp))
            MedicalItem(
                titulo = "Test de Romberg superado",
                desc = "Marcha recta sin dismetría y estabilidad ocular en cubierta."
            )
        }
    }
}

@Composable
private fun MedicalItem(titulo: String, desc: String, badge: String? = null) {
    Row(verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier.size(24.dp).clip(CircleShape).background(AquaGreen),
            contentAlignment = Alignment.Center
        ) {
            Text("✓", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(titulo, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = AquaNavy, modifier = Modifier.weight(1f))
                if (badge != null) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(shape = RoundedCornerShape(6.dp), color = AquaGreenBg) {
                        Text(
                            badge,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            fontSize = 10.sp, fontWeight = FontWeight.Bold, color = AquaGreenDark
                        )
                    }
                }
            }
            Text(desc, fontSize = 12.sp, color = AquaTextGrey, lineHeight = 15.sp)
        }
    }
}

// ---------------------------------------------------------------------------
// HISTORIAL3 — Historial local + guardar y cerrar
// ---------------------------------------------------------------------------

@Composable
private fun HistorialLocalSection() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Text("⛨", fontSize = 18.sp, color = AquaNavy)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("HISTORIAL LOCAL\nCONSOLIDADO", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = AquaNavy, lineHeight = 15.sp)
                }
                Surface(shape = RoundedCornerShape(6.dp), color = AquaBackground) {
                    Text(
                        "ROOM DB: 28\nFAENAS",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontSize = 9.sp, fontWeight = FontWeight.Bold, color = AquaTextGrey,
                        textAlign = TextAlign.Center, lineHeight = 11.sp
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            faenasDemo.forEachIndexed { index, faena ->
                FaenaRow(faena)
                if (index < faenasDemo.lastIndex) {
                    HorizontalDivider(color = AquaDivider, thickness = 1.dp, modifier = Modifier.padding(vertical = 10.dp))
                }
            }
        }
    }
}

@Composable
private fun FaenaRow(faena: FaenaHistorial) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("✈", fontSize = 14.sp, color = AquaNavy)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(faena.codigo, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = AquaNavy)
                }
                Text(
                    "${faena.buzo} • ${faena.modulo}",
                    fontSize = 12.sp, color = AquaTextGrey
                )
            }
            Surface(shape = RoundedCornerShape(16.dp), color = AquaGreenBg) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("✓", color = AquaGreenDark, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(faena.estado, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = AquaGreenDark)
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            FaenaDato("PROFUNDIDAD", faena.profundidad, Modifier.weight(1f))
            FaenaDato("DURACIÓN", faena.duracion, Modifier.weight(1f))
            FaenaDato("FECHA", faena.fecha, Modifier.weight(1f))
        }
    }
}

@Composable
private fun FaenaDato(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.Start) {
        Text(label, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = AquaTextGrey)
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = AquaNavy)
    }
}

@Composable
private fun GuardarButton(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = AquaOrange)
    ) {
        Text("◫", fontSize = 16.sp, color = Color.White)
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            "GUARDAR EN SQLITE Y CERRAR\nFAENA",
            color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp,
            textAlign = TextAlign.Center, lineHeight = 15.sp
        )
    }
}

@Composable
private fun FirmaDigitalLine() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(20.dp).clip(CircleShape).background(AquaGreenBg),
            contentAlignment = Alignment.Center
        ) {
            Text("⛨", fontSize = 12.sp, color = AquaGreenDark)
        }
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            "Firma Digital Criptográfica SHA-256 Lista para Sincronizar",
            fontSize = 11.sp, color = AquaTextGrey, textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun AquaBottomNav() {
    Surface(
        color = Color.White,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp, horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BottomNavItem("▦", "Inicio", false)
            BottomNavItem("☑", "AST/Check", false)
            BottomNavItem("◉", "Registro", true)
            BottomNavItem("↻", "Historial", false)
            BottomNavItem("◍", "Perfil", false)
        }
    }
}

@Composable
private fun BottomNavItem(icono: String, label: String, seleccionado: Boolean) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 8.dp)
    ) {
        Text(
            icono,
            fontSize = 20.sp,
            color = if (seleccionado) AquaOrange else AquaTextGrey,
            fontWeight = FontWeight.Bold
        )
        Text(
            label,
            fontSize = 10.sp,
            color = if (seleccionado) AquaOrange else AquaTextGrey,
            fontWeight = if (seleccionado) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun HistorialScreenPreview() {
    AquaFlowTheme(dynamicColor = false) {
        HistorialScreen()
    }
}
