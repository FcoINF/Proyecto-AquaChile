package com.duoc.aquaflow

import com.duoc.aquaflow.data.repository.PreChequeoRepository
import com.duoc.aquaflow.model.ChecklistFaena
import com.duoc.aquaflow.model.Decision
import com.duoc.aquaflow.model.EncuestaSalud
import com.duoc.aquaflow.model.EstadoFaena
import com.duoc.aquaflow.model.NivelSalud
import com.duoc.aquaflow.model.PreChequeo
import com.duoc.aquaflow.model.RegistroDecision
import com.duoc.aquaflow.model.Rol
import com.duoc.aquaflow.model.Usuario
import com.duoc.aquaflow.viewmodel.DetalleAperturaViewModel
import com.duoc.aquaflow.viewmodel.FaenaViewModel
import com.duoc.aquaflow.viewmodel.HistorialViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FaenaViewModelPersistenciaTest {
    private val dispatcher = StandardTestDispatcher()
    private val supervisor = Usuario("supervisor", "1234", Rol.SUPERVISOR)
    private val jefe = Usuario("jefe", "1234", Rol.JEFE)
    private val encuesta = EncuestaSalud(120, 80, 98, emptyList(), NivelSalud.NORMAL)

    @Before fun preparar() { Dispatchers.setMain(dispatcher) }
    @After fun terminar() { Dispatchers.resetMain() }

    private fun faenaConfirmada(registro: RegistroMemoria?): FaenaViewModel {
        val vm = FaenaViewModel(BorradoresMemoria(), registro).apply { iniciar(supervisor) }
        vm.actualizar(
            PreChequeo("x", "Centro demo", "20/10/2026", "09:30", "Buzo demo", "supervisor", true, true, true)
        )
        assertTrue(vm.confirmarApertura())
        return vm
    }

    @Test fun guardaCadaSeccionConElIdDeLaFaena() = runTest {
        val registro = RegistroMemoria()
        val vm = faenaConfirmada(registro)
        val id = vm.ui.value.apertura.id
        vm.registrarChecklist(ChecklistFaena(id, emptyList()))
        advanceUntilIdle()
        vm.registrarEncuesta(encuesta)
        advanceUntilIdle()
        assertEquals(listOf("checklist:$id", "encuesta:$id"), registro.llamadas)
    }

    @Test fun sinAperturaConfirmadaNoGuardaNada() = runTest {
        val registro = RegistroMemoria()
        val vm = FaenaViewModel(BorradoresMemoria(), registro).apply { iniciar(supervisor) }
        assertFalse(vm.registrarEncuesta(encuesta))
        advanceUntilIdle()
        assertTrue(registro.llamadas.isEmpty())
    }

    @Test fun siFallaElGuardadoLosDatosSiguenEnPantallaYSeAvisa() = runTest {
        val registro = RegistroMemoria().apply { fallar = true }
        val vm = faenaConfirmada(registro)
        assertTrue(vm.registrarEncuesta(encuesta))
        advanceUntilIdle()
        assertEquals(encuesta, vm.ui.value.encuesta)
        assertNotNull(vm.ui.value.error)
    }

    @Test fun sinRegistroSigueFuncionandoSoloEnMemoria() {
        val vm = faenaConfirmada(null)
        assertTrue(vm.registrarEncuesta(encuesta))
        assertEquals(encuesta, vm.ui.value.encuesta)
    }

    @Test fun elHistorialMuestraElEstadoRealYFiltraPorEl() = runTest {
        val repo = PreChequeoRepository(DaoMemoria())
        repo.guardar(PreChequeo("f1", "Norte", "22/10/2026", "09:00", "Ana", "Luis"))
        repo.guardar(PreChequeo("f2", "Sur", "22/10/2026", "10:00", "Pedro", "Luis"))
        repo.guardarDecision("f2", RegistroDecision(Decision.RECHAZADA, "Falla", "jefe"))
        val vm = HistorialViewModel(repo, jefe)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} }
        advanceUntilIdle()
        assertEquals(
            mapOf("f1" to EstadoFaena.PRECHEQUEO, "f2" to EstadoFaena.RECHAZADA),
            vm.state.value.faenas.associate { it.id to it.estado }
        )
        vm.onEstado(EstadoFaena.RECHAZADA)
        advanceUntilIdle()
        assertEquals(listOf("f2"), vm.state.value.faenas.map { it.id })
    }

    @Test fun elDetalleGuardaLaDecisionYMuestraElFallo() = runTest {
        val repo = PreChequeoRepository(DaoMemoria())
        repo.guardar(PreChequeo("f1", "Norte", "22/10/2026", "09:00", "Ana", "Luis"))
        val registro = RegistroMemoria()
        val vm = DetalleAperturaViewModel(repo, jefe, "f1", registro)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} }
        advanceUntilIdle()
        assertNotNull(vm.state.value.faena)
        vm.decidir(RegistroDecision(Decision.AUTORIZADA, "", "jefe"))
        advanceUntilIdle()
        assertEquals(listOf("decision:f1"), registro.llamadas)
        assertNull(vm.state.value.errorDecision)
        registro.fallar = true
        vm.decidir(RegistroDecision(Decision.AUTORIZADA, "", "jefe"))
        advanceUntilIdle()
        assertNotNull(vm.state.value.errorDecision)
    }
}
