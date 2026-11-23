package com.duoc.aquaflow.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.duoc.aquaflow.model.MetadatosFoto
import com.duoc.aquaflow.ui.theme.AquaAzulAccion
import java.text.DateFormat
import java.util.Date
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun EvidenciasGaleria(
    fotos: List<String>,
    nombresPorFoto: Map<String, String> = emptyMap(),
    categoriasPorFoto: Map<String, String> = emptyMap(),
    onQuitar: ((String) -> Unit)? = null,
    metadatos: Map<String, MetadatosFoto> = emptyMap(),
    onElegirReferencia: ((String) -> Unit)? = null,
    onComparar: ((String, Boolean) -> Unit)? = null
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        fotos.forEachIndexed { indice, referencia ->
            val bitmap = cargarFotoEvidencia(referencia).bitmap
            val metadata = metadatos[referencia]
            val imagenReferencia = metadata?.referencia
            var comparar by remember(referencia) { mutableStateOf(false) }
            var ampliada by rememberSaveable(referencia) { mutableStateOf<String?>(null) }
            val patron = cargarFotoEvidencia(imagenReferencia).bitmap
            val similitud by produceState<Int?>(null, bitmap, patron) {
                value = if (bitmap != null && patron != null) withContext(Dispatchers.Default) {
                    similitudVisual(bitmap, patron)
                } else null
            }
            val nombre = nombresPorFoto[referencia]
                ?.takeIf(String::isNotBlank)
                ?: "Evidencia ${indice + 1}"
            val categoria = categoriasPorFoto[referencia]
                ?.takeIf(String::isNotBlank)
                ?: "Equipo asociado no disponible"

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFD8E1EA)),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(92.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFF1F5F9))
                            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
                            .clickable(onClickLabel = "Ampliar foto") { ampliada = referencia },
                        contentAlignment = Alignment.Center
                    ) {
                        bitmap?.let {
                            Image(
                                bitmap = it.asImageBitmap(),
                                contentDescription = "Foto de $nombre",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } ?: Icon(
                            imageVector = Icons.Outlined.PhotoCamera,
                            contentDescription = "Vista previa no disponible para $nombre",
                            tint = Color(0xFF64748B),
                            modifier = Modifier.size(30.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "EVIDENCIA ${(indice + 1).toString().padStart(2, '0')}",
                            style = MaterialTheme.typography.labelSmall,
                            color = AquaAzulAccion,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(3.dp))
                        Text(
                            text = nombre,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E293B),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = categoria,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF64748B),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(6.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Outlined.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF059669),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                "Fotografía capturada",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color(0xFF047857)
                            )
                        }
                        val fecha = metadata?.capturadaEn?.let { DateFormat.getDateTimeInstance().format(Date(it)) }
                        val autor = metadata?.capturadaPor?.takeIf(String::isNotBlank)
                        val datosCaptura = listOfNotNull(fecha, autor).joinToString(" · ")
                        Text(datosCaptura.ifBlank { "Datos de captura sin registrar" }, style = MaterialTheme.typography.bodySmall)
                        if (metadata != null || onElegirReferencia != null) {
                            Text(when (metadata?.coincide) {
                                true -> "Coincide con la referencia"
                                false -> "Difiere de la referencia"
                                null -> if (imagenReferencia == null) "Sin foto de referencia" else "Revisión manual pendiente"
                            }, style = MaterialTheme.typography.bodySmall)
                        }
                        similitud?.let {
                            Text("Similitud visual orientativa: $it %", style = MaterialTheme.typography.bodySmall)
                        }
                        if (onElegirReferencia != null) {
                            TextButton(onClick = { onElegirReferencia(referencia) }) {
                                Text(if (imagenReferencia == null) "Elegir referencia" else "Cambiar referencia")
                            }
                        }
                        if (imagenReferencia != null) {
                            TextButton(onClick = { ampliada = imagenReferencia }) { Text("Ampliar referencia") }
                            TextButton(onClick = { comparar = true }) { Text("Comparar fotos") }
                        }
                        if (onQuitar != null) {
                            TextButton(
                                onClick = { onQuitar(referencia) },
                                modifier = Modifier.heightIn(min = 48.dp),
                                contentPadding = PaddingValues(horizontal = 0.dp)
                            ) {
                                Icon(
                                    Icons.Outlined.DeleteOutline,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text("Quitar evidencia")
                            }
                        }
                    }
                }
            }
            ampliada?.let { ruta ->
                val tipo = if (ruta == referencia) "Captura" else "Referencia"
                VisorEvidencia("$tipo · $nombre", ruta, onCerrar = { ampliada = null })
            }
            if (comparar && imagenReferencia != null) {
                val imagen = patron
                AlertDialog(
                    onDismissRequest = { comparar = false },
                    title = { Text(nombre) },
                    text = {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            FotoComparacion("Captura", bitmap, Modifier.weight(1f))
                            FotoComparacion("Referencia", imagen, Modifier.weight(1f))
                        }
                    },
                    confirmButton = {
                        if (onComparar == null) {
                            TextButton(onClick = { comparar = false }) { Text("Cerrar") }
                        } else {
                            TextButton(enabled = imagen != null && bitmap != null, onClick = {
                                onComparar(referencia, true)
                                comparar = false
                            }) { Text("Coincide") }
                        }
                    },
                    dismissButton = {
                        if (onComparar != null) {
                            TextButton(enabled = imagen != null && bitmap != null, onClick = {
                                onComparar(referencia, false)
                                comparar = false
                            }) { Text("Difiere") }
                        }
                    }
                )
            }
        }
    }
}

private fun similitudVisual(foto: Bitmap, referencia: Bitmap): Int {
    val lado = 32
    val captura = Bitmap.createScaledBitmap(foto, lado, lado, true)
    val patron = Bitmap.createScaledBitmap(referencia, lado, lado, true)
    var diferencia = 0
    for (y in 0 until lado) {
        for (x in 0 until lado) {
            val a = captura.getPixel(x, y)
            val b = patron.getPixel(x, y)
            diferencia += kotlin.math.abs(android.graphics.Color.red(a) - android.graphics.Color.red(b))
            diferencia += kotlin.math.abs(android.graphics.Color.green(a) - android.graphics.Color.green(b))
            diferencia += kotlin.math.abs(android.graphics.Color.blue(a) - android.graphics.Color.blue(b))
        }
    }
    return 100 - (100 * diferencia / (lado * lado * 3 * 255))
}

@Composable
private fun FotoComparacion(etiqueta: String, foto: android.graphics.Bitmap?, modifier: Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(etiqueta, style = MaterialTheme.typography.labelMedium)
        if (foto == null) Text("Imagen no disponible")
        else Image(foto.asImageBitmap(), etiqueta, Modifier.fillMaxWidth().height(160.dp), contentScale = ContentScale.Fit)
    }
}
