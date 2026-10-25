package com.duoc.aquaflow.ui.components

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.duoc.aquaflow.data.local.EvidenciasFotos
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun EvidenciasGaleria(fotos: List<String>, onQuitar: ((String) -> Unit)? = null) {
    val context = LocalContext.current
    fotos.forEachIndexed { indice, referencia ->
        val bitmap by produceState<android.graphics.Bitmap?>(null, referencia) {
            value = withContext(Dispatchers.IO) {
                runCatching {
                    val uri = Uri.parse(referencia)
                    if (!EvidenciasFotos.esPropia(context, uri)) null else
                        context.contentResolver.openInputStream(uri)?.use {
                            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = 8 })
                        }
                }.getOrNull()
            }
        }
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp)) {
                Text("Evidencia ${indice + 1}")
                bitmap?.let { Image(it.asImageBitmap(), "Foto de evidencia ${indice + 1}", Modifier.fillMaxWidth().height(180.dp)) }
                    ?: Text("Vista previa no disponible. Si el archivo falta, vuelve a capturar la evidencia.")
                if (onQuitar != null) TextButton(onClick = { onQuitar(referencia) }) { Text("Quitar del checklist") }
            }
        }
    }
}
