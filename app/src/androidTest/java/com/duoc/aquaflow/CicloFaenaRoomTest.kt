package com.duoc.aquaflow

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.duoc.aquaflow.data.local.AquaFlowDatabase
import com.duoc.aquaflow.data.repository.PreChequeoRepository
import com.duoc.aquaflow.model.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

class CicloFaenaRoomTest {
    @Test fun aperturaSeccionesDecisionYCierreSobrevivenReinicio() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val nombre = "ciclo-${UUID.randomUUID()}.db"
        var db = Room.databaseBuilder(context, AquaFlowDatabase::class.java, nombre).build()
        try {
            val supervisor = Usuario("supervisor", "1234", Rol.SUPERVISOR)
            val jefe = Usuario("jefe", "1234", Rol.JEFE)
            var repo = PreChequeoRepository(db.preChequeoDao())
            val apertura = PreChequeo("f1", "Centro", "24/10/2026", "10:00", "Ana", "supervisor", true, true, true)
            repo.guardar(apertura)
            repo.guardarChecklist("f1", ChecklistFaena("f1", listOf(ItemEquipamiento("1", "Equipo", "", EstadoEquipo.CUMPLE))))
            repo.guardarEncuesta("f1", EncuestaSalud(120, 80, 98, emptyList(), NivelSalud.NORMAL))
            repo.guardarAst("f1", FormularioAst("Limpieza", false, listOf(true), listOf(PasoAst("Tarea", "Peligro", "Control")), ResultadoAst.CUMPLE))
            repo.decidir("f1", RegistroDecision(Decision.AUTORIZADA, "", "ignorado"), jefe)
            repo.cerrar("f1", RegistroCierre(25, "Sin novedades", true), supervisor)
            db.close()
            db = Room.databaseBuilder(context, AquaFlowDatabase::class.java, nombre).build()
            repo = PreChequeoRepository(db.preChequeoDao())
            val guardada = repo.recuperarFaena("supervisor")!!
            assertEquals(apertura, guardada.resumen.apertura)
            assertNotNull(guardada.resumen.checklist)
            assertNotNull(guardada.resumen.ast)
            assertNotNull(guardada.resumen.encuesta)
            assertEquals("jefe", guardada.decision!!.decididoPor)
            assertEquals("supervisor", guardada.cierre!!.cerradoPor)
            assertEquals(25, guardada.cierre.duracionMinutos)
            assertEquals(EstadoFaena.CERRADA, repo.observarFaenas(jefe).first().single().estado)
            assertTrue(runCatching { repo.cerrar("f1", RegistroCierre(30, "Otro cierre", true), jefe) }.isFailure)
        } finally { db.close(); context.deleteDatabase(nombre) }
    }
}
