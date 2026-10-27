package com.duoc.aquaflow

import com.duoc.aquaflow.data.local.FaenaCodec
import com.duoc.aquaflow.model.ChecklistFaena
import com.duoc.aquaflow.model.Decision
import com.duoc.aquaflow.model.EncuestaSalud
import com.duoc.aquaflow.model.EstadoEquipo
import com.duoc.aquaflow.model.FormularioAst
import com.duoc.aquaflow.model.ItemEquipamiento
import com.duoc.aquaflow.model.NivelSalud
import com.duoc.aquaflow.model.PasoAst
import com.duoc.aquaflow.model.RegistroDecision
import com.duoc.aquaflow.model.ResultadoAst
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FaenaCodecTest {
    @Test fun checklistVuelveIgualConNotasYFotos() {
        val original = ChecklistFaena(
            "f1",
            listOf(
                ItemEquipamiento("1", "Compresor", "Test de CO \"OK\"", EstadoEquipo.CUMPLE),
                ItemEquipamiento("2", "Umbilical", "Roce\nsuperficial", EstadoEquipo.NO_CUMPLE, "Reemplazar, ñandú")
            ),
            listOf("file:///foto1.jpg", "file:///foto2.jpg")
        )
        assertEquals(original, FaenaCodec.checklistDesde(FaenaCodec.checklistATexto(original)))
    }

    @Test fun astVuelveIgual() {
        val original = FormularioAst(
            "Inspección de redes", true, listOf(true, false, true),
            listOf(PasoAst("Bajar", "Corriente", "Línea guía", true), PasoAst()),
            ResultadoAst.REQUIERE_REVISION, com.duoc.aquaflow.model.EstadoSeccion.BORRADOR
        )
        assertEquals(original, FaenaCodec.astDesde(FaenaCodec.astATexto(original)))
    }

    @Test fun encuestaVuelveIgual() {
        val original = EncuestaSalud(128, 84, 96, listOf("¿Durmió menos de 6 horas?"), NivelSalud.ATENCION)
        assertEquals(original, FaenaCodec.encuestaDesde(FaenaCodec.encuestaATexto(original)))
    }

    @Test fun decisionVuelveIgual() {
        val original = RegistroDecision(Decision.RECHAZADA, "Falla en el compresor", "jefe")
        assertEquals(original, FaenaCodec.decisionDesde(FaenaCodec.decisionATexto(original)))
    }

    @Test fun textoDanadoDevuelveNullEnVezDeFallar() {
        assertNull(FaenaCodec.checklistDesde("no es json"))
        assertNull(FaenaCodec.astDesde("{}"))
        assertNull(FaenaCodec.encuestaDesde("""{"sistolica":120,"diastolica":80,"saturacion":98,"alertas":[],"nivel":"INEXISTENTE"}"""))
        assertNull(FaenaCodec.decisionDesde(""))
    }
}
