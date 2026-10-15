package com.duoc.aquaflow.ui.components

import android.content.ActivityNotFoundException
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.duoc.aquaflow.model.Evidencia
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import com.duoc.aquaflow.model.TipoEvidencia
import com.duoc.aquaflow.ui.theme.AquaNavyContainer
import com.duoc.aquaflow.ui.theme.AquaOnSurfaceVariant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private sealed interface CargaImagen {
    data object Cargando : CargaImagen
    data object Error : CargaImagen
    data class Lista(val bitmap: ImageBitmap) : CargaImagen
}

/** Foto de evidencia leída del almacenamiento interno (reducida y con rotación EXIF corregida). */
@Composable
fun EvidenciaImagen(rutaRelativa: String, descripcion: String, modifier: Modifier = Modifier) {
    val storage = LocalAppContainer.current.evidenciaStorage
    val carga by produceState<CargaImagen>(initialValue = CargaImagen.Cargando, rutaRelativa) {
        value = withContext(Dispatchers.IO) {
            val bitmap = runCatching { storage.cargarBitmap(rutaRelativa, 720) }.getOrNull()
            if (bitmap == null) CargaImagen.Error else CargaImagen.Lista(bitmap.asImageBitmap())
        }
    }
    Box(modifier.background(AquaNavyContainer), contentAlignment = Alignment.Center) {
        when (val actual = carga) {
            CargaImagen.Cargando -> CircularProgressIndicator()
            CargaImagen.Error -> Icon(
                Icons.Filled.Warning,
                contentDescription = "Imagen no disponible",
                tint = AquaOnSurfaceVariant
            )
            is CargaImagen.Lista -> Image(
                bitmap = actual.bitmap,
                contentDescription = descripcion,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

/** Acciones para registrar una evidencia: cámara del sistema o galería. */
class EvidenciaLauncher(
    val capturar: (tipo: TipoEvidencia, itemId: String?) -> Unit,
    val desdeGaleria: (tipo: TipoEvidencia, itemId: String?) -> Unit
)

/**
 * Prepara los lanzadores de cámara y galería. La foto se escribe en el almacenamiento interno y
 * [onEvidencia] recibe su ruta relativa para asociarla a la faena. Los errores se informan por [onError].
 */
@Composable
fun rememberEvidenciaLauncher(
    faenaId: Long,
    onEvidencia: (tipo: TipoEvidencia, itemId: String?, rutaRelativa: String) -> Unit,
    onError: (String) -> Unit
): EvidenciaLauncher {
    val storage = LocalAppContainer.current.evidenciaStorage
    val scope = rememberCoroutineScope()
    val alEvidencia by rememberUpdatedState(onEvidencia)
    val alError by rememberUpdatedState(onError)

    // Se guardan con rememberSaveable para sobrevivir a la muerte del proceso mientras la cámara está abierta.
    var rutaPendiente by rememberSaveable { mutableStateOf<String?>(null) }
    var tipoPendiente by rememberSaveable { mutableStateOf<String?>(null) }
    var itemPendiente by rememberSaveable { mutableStateOf<String?>(null) }

    val camara = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { exito ->
        val ruta = rutaPendiente
        val tipo = tipoPendiente?.let { runCatching { TipoEvidencia.valueOf(it) }.getOrNull() }
        if (ruta != null) {
            if (exito && tipo != null && storage.existe(ruta)) {
                alEvidencia(tipo, itemPendiente, ruta)
            } else {
                storage.eliminar(ruta)
                if (exito) alError("No se pudo guardar la foto.")
            }
        }
        rutaPendiente = null
        tipoPendiente = null
        itemPendiente = null
    }

    val galeria = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        val tipo = tipoPendiente?.let { runCatching { TipoEvidencia.valueOf(it) }.getOrNull() }
        val item = itemPendiente
        tipoPendiente = null
        itemPendiente = null
        if (uri != null && tipo != null) {
            scope.launch {
                val ruta = withContext(Dispatchers.IO) { storage.importar(uri, faenaId) }
                if (ruta != null) alEvidencia(tipo, item, ruta) else alError("No se pudo importar la imagen.")
            }
        }
    }

    return remember(storage, faenaId, camara, galeria) {
        EvidenciaLauncher(
            capturar = { tipo, itemId ->
                val archivo = storage.nuevoArchivo(faenaId)
                rutaPendiente = storage.rutaRelativa(archivo)
                tipoPendiente = tipo.name
                itemPendiente = itemId
                try {
                    camara.launch(storage.uriParaCamara(archivo))
                } catch (e: ActivityNotFoundException) {
                    storage.eliminar(storage.rutaRelativa(archivo))
                    rutaPendiente = null
                    alError("Este dispositivo no tiene una aplicación de cámara disponible.")
                }
            },
            desdeGaleria = { tipo, itemId ->
                tipoPendiente = tipo.name
                itemPendiente = itemId
                try {
                    galeria.launch("image/*")
                } catch (e: ActivityNotFoundException) {
                    tipoPendiente = null
                    alError("No hay una aplicación de galería disponible.")
                }
            }
        )
    }
}

/** Miniaturas de las evidencias de un registro. Pulsar amplía; si [editable], permite eliminar. */
@Composable
fun FotosFila(
    evidencias: List<Evidencia>,
    editable: Boolean,
    onEliminar: (Evidencia) -> Unit,
    modifier: Modifier = Modifier
) {
    var ampliada by remember { mutableStateOf<Evidencia?>(null) }
    var porEliminar by remember { mutableStateOf<Evidencia?>(null) }

    if (evidencias.isNotEmpty()) {
        Row(
            modifier = modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            evidencias.forEach { evidencia ->
                Box(
                    Modifier
                        .size(92.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { ampliada = evidencia }
                ) {
                    EvidenciaImagen(evidencia.ruta, "Evidencia: ${evidencia.tipo.etiqueta}", Modifier.fillMaxSize())
                    if (editable) {
                        IconButton(
                            onClick = { porEliminar = evidencia },
                            modifier = Modifier.align(Alignment.TopEnd).size(36.dp)
                        ) {
                            Icon(
                                Icons.Filled.Delete,
                                contentDescription = "Eliminar evidencia",
                                tint = Color.White,
                                modifier = Modifier.background(Color(0x99000000), CircleShape).padding(5.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    ampliada?.let { evidencia ->
        Dialog(onDismissRequest = { ampliada = null }) {
            EvidenciaImagen(
                rutaRelativa = evidencia.ruta,
                descripcion = "Evidencia ampliada: ${evidencia.tipo.etiqueta}",
                modifier = Modifier.fillMaxWidth().aspectRatio(0.75f).clip(RoundedCornerShape(16.dp))
            )
        }
    }
    porEliminar?.let { evidencia ->
        ConfirmDialog(
            titulo = "Eliminar evidencia",
            texto = "Se eliminará la foto de forma permanente.",
            textoConfirmar = "Eliminar",
            onConfirmar = {
                onEliminar(evidencia)
                porEliminar = null
            },
            onCerrar = { porEliminar = null }
        )
    }
}

/** Botones para agregar una foto con la cámara o desde la galería. */
@Composable
fun AccionesEvidencia(
    onCamara: () -> Unit,
    onGaleria: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = onCamara, modifier = Modifier.weight(1f)) {
            Icon(Icons.Filled.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
            Text("  Tomar foto")
        }
        OutlinedButton(onClick = onGaleria, modifier = Modifier.weight(1f)) {
            Icon(Icons.Filled.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
            Text("  Galería")
        }
    }
}
