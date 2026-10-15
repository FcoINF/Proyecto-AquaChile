package com.duoc.aquaflow.domain

import kotlin.coroutines.cancellation.CancellationException

/** Errores de campo de un formulario: clave del campo -> mensaje para el usuario. */
typealias FieldErrors = Map<String, String>

/** Errores de dominio esperables. Todos llevan un mensaje apto para mostrar al usuario. */
sealed interface AppError {
    val mensaje: String

    data object Forbidden : AppError {
        override val mensaje: String = "No tienes permisos para realizar esta acción."
    }

    data object NotFound : AppError {
        override val mensaje: String = "No se encontró el registro solicitado."
    }

    data object InvalidCredentials : AppError {
        override val mensaje: String = "RUT, PIN o rol incorrectos."
    }

    data class Locked(val segundos: Long) : AppError {
        override val mensaje: String = "Demasiados intentos fallidos. Intenta nuevamente en $segundos s."
    }

    /** La operación no es válida para el estado actual del registro (p. ej. transición imposible). */
    data class InvalidState(override val mensaje: String) : AppError

    data class Validation(val errores: FieldErrors) : AppError {
        override val mensaje: String = errores.values.firstOrNull() ?: "Los datos ingresados no son válidos."
    }

    /** Condiciones de seguridad no cumplidas que impiden la operación. */
    data class Blocked(val motivos: List<String>) : AppError {
        override val mensaje: String = motivos.joinToString("\n")
    }

    data class Unexpected(override val mensaje: String) : AppError
}

sealed interface AppResult<out T> {
    data class Ok<out T>(val value: T) : AppResult<T>
    data class Fail(val error: AppError) : AppResult<Nothing>
}

fun <T> AppResult<T>.getOrNull(): T? = (this as? AppResult.Ok)?.value

fun fail(error: AppError): AppResult.Fail = AppResult.Fail(error)

fun validationFail(vararg errores: Pair<String, String>): AppResult.Fail =
    AppResult.Fail(AppError.Validation(errores.toMap()))

/** Ejecuta [block] convirtiendo excepciones inesperadas en [AppError.Unexpected] (sin tragar cancelaciones). */
suspend fun <T> runCatchingApp(block: suspend () -> AppResult<T>): AppResult<T> =
    try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        AppResult.Fail(AppError.Unexpected(e.message ?: "Ocurrió un error inesperado."))
    }
