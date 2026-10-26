package com.duoc.aquaflow

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.duoc.aquaflow.data.local.AquaFlowDatabase
import com.duoc.aquaflow.data.repository.PreChequeoRepository
import com.duoc.aquaflow.domain.ReglasDecision
import com.duoc.aquaflow.model.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

class EstadosSeccionesRoomTest {
    @Test fun borradorSobreviveReaperturaYBloqueaDecisionHastaCompletarse() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val nombre = "estados-${UUID.randomUUID()}.db"
        var db = Room.databaseBuilder(context, AquaFlowDatabase::class.java, nombre).build()
        try {
            var repo = PreChequeoRepository(db.preChequeoDao())
            val apertura = PreChequeo("f1", "Centro", "25/10/2026", "10:00", "Ana", "Luis", true, true, true)
            val c = ChecklistFaena("f1", listOf(ItemEquipamiento("1", "Equipo", "", EstadoEquipo.CUMPLE)))
            val a = FormularioAst("Limpieza", false, List(PREGUNTAS_AST.size) { true }, listOf(PasoAst("Tarea", "Peligro", "Control")), ResultadoAst.CUMPLE)
            val e = EncuestaSalud(120, 80, 98, emptyList(), NivelSalud.NORMAL)
            repo.guardar(apertura)
            repo.guardarChecklist("f1", c.copy(estado = EstadoSeccion.BORRADOR))
            repo.guardarAst("f1", a.copy(estado = EstadoSeccion.BORRADOR))
            repo.guardarTareas("f1", tareasCompletas())
            repo.guardarEncuesta("f1", e.copy(estado = EstadoSeccion.BORRADOR))
            repo.guardar(apertura) // Guardar la misma apertura no borra los estados.
            db.close()
            db = Room.databaseBuilder(context, AquaFlowDatabase::class.java, nombre).build()
            repo = PreChequeoRepository(db.preChequeoDao())
            val r = repo.recuperarFaena("Luis")!!.resumen
            assertEquals(EstadoSeccion.BORRADOR, r.checklist!!.estado)
            assertEquals(EstadoSeccion.BORRADOR, r.ast!!.estado)
            assertEquals(EstadoSeccion.BORRADOR, r.encuesta!!.estado)
            for (d in Decision.entries) {
                assertTrue(runCatching { repo.guardarDecision("f1", RegistroDecision(d, "Motivo", "jefe")) }.isFailure)
            }
            repo.guardarChecklist("f1", c)
            repo.guardarAst("f1", a)
            repo.guardarTareas("f1", tareasCompletas())
            repo.guardarEncuesta("f1", e)
            assertNotNull(ReglasDecision.resultado(repo.recuperarFaena("Luis")!!.resumen))
            repo.decidir("f1", RegistroDecision(Decision.AUTORIZADA, "", "ignorado"), Usuario("jefe", "1234", Rol.JEFE))
            assertEquals("jefe", repo.recuperarFaena("Luis")!!.decision!!.decididoPor)
        } finally { db.close(); context.deleteDatabase(nombre) }
    }
}
