package com.duoc.aquaflow

import com.duoc.aquaflow.data.repository.PreChequeoRepository
import com.duoc.aquaflow.model.ChecklistFaena
import com.duoc.aquaflow.model.Decision
import com.duoc.aquaflow.model.EncuestaSalud
import com.duoc.aquaflow.model.EstadoEquipo
import com.duoc.aquaflow.model.EstadoFaena
import com.duoc.aquaflow.model.FormularioAst
import com.duoc.aquaflow.model.ItemEquipamiento
import com.duoc.aquaflow.model.NivelSalud
import com.duoc.aquaflow.model.PasoAst
import com.duoc.aquaflow.model.PreChequeo
import com.duoc.aquaflow.model.RegistroDecision
import com.duoc.aquaflow.model.ResultadoAst
import com.duoc.aquaflow.model.Rol
import com.duoc.aquaflow.model.Usuario
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class FaenaPersistenciaTest {
    private val dao = DaoMemoria()
    private val repo = PreChequeoRepository(dao)
    private val jefe = Usuario("jefe", "1234", Rol.JEFE)
    private val luis = Usuario("Luis", "1234", Rol.SUPERVISOR)
    private val apertura = PreChequeo("f1", "Centro Norte", "22/10/2026", "09:00", "Ana", "Luis", true, true, true)
    private val checklist = ChecklistFaena("f1", listOf(ItemEquipamiento("1", "Compresor", "ok", EstadoEquipo.CUMPLE)))
    private val ast = FormularioAst("Limpieza de fondos", false, List(com.duoc.aquaflow.model.PREGUNTAS_AST.size) { true }, listOf(PasoAst("t", "p", "c")), ResultadoAst.CUMPLE)
    private val encuesta = EncuestaSalud(120, 80, 98, emptyList(), NivelSalud.NORMAL)

    private suspend fun guardarTodo() {
        repo.guardar(apertura)
        repo.guardarChecklist("f1", checklist)
        repo.guardarAst("f1", ast)
        repo.guardarTareas("f1", tareasCompletas())
        repo.guardarEncuesta("f1", encuesta)
    }

    @Test fun guardaYLeeTodasLasSecciones() = runTest {
        guardarTodo()
        val faena = repo.observarFaena("f1", jefe).first()!!
        assertEquals(apertura, faena.resumen.apertura)
        assertEquals(checklist, faena.resumen.checklist)
        assertEquals(ast, faena.resumen.ast)
        assertEquals(encuesta, faena.resumen.encuesta)
        assertEquals(EstadoFaena.PRECHEQUEO, faena.estado)
    }

    @Test fun guardarLaMismaAperturaConservaLasSecciones() = runTest {
        guardarTodo()
        repo.guardar(apertura)
        assertNotNull(repo.observarFaena("f1", jefe).first()!!.resumen.checklist)
    }

    @Test fun cambiarCondicionesInicialesInvalidaLasSecciones() = runTest {
        guardarTodo()
        // Las condiciones iniciales forman parte del expediente persistido.
        repo.guardar(apertura.copy(condicionesListas = false))
        assertNull(repo.observarFaena("f1", jefe).first()!!.resumen.checklist)
    }

    @Test fun cambiarLaAperturaDejaLasSeccionesEnBlanco() = runTest {
        guardarTodo()
        repo.guardar(apertura.copy(buzoAsignado = "Otro buzo"))
        val faena = repo.observarFaena("f1", jefe).first()!!
        assertEquals("Otro buzo", faena.resumen.apertura.buzoAsignado)
        assertNull(faena.resumen.checklist)
        assertNull(faena.resumen.ast)
        assertNull(faena.resumen.encuesta)
    }

    @Test fun laDecisionDefineElEstadoYNoSePuedeSobrescribir() = runTest {
        guardarTodo()
        repo.guardarDecision("f1", RegistroDecision(Decision.AUTORIZADA, "", "jefe"))
        assertEquals(true, runCatching { repo.guardarDecision("f1", RegistroDecision(Decision.RECHAZADA, "otro", "jefe")) }.isFailure)
        val faena = repo.observarFaena("f1", jefe).first()!!
        assertEquals(EstadoFaena.AUTORIZADA, faena.estado)
        assertEquals(Decision.AUTORIZADA, faena.decision?.decision)
    }

    @Test fun elSupervisorSoloVeSusFaenas() = runTest {
        guardarTodo()
        repo.guardar(apertura.copy(id = "f2", supervisorACargo = "Pedro"))
        assertEquals(listOf("f1"), repo.observarFaenas(luis).first().map { it.resumen.apertura.id })
        assertNull(repo.observarFaena("f2", luis).first())
        assertEquals(2, repo.observarFaenas(jefe).first().size)
    }

    @Test fun seccionesSobreFaenaInexistenteNoCreanFilas() = runTest {
        assertEquals(true, runCatching { repo.guardarChecklist("no-existe", checklist.copy(idFaena = "no-existe")) }.isFailure)
        assertEquals(0, repo.observarFaenas(jefe).first().size)
    }
}
