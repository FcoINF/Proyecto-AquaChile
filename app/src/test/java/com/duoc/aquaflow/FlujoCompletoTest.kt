package com.duoc.aquaflow

import androidx.lifecycle.SavedStateHandle
import com.duoc.aquaflow.data.repository.PreChequeoRepository
import com.duoc.aquaflow.data.local.FaenaCodec
import com.duoc.aquaflow.domain.*
import com.duoc.aquaflow.model.*
import com.duoc.aquaflow.viewmodel.TareasViewModel
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class FlujoCompletoTest {
    private val supervisor = Usuario("supervisor", "1234", Rol.SUPERVISOR)
    private val jefe = Usuario("jefe", "1234", Rol.JEFE)
    private val inicial = EncuestaSalud(120, 80, 98, emptyList(), NivelSalud.NORMAL)
    private val cierre = RegistroCierre(30, "Revisión final", true, saludSalida = saludSalida())
    private suspend fun preparar(repo: PreChequeoRepository) {
        repo.guardar(PreChequeo("f1", "Centro", "25/10/2026", "10:00", "Ana", supervisor.nombre, true, true, true))
        repo.guardarChecklist("f1", ChecklistFaena("f1", listOf(ItemEquipamiento("1", "Equipo", "", EstadoEquipo.CUMPLE)), listOf("foto-de-prueba")))
        repo.guardarEncuesta("f1", inicial)
        repo.guardarAst("f1", FormularioAst("Limpieza", false, List(PREGUNTAS_AST.size) { true }, emptyList(), ResultadoAst.CUMPLE))
    }
    @Test fun cicloExigeTareasAutorizacionInicioYSaludDeSalidaSeparada() = runTest {
        val repo = PreChequeoRepository(DaoMemoria())
        preparar(repo)
        val decision = RegistroDecision(Decision.AUTORIZADA, "", "falso")
        assertTrue(runCatching { repo.decidir("f1", decision, jefe) }.isFailure)
        repo.guardarTareas("f1", tareasCompletas().copy(estado = EstadoSeccion.BORRADOR))
        assertTrue(runCatching { repo.decidir("f1", decision, jefe) }.isFailure)
        repo.guardarTareas("f1", tareasCompletas())
        assertTrue(runCatching { repo.iniciar("f1", supervisor) }.isFailure)
        assertTrue(runCatching { repo.decidir("f1", decision, supervisor) }.isFailure)
        repo.decidir("f1", decision, jefe)
        assertEquals(EstadoFaena.AUTORIZADA, repo.recuperarFaenaPorId("f1")!!.estado)
        assertTrue(runCatching { repo.cerrar("f1", cierre, supervisor) }.isFailure)
        assertTrue(runCatching { repo.iniciar("f1", jefe) }.isFailure)
        assertTrue(runCatching { repo.iniciar("f1", supervisor.copy(nombre = "otro")) }.isFailure)
        repo.iniciar("f1", supervisor)
        val enCurso = repo.recuperarFaenaPorId("f1")!!
        assertEquals(EstadoFaena.EN_EJECUCION, enCurso.estado)
        assertEquals(supervisor.nombre, enCurso.inicio!!.iniciadoPor)
        assertTrue(enCurso.inicio.iniciadoEn > 0)
        assertTrue(runCatching { repo.iniciar("f1", supervisor) }.isFailure)
        assertTrue(runCatching { repo.guardarTareas("f1", tareasCompletas()) }.isFailure)
        assertTrue(runCatching { repo.cerrar("f1", cierre.copy(saludSalida = null), supervisor) }.isFailure)
        assertTrue(runCatching { repo.cerrar("f1", cierre.copy(saludSalida = saludSalida().copy(saturacion = 0)), supervisor) }.isFailure)
        assertTrue(runCatching { repo.cerrar("f1", cierre.copy(saludSalida = saludSalida().copy(estado = EstadoSeccion.BORRADOR)), supervisor) }.isFailure)
        assertTrue(runCatching { repo.cerrar("f1", cierre, jefe) }.isFailure)
        repo.cerrar("f1", cierre, supervisor)
        val final = repo.recuperarFaenaPorId("f1")!!
        assertEquals(EstadoFaena.CERRADA, final.estado)
        assertEquals(inicial, final.resumen.encuesta)
        assertEquals(saludSalida(), final.cierre!!.saludSalida)
        assertEquals(final.inicio, enCurso.inicio)
        assertTrue(runCatching { repo.cerrar("f1", cierre, supervisor) }.isFailure)
    }

    @Test fun rechazadaNoPermiteInicio() = runTest {
        val repo = PreChequeoRepository(DaoMemoria())
        preparar(repo); repo.guardarTareas("f1", tareasCompletas())
        repo.decidir("f1", RegistroDecision(Decision.RECHAZADA, "Revisar", "jefe"), jefe)
        assertTrue(runCatching { repo.iniciar("f1", supervisor) }.isFailure)
    }

    @Test fun tareasSeValidanConservanBorradorYSeInvalidanAlEditar() {
        val saved = SavedStateHandle()
        val vm = TareasViewModel(saved)
        vm.cargar(null)
        assertNull(vm.validar())
        vm.agregar(); vm.verificar(0, true)
        assertNull(vm.validar())
        vm.cambiar(0, "Inspeccionar red"); vm.verificar(0, true)
        assertNotNull(vm.validar())
        assertEquals(vm.borrador(), TareasViewModel(saved).borrador())
        vm.cambiar(0, "Otra tarea")
        assertNull(vm.validar())
        assertEquals(vm.borrador(), FaenaCodec.tareasDesde(FaenaCodec.tareasATexto(vm.borrador())))
    }

    @Test fun filtroFechaCentroBuzoYEstadoSeCombinan() {
        val base = HistorialItem("1", "1", "Centro Norte", "Ana", "supervisor", "25/10/2026", "10:00", EstadoFaena.EN_EJECUCION)
        val items = listOf(base, base.copy(id = "2", fecha = "24/10/2026"), base.copy(id = "3", centroCultivo = "Sur", buzoAsignado = "Luis"))
        assertEquals(listOf("1"), HistorialFiltro("Norte", EstadoFaena.EN_EJECUCION, "25/10/2026").aplicar(items).map { it.id })
        assertEquals(listOf("1"), HistorialFiltro("Ana", fecha = "25/10/2026").aplicar(items).map { it.id })
        assertTrue(HistorialFiltro(fecha = "31/02/2026").aplicar(items).isEmpty())
        assertNotNull(HistorialFiltro(fecha = "31/02/2026").errorFecha)
        assertEquals(3, HistorialFiltro().aplicar(items).size)
    }

    @Test fun cierreHistoricoSinSaludDeSalidaSeLeeSinInventarla() {
        val json = """{"duracionMinutos":30,"observaciones":"Histórico","revisionSalida":true,"cerradoPor":"supervisor","cerradoEn":1}"""
        assertNull(FaenaCodec.cierreDesde(json)!!.saludSalida)
        assertEquals(cierre, FaenaCodec.cierreDesde(FaenaCodec.cierreATexto(cierre)))
    }
}
