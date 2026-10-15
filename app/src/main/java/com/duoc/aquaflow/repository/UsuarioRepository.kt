package com.duoc.aquaflow.repository

import com.duoc.aquaflow.data.local.dao.UsuarioDao
import com.duoc.aquaflow.domain.AppError
import com.duoc.aquaflow.domain.AppResult
import com.duoc.aquaflow.domain.fail
import com.duoc.aquaflow.domain.rules.AccessPolicy
import com.duoc.aquaflow.domain.rules.PinHasher
import com.duoc.aquaflow.domain.rules.Rut
import com.duoc.aquaflow.domain.rules.Validadores
import com.duoc.aquaflow.domain.runCatchingApp
import com.duoc.aquaflow.model.Rol
import com.duoc.aquaflow.model.Usuario
import kotlinx.coroutines.flow.Flow

interface UsuarioRepository {
    fun observarUsuarios(): Flow<List<Usuario>>

    suspend fun crear(
        actor: Usuario,
        nombre: String,
        rut: String,
        rol: Rol,
        pin: String,
        pinConfirmacion: String
    ): AppResult<Long>

    suspend fun cambiarActivo(actor: Usuario, usuarioId: Long, activo: Boolean): AppResult<Unit>
}

class RoomUsuarioRepository(
    private val usuarioDao: UsuarioDao,
    private val ahora: () -> Long = { System.currentTimeMillis() }
) : UsuarioRepository {

    override fun observarUsuarios(): Flow<List<Usuario>> = usuarioDao.observeAll()

    override suspend fun crear(
        actor: Usuario,
        nombre: String,
        rut: String,
        rol: Rol,
        pin: String,
        pinConfirmacion: String
    ): AppResult<Long> = runCatchingApp {
        val admin = usuarioDao.findById(actor.id)
        if (admin == null || !AccessPolicy.puedeGestionarUsuarios(admin)) return@runCatchingApp fail(AppError.Forbidden)

        val errores = Validadores.nuevoUsuario(nombre, rut, pin, pinConfirmacion).toMutableMap()
        val rutNormalizado = Rut.normalizar(rut)
        if (rutNormalizado != null && !errores.containsKey("rut") && usuarioDao.findByRut(rutNormalizado) != null) {
            errores["rut"] = "Ya existe un usuario con ese RUT."
        }
        if (errores.isNotEmpty() || rutNormalizado == null) {
            return@runCatchingApp fail(AppError.Validation(errores))
        }

        val sal = PinHasher.nuevaSal()
        val id = usuarioDao.insert(
            Usuario(
                nombre = nombre.trim(),
                rut = rutNormalizado,
                rol = rol,
                pinHash = PinHasher.hash(pin, sal),
                pinSalt = sal,
                creadoAt = ahora()
            )
        )
        AppResult.Ok(id)
    }

    override suspend fun cambiarActivo(actor: Usuario, usuarioId: Long, activo: Boolean): AppResult<Unit> =
        runCatchingApp {
            val admin = usuarioDao.findById(actor.id)
            if (admin == null || !AccessPolicy.puedeGestionarUsuarios(admin)) return@runCatchingApp fail(AppError.Forbidden)
            if (usuarioId == admin.id && !activo) {
                return@runCatchingApp fail(AppError.InvalidState("No puedes desactivar tu propio usuario."))
            }
            val objetivo = usuarioDao.findById(usuarioId) ?: return@runCatchingApp fail(AppError.NotFound)
            usuarioDao.update(objetivo.copy(activo = activo))
            AppResult.Ok(Unit)
        }
}
