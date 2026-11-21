package com.duoc.aquaflow.ui.components

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.duoc.aquaflow.data.local.EvidenciasFotos
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
            val bitmap = cargarFoto(referencia)
            val metadata = metadatos[referencia]
            var comparar by remember(referencia) { mutableStateOf(false) }
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
                            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp)),
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
                        metadata?.let { datos ->
                            val fecha = DateFormat.getDateTimeInstance().format(Date(datos.capturadaEn))
                            Text("$fecha · ${datos.capturadaPor}", style = MaterialTheme.typography.bodySmall)
                            Text(when (datos.coincide) {
                                true -> "Coincide con la referencia"
                                false -> "Difiere de la referencia"
                                null -> "Comparación pendiente"
                            }, style = MaterialTheme.typography.bodySmall)
                            if (onElegirReferencia != null) {
                                TextButton(onClick = { onElegirReferencia(referencia) }) {
                                    Text(if (datos.referencia == null) "Elegir referencia" else "Cambiar referencia")
                                }
                            }
                            if (datos.referencia != null) {
                                TextButton(onClick = { comparar = true }) { Text("Comparar fotos") }
                            }
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
            val imagenReferencia = metadata?.referencia
            if (comparar && imagenReferencia != null) {
                val imagen = cargarFoto(imagenReferencia)
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

@Composable
private fun cargarFoto(referencia: String): android.graphics.Bitmap? {
    val context = LocalContext.current
    val bitmap by produceState<android.graphics.Bitmap?>(null, referencia) {
        value = null
        value = withContext(Dispatchers.IO) {
            runCatching {
                val uri = Uri.parse(referencia)
                if (!EvidenciasFotos.esPropia(context, uri)) return@runCatching null
                val resolver = context.contentResolver
                val opciones = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opciones) }
                var muestra = 1
                while (maxOf(opciones.outWidth, opciones.outHeight) / muestra > 1024) muestra *= 2
                opciones.inJustDecodeBounds = false
                opciones.inSampleSize = muestra
                resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opciones) }
            }.getOrNull()
        }
    }
    return bitmap
}

@Composable
private fun FotoComparacion(etiqueta: String, foto: android.graphics.Bitmap?, modifier: Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(etiqueta, style = MaterialTheme.typography.labelMedium)
        if (foto == null) Text("Imagen no disponible")
        else Image(foto.asImageBitmap(), etiqueta, Modifier.fillMaxWidth().height(160.dp), contentScale = ContentScale.Fit)
    }
}
