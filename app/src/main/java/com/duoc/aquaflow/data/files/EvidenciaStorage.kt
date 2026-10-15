package com.duoc.aquaflow.data.files

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import androidx.core.content.FileProvider
import com.duoc.aquaflow.repository.EvidenciaArchivos
import java.io.File
import java.util.UUID

/**
 * Almacena las fotos de evidencia en el almacenamiento interno de la app
 * (filesDir/evidencias/<faenaId>/<uuid>.jpg). En la base de datos solo se guarda la ruta relativa.
 */
class EvidenciaStorage(private val context: Context) : EvidenciaArchivos {

    private val raiz: File
        get() = File(context.filesDir, "evidencias").also { it.mkdirs() }

    /** Archivo nuevo y vacío para que la cámara escriba la foto. */
    fun nuevoArchivo(faenaId: Long): File {
        val dir = File(raiz, faenaId.toString()).also { it.mkdirs() }
        return File(dir, "${UUID.randomUUID()}.jpg")
    }

    fun rutaRelativa(archivo: File): String = archivo.relativeTo(raiz).path

    /** Resuelve la ruta relativa evitando salir del directorio de evidencias. */
    fun resolver(rutaRelativa: String): File? {
        val archivo = File(raiz, rutaRelativa)
        val dentro = archivo.canonicalPath.startsWith(raiz.canonicalPath + File.separator)
        return if (dentro) archivo else null
    }

    fun uriParaCamara(archivo: File): Uri =
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", archivo)

    /** Copia una imagen elegida de la galería al almacenamiento interno. Devuelve la ruta relativa o null si falla. */
    fun importar(origen: Uri, faenaId: Long): String? {
        val destino = nuevoArchivo(faenaId)
        return try {
            val entrada = context.contentResolver.openInputStream(origen) ?: return null
            entrada.use { input -> destino.outputStream().use { output -> input.copyTo(output) } }
            if (destino.length() > 0) rutaRelativa(destino) else null
        } catch (e: Exception) {
            destino.delete()
            null
        }
    }

    override fun eliminar(rutaRelativa: String) {
        resolver(rutaRelativa)?.delete()
    }

    fun existe(rutaRelativa: String): Boolean = resolver(rutaRelativa)?.let { it.exists() && it.length() > 0 } ?: false

    /** Carga la imagen reducida (lado mayor ≤ [maxLado]) y corrige la rotación EXIF. */
    fun cargarBitmap(rutaRelativa: String, maxLado: Int): Bitmap? {
        val archivo = resolver(rutaRelativa) ?: return null
        if (!archivo.exists()) return null
        val limites = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(archivo.path, limites)
        if (limites.outWidth <= 0 || limites.outHeight <= 0) return null

        var muestreo = 1
        while (limites.outWidth / (muestreo * 2) >= maxLado || limites.outHeight / (muestreo * 2) >= maxLado) {
            muestreo *= 2
        }
        val opciones = BitmapFactory.Options().apply { inSampleSize = muestreo }
        val bitmap = BitmapFactory.decodeFile(archivo.path, opciones) ?: return null

        val grados = try {
            when (ExifInterface(archivo.path).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
        } catch (e: Exception) {
            0f
        }
        if (grados == 0f) return bitmap
        val matriz = Matrix().apply { postRotate(grados) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matriz, true)
    }
}
