package com.duoc.aquaflow.data.local

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.util.UUID

object EvidenciasFotos {
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
