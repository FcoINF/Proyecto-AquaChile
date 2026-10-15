package com.duoc.aquaflow.domain

import com.duoc.aquaflow.TestData
import com.duoc.aquaflow.domain.catalog.Catalogos
import com.duoc.aquaflow.domain.config.ReglasConfig
import com.duoc.aquaflow.domain.rules.FaenaEvaluator
import com.duoc.aquaflow.model.EstadoEquipo
import com.duoc.aquaflow.model.Evidencia
import com.duoc.aquaflow.model.RespuestaChecklist
import com.duoc.aquaflow.model.TipoEvidencia
import com.duoc.aquaflow.model.TipoFormulario
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FaenaEvaluatorTest {

    @Test
    fun `una faena sin datos esta incompleta y no se puede enviar`() {
        val evaluacion = FaenaEvaluator.evaluar(TestData.detalleVacio())
        assertFalse(evaluacion.todasCompletas)
        assertFalse(evaluacion.puedeEnviarARevision)
        assertFalse(evaluacion.puedeAutorizar)
        assertTrue(evaluacion.faltantes.isNotEmpty())
    }

    @Test
    fun `una faena completa y favorable se puede enviar y autorizar`() {
        val evaluacion = FaenaEvaluator.evaluar(TestData.detalleCompleto())
        assertTrue(evaluacion.faltantes.toString(), evaluacion.todasCompletas)
        assertTrue(evaluacion.bloqueos.isEmpty())
        assertTrue(evaluacion.puedeEnviarARevision)
        assertTrue(evaluacion.puedeAutorizar)
    }

    @Test
    fun `un equipo que no cumple bloquea la autorizacion pero no el envio`() {
        val base = TestData.detalleCompleto()
        val checklist = base.checklist.map {
            if (it.itemId == "EQ_COMPRESOR") RespuestaChecklist(it.faenaId, it.itemId, EstadoEquipo.NO_CUMPLE, "Filtro saturado") else it
        }
        val evaluacion = FaenaEvaluator.evaluar(base.copy(checklist = checklist))
        assertTrue(evaluacion.puedeEnviarARevision)
        assertFalse(evaluacion.puedeAutorizar)
        assertEquals(1, evaluacion.checklist.bloqueos.size)
    }

    @Test
    fun `un equipo observado exige nota pero no bloquea`() {
        val base = TestData.detalleCompleto()
        val sinNota = base.checklist.map {
            if (it.itemId == "EQ_UMBILICAL") RespuestaChecklist(it.faenaId, it.itemId, EstadoEquipo.OBSERVADO, "") else it
        }
        val incompleta = FaenaEvaluator.evaluar(base.copy(checklist = sinNota))
        assertFalse(incompleta.checklist.completa)
        assertTrue(incompleta.checklist.bloqueos.isEmpty())

        val conNota = base.checklist.map {
            if (it.itemId == "EQ_UMBILICAL") RespuestaChecklist(it.faenaId, it.itemId, EstadoEquipo.OBSERVADO, "Roce externo") else it
        }
        val completa = FaenaEvaluator.evaluar(base.copy(checklist = conNota))
        assertTrue(completa.checklist.completa)
        assertEquals(1, completa.checklist.advertencias.size)
        assertTrue(completa.puedeAutorizar)
    }

    @Test
    fun `se puede exigir foto en las fallas por configuracion`() {
        val base = TestData.detalleCompleto()
        val checklist = base.checklist.map {
            if (it.itemId == "EQ_TRAJE") RespuestaChecklist(it.faenaId, it.itemId, EstadoEquipo.OBSERVADO, "Costura") else it
        }
        val config = ReglasConfig(exigirFotoEnFallas = true)
        val sinFoto = FaenaEvaluator.evaluarChecklist(checklist, emptyList(), config)
        assertFalse(sinFoto.completa)

        val foto = Evidencia(faenaId = 10, tipo = TipoEvidencia.EQUIPO, itemId = "EQ_TRAJE", ruta = "10/a.jpg", creadoAt = 0)
        assertTrue(FaenaEvaluator.evaluarChecklist(checklist, listOf(foto), config).completa)
    }

    @Test
    fun `una respuesta NO en el gating del AST bloquea la autorizacion`() {
        val base = TestData.detalleCompleto()
        val respuestas = base.respuestas.map {
            if (it.formulario == TipoFormulario.AST_GATING && it.clave == "AST_G_AMBIENTE") it.copy(respuesta = false) else it
        }
        val evaluacion = FaenaEvaluator.evaluar(base.copy(respuestas = respuestas))
        assertTrue(evaluacion.ast.completa)
        assertEquals(1, evaluacion.ast.bloqueos.size)
        assertFalse(evaluacion.puedeAutorizar)
    }

    @Test
    fun `el AST exige verificar todos los riesgos`() {
        val base = TestData.detalleCompleto()
        val respuestas = base.respuestas.filterNot { it.formulario == TipoFormulario.AST_RIESGO && it.clave == "AST_R_SUMINISTRO" }
        val evaluacion = FaenaEvaluator.evaluar(base.copy(respuestas = respuestas))
        assertFalse(evaluacion.ast.completa)
        assertEquals(1, evaluacion.ast.faltantes.size)
    }

    @Test
    fun `una declaracion de salud desfavorable deja al buzo no apto`() {
        val base = TestData.detalleCompleto()
        val respuestas = base.respuestas.map {
            if (it.formulario == TipoFormulario.SALUD_DECLARACION && it.clave == "SAL_SIN_SUSTANCIAS") it.copy(respuesta = false) else it
        }
        val evaluacion = FaenaEvaluator.evaluar(base.copy(respuestas = respuestas))
        assertEquals(1, evaluacion.salud.bloqueos.size)
        assertFalse(evaluacion.puedeAutorizar)
    }

    @Test
    fun `signos fuera de la referencia solo avisan por defecto`() {
        val base = TestData.detalleCompleto()
        val fuera = base.copy(encuestaSalud = TestData.encuesta(sistolica = 170, diastolica = 100, spo2 = 90, temperatura = 38.5))
        val evaluacion = FaenaEvaluator.evaluar(fuera)
        assertEquals(3, evaluacion.salud.advertencias.size)
        assertTrue(evaluacion.salud.bloqueos.isEmpty())
        assertTrue(evaluacion.puedeAutorizar)
    }

    @Test
    fun `la configuracion puede hacer que los signos fuera de rango bloqueen`() {
        val fuera = TestData.detalleCompleto().copy(encuestaSalud = TestData.encuesta(spo2 = 90))
        val evaluacion = FaenaEvaluator.evaluar(fuera, ReglasConfig(bloquearAutorizacionPorSignos = true))
        assertFalse(evaluacion.puedeAutorizar)
    }

    @Test
    fun `faltan signos vitales`() {
        val sinSignos = TestData.detalleCompleto().copy(
            encuestaSalud = TestData.encuesta(sistolica = null, diastolica = null, spo2 = null, temperatura = null)
        )
        val evaluacion = FaenaEvaluator.evaluar(sinSignos)
        assertEquals(3, evaluacion.salud.faltantes.size)
    }

    @Test
    fun `el catalogo cubre lo que evalua el AST y la salud`() {
        assertTrue(Catalogos.equipos.isNotEmpty() && Catalogos.gatingAst.isNotEmpty())
        assertTrue(Catalogos.riesgosAst.isNotEmpty() && Catalogos.declaracionSalud.isNotEmpty())
        val ids = Catalogos.equipos.map { it.id } + Catalogos.gatingAst.map { it.id } +
            Catalogos.riesgosAst.map { it.id } + Catalogos.declaracionSalud.map { it.id }
        assertEquals("los ids del catálogo deben ser únicos", ids.size, ids.toSet().size)
    }
}
