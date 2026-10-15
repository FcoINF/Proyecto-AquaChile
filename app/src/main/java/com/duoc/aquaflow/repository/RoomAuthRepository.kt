package com.duoc.aquaflow.repository

import com.duoc.aquaflow.data.local.dao.UsuarioDao
import com.duoc.aquaflow.domain.AppError
import com.duoc.aquaflow.domain.AppResult
import com.duoc.aquaflow.domain.config.ReglasConfig
import com.duoc.aquaflow.domain.fail
import com.duoc.aquaflow.domain.rules.PinHasher
import com.duoc.aquaflow.domain.rules.Rut
import com.duoc.aquaflow.domain.rules.Validadores
import com.duoc.aquaflow.domain.validationFail
import com.duoc.aquaflow.model.Rol
import com.duoc.aquaflow.model.SessionState
import com.duoc.aquaflow.model.Usuario
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class RoomAuthRepository(
    private val usuarioDao: UsuarioDao,
    private val sessionStore: SessionStore,
    private val esperarSemillas: suspend () -> Unit,
    private val config: ReglasConfig = ReglasConfig(),
    private val ahora: () -> Long = { System.currentTimeMillis() }
) : AuthRepository {

    private val _session = MutableStateFlow<SessionState>(SessionState.Loading)
    override val session: StateFlow<SessionState> = _session.asStateFlow()

    private class Intentos(var fallos: Int = 0, var bloqueadoHasta: Long = 0L)

    private val intentos = mutableMapOf<String, Intentos>()
    private val lock = Any()

    override suspend fun restoreSession() {
        if (_session.value is SessionState.LoggedIn) return
        esperarSemillas()
        val usuario = sessionStore.leerUsuarioId()?.let { usuarioDao.findById(it) }
        if (usuario != null && usuario.activo) {
            _session.value = SessionState.LoggedIn(usuario)
        } else {
            sessionStore.limpiar()
            _session.value = SessionState.LoggedOut
        }
    }

    override suspend fun login(rut: String, pin: String, rol: Rol): AppResult<Usuario> {
        esperarSemillas()
        val rutNormalizado = Rut.normalizar(rut)
        if (rutNormalizado == null || !Rut.esValido(rutNormalizado)) {
            return validationFail("rut" to "RUT inválido.")
        }
        if (!Validadores.pinValido(pin)) return validationFail("pin" to "El PIN debe tener 4 dígitos.")

        val clave = "rut:$rutNormalizado"
        val bloqueo = segundosDeBloqueo(clave)
        if (bloqueo > 0) return fail(AppError.Locked(bloqueo))

        val usuario = usuarioDao.findByRut(rutNormalizado)
        val valido = usuario != null && usuario.activo && usuario.rol == rol &&
            PinHasher.verificar(pin, usuario.pinSalt, usuario.pinHash)
        if (usuario == null || !valido) {
            registrarFallo(clave)
            return fail(AppError.InvalidCredentials)
        }
        limpiarIntentos(clave)
        sessionStore.guardar(usuario.id)
        _session.value = SessionState.LoggedIn(usuario)
        return AppResult.Ok(usuario)
    }

    override suspend fun logout() {
        sessionStore.limpiar()
        _session.value = SessionState.LoggedOut
    }

    override suspend fun verifyPin(usuarioId: Long, pin: String): Boolean {
        if (!Validadores.pinValido(pin)) return false
        val clave = "id:$usuarioId"
        if (segundosDeBloqueo(clave) > 0) return false
        val usuario = usuarioDao.findById(usuarioId)
        val valido = usuario != null && usuario.activo && PinHasher.verificar(pin, usuario.pinSalt, usuario.pinHash)
        if (valido) limpiarIntentos(clave) else registrarFallo(clave)
        return valido
    }

    private fun segundosDeBloqueo(clave: String): Long = synchronized(lock) {
        val intento = intentos[clave]
        val restanteMs = (intento?.bloqueadoHasta ?: 0L) - ahora()
        if (restanteMs > 0) (restanteMs + 999) / 1000 else 0L
    }

    private fun registrarFallo(clave: String) = synchronized(lock) {
        val intento = intentos.getOrPut(clave) { Intentos() }
        intento.fallos += 1
        if (intento.fallos >= config.maxIntentosLogin) {
            intento.fallos = 0
            intento.bloqueadoHasta = ahora() + config.bloqueoLoginSegundos * 1000
        }
    }

    private fun limpiarIntentos(clave: String) = synchronized(lock) { intentos.remove(clave) }
}
