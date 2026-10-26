package com.duoc.aquaflow.ui.screens.historial

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Waves
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.duoc.aquaflow.domain.Reglas
import com.duoc.aquaflow.domain.ReglasSecciones
import com.duoc.aquaflow.domain.ReglasDecision
import com.duoc.aquaflow.model.*
import com.duoc.aquaflow.ui.components.CierreFaenaFormulario
import com.duoc.aquaflow.ui.components.EvidenciasGaleria
import com.duoc.aquaflow.ui.components.Guardando
import com.duoc.aquaflow.ui.components.SemaforoAst
import com.duoc.aquaflow.ui.components.SemaforoFaena
import com.duoc.aquaflow.ui.components.SemaforoSalud
import com.duoc.aquaflow.ui.theme.AquaAzul
import com.duoc.aquaflow.ui.theme.AquaNaranja
import com.duoc.aquaflow.ui.theme.AquaVerde
import com.duoc.aquaflow.ui.theme.SemaforoAmbar
import com.duoc.aquaflow.viewmodel.ResumenFaenaViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Detalle de una faena guardada, pensado para la consulta desde el historial.
 *
 * Mantiene las acciones existentes (decidir, cerrar) pero presenta la información
 * como un expediente operativo: cabecera, métricas, chequeo médico, condiciones,
 * checklist, AST, decisión y cierre.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetalleFaenaScreen(
    vm: ResumenFaenaViewModel,
    faena: FaenaGuardada,
    rol: Rol,
    nombreUsuario: String,
    onDecidir: (RegistroDecision) -> Unit,
    onVolver: () -> Unit,
    onFinalizar: () -> Unit,
    onCerrar: ((RegistroCierre) -> Unit)? = null,
    onIniciar: (() -> Unit)? = null,
    ocupado: Boolean = false,
    errorExterno: String? = null
) {
    val ui by vm.ui.collectAsState()
    val resumen = faena.resumen
    val apertura = resumen.apertura
    val decision = faena.decision
    val resultado = ReglasDecision.resultado(resumen)
    val faltantes = ReglasDecision.faltantes(resumen)
    val estados = ReglasSecciones.estados(resumen)

    Guardando(ocupado)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detalle de faena") },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver al historial"
                        )
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { CabeceraFaena(faena) }

            if (faltantes.isNotEmpty()) {
                item {
                    Aviso(
                        texto = "Expediente incompleto · Falta: ${faltantes.joinToString()}.",
                        fondo = MaterialTheme.colorScheme.errorContainer,
                        contenido = MaterialTheme.colorScheme.onErrorContainer,
                        icono = Icons.Filled.Info
                    )
                }
            }

            if (!errorExterno.isNullOrBlank()) {
                item {
                    Aviso(
                        texto = errorExterno,
                        fondo = MaterialTheme.colorScheme.errorContainer,
                        contenido = MaterialTheme.colorScheme.onErrorContainer,
                        icono = Icons.Filled.Warning
                    )
                }
            }

            item { FilaMetricas(faena) }

            item { SeccionSalud(resumen.encuesta) }
            item { SeccionCondiciones(apertura) }
            item { SeccionChecklist(resumen.checklist, estados.getValue("Checklist")) }
            item { SeccionAst(resumen.ast) }

            item {
                SeccionDecision(
                    vm = vm,
                    resumen = resumen,
                    decision = decision,
                    rol = rol,
                    nombreUsuario = nombreUsuario,
                    onDecidir = onDecidir,
                    motivo = ui.motivo,
                    uiError = ui.error,
                    puedeDecidir = resultado != null && !ocupado
                )
            }

            val propietario = rol == Rol.SUPERVISOR && apertura.supervisorACargo == nombreUsuario
            if (faena.inicio != null) item { Text("Inicio: ${formatearFecha(faena.inicio.iniciadoEn)} · ${faena.inicio.iniciadoPor}") }
            if (propietario && faena.estado == EstadoFaena.AUTORIZADA && onIniciar != null) item {
                Button(onClick = onIniciar, enabled = !ocupado && faltantes.isEmpty(), modifier = Modifier.fillMaxWidth()) { Text("Cierre faena") }
            }
            val cierre = faena.cierre
            when {
                cierre != null -> item {
                    SeccionCierre(cierre)
                    Text("Salud de salida · Buzo: ${apertura.buzoAsignado}")
                    if (cierre.saludSalida != null) SeccionSalud(cierre.saludSalida)
                    else Text("Cierre histórico sin encuesta de salida registrada.")
                }
                propietario && faena.estado == EstadoFaena.EN_EJECUCION && onCerrar != null -> item {
                    SeccionDetalle(
                        titulo = "Registro posterior y cierre",
                        icono = Icons.Filled.AccessTime,
                        subtitulo = "Completa los datos al finalizar la inmersión"
                    ) {
                        Text("Encuesta de salud de salida · Buzo: ${apertura.buzoAsignado}")
                        CierreFaenaFormulario(apertura.id, ocupado = ocupado) { onCerrar(it) }
                    }
                }
            }

            if (decision != null || !ReglasDecision.puedeDecidir(rol)) {
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        Button(onClick = onFinalizar) { Text("Volver al historial") }
                    }
                }
            }

            item { Spacer(Modifier.height(4.dp)) }
        }
    }
}

