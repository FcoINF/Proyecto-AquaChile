package com.duoc.aquaflow.viewmodel

import com.duoc.aquaflow.domain.AppError
import com.duoc.aquaflow.domain.FieldErrors

/** Errores por campo si el error es de validación; vacío en otro caso. */
internal fun AppError.camposConError(): FieldErrors = (this as? AppError.Validation)?.errores ?: emptyMap()

/** Convierte texto de un campo numérico: vacío -> null (válido), no numérico -> [NumeroInvalido]. */
internal sealed interface Lectura<out T> {
    data class Valor<T>(val valor: T?) : Lectura<T>
    data object NumeroInvalido : Lectura<Nothing>
}

internal fun String.leerEntero(): Lectura<Int> {
    val texto = trim()
    if (texto.isEmpty()) return Lectura.Valor(null)
    return texto.toIntOrNull()?.let { Lectura.Valor(it) } ?: Lectura.NumeroInvalido
}

internal fun String.leerDecimal(): Lectura<Double> {
    val texto = trim().replace(',', '.')
    if (texto.isEmpty()) return Lectura.Valor(null)
    return texto.toDoubleOrNull()?.let { Lectura.Valor(it) } ?: Lectura.NumeroInvalido
}

internal fun <T> Lectura<T>.valorOrNull(): T? = when (this) {
    is Lectura.Valor -> valor
    Lectura.NumeroInvalido -> null
}
