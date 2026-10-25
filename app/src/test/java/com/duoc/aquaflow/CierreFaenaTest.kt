package com.duoc.aquaflow

import androidx.lifecycle.SavedStateHandle
import com.duoc.aquaflow.data.repository.PreChequeoRepository
import com.duoc.aquaflow.model.*
import com.duoc.aquaflow.viewmodel.CierreFaenaViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class CierreFaenaTest {
    private val supervisor = Usuario("supervisor", "1234", Rol.SUPERVISOR)
    private val cierre = RegistroCierre(30, "Sin novedades", true, "suplantado", 1)

    @Test fun cierrePersistidoDefineEstadoYNoSeSobrescribe() = runTest {
        val repo = PreChequeoRepository(DaoMemoria())
        repo.guardar(PreChequeo("f1", "Centro", "24/10/2026", "10:00", "Ana", supervisor.nombre, true, true, true))
        repo.guardarChecklist("f1", ChecklistFaena("f1", listOf(ItemEquipamiento("1", "Equipo", "", EstadoEquipo.CUMPLE))))
        repo.guardarEncuesta("f1", EncuestaSalud(120, 80, 98, emptyList(), NivelSalud.NORMAL))
        repo.guardarAst("f1", FormularioAst("Limpieza", false, List(PREGUNTAS_AST.size) { true }, listOf(PasoAst("Tarea", "Peligro", "Control")), ResultadoAst.CUMPLE))
        assertTrue(runCatching { repo.cerrar("f1", cierre, supervisor) }.isFailure)
        repo.guardarDecision("f1", RegistroDecision(Decision.AUTORIZADA, "", "jefe"))
        assertTrue(runCatching { repo.cerrar("f1", cierre, supervisor.copy(nombre = "otro")) }.isFailure)
        assertTrue(runCatching { repo.cerrar("f1", cierre.copy(revisionSalida = false), supervisor) }.isFailure)
        repo.cerrar("f1", cierre, supervisor)
        val faena = repo.observarFaena("f1", supervisor).first()!!
        assertEquals(EstadoFaena.CERRADA, faena.estado)
        assertEquals("supervisor", faena.cierre!!.cerradoPor)
        assertTrue(faena.cierre.cerradoEn > 1)
        assertEquals(30, faena.cierre.duracionMinutos)
        assertTrue(runCatching { repo.cerrar("f1", cierre.copy(duracionMinutos = 50), supervisor) }.isFailure)
        assertTrue(runCatching { repo.guardar(faena.resumen.apertura) }.isFailure)
        assertEquals(30, repo.recuperarFaena("supervisor")!!.cierre!!.duracionMinutos)
    }

    @Test fun faenaRechazadaNoSePuedeCerrarNiPorJefatura() = runTest {
        val repo = PreChequeoRepository(DaoMemoria())
        repo.guardar(PreChequeo("f1", "Centro", "24/10/2026", "10:00", "Ana", supervisor.nombre, true, true, true))
        repo.guardarChecklist("f1", ChecklistFaena("f1", listOf(ItemEquipamiento("1", "Equipo", "", EstadoEquipo.CUMPLE))))
        repo.guardarEncuesta("f1", EncuestaSalud(120, 80, 98, emptyList(), NivelSalud.NORMAL))
        repo.guardarAst("f1", FormularioAst("Limpieza", false, List(PREGUNTAS_AST.size) { true }, listOf(PasoAst("Tarea", "Peligro", "Control")), ResultadoAst.CUMPLE))
        repo.guardarDecision("f1", RegistroDecision(Decision.RECHAZADA, "Revisar equipo", "jefe"))
        assertTrue(runCatching { repo.cerrar("f1", cierre, Usuario("jefe", "1234", Rol.JEFE)) }.isFailure)
        assertEquals(EstadoFaena.RECHAZADA, repo.recuperarFaena("supervisor")!!.estado)
    }

    @Test fun formularioValidaYConservaBorrador() {
        val saved = SavedStateHandle()
        val vm = CierreFaenaViewModel(saved)
        listOf("", "-1", "0", "1.5", "9999999999").forEach {
            vm.minutos(it)
            assertNull(vm.validar())
        }
        vm.minutos("30")
        vm.observaciones("Sin novedades")
        assertNull(vm.validar())
        vm.revision(true)
        assertNotNull(vm.validar())
        assertEquals(vm.validar(), CierreFaenaViewModel(saved).validar())
    }
}
