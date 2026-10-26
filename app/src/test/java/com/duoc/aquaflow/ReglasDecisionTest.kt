package com.duoc.aquaflow

import com.duoc.aquaflow.domain.ReglasDecision
import com.duoc.aquaflow.model.ChecklistFaena
import com.duoc.aquaflow.model.Decision
import com.duoc.aquaflow.model.EncuestaSalud
import com.duoc.aquaflow.model.EstadoEquipo
import com.duoc.aquaflow.model.FormularioAst
import com.duoc.aquaflow.model.ItemEquipamiento
import com.duoc.aquaflow.model.NivelSalud
import com.duoc.aquaflow.model.PREGUNTAS_AST
import com.duoc.aquaflow.model.PasoAst
import com.duoc.aquaflow.model.PreChequeo
import com.duoc.aquaflow.model.ResultadoAst
import com.duoc.aquaflow.model.ResultadoFaena
import com.duoc.aquaflow.model.ResumenFaena
import com.duoc.aquaflow.model.Rol
import com.duoc.aquaflow.viewmodel.ResumenFaenaViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private fun cl(vararg estados: EstadoEquipo) =
    ChecklistFaena("f1", estados.mapIndexed { i, e -> ItemEquipamiento("$i", "Equipo $i", "desc", e) })

private fun astDe(r: ResultadoAst = ResultadoAst.CUMPLE) = FormularioAst(
    "Inspección de redes", false, List(PREGUNTAS_AST.size) { true }, listOf(PasoAst("t", "p", "c")), r
)

private fun salud(sat: Int = 98, alertas: List<String> = emptyList()) =
    EncuestaSalud(120, 80, sat, alertas, NivelSalud.NORMAL)

private fun resumen(
    checklist: ChecklistFaena? = cl(EstadoEquipo.CUMPLE, EstadoEquipo.CUMPLE),
    ast: FormularioAst? = astDe(),
    encuesta: EncuestaSalud? = salud()
) = ResumenFaena(PreChequeo("f1", "Centro Norte", "25/10/2026", "10:00", "Ana", "Luis", true, true, true), checklist, ast, encuesta, tareasCompletas())

class ReglasDecisionTest {

    @Test fun todoCumple_esCumple() =
        assertEquals(ResultadoFaena.CUMPLE, ReglasDecision.resultado(resumen()))

    @Test fun sinChecklist_noHayResultadoYFaltaChecklist() {
        val r = resumen(checklist = null)
        assertNull(ReglasDecision.resultado(r))
        assertEquals(listOf("Checklist"), ReglasDecision.faltantes(r))
    }

    @Test fun checklistConPendientes_cuentaComoFaltante() =
        assertTrue("Checklist" in ReglasDecision.faltantes(resumen(checklist = cl(EstadoEquipo.CUMPLE, EstadoEquipo.PENDIENTE))))

    @Test fun faltanVariasPartes_lasListaTodas() =
        assertEquals(listOf("Checklist", "AST", "Encuesta de salud"),
            ReglasDecision.faltantes(resumen(checklist = null, ast = null, encuesta = null)))

    @Test fun equipoNoCumple_requiereRevision() =
        assertEquals(ResultadoFaena.REQUIERE_REVISION,
            ReglasDecision.resultado(resumen(checklist = cl(EstadoEquipo.CUMPLE, EstadoEquipo.NO_CUMPLE))))

    @Test fun equipoObservado_esObservado() =
        assertEquals(ResultadoFaena.OBSERVADO,
            ReglasDecision.resultado(resumen(checklist = cl(EstadoEquipo.OBSERVADO))))

    @Test fun astObservado_esObservado() =
        assertEquals(ResultadoFaena.OBSERVADO, ReglasDecision.resultado(resumen(ast = astDe(ResultadoAst.OBSERVADO))))

    @Test fun astRojo_requiereRevision() =
        assertEquals(ResultadoFaena.REQUIERE_REVISION,
            ReglasDecision.resultado(resumen(ast = astDe(ResultadoAst.REQUIERE_REVISION))))

