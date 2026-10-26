package com.duoc.aquaflow

import androidx.lifecycle.SavedStateHandle
import com.duoc.aquaflow.data.repository.PreChequeoRepository
import com.duoc.aquaflow.domain.ReglasDecision
import com.duoc.aquaflow.model.*
import com.duoc.aquaflow.viewmodel.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BorradoresDevelopTest {
    private val dispatcher = StandardTestDispatcher()
    private val usuario = Usuario("supervisor", "1234", Rol.SUPERVISOR)
    private val apertura = PreChequeo("f1", "Centro", "25/10/2026", "10:00", "Ana", "supervisor", true, true, true)
    private val c = ChecklistFaena("f1", listOf(ItemEquipamiento("1", "Equipo", "", EstadoEquipo.CUMPLE, categoria = "Primario", badge = "OK")))
    private val a = FormularioAst("Limpieza", false, List(PREGUNTAS_AST.size) { true }, emptyList(), ResultadoAst.CUMPLE)
    private val e = EncuestaSalud(120, 80, 98, emptyList(), NivelSalud.NORMAL)
    @Before fun preparar() { Dispatchers.setMain(dispatcher) }
    @After fun terminar() { Dispatchers.resetMain() }

    @Test fun botonesBorradorPersistenSuEstadoYReabrirPorIdLoConserva() = runTest {
        val repo = PreChequeoRepository(DaoMemoria())
        repo.guardar(apertura)
        repo.guardarTareas("f1", tareasCompletas())
        val vm = FaenaViewModel(repo, repo)
        vm.iniciar(usuario, "f1")
        advanceUntilIdle()
        vm.guardarBorradorChecklist(c)
        advanceUntilIdle()
        vm.guardarBorradorAst(a)
        advanceUntilIdle()
        vm.guardarBorradorEncuesta(e)
        advanceUntilIdle()
        val recuperado = FaenaViewModel(repo, repo)
        recuperado.iniciar(usuario, "f1")
        advanceUntilIdle()
        assertEquals(EstadoSeccion.BORRADOR, recuperado.ui.value.checklist!!.estado)
        assertEquals("Primario", recuperado.ui.value.checklist!!.items.single().categoria)
        assertEquals("OK", recuperado.ui.value.checklist!!.items.single().badge)
        assertEquals(EstadoSeccion.BORRADOR, recuperado.ui.value.ast!!.estado)
        assertEquals(EstadoSeccion.BORRADOR, recuperado.ui.value.encuesta!!.estado)
        assertNull(ReglasDecision.resultado(repo.recuperarFaenaPorId("f1")!!.resumen))
        recuperado.registrarChecklist(c)
        advanceUntilIdle()
        recuperado.registrarAst(a)
        advanceUntilIdle()
        recuperado.registrarEncuesta(e)
        advanceUntilIdle()
        assertEquals(ResultadoFaena.CUMPLE, ReglasDecision.resultado(repo.recuperarFaenaPorId("f1")!!.resumen))
    }

    @Test fun formulariosDistinguenGuardarBorradorDeConfirmarConLosMismosDatos() {
        val checklist = ChecklistViewModel(SavedStateHandle()).apply { cargar("f1", c) }
        val ast = AstViewModel().apply { cargar(a) }
        val salud = EncuestaSaludViewModel().apply { cargar(e) }
        assertEquals(EstadoSeccion.BORRADOR, checklist.borrador().estado)
        assertEquals(EstadoSeccion.BORRADOR, ast.borrador().estado)
        assertEquals(EstadoSeccion.BORRADOR, salud.borrador().estado)
        assertEquals(EstadoSeccion.COMPLETA, checklist.guardar()!!.estado)
        assertEquals(EstadoSeccion.COMPLETA, ast.guardar()!!.estado)
        assertEquals(EstadoSeccion.COMPLETA, salud.guardar()!!.estado)
    }

    @Test fun borradorNoSeDuplicaDuranteGuardadoYNoEditaDecisiones() = runTest {
        val repo = PreChequeoRepository(DaoMemoria())
        repo.guardar(apertura)
        repo.guardarTareas("f1", tareasCompletas())
        repo.guardarChecklist("f1", c)
        repo.guardarAst("f1", a)
        repo.guardarTareas("f1", tareasCompletas())
        repo.guardarEncuesta("f1", e)
        val vm = FaenaViewModel(repo, repo)
        vm.iniciar(usuario, "f1")
        advanceUntilIdle()
        var guardados = 0
        vm.guardarBorradorChecklist(c) { guardados++ }
        vm.guardarBorradorChecklist(c) { guardados++ }
        advanceUntilIdle()
        assertEquals(1, guardados)
        vm.registrarChecklist(c)
        advanceUntilIdle()
        repo.decidir("f1", RegistroDecision(Decision.AUTORIZADA, "", "jefe"), Usuario("jefe", "1234", Rol.JEFE))
        vm.cargarFaena("f1")
        advanceUntilIdle()
        vm.guardarBorradorEncuesta(e)
        advanceUntilIdle()
        assertEquals(EstadoSeccion.COMPLETA, vm.ui.value.encuesta!!.estado)
        assertEquals(EstadoSeccion.COMPLETA, repo.recuperarFaenaPorId("f1")!!.resumen.encuesta!!.estado)
    }

    @Test fun cargarAperturaIncompletaNoLaConfirma() = runTest {
        val repo = PreChequeoRepository(DaoMemoria())
        repo.guardar(apertura.copy(embarcacionLista = null))
        val vm = FaenaViewModel(repo, repo)
        vm.iniciar(usuario, "f1")
        advanceUntilIdle()
        assertFalse(vm.ui.value.aperturaConfirmada)
        assertFalse(vm.registrarChecklist(c))
    }
}
