package com.duoc.aquaflow

import com.duoc.aquaflow.data.repository.PreChequeoRepository
import com.duoc.aquaflow.model.*
import com.duoc.aquaflow.viewmodel.FaenaViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class GuardadoSeguroTest {
    private val dispatcher = StandardTestDispatcher()
    private val supervisor = Usuario("supervisor", "1234", Rol.SUPERVISOR)
    private val salud = EncuestaSalud(120, 80, 98, emptyList(), NivelSalud.NORMAL)
    @Before fun preparar() { Dispatchers.setMain(dispatcher) }
    @After fun terminar() { Dispatchers.resetMain() }

    @Test fun navegaSoloTrasGuardarYReintentaSinDuplicar() = runTest {
        val registro = RegistroMemoria().apply { fallar = true }
        val vm = FaenaViewModel(BorradoresMemoria(), registro).apply { iniciar(supervisor) }
        vm.actualizar(vm.ui.value.apertura.copy(centroCultivo = "Centro", buzoAsignado = "Ana",
            condicionesListas = true, embarcacionLista = true, comunicacionesOperativas = true))
        assertTrue(vm.confirmarApertura())
        var navegaciones = 0
        assertTrue(vm.registrarEncuesta(salud) { navegaciones++ })
        assertEquals(0, navegaciones)
        assertTrue(vm.ui.value.ocupado)
        assertFalse(vm.registrarEncuesta(salud) { navegaciones++ })
        advanceUntilIdle()
        assertEquals(0, navegaciones)
        assertEquals(salud, vm.ui.value.encuesta)
        assertNotNull(vm.ui.value.error)
        registro.fallar = false
        vm.registrarEncuesta(salud) { navegaciones++ }
        advanceUntilIdle()
        assertEquals(1, navegaciones)
        assertEquals(1, registro.llamadas.size)
        assertFalse(vm.ui.value.ocupado)
    }

    @Test fun recuperaSeccionesYProtegeExpedienteDecidido() = runTest {
        val repo = PreChequeoRepository(DaoMemoria())
        val apertura = PreChequeo("f1", "Centro", "24/10/2026", "09:00", "Ana", "supervisor", true, true, true)
        val checklist = ChecklistFaena("f1", listOf(ItemEquipamiento("1", "Equipo", "", EstadoEquipo.CUMPLE)))
        repo.guardar(apertura)
        repo.guardarChecklist("f1", checklist)
        repo.guardarEncuesta("f1", salud)
        val vm = FaenaViewModel(repo, repo).apply { iniciar(supervisor) }
        vm.recuperarApertura()
        advanceUntilIdle()
        assertEquals(checklist, vm.ui.value.checklist)
        assertEquals(salud, vm.ui.value.encuesta)
        val decision = RegistroDecision(Decision.RECHAZADA, "Revisar", "jefe")
        repo.guardarDecision("f1", decision)
        assertTrue(runCatching { repo.guardar(apertura.copy(buzoAsignado = "Otro")) }.isFailure)
        assertTrue(runCatching { repo.guardarEncuesta("f1", salud.copy(sistolica = 130)) }.isFailure)
        assertEquals(salud, repo.observarFaena("f1", supervisor).first()!!.resumen.encuesta)
        vm.recuperarApertura()
        advanceUntilIdle()
        vm.actualizar(apertura.copy(buzoAsignado = "Otro"))
        assertEquals("Ana", vm.ui.value.apertura.buzoAsignado)
        assertEquals(decision, vm.ui.value.decision)
    }

    @Test fun supervisorNoPuedeDecidirYJefaturaNoDecideUnaFaenaIncompleta() = runTest {
        val repo = PreChequeoRepository(DaoMemoria())
        repo.guardar(PreChequeo("f1", "Centro", "24/10/2026", "09:00", "Ana", "supervisor"))
        val decision = RegistroDecision(Decision.AUTORIZADA, "", "nombre falsificado")
        assertTrue(runCatching { repo.decidir("f1", decision, supervisor) }.isFailure)
        assertTrue(runCatching { repo.decidir("f1", decision, Usuario("jefe", "1234", Rol.JEFE)) }.isFailure)
        assertNull(repo.observarFaena("f1", supervisor).first()!!.decision)
    }
}