    @Test fun saludCritica_requiereRevision() =
        assertEquals(ResultadoFaena.REQUIERE_REVISION, ReglasDecision.resultado(resumen(encuesta = salud(sat = 90))))

    @Test fun saludConAlerta_esObservado() =
        assertEquals(ResultadoFaena.OBSERVADO, ReglasDecision.resultado(resumen(encuesta = salud(alertas = listOf("a")))))

    @Test fun ganaElPeor() =
        assertEquals(ResultadoFaena.REQUIERE_REVISION,
            ReglasDecision.resultado(resumen(checklist = cl(EstadoEquipo.OBSERVADO), encuesta = salud(sat = 90))))

    @Test fun soloLaJefaturaDecide() {
        assertTrue(ReglasDecision.puedeDecidir(Rol.JEFE))
        assertFalse(ReglasDecision.puedeDecidir(Rol.SUPERVISOR))
    }

    @Test fun rechazoSinMotivo_esError() =
        assertNotNull(ReglasDecision.errorDecision(Decision.RECHAZADA, ResultadoFaena.CUMPLE, "  "))

    @Test fun autorizarVerdeSinMotivo_esValido() =
        assertNull(ReglasDecision.errorDecision(Decision.AUTORIZADA, ResultadoFaena.CUMPLE, ""))

    @Test fun autorizarRojoSinMotivo_esError() =
        assertNotNull(ReglasDecision.errorDecision(Decision.AUTORIZADA, ResultadoFaena.REQUIERE_REVISION, ""))

    @Test fun autorizarRojoConMotivo_esValido() =
        assertNull(ReglasDecision.errorDecision(Decision.AUTORIZADA, ResultadoFaena.REQUIERE_REVISION, "Equipo repuesto"))
}

class ResumenFaenaViewModelTest {

    @Test fun supervisor_noPuedeDecidir() {
        val vm = ResumenFaenaViewModel()
        assertNull(vm.decidir(Decision.AUTORIZADA, resumen(), Rol.SUPERVISOR, "Ana"))
        assertEquals("Solo la jefatura puede autorizar o rechazar", vm.ui.value.error)
    }

    @Test fun resumenIncompleto_noDecide() {
        val vm = ResumenFaenaViewModel()
        assertNull(vm.decidir(Decision.AUTORIZADA, resumen(checklist = null), Rol.JEFE, "Luis"))
        assertEquals("Faltan datos para decidir: Checklist", vm.ui.value.error)
    }

    @Test fun jefeAutorizaFaenaVerde_devuelveRegistro() {
        val registro = ResumenFaenaViewModel().decidir(Decision.AUTORIZADA, resumen(), Rol.JEFE, "Luis")!!
        assertEquals(Decision.AUTORIZADA, registro.decision)
        assertEquals("Luis", registro.decididoPor)
    }

    @Test fun rechazarSinMotivo_noRegistra() {
        val vm = ResumenFaenaViewModel()
        assertNull(vm.decidir(Decision.RECHAZADA, resumen(), Rol.JEFE, "Luis"))
        assertEquals("Indica el motivo del rechazo", vm.ui.value.error)
    }

    @Test fun rechazarConMotivo_guardaMotivoSinEspacios() {
        val vm = ResumenFaenaViewModel()
        vm.cambiarMotivo("  Compresor fuera de servicio ")
        val registro = vm.decidir(Decision.RECHAZADA, resumen(), Rol.JEFE, "Luis")!!
        assertEquals("Compresor fuera de servicio", registro.motivo)
    }

    @Test fun autorizarRojoSinMotivo_noRegistra() {
        val vm = ResumenFaenaViewModel()
        val rojo = resumen(checklist = cl(EstadoEquipo.NO_CUMPLE))
        assertNull(vm.decidir(Decision.AUTORIZADA, rojo, Rol.JEFE, "Luis"))
    }
}