package com.duoc.aquaflow.repository

import com.duoc.aquaflow.FakeSessionStore
import com.duoc.aquaflow.FakeUsuarioDao
import com.duoc.aquaflow.TestData
import com.duoc.aquaflow.domain.AppError
import com.duoc.aquaflow.domain.AppResult
import com.duoc.aquaflow.domain.config.ReglasConfig
import com.duoc.aquaflow.model.Rol
import com.duoc.aquaflow.model.SessionState
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RoomAuthRepositoryTest {
    private val supervisor = TestData.usuario(id = 1, rol = Rol.SUPERVISOR, rut = "15892403-K", pin = "4921")
    private val admin = TestData.usuario(id = 2, rol = Rol.ADMINISTRADOR, rut = "12433910-3", pin = "2468")
    private val inactivo = TestData.usuario(id = 3, rol = Rol.SUPERVISOR, rut = "14785236-3", pin = "1357", activo = false)

    private var reloj = 1_000_000L
    private val config = ReglasConfig(maxIntentosLogin = 3, bloqueoLoginSegundos = 30)

    private fun repo(store: FakeSessionStore = FakeSessionStore()) =
        RoomAuthRepository(FakeUsuarioDao(listOf(supervisor, admin, inactivo)), store, {}, config) { reloj }

    @Test
    fun `login correcto abre la sesion y la guarda`() = runTest {
        val store = FakeSessionStore()
        val repo = repo(store)
        val resultado = repo.login("15.892.403-K", "4921", Rol.SUPERVISOR)
        assertTrue(resultado is AppResult.Ok)
        assertEquals(SessionState.LoggedIn(supervisor), repo.session.value)
        assertEquals(1L, store.usuarioId)
    }

    @Test
    fun `PIN incorrecto no abre sesion`() = runTest {
        val repo = repo()
        val resultado = repo.login("15892403-K", "0000", Rol.SUPERVISOR)
        assertEquals(AppError.InvalidCredentials, (resultado as AppResult.Fail).error)
        assertFalse(repo.session.value is SessionState.LoggedIn)
    }

    @Test
    fun `el rol elegido debe coincidir con el del usuario`() = runTest {
        val resultado = repo().login("15892403-K", "4921", Rol.ADMINISTRADOR)
        assertEquals(AppError.InvalidCredentials, (resultado as AppResult.Fail).error)
    }

    @Test
    fun `un usuario inactivo no puede ingresar`() = runTest {
        val resultado = repo().login("14785236-3", "1357", Rol.SUPERVISOR)
        assertEquals(AppError.InvalidCredentials, (resultado as AppResult.Fail).error)
    }

    @Test
    fun `un usuario inexistente recibe el mismo error que un PIN malo`() = runTest {
        val resultado = repo().login("17654321-3", "1234", Rol.SUPERVISOR)
        assertEquals(AppError.InvalidCredentials, (resultado as AppResult.Fail).error)
    }

    @Test
    fun `RUT o PIN con formato invalido se informan como validacion`() = runTest {
        val repo = repo()
        val rutMalo = (repo.login("12345678-9", "4921", Rol.SUPERVISOR) as AppResult.Fail).error as AppError.Validation
        assertTrue(rutMalo.errores.containsKey("rut"))
        val pinMalo = (repo.login("15892403-K", "49", Rol.SUPERVISOR) as AppResult.Fail).error as AppError.Validation
        assertTrue(pinMalo.errores.containsKey("pin"))
    }

    @Test
    fun `tras demasiados intentos fallidos la cuenta se bloquea temporalmente`() = runTest {
        val repo = repo()
        repeat(3) { repo.login("15892403-K", "0000", Rol.SUPERVISOR) }

        val bloqueado = repo.login("15892403-K", "4921", Rol.SUPERVISOR)
        val error = (bloqueado as AppResult.Fail).error
        assertTrue(error is AppError.Locked)
        assertEquals(30L, (error as AppError.Locked).segundos)

        reloj += 31_000
        assertTrue(repo.login("15892403-K", "4921", Rol.SUPERVISOR) is AppResult.Ok)
    }

    @Test
    fun `restaurar la sesion recupera al usuario activo`() = runTest {
        val repo = repo(FakeSessionStore(usuarioId = 2))
        repo.restoreSession()
        assertEquals(SessionState.LoggedIn(admin), repo.session.value)
    }

    @Test
    fun `restaurar la sesion de un usuario inexistente o inactivo la cierra`() = runTest {
        val storeInexistente = FakeSessionStore(usuarioId = 999)
        val repo1 = repo(storeInexistente)
        repo1.restoreSession()
        assertEquals(SessionState.LoggedOut, repo1.session.value)
        assertNull(storeInexistente.usuarioId)

        val storeInactivo = FakeSessionStore(usuarioId = 3)
        val repo2 = repo(storeInactivo)
        repo2.restoreSession()
        assertEquals(SessionState.LoggedOut, repo2.session.value)
    }

    @Test
    fun `cerrar sesion limpia el estado y el almacenamiento`() = runTest {
        val store = FakeSessionStore()
        val repo = repo(store)
        repo.login("15892403-K", "4921", Rol.SUPERVISOR)
        repo.logout()
        assertEquals(SessionState.LoggedOut, repo.session.value)
        assertNull(store.usuarioId)
    }

    @Test
    fun `verifyPin acepta el PIN propio y bloquea tras fallos repetidos`() = runTest {
        val repo = repo()
        assertTrue(repo.verifyPin(2, "2468"))
        assertFalse(repo.verifyPin(2, "1111"))
        assertFalse(repo.verifyPin(2, "1111"))
        assertFalse(repo.verifyPin(2, "1111"))
        // Bloqueado: ni siquiera el PIN correcto funciona hasta que pase el tiempo.
        assertFalse(repo.verifyPin(2, "2468"))
        reloj += 31_000
        assertTrue(repo.verifyPin(2, "2468"))
    }

    @Test
    fun `verifyPin rechaza usuarios inactivos o inexistentes`() = runTest {
        val repo = repo()
        assertFalse(repo.verifyPin(3, "1357"))
        assertFalse(repo.verifyPin(404, "1234"))
    }
}
