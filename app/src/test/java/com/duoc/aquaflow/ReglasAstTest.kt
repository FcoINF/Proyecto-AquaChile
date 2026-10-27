package com.duoc.aquaflow

import com.duoc.aquaflow.domain.ReglasAst
import com.duoc.aquaflow.model.PREGUNTAS_AST
import com.duoc.aquaflow.model.PasoAst
import com.duoc.aquaflow.model.ResultadoAst
import com.duoc.aquaflow.model.TIPOLOGIAS_AST
import com.duoc.aquaflow.viewmodel.AstViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private val TODO_SI: List<Boolean?> = List(PREGUNTAS_AST.size) { true }
private val PASO_OK = PasoAst("Revisar red", "Corriente fuerte", "Cabo de seguridad")
private fun conNo(i: Int): List<Boolean?> = TODO_SI.toMutableList().also { it[i] = false }

class ReglasAstTest {

    @Test fun formularioCompleto_noTieneError() =
        assertNull(ReglasAst.error(TIPOLOGIAS_AST[0], TODO_SI, listOf(PASO_OK)))

    @Test fun sinTipologia_esError() =
        assertEquals("Elige la tipología de la tarea", ReglasAst.error(" ", TODO_SI, listOf(PASO_OK)))

    @Test fun respuestasIncompletas_esError() {
        val incompletas: List<Boolean?> = TODO_SI.toMutableList().also { it[2] = null }
        assertFalse(ReglasAst.respuestasCompletas(incompletas))
        assertEquals("Responde todas las preguntas del chequeo inicial",
            ReglasAst.error(TIPOLOGIAS_AST[0], incompletas, listOf(PASO_OK)))
    }

    @Test fun preguntaCriticaNo_esError() =
        assertEquals("Debes confirmar todas las preguntas críticas para continuar",
            ReglasAst.error(TIPOLOGIAS_AST[0], conNo(0), listOf(PASO_OK)))

    @Test fun pasoSinPeligro_esError() =
        assertEquals("Cada paso necesita tarea y peligro",
            ReglasAst.error(TIPOLOGIAS_AST[0], TODO_SI, listOf(PASO_OK.copy(peligro = ""))))

    @Test fun peligroSinControl_esError() =
        assertEquals("Cada peligro necesita su medida de control",
            ReglasAst.error(TIPOLOGIAS_AST[0], TODO_SI, listOf(PASO_OK.copy(control = ""))))

    @Test fun todoSi_esCumple() =
        assertEquals(ResultadoAst.CUMPLE, ReglasAst.resultado(TODO_SI, listOf(PASO_OK), false))

    @Test fun preguntaNoCriticaNo_esObservado() =
        assertEquals(ResultadoAst.OBSERVADO, ReglasAst.resultado(conNo(3), listOf(PASO_OK), false))

    @Test fun riesgoCritico_esObservado() =
        assertEquals(ResultadoAst.OBSERVADO, ReglasAst.resultado(TODO_SI, listOf(PASO_OK.copy(critico = true)), false))

    @Test fun requiereNuevoAst_esObservado() =
        assertEquals(ResultadoAst.OBSERVADO, ReglasAst.resultado(TODO_SI, listOf(PASO_OK), true))
}

class AstViewModelTest {

    private fun vmCompleto() = AstViewModel().apply {
        elegirTipologia(TIPOLOGIAS_AST[0])
        PREGUNTAS_AST.indices.forEach { responder(it, true) }
        cambiarPaso(0, PASO_OK)
    }

    @Test fun vacio_noGuardaYPideCompletar() {
        val vm = AstViewModel()
        assertNull(vm.guardar())
        assertEquals("Elige la tipología de la tarea", vm.ui.value.error)
    }

    @Test fun completo_guardaConResultadoCumple() {
        val f = vmCompleto().guardar()!!
        assertEquals(TIPOLOGIAS_AST[0], f.tipologia)
        assertEquals(ResultadoAst.CUMPLE, f.resultado)
    }

    @Test fun cargar_restauraLoGuardado() {
        val guardado = vmCompleto().apply { alternarNuevoAst() }.guardar()!!
        val otro = AstViewModel().apply { cargar(guardado) }
        assertEquals(guardado, otro.guardar())
    }

    @Test fun semaforoEnVivo_apareceAlResponderTodo() {
        val vm = AstViewModel()
        assertNull(vm.ui.value.resultado)
        PREGUNTAS_AST.indices.forEach { vm.responder(it, true) }
        assertEquals(ResultadoAst.CUMPLE, vm.ui.value.resultado)
    }

    @Test fun nuncaQuedaSinPasos() {
        val vm = AstViewModel().apply { agregarPaso() }
        vm.quitarPaso(0)
        assertEquals(1, vm.ui.value.pasos.size)
    }

    @Test fun respetaElMaximoDePasos() {
        val vm = AstViewModel()
        repeat(ReglasAst.MAX_PASOS + 5) { vm.agregarPaso() }
        assertEquals(ReglasAst.MAX_PASOS, vm.ui.value.pasos.size)
    }

    @Test fun responderFueraDeIndice_seIgnora() {
        val vm = AstViewModel()
        vm.responder(99, true)
        assertTrue(vm.ui.value.respuestas.all { it == null })
    }
}
