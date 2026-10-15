package com.duoc.aquaflow.repository

import com.duoc.aquaflow.domain.AppResult
import com.duoc.aquaflow.model.Rol
import com.duoc.aquaflow.model.SessionState
import com.duoc.aquaflow.model.Usuario
import kotlinx.coroutines.flow.StateFlow

/** Persistencia mínima de la sesión (solo el id del usuario). */
interface SessionStore {
    fun leerUsuarioId(): Long?
    fun guardar(usuarioId: Long)
    fun limpiar()
}

interface AuthRepository {
    val session: StateFlow<SessionState>

    /** Recupera la sesión guardada (si el usuario sigue activo) o la deja en [SessionState.LoggedOut]. */
    suspend fun restoreSession()

    suspend fun login(rut: String, pin: String, rol: Rol): AppResult<Usuario>

    suspend fun logout()

    /** Verifica el PIN del usuario (firmas y autorizaciones). Cuenta intentos fallidos. */
    suspend fun verifyPin(usuarioId: Long, pin: String): Boolean
}
