package com.duoc.aquaflow

import com.duoc.aquaflow.data.local.AperturaEntity
import com.duoc.aquaflow.data.local.PreChequeoDao
import com.duoc.aquaflow.data.repository.PreChequeoRepository
import com.duoc.aquaflow.model.*
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class PreChequeoRepositoryTest {
    private val supervisor = Usuario("supervisor", "", Rol.SUPERVISOR)
    private val jefe = Usuario("jefe", "", Rol.JEFE)
    private val apertura = PreChequeo(id = "f-1", centroCultivo = "Centro", supervisorACargo = supervisor.nombre)
    private val fila = AperturaEntity.desde(apertura, 1)
    private val dao = mockk<PreChequeoDao>()
    private val repo = PreChequeoRepository(dao)

    @Test fun persisteBorradorYRecuperaPorSupervisorOId() = runTest {
        coEvery { dao.guardarBorrador(any()) } coAnswers { Unit }
        coEvery { dao.ultimaApertura(supervisor.nombre) } returns fila
        coEvery { dao.buscar(apertura.id) } returns fila
        repo.guardar(apertura)
        assertEquals(apertura, repo.recuperar(supervisor.nombre))
        assertEquals(apertura, repo.recuperarFaena(supervisor.nombre)?.resumen?.apertura)
        assertEquals(apertura, repo.recuperarFaenaPorId(apertura.id)?.resumen?.apertura)
        coVerify { dao.guardarBorrador(match { it.id == apertura.id && it.supervisorACargo == supervisor.nombre }) }
        coEvery { dao.ultimaApertura("otro") } returns null
        coEvery { dao.buscar("otra") } returns null
        assertNull(repo.recuperar("otro"))
        assertNull(repo.recuperarFaena("otro"))
        assertNull(repo.recuperarFaenaPorId("otra"))
    }

    @Test fun distingueObservacionesPorRolYConservaNulos() = runTest {
        every { dao.observarAperturas(supervisor.nombre) } returns flowOf(listOf(fila))
        every { dao.observarAperturas(null) } returns flowOf(listOf(fila))
        every { dao.observarApertura(apertura.id, supervisor.nombre) } returns flowOf(fila)
        every { dao.observarApertura(apertura.id, null) } returns flowOf(fila)
        every { dao.observarApertura("otra", supervisor.nombre) } returns flowOf(null)
        assertEquals(listOf(apertura), repo.observar(supervisor).first())
        assertEquals(listOf(apertura), repo.observar(jefe).first())
        assertEquals(apertura, repo.observarDetalle(apertura.id, supervisor).first())
        assertEquals(apertura, repo.observarFaena(apertura.id, supervisor).first()?.resumen?.apertura)
        assertNotNull(repo.observarFaenas(jefe).first())
        assertNull(repo.observarFaena("otra", supervisor).first())
    }

    @Test fun guardaSeccionesYPropagaConflictosDePersistencia() = runTest {
        val checklist = ChecklistFaena(apertura.id, emptyList())
        val ast = FormularioAst("Inspección", false, listOf(true), emptyList(), ResultadoAst.CUMPLE)
        val tareas = tareasCompletas()
        val salud = saludSalida()
        coEvery { dao.guardarChecklist(any(), any(), any()) } returnsMany listOf(1, 0)
        coEvery { dao.guardarAst(any(), any(), any()) } returnsMany listOf(1, 0)
        coEvery { dao.guardarEncuesta(any(), any(), any()) } returnsMany listOf(1, 0)
        coEvery { dao.guardarTareas(any(), any(), any()) } returnsMany listOf(1, 0)
        repo.guardarChecklist(apertura.id, checklist)
        repo.guardarAst(apertura.id, ast)
        repo.guardarEncuesta(apertura.id, salud)
        repo.guardarTareas(apertura.id, tareas)
        assertTrue(runCatching { repo.guardarChecklist("otra", checklist) }.exceptionOrNull() is IllegalArgumentException)
        assertTrue(runCatching { repo.guardarChecklist(apertura.id, checklist) }.exceptionOrNull() is IllegalStateException)
        assertTrue(runCatching { repo.guardarAst(apertura.id, ast) }.exceptionOrNull() is IllegalStateException)
        assertTrue(runCatching { repo.guardarEncuesta(apertura.id, salud) }.exceptionOrNull() is IllegalStateException)
        assertTrue(runCatching { repo.guardarTareas(apertura.id, tareas) }.exceptionOrNull() is IllegalStateException)
    }

    @Test fun decisionUsaCuentaAutenticadaYRegistraElIntento() = runTest {
        val decision = RegistroDecision(Decision.RECHAZADA, " Pendiente ", "otra cuenta")
        coEvery { dao.guardarDecisionValidada(any(), any(), any()) } returns null
        repo.decidir(apertura.id, decision, jefe)
        coVerify { dao.guardarDecisionValidada(apertura.id, RegistroDecision(Decision.RECHAZADA, "Pendiente", jefe.nombre), false) }
        repo.auditarAutorizacion(apertura.id, " Evidencia ", jefe)
        coVerify { dao.guardarDecisionValidada(apertura.id, RegistroDecision(Decision.AUTORIZADA, "Evidencia", jefe.nombre), true) }
        assertTrue(runCatching { repo.decidir(apertura.id, decision, supervisor) }.exceptionOrNull() is IllegalArgumentException)
        assertTrue(runCatching { repo.auditarAutorizacion(apertura.id, "", supervisor) }.exceptionOrNull() is IllegalArgumentException)
        coEvery { dao.guardarDecisionValidada(any(), any(), any()) } returns "Falta evidencia."
        assertEquals("Falta evidencia.", runCatching { repo.guardarDecision(apertura.id, decision) }.exceptionOrNull()?.message)
    }

    @Test fun registraBloqueosYDelegaInicioYCierreEnLasOperacionesValidadas() = runTest {
        val intento = IntentoBloqueado(supervisor.nombre, 10, "Test vencido", "APERTURA")
        val cierre = RegistroCierre(20, "", true)
        coEvery { dao.registrarBloqueo(any(), intento) } coAnswers { Unit }
        coEvery { dao.iniciarValidada(apertura.id, supervisor) } coAnswers { Unit }
        coEvery { dao.cerrarValidada(apertura.id, cierre, supervisor) } coAnswers { Unit }
        repo.registrarBloqueo(apertura, intento)
        assertTrue(runCatching { repo.registrarBloqueo(apertura, intento.copy(responsable = jefe.nombre)) }.isFailure)
        assertTrue(runCatching { repo.guardar(apertura.copy(id = "")) }.isFailure)
        repo.iniciar(apertura.id, supervisor)
        repo.cerrar(apertura.id, cierre, supervisor)
        coVerify { dao.registrarBloqueo(match { it.id == apertura.id }, intento) }
        coVerify { dao.iniciarValidada(apertura.id, supervisor) }
        coVerify { dao.cerrarValidada(apertura.id, cierre, supervisor) }
    }
}
