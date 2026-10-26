package com.duoc.aquaflow

import com.duoc.aquaflow.data.local.FaenaCodec
import com.duoc.aquaflow.data.repository.PreChequeoRepository
import com.duoc.aquaflow.domain.ReglasDecision
import com.duoc.aquaflow.domain.ReglasSecciones
import com.duoc.aquaflow.model.*
import kotlinx.coroutines.test.runTest
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class EstadosSeccionesTest {
    private val apertura = PreChequeo("f1", "Centro", "25/10/2026", "10:00", "Ana", "Luis", true, true, true)
    private val checklist = ChecklistFaena("f1", listOf(ItemEquipamiento("1", "Equipo", "", EstadoEquipo.CUMPLE)))
    private val ast = FormularioAst("Limpieza", false, List(PREGUNTAS_AST.size) { true }, listOf(PasoAst("Tarea", "Peligro", "Control")), ResultadoAst.CUMPLE)
    private val salud = EncuestaSalud(120, 80, 98, emptyList(), NivelSalud.NORMAL)
    private val completo = ResumenFaena(apertura, checklist, ast, salud, tareasCompletas())

    @Test fun distingueAusenteBorradorYCompletaSinConfundirRiesgo() {
        assertEquals(EstadoSeccion.SIN_REGISTRAR, ReglasSecciones.ast(null))
        assertEquals(EstadoSeccion.BORRADOR, ReglasSecciones.ast(ast.copy(respuestas = emptyList())))
        assertEquals(EstadoSeccion.BORRADOR, ReglasSecciones.ast(ast.copy(pasos = listOf(PasoAst()))))
        assertEquals(EstadoSeccion.BORRADOR, ReglasSecciones.encuesta(salud.copy(saturacion = 0)))
        assertEquals(EstadoSeccion.BORRADOR, ReglasSecciones.checklist(checklist.copy(items = emptyList())))
        assertEquals(EstadoSeccion.BORRADOR, ReglasSecciones.checklist(checklist.copy(items = checklist.items.map { it.copy(estado = EstadoEquipo.PENDIENTE) })))
        assertEquals(EstadoSeccion.COMPLETA, ReglasSecciones.encuesta(salud.copy(saturacion = 90)))
        assertEquals(ResultadoFaena.REQUIERE_REVISION, ReglasDecision.resultado(completo.copy(encuesta = salud.copy(saturacion = 90))))
        assertNull(ReglasDecision.errorDecision(Decision.AUTORIZADA, ResultadoFaena.REQUIERE_REVISION, "Revisado"))
    }

    @Test fun jsonConservaBorradoresInclusoConDatosValidos() {
        val c = checklist.copy(estado = EstadoSeccion.BORRADOR)
        val a = ast.copy(estado = EstadoSeccion.BORRADOR)
        val e = salud.copy(estado = EstadoSeccion.BORRADOR)
        assertEquals(c, FaenaCodec.checklistDesde(FaenaCodec.checklistATexto(c)))
        assertEquals(a, FaenaCodec.astDesde(FaenaCodec.astATexto(a)))
        assertEquals(e, FaenaCodec.encuestaDesde(FaenaCodec.encuestaATexto(e)))
        assertEquals("BORRADOR", JSONObject(FaenaCodec.astATexto(a)).getString("estado"))
    }

    @Test fun jsonAntiguoSeValidaYEstadoDesconocidoNoCompleta() {
        val viejo = JSONObject(FaenaCodec.astATexto(ast)).apply { remove("estado") }
        assertEquals(EstadoSeccion.BORRADOR, FaenaCodec.astDesde(viejo.toString())!!.estado)
        viejo.put("tipologia", "")
        assertEquals(EstadoSeccion.BORRADOR, FaenaCodec.astDesde(viejo.toString())!!.estado)
        viejo.put("tipologia", "Limpieza").put("estado", "NUEVO_ESTADO")
        assertEquals(EstadoSeccion.BORRADOR, FaenaCodec.astDesde(viejo.toString())!!.estado)
        viejo.put("estado", "COMPLETA").put("respuestas", org.json.JSONArray())
        assertEquals(EstadoSeccion.BORRADOR, FaenaCodec.astDesde(viejo.toString())!!.estado)
    }

    @Test fun ningunaViaPermiteDecidirCadaTipoDeExpedienteIncompleto() = runTest {
        val casos = listOf(
            completo.copy(apertura = apertura.copy(condicionesListas = false)),
            completo.copy(checklist = null), completo.copy(ast = null), completo.copy(encuesta = null),
            completo.copy(checklist = checklist.copy(estado = EstadoSeccion.BORRADOR)),
            completo.copy(ast = ast.copy(estado = EstadoSeccion.BORRADOR)),
            completo.copy(encuesta = salud.copy(estado = EstadoSeccion.BORRADOR)),
            completo.copy(ast = ast.copy(tipologia = "")),
            completo.copy(encuesta = salud.copy(sistolica = 0))
        )
        for (caso in casos) {
            val repo = PreChequeoRepository(DaoMemoria())
            repo.guardar(caso.apertura)
            caso.checklist?.let { repo.guardarChecklist("f1", it) }
            caso.ast?.let { repo.guardarAst("f1", it) }
            caso.encuesta?.let { repo.guardarEncuesta("f1", it) }
            assertNull(ReglasDecision.resultado(repo.recuperarFaena("Luis")!!.resumen))
            for (decision in Decision.entries) {
                val d = RegistroDecision(decision, "Motivo", "jefe")
                assertTrue(runCatching { repo.guardarDecision("f1", d) }.isFailure)
                assertTrue(runCatching { repo.decidir("f1", d, Usuario("jefe", "1234", Rol.JEFE)) }.isFailure)
            }
            assertNull(repo.recuperarFaena("Luis")!!.decision)
        }
    }

    @Test fun astDeDevelopPermitePasosVaciosYExigeCriticasConfirmadas() {
        assertEquals(EstadoSeccion.COMPLETA, ReglasSecciones.ast(ast.copy(pasos = emptyList())))
        assertEquals(EstadoSeccion.BORRADOR, ReglasSecciones.ast(ast.copy(respuestas = List(PREGUNTAS_AST.size) { false })))
    }

    @Test fun seccionDeOtraFaenaBloqueaResumen() {
        assertNull(ReglasDecision.resultado(completo.copy(checklist = checklist.copy(idFaena = "otra"))))
    }
}
