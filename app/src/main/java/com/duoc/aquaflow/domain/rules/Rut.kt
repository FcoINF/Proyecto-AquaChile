package com.duoc.aquaflow.domain.rules

/** Utilidades para RUT chileno (módulo 11). */
object Rut {

    /** Devuelve "cuerpo-DV" en mayúsculas sin puntos, o null si el formato es inválido. */
    fun normalizar(input: String): String? {
        val limpio = input.uppercase().filter { it.isDigit() || it == 'K' }
        if (limpio.length < 2) return null
        val cuerpo = limpio.dropLast(1)
        val dv = limpio.last()
        if (!cuerpo.all { it.isDigit() } || cuerpo.length !in 7..8) return null
        return "$cuerpo-$dv"
    }

    fun esValido(input: String): Boolean {
        val normalizado = normalizar(input) ?: return false
        val cuerpo = normalizado.substringBefore('-')
        return calcularDv(cuerpo) == normalizado.last()
    }

    fun calcularDv(cuerpo: String): Char {
        var suma = 0
        var factor = 2
        for (c in cuerpo.reversed()) {
            suma += (c - '0') * factor
            factor = if (factor == 7) 2 else factor + 1
        }
        return when (val r = 11 - suma % 11) {
            11 -> '0'
            10 -> 'K'
            else -> '0' + r
        }
    }

    /** "12345678-5" -> "12.345.678-5". Devuelve el texto original si no es normalizable. */
    fun formatear(input: String): String {
        val normalizado = normalizar(input) ?: return input
        val cuerpo = normalizado.substringBefore('-')
        val dv = normalizado.last()
        val conPuntos = cuerpo.reversed().chunked(3).joinToString(".").reversed()
        return "$conPuntos-$dv"
    }
}
