package com.duoc.aquaflow.data.local

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import java.io.File
import java.util.UUID

object EvidenciasFotos {
    fun cargarBitmap(context: Context, uri: Uri, maximo: Int): Bitmap? {
        if (!esPropia(context, uri)) return null
        val resolver = context.contentResolver
        return if (Build.VERSION.SDK_INT >= 28) {
            ImageDecoder.decodeBitmap(ImageDecoder.createSource(resolver, uri)) { decoder, info, _ ->
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                val mayor = maxOf(info.size.width, info.size.height)
                if (mayor > maximo) decoder.setTargetSize(
                    (info.size.width.toLong() * maximo / mayor).toInt().coerceAtLeast(1),
                    (info.size.height.toLong() * maximo / mayor).toInt().coerceAtLeast(1)
                )
            }
        } else {
            val opciones = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opciones) }
            if (opciones.outWidth <= 0 || opciones.outHeight <= 0) return null
            var muestra = 1
            while (maxOf(opciones.outWidth, opciones.outHeight) / muestra > maximo) muestra *= 2
            opciones.inJustDecodeBounds = false
            opciones.inSampleSize = muestra
            resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opciones) }
        }
    }

    fun referencias(context: Context): Map<String, String> =
        context.getSharedPreferences("referencias_equipo", Context.MODE_PRIVATE).all.mapNotNull { (equipo, uri) ->
            (uri as? String)?.let { equipo to it }
        }.toMap()

    fun registrarReferencia(context: Context, equipo: String, origen: Uri): String {
        val copia = importarReferencia(context, origen)
        val guardada = context.getSharedPreferences("referencias_equipo", Context.MODE_PRIVATE)
            .edit().putString(equipo, copia.toString()).commit()
        if (!guardada) {
            descartar(context, copia)
            error("No se pudo guardar la referencia del equipo.")
        }
        return copia.toString()
    }

    fun leer(context: Context, uri: Uri): ByteArray {
        require(esPropia(context, uri)) { "La fotografía no pertenece a esta aplicación." }
        val maximo = 12 * 1024 * 1024
        return requireNotNull(context.contentResolver.openInputStream(uri)).use { entrada ->
            val destino = java.io.ByteArrayOutputStream()
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val cantidad = entrada.read(buffer)
                if (cantidad == -1) break
                require(destino.size() + cantidad <= maximo) { "La fotografía supera 12 MB." }
                destino.write(buffer, 0, cantidad)
            }
            require(destino.size() > 0) { "La fotografía está vacía." }
            destino.toByteArray()
        }
    }

    suspend fun descargar(context: Context, clave: String, obtener: suspend () -> ByteArray): String {
        val carpeta = File(context.filesDir, "evidencias")
        check(carpeta.isDirectory || carpeta.mkdirs())
        val digest = java.security.MessageDigest.getInstance("SHA-256").digest(clave.toByteArray())
        val archivo = File(carpeta, digest.joinToString("") { "%02x".format(it) } + ".jpg")
        if (!archivo.isFile) {
            val bytes = obtener()
            val temporal = File.createTempFile("foto-", ".tmp", carpeta)
            try {
                temporal.writeBytes(bytes)
                check(temporal.renameTo(archivo) || archivo.isFile) { "No se pudo guardar la fotografía." }
            } finally {
                temporal.delete()
            }
        }
        return FileProvider.getUriForFile(context, "${context.packageName}.evidencias", archivo).toString()
    }

    fun importarReferencia(context: Context, origen: Uri): Uri {
        val destino = crear(context)
        try {
            val resolver = context.contentResolver
            val opciones = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
            requireNotNull(resolver.openInputStream(origen)).use {
                android.graphics.BitmapFactory.decodeStream(it, null, opciones)
            }
            require(opciones.outWidth > 0 && opciones.outHeight > 0)
            requireNotNull(resolver.openInputStream(origen)).use { entrada ->
                requireNotNull(resolver.openOutputStream(destino)).use { salida ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    var total = 0
                    while (true) {
                        val cantidad = entrada.read(buffer)
                        if (cantidad == -1) break
                        total += cantidad
                        require(total <= 12 * 1024 * 1024)
                        salida.write(buffer, 0, cantidad)
                    }
                }
            }
            return destino
        } catch (e: Exception) {
            descartar(context, destino)
            throw e
        }
    }

    fun crear(context: Context): Uri {
        val carpeta = File(context.filesDir, "evidencias")
        check(carpeta.isDirectory || carpeta.mkdirs()) { "No se pudo preparar el almacenamiento." }
        val archivo = File(carpeta, "${UUID.randomUUID()}.jpg")
        check(archivo.createNewFile()) { "No se pudo crear el archivo de evidencia." }
        return FileProvider.getUriForFile(context, "${context.packageName}.evidencias", archivo)
    }

    fun esPropia(context: Context, uri: Uri) =
        uri.scheme == "content" && uri.authority == "${context.packageName}.evidencias"

    fun existe(context: Context, uri: Uri): Boolean = esPropia(context, uri) && runCatching {
        context.contentResolver.openInputStream(uri)?.use { it.read() != -1 } == true
    }.getOrDefault(false)

    fun descartar(context: Context, uri: Uri) {
        if (esPropia(context, uri)) runCatching { context.contentResolver.delete(uri, null, null) }
    }
}
