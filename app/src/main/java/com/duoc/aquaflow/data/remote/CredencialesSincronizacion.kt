package com.duoc.aquaflow.data.remote

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.AtomicFile
import com.google.gson.Gson
import java.io.File
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

internal object CredencialesSincronizacion {
    data class Sesion(val cuenta: String, val servidor: String, val autorizacion: String)

    private const val ALIAS = "aquaflow.sincronizacion"
    private fun archivo(contexto: Context) = AtomicFile(File(contexto.noBackupFilesDir, "sincronizacion.sesion"))
    private fun almacen() = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }

    @Synchronized
    fun guardar(contexto: Context, sesion: Sesion) {
        val clave = almacen().getKey(ALIAS, null) as? SecretKey
            ?: KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").run {
                init(KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build())
                generateKey()
            }
        val cifrado = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.ENCRYPT_MODE, clave) }
        val archivo = archivo(contexto)
        val salida = archivo.startWrite()
        try {
            salida.write(cifrado.iv)
            salida.write(cifrado.doFinal(Gson().toJson(sesion).toByteArray(Charsets.UTF_8)))
            archivo.finishWrite(salida)
        } catch (error: Exception) {
            archivo.failWrite(salida)
            throw error
        }
    }

    @Synchronized
    fun leer(contexto: Context): Sesion? = runCatching {
        val bytes = archivo(contexto).readFully()
        val clave = almacen().getKey(ALIAS, null) as SecretKey
        val cifrado = Cipher.getInstance("AES/GCM/NoPadding")
        cifrado.init(Cipher.DECRYPT_MODE, clave, GCMParameterSpec(128, bytes.copyOfRange(0, 12)))
        Gson().fromJson(String(cifrado.doFinal(bytes, 12, bytes.size - 12), Charsets.UTF_8), Sesion::class.java)
    }.getOrNull()

    @Synchronized
    fun borrar(contexto: Context) { archivo(contexto).delete() }
}
