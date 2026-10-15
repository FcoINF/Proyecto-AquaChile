package com.duoc.aquaflow.domain.rules

import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/** Hash de PIN con sal aleatoria (PBKDF2-HMAC-SHA256). El PIN nunca se guarda en claro. */
object PinHasher {
    private const val ITERACIONES = 10_000
    private const val BITS = 256
    private const val BYTES_SAL = 16

    fun nuevaSal(): String {
        val bytes = ByteArray(BYTES_SAL)
        SecureRandom().nextBytes(bytes)
        return bytes.toHex()
    }

    fun hash(pin: String, salHex: String): String {
        val spec = PBEKeySpec(pin.toCharArray(), salHex.hexToBytes(), ITERACIONES, BITS)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        return factory.generateSecret(spec).encoded.toHex()
    }

    fun verificar(pin: String, salHex: String, hashEsperado: String): Boolean =
        MessageDigest.isEqual(hash(pin, salHex).toByteArray(), hashEsperado.toByteArray())

    private fun ByteArray.toHex(): String =
        joinToString("") { "%02x".format(it.toInt() and 0xff) }

    private fun String.hexToBytes(): ByteArray =
        ByteArray(length / 2) { i -> substring(i * 2, i * 2 + 2).toInt(16).toByte() }
}
