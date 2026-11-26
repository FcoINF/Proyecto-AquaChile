package com.duoc.aquaflow.ui.components

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.duoc.aquaflow.data.local.EvidenciasFotos
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun VisorEvidencia(titulo: String, referencia: String, onCerrar: () -> Unit) {
    val foto = cargarFotoEvidencia(referencia, 2048)
    var escala by remember(referencia) { mutableStateOf(1f) }
    var desplazamiento by remember(referencia) { mutableStateOf(Offset.Zero) }

    Dialog(onDismissRequest = onCerrar, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = Color(0xFF142432), contentColor = Color.White) {
            Column {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(titulo, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                    IconButton(onClick = onCerrar) { Icon(Icons.Default.Close, "Cerrar imagen") }
                }
                BoxWithConstraints(Modifier.fillMaxWidth().weight(1f).clipToBounds(), contentAlignment = Alignment.Center) {
                    val densidad = LocalDensity.current
                    val ancho = with(densidad) { maxWidth.toPx() }
                    val alto = with(densidad) { maxHeight.toPx() }
                    val transformacion = rememberTransformableState { zoom, movimiento, _ ->
                        escala = (escala * zoom).coerceIn(1f, 5f)
                        val x = ancho * (escala - 1f) / 2f
                        val y = alto * (escala - 1f) / 2f
                        desplazamiento = Offset(
                            (desplazamiento.x + movimiento.x).coerceIn(-x, x),
                            (desplazamiento.y + movimiento.y).coerceIn(-y, y)
                        )
                    }
                    when {
                        foto.cargando -> Text("Cargando imagen…")
                        foto.bitmap == null -> Text("Imagen no disponible")
                        else -> Box(Modifier.fillMaxSize().transformable(transformacion)) {
                            Image(
                                foto.bitmap.asImageBitmap(), titulo,
                                Modifier.fillMaxSize().graphicsLayer {
                                    scaleX = escala
                                    scaleY = escala
                                    translationX = desplazamiento.x
                                    translationY = desplazamiento.y
                                },
                                contentScale = ContentScale.Fit
                            )
                        }
                    }
                }
                Row(Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.Center) {
                    TextButton(onClick = {
                        escala = (escala - 0.5f).coerceAtLeast(1f)
                        desplazamiento = Offset.Zero
                    }, enabled = foto.bitmap != null && escala > 1f) { Text("Alejar", color = if (escala > 1f) Color.White else Color.Gray) }
                    TextButton(onClick = {
                        escala = (escala + 0.5f).coerceAtMost(5f)
                    }, enabled = foto.bitmap != null && escala < 5f) { Text("Acercar", color = if (escala < 5f) Color.White else Color.Gray) }
                }
            }
        }
    }
}

internal data class FotoEvidenciaState(val bitmap: Bitmap? = null, val cargando: Boolean = true)

@Composable
internal fun cargarFotoEvidencia(referencia: String?, maximo: Int = 384): FotoEvidenciaState {
    val context = LocalContext.current
    val estado by produceState(FotoEvidenciaState(), referencia, maximo) {
        value = FotoEvidenciaState()
        value = withContext(Dispatchers.IO) {
            val bitmap = referencia?.let { ruta ->
                runCatching { EvidenciasFotos.cargarBitmap(context, Uri.parse(ruta), maximo) }.getOrNull()
            }
            FotoEvidenciaState(bitmap, cargando = false)
        }
    }
    return estado
}