// ---------------------------------------------------------------------------
// Cabecera
// ---------------------------------------------------------------------------

@Composable
private fun CabeceraFaena(faena: FaenaGuardada) {
    val apertura = faena.resumen.apertura
    val resultado = ReglasDecision.resultado(faena.resumen)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = AquaAzul)
    ) {
        Column(
            modifier = Modifier.padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(color = AquaNaranja.copy(alpha = 0.18f), shape = CircleShape) {
                    Icon(
                        Icons.Filled.Waves,
                        contentDescription = null,
                        tint = AquaNaranja,
                        modifier = Modifier.padding(10.dp).size(20.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "FAENA",
                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 2.sp),
                        color = Color.White.copy(alpha = 0.65f)
                    )
                    Text(
                        apertura.centroCultivo.ifBlank { "Centro por completar" },
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                EstadoPildora(faena.estado)
                Icon(
                    Icons.Filled.DateRange,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    "${apertura.fecha.ifBlank { "Sin fecha" }}${if (apertura.hora.isNotBlank()) " · ${apertura.hora}" else ""}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.85f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (resultado != null) {
                SemaforoFaena(resultado)
            }

            HorizontalDivider(color = Color.White.copy(alpha = 0.12f))

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                PersonaCabecera(
                    etiqueta = "Buzo",
                    nombre = apertura.buzoAsignado,
                    modifier = Modifier.weight(1f)
                )
                PersonaCabecera(
                    etiqueta = "Supervisor",
                    nombre = apertura.supervisorACargo,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun EstadoPildora(estado: EstadoFaena) {
    val color = colorEstado(estado)
    Surface(color = color.copy(alpha = 0.2f), shape = RoundedCornerShape(50)) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(7.dp).background(color, CircleShape))
            Spacer(Modifier.width(7.dp))
            Text(
                estado.etiqueta,
                style = MaterialTheme.typography.labelMedium,
                color = Color.White
            )
        }
    }
}

@Composable
private fun PersonaCabecera(etiqueta: String, nombre: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            Icons.Filled.Person,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.55f),
            modifier = Modifier.size(18.dp)
        )
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                etiqueta,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.6f)
            )
            Text(
                nombre.ifBlank { "Por completar" },
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun colorEstado(estado: EstadoFaena): Color = when (estado) {
    EstadoFaena.PRECHEQUEO -> AquaNaranja
    EstadoFaena.EN_EJECUCION, EstadoFaena.AUTORIZADA -> AquaVerde
    EstadoFaena.RECHAZADA -> Color(0xFFFF8A80)
    EstadoFaena.CERRADA -> Color(0xFFB0BEC5)
}

// ---------------------------------------------------------------------------
// Métricas
// ---------------------------------------------------------------------------

@Composable
private fun FilaMetricas(faena: FaenaGuardada) {
    val encuesta = faena.resumen.encuesta?.takeIf { ReglasSecciones.encuesta(it) == EstadoSeccion.COMPLETA }
    val cierre = faena.cierre

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        MetricaResumen(
            etiqueta = "Inmersión",
            valor = cierre?.let { "${it.duracionMinutos} min" } ?: "—",
            detalle = if (cierre != null) "Registrada" else "Sin cierre",
            icono = Icons.Filled.AccessTime,
            modifier = Modifier.weight(1f)
        )
        MetricaResumen(
            etiqueta = "Presión",
            valor = encuesta?.let { "${it.sistolica}/${it.diastolica}" } ?: "—",
            detalle = "mmHg",
            icono = Icons.Filled.Favorite,
            modifier = Modifier.weight(1f)
        )
        MetricaResumen(
            etiqueta = "Saturación",
            valor = encuesta?.let { "${it.saturacion}%" } ?: "—",
            detalle = "O₂",
            icono = Icons.Filled.Air,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun MetricaResumen(
    etiqueta: String,
    valor: String,
    detalle: String,
    icono: ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                icono,
                contentDescription = null,
                tint = AquaNaranja,
                modifier = Modifier.size(18.dp)
            )
            Text(
                valor,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Column {
                Text(
                    etiqueta,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
                Text(
                    detalle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    maxLines = 1
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Secciones
// ---------------------------------------------------------------------------

@Composable
private fun SeccionSalud(encuesta: EncuestaSalud?) {
    val estado = ReglasSecciones.encuesta(encuesta)
    SeccionDetalle(
        titulo = "Chequeo médico",
        icono = Icons.Filled.Favorite,
        subtitulo = "Estado: ${estado.texto} · Signos vitales antes de la inmersión"
    ) {
        if (encuesta == null) {
            TextoVacio("Sin registro de presión ni saturación para esta faena.")
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                DatoSalud(
                    etiqueta = "Presión arterial",
                    valor = "${encuesta.sistolica}/${encuesta.diastolica}",
                    unidad = "mmHg",
                    modifier = Modifier.weight(1f)
                )
                DatoSalud(
                    etiqueta = "Saturación",
                    valor = "${encuesta.saturacion}",
                    unidad = "%",
                    modifier = Modifier.weight(1f)
                )
            }

            if (estado == EstadoSeccion.COMPLETA) SemaforoSalud(
                Reglas.nivelSalud(
                    encuesta.sistolica,
                    encuesta.diastolica,
                    encuesta.saturacion,
                    encuesta.alertasEncuesta.size
                )
            )

            if (encuesta.alertasEncuesta.isEmpty()) {
                Text(
                    "Sin alertas declaradas en la encuesta.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Alertas declaradas",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.error
                    )
                    encuesta.alertasEncuesta.forEach { alerta ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                Icons.Filled.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(alerta, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SeccionCondiciones(apertura: PreChequeo) {
    SeccionDetalle(
        titulo = "Condiciones iniciales",
        icono = Icons.Filled.CheckCircle,
        subtitulo = "Apertura: ${ReglasSecciones.apertura(apertura).texto} · Verificación previa a la faena"
    ) {
        FilaCondicion("Condiciones generales listas", apertura.condicionesListas)
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        FilaCondicion("Embarcación lista", apertura.embarcacionLista)
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        FilaCondicion("Comunicaciones operativas", apertura.comunicacionesOperativas)
    }
}

@Composable
private fun SeccionChecklist(checklist: ChecklistFaena?, estado: EstadoSeccion) {
    val items = checklist?.items.orEmpty()
    val conformes = items.count { it.estado == EstadoEquipo.CUMPLE }
    val fotos = checklist?.fotosEvidencia.orEmpty()

    SeccionDetalle(
        titulo = "Checklist de equipos",
        icono = Icons.Filled.CheckCircle,
        subtitulo = "Estado: ${estado.texto} · $conformes de ${items.size} equipos conformes"
    ) {
        if (items.isEmpty()) {
            TextoVacio("El checklist de equipos aún no está registrado.")
        } else {
            items.forEachIndexed { indice, item ->
                if (indice > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                FilaEquipo(item)
            }
        }

        if (fotos.isNotEmpty()) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Text(
                "Evidencias fotográficas (${fotos.size})",
                style = MaterialTheme.typography.labelLarge
            )
            EvidenciasGaleria(fotos)
        }
    }
}

@Composable
private fun SeccionAst(ast: FormularioAst?) {
    val estado = ReglasSecciones.ast(ast)
    SeccionDetalle(
        titulo = "Análisis de seguridad (AST)",
        icono = Icons.AutoMirrored.Filled.Assignment,
        subtitulo = "Riesgos y controles de la tarea"
    ) {
        Text("Estado: ${estado.texto}", style = MaterialTheme.typography.bodySmall)
        if (ast == null) {
            TextoVacio("El AST aún no está registrado.")
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    ast.tipologia.ifBlank { "Tarea sin tipificar" },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                if (estado == EstadoSeccion.COMPLETA) SemaforoAst(ast.resultado)
            }

            Text(
                "${ast.pasos.size} paso(s) · ${ast.pasos.count { it.critico }} riesgo(s) crítico(s)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            com.duoc.aquaflow.ui.components.AntecedentesAstDetalle(ast.antecedentes)
            if (ast.requiereNuevoAst) {
                Aviso(
                    texto = "Requiere un nuevo AST antes de continuar.",
                    fondo = MaterialTheme.colorScheme.errorContainer,
                    contenido = MaterialTheme.colorScheme.onErrorContainer,
                    icono = Icons.Filled.Warning
                )
            }

            if (ast.pasos.isNotEmpty()) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Text("Pasos de la tarea", style = MaterialTheme.typography.labelLarge)
                ast.pasos.forEachIndexed { indice, paso ->
                    if (indice > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    PasoAstDetalle(paso, indice + 1)
                    paso.fechasRevision.forEachIndexed { revision, fecha ->
                        if (fecha.isNotBlank()) Text("Revisión ${revision + 1}: $fecha")
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Text("Verificación inicial", style = MaterialTheme.typography.labelLarge)
            (if (ast.versionPreguntas >= 2) PREGUNTAS_AST else com.duoc.aquaflow.model.PREGUNTAS_AST_ANTERIORES).forEachIndexed { indice, pregunta ->
                val respuesta = ast.respuestas.getOrNull(indice)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        when (respuesta) {
                            true -> Icons.Filled.CheckCircle
                            false -> Icons.Filled.Cancel
                            null -> Icons.Filled.Info
                        },
                        contentDescription = null,
                        tint = when (respuesta) {
                            true -> AquaVerde
                            false -> MaterialTheme.colorScheme.error
                            null -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        "${indice + 1}. ${pregunta.texto}",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f)
                    )
                    if (pregunta.critica) {
                        Text(
                            "Crítica",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SeccionDecision(
    vm: ResumenFaenaViewModel,
    resumen: ResumenFaena,
    decision: RegistroDecision?,
    rol: Rol,
    nombreUsuario: String,
    onDecidir: (RegistroDecision) -> Unit,
    motivo: String,
    uiError: String?,
    puedeDecidir: Boolean
) {
    SeccionDetalle(
        titulo = "Decisión",
        icono = Icons.AutoMirrored.Filled.Assignment,
        subtitulo = "Autorización de la jefatura"
    ) {
        when {
            decision != null -> {
                val autorizada = decision.decision == Decision.AUTORIZADA
                val color = if (autorizada) AquaVerde else MaterialTheme.colorScheme.error

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(color = color.copy(alpha = 0.12f), shape = CircleShape) {
                        Icon(
                            if (autorizada) Icons.Filled.CheckCircle else Icons.Filled.Cancel,
                            contentDescription = null,
                            tint = color,
                            modifier = Modifier.padding(10.dp).size(22.dp)
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            decision.decision.texto,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = color
                        )
                        Text(
                            "Registrada por ${decision.decididoPor.ifBlank { "usuario no registrado" }}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (decision.motivo.isNotBlank()) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Text(
                                "Motivo",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(decision.motivo, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }

            !ReglasDecision.puedeDecidir(rol) -> {
                Text(
                    "Solo la jefatura autoriza o rechaza la faena. Ya quedó guardada en el dispositivo para su revisión.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            else -> {
                Text(
                    "Revisa el expediente y registra la decisión de la jefatura.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = motivo,
                    onValueChange = vm::cambiarMotivo,
                    label = { Text("Motivo (obligatorio al rechazar)") },
                    modifier = Modifier.fillMaxWidth()
                )
                uiError?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        enabled = puedeDecidir,
                        onClick = {
                            vm.decidir(Decision.AUTORIZADA, resumen, rol, nombreUsuario)?.let(onDecidir)
                        },
                        modifier = Modifier.weight(1f)
                    ) { Text("Autorizar") }
                    OutlinedButton(
                        enabled = puedeDecidir,
                        onClick = {
                            vm.decidir(Decision.RECHAZADA, resumen, rol, nombreUsuario)?.let(onDecidir)
                        },
                        modifier = Modifier.weight(1f)
                    ) { Text("Rechazar") }
                }
            }
        }
    }
}

@Composable
private fun SeccionCierre(cierre: RegistroCierre) {
    SeccionDetalle(
        titulo = "Cierre de faena",
        icono = Icons.Filled.AccessTime,
        subtitulo = "Registro posterior a la inmersión"
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            DatoCompacto(
                etiqueta = "Tiempo de inmersión",
                valor = "${cierre.duracionMinutos} min",
                modifier = Modifier.weight(1f)
            )
            DatoCompacto(
                etiqueta = "Revisión de salida",
                valor = if (cierre.revisionSalida) "Realizada" else "Pendiente",
                modifier = Modifier.weight(1f)
            )
        }

        if (cierre.observaciones.isNotBlank()) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        "Observaciones",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(cierre.observaciones, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        Text(
            "Cerrada por ${cierre.cerradoPor.ifBlank { "usuario no registrado" }}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (cierre.cerradoEn > 0L) {
            Text(
                formatearFecha(cierre.cerradoEn),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Piezas reutilizables
// ---------------------------------------------------------------------------

@Composable
private fun SeccionDetalle(
    titulo: String,
    icono: ImageVector,
    modifier: Modifier = Modifier,
    subtitulo: String? = null,
    contenido: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = CircleShape) {
                    Icon(
                        icono,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(9.dp).size(18.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        titulo,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    subtitulo?.let {
                        Text(
                            it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            contenido()
        }
    }
}

@Composable
private fun DatoSalud(
    etiqueta: String,
    valor: String,
    unidad: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                etiqueta,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                valor,
                style = MaterialTheme.typography.titleLarge,
                color = AquaAzul,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                unidad,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DatoCompacto(etiqueta: String, valor: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                etiqueta,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                valor,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = AquaAzul
            )
        }
    }
}

@Composable
private fun FilaCondicion(texto: String, valor: Boolean?) {
    val color = when (valor) {
        true -> AquaVerde
        false -> MaterialTheme.colorScheme.error
        null -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val icono = when (valor) {
        true -> Icons.Filled.CheckCircle
        false -> Icons.Filled.Cancel
        null -> Icons.Filled.Info
    }
    val estado = when (valor) {
        true -> "Sí"
        false -> "No"
        null -> "Sin responder"
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icono, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
        Text(texto, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Text(estado, style = MaterialTheme.typography.labelMedium, color = color)
    }
}

@Composable
private fun FilaEquipo(item: ItemEquipamiento) {
    val color = when (item.estado) {
        EstadoEquipo.CUMPLE -> AquaVerde
        EstadoEquipo.NO_CUMPLE -> MaterialTheme.colorScheme.error
        EstadoEquipo.OBSERVADO -> SemaforoAmbar
        EstadoEquipo.PENDIENTE -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val icono = when (item.estado) {
        EstadoEquipo.CUMPLE -> Icons.Filled.CheckCircle
        EstadoEquipo.NO_CUMPLE -> Icons.Filled.Cancel
        EstadoEquipo.OBSERVADO -> Icons.Filled.Warning
        EstadoEquipo.PENDIENTE -> Icons.Filled.Info
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(icono, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                item.titulo,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            val referencia = listOf(item.categoria, item.badge)
                .filter { it.isNotBlank() }
                .joinToString(" · ")
            if (referencia.isNotBlank()) {
                Text(
                    referencia,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (item.notas.isNotBlank()) {
                Text(
                    item.notas,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Surface(color = color.copy(alpha = 0.12f), shape = RoundedCornerShape(50)) {
            Text(
                textoEstado(item.estado),
                style = MaterialTheme.typography.labelSmall,
                color = color,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
            )
        }
    }
}

@Composable
private fun PasoAstDetalle(paso: PasoAst, numero: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Top
        ) {
            Text(
                "$numero. ${paso.tarea.ifBlank { "Paso sin descripción" }}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            if (paso.critico) {
                Surface(color = MaterialTheme.colorScheme.errorContainer, shape = RoundedCornerShape(50)) {
                    Text(
                        "Crítico",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
        }
        if (paso.peligro.isNotBlank()) {
            Text(
                "Peligro · ${paso.peligro}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (paso.control.isNotBlank()) {
            Text(
                "Control · ${paso.control}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun Aviso(
    texto: String,
    fondo: Color,
    contenido: Color,
    icono: ImageVector
) {
    Surface(color = fondo, shape = RoundedCornerShape(16.dp)) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(icono, contentDescription = null, tint = contenido, modifier = Modifier.size(18.dp))
            Text(texto, style = MaterialTheme.typography.bodySmall, color = contenido)
        }
    }
}

@Composable
private fun TextoVacio(texto: String) {
    Text(
        texto,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

private fun textoEstado(estado: EstadoEquipo): String = when (estado) {
    EstadoEquipo.CUMPLE -> "Cumple"
    EstadoEquipo.NO_CUMPLE -> "No cumple"
    EstadoEquipo.OBSERVADO -> "Observado"
    EstadoEquipo.PENDIENTE -> "Pendiente"
}

private fun formatearFecha(millis: Long): String =
    SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(millis))
