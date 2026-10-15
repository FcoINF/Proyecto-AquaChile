package com.duoc.aquaflow.viewmodel

import com.duoc.aquaflow.FakeAuthRepository
import com.duoc.aquaflow.FakeFaenaRepository
import com.duoc.aquaflow.MainDispatcherRule
import com.duoc.aquaflow.TestData
import com.duoc.aquaflow.domain.AppError
import com.duoc.aquaflow.domain.AppResult
import com.duoc.aquaflow.domain.config.ReglasConfig
import com.duoc.aquaflow.model.EstadoEquipo
import com.duoc.aquaflow.model.EstadoFaena
import com.duoc.aquaflow.model.Rol
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class DecisionViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    private val admin = TestData.usuario(id = 3, rol = Rol.ADMINISTRADOR)
    private val supervisor = TestData.usuario(id = 1, rol = Rol.SUPERVISOR)
    private val auth = FakeAuthRepository()

    private fun pendiente() = TestData.detalleCompleto(TestData.faena(estado = EstadoFaena.PENDIENTE_AUTORIZACION))

    private fun vmAdmin(repo: FakeFaenaRepository) = DecisionViewModel(admin, 10, repo, auth, ReglasConfig())

    @Test
    fun `el administrador puede autorizar una faena completa y favorable`() {
        val vm = vmAdmin(FakeFaenaRepository(pendiente()))
        assertTrue(vm.state.value.puedeDecidir)
        assertTrue(vm.state.value.puedeAutorizar)
    }

    @Test
    fun `un equipo que no cumple impide autorizar pero permite rechazar`() {
        val base = pendiente()
        val conFalla = base.copy(
            checklist = base.checklist.map { it.copy(estado = EstadoEquipo.NO_CUMPLE, nota = "Falla") }
        )
        val vm = vmAdmin(FakeFaenaRepository(conFalla))
        assertTrue(vm.state.value.puedeDecidir)
        assertFalse(vm.state.value.puedeAutorizar)
    }

    @Test
    fun `un PIN incorrecto no autoriza`() {
        val repo = FakeFaenaRepository(pendiente())
        auth.pinCorrecto = false
        val vm = vmAdmin(repo)
        var terminado = false

        vm.autorizar("0000") { terminado = true }

        assertEquals(0, repo.llamadasAutorizar)
        assertFalse(terminado)
        assertNotNull(vm.state.value.errorDialogo)
        assertFalse(vm.state.value.operando)
    }

    @Test
    fun `un PIN correcto autoriza y avisa al terminar`() {
        val repo = FakeFaenaRepository(pendiente())
        auth.pinCorrecto = true
        val vm = vmAdmin(repo)
        var terminado = false

        vm.autorizar("2468") { terminado = true }

        assertEquals(1, repo.llamadasAutorizar)
        assertTrue(terminado)
        assertEquals("Faena autorizada.", vm.state.value.mensaje)
        assertNull(vm.state.value.errorDialogo)
    }

    @Test
    fun `si el repositorio bloquea la autorizacion el motivo se muestra`() {
        val repo = FakeFaenaRepository(pendiente())
        repo.resultadoAutorizar = AppResult.Fail(AppError.Blocked(listOf("No se puede autorizar", "Equipo X no cumple")))
        auth.pinCorrecto = true
        val vm = vmAdmin(repo)
        var terminado = false

        vm.autorizar("2468") { terminado = true }

        assertFalse(terminado)
        assertTrue(vm.state.value.errorDialogo!!.contains("Equipo X no cumple"))
    }

    @Test
    fun `rechazar envia el motivo y un motivo invalido se informa en el dialogo`() {
        val repo = FakeFaenaRepository(pendiente())
        repo.resultadoRechazar = AppResult.Fail(AppError.Validation(mapOf("motivo" to "Describe el motivo.")))
        val vm = vmAdmin(repo)
        var terminado = false

        vm.rechazar("corto") { terminado = true }

        assertEquals("corto", repo.ultimoMotivo)
        assertFalse(terminado)
        assertEquals("Describe el motivo.", vm.state.value.errorDialogo)

        repo.resultadoRechazar = AppResult.Ok(Unit)
        vm.rechazar("Presión de rescate insuficiente") { terminado = true }
        assertTrue(terminado)
    }

    @Test
    fun `el supervisor responsable puede firmar solo con la informacion completa`() {
        val completa = TestData.detalleCompleto(TestData.faena(supervisorId = 1, estado = EstadoFaena.PRECHEQUEO))
        val vmCompleta = DecisionViewModel(supervisor, 10, FakeFaenaRepository(completa), auth, ReglasConfig())
        assertTrue(vmCompleta.state.value.puedeEnviar)
        assertFalse(vmCompleta.state.value.puedeDecidir)

        val vacia = TestData.detalleVacio(TestData.faena(supervisorId = 1, estado = EstadoFaena.PRECHEQUEO))
        val vmVacia = DecisionViewModel(supervisor, 10, FakeFaenaRepository(vacia), auth, ReglasConfig())
        assertFalse(vmVacia.state.value.puedeEnviar)
    }

    @Test
    fun `el supervisor no puede decidir ni siquiera con la faena pendiente`() {
        val vm = DecisionViewModel(supervisor, 10, FakeFaenaRepository(pendiente()), auth, ReglasConfig())
        assertFalse(vm.state.value.puedeDecidir)
        assertFalse(vm.state.value.puedeAutorizar)
    }

    @Test
    fun `firmar con PIN correcto envia a revision`() {
        val completa = TestData.detalleCompleto(TestData.faena(supervisorId = 1))
        val repo = FakeFaenaRepository(completa)
        auth.pinCorrecto = true
        val vm = DecisionViewModel(supervisor, 10, repo, auth, ReglasConfig())
        var terminado = false

        vm.firmarYEnviar("4921") { terminado = true }

        assertEquals(1, repo.llamadasEnviar)
        assertTrue(terminado)
    }

    @Test
    fun `una faena inexistente o ajena queda como no disponible`() {
        val vm = vmAdmin(FakeFaenaRepository(null))
        assertFalse(vm.state.value.disponible)
        assertFalse(vm.state.value.puedeDecidir)
    }
}
