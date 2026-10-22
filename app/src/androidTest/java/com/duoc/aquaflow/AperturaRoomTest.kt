package com.duoc.aquaflow

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.duoc.aquaflow.data.local.AquaFlowDatabase
import com.duoc.aquaflow.data.repository.PreChequeoRepository
import com.duoc.aquaflow.model.PreChequeo
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.Assert.*
import java.util.UUID

class AperturaRoomTest {
    @Test fun conservaAperturaAlReabrirBaseYActualizaSinDuplicar() = runBlocking {
        val contexto = InstrumentationRegistry.getInstrumentation().targetContext
        val nombre = "prueba-apertura-${UUID.randomUUID()}.db"
        var db = Room.databaseBuilder(contexto, AquaFlowDatabase::class.java, nombre).build()
        try {
            val apertura = PreChequeo("id-1", "Centro", "22/10/2026", "09:30", "Buzo", "supervisor")
            PreChequeoRepository(db.preChequeoDao()).guardar(apertura)
            db.close()
            db = Room.databaseBuilder(contexto, AquaFlowDatabase::class.java, nombre).build()
            val repo = PreChequeoRepository(db.preChequeoDao())
            assertEquals(apertura, repo.recuperar("supervisor"))
            assertNull(repo.recuperar("otro"))
            val editada = apertura.copy(buzoAsignado = "Otro buzo")
            repo.guardar(editada)
            assertEquals(editada, repo.recuperar("supervisor"))
            db.openHelper.readableDatabase.query("SELECT COUNT(*) FROM aperturas").use {
                assertTrue(it.moveToFirst())
                assertEquals(1, it.getInt(0))
            }
        } finally {
            db.close()
            contexto.deleteDatabase(nombre)
        }
    }
}
