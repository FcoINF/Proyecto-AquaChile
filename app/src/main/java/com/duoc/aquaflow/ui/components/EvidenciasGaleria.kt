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
import com.duoc.aquaflow.ui.theme.AquaAzulAccion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun EvidenciasGaleria(
    fotos: List<String>,
    nombresPorFoto: Map<String, String> = emptyMap(),
    categoriasPorFoto: Map<String, String> = emptyMap(),
    onQuitar: ((String) -> Unit)? = null
) {
    val context = LocalContext.current
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        fotos.forEachIndexed { indice, referencia ->
            val bitmap by produceState<android.graphics.Bitmap?>(null, referencia) {
                value = withContext(Dispatchers.IO) {
                    runCatching {
                        val uri = Uri.parse(referencia)
                        if (!EvidenciasFotos.esPropia(context, uri)) null else
                            context.contentResolver.openInputStream(uri)?.use {
                                BitmapFactory.decodeStream(
                                    it,
                                    null,
                                    BitmapFactory.Options().apply { inSampleSize = 4 }
                                )
                            }
                    }.getOrNull()
                }
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
        }
    }
}
