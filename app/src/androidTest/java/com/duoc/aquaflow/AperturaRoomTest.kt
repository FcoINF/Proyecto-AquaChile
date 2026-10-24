package com.duoc.aquaflow

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.duoc.aquaflow.data.local.AquaFlowDatabase
import com.duoc.aquaflow.data.repository.PreChequeoRepository
import com.duoc.aquaflow.model.PreChequeo
import com.duoc.aquaflow.model.Rol
import com.duoc.aquaflow.model.Usuario
import com.duoc.aquaflow.data.local.AperturaEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.Assert.*
import java.util.UUID

class AperturaRoomTest {
    @Test fun historialYDetalleRespetanUsuarioYOrdenPersistido() = runBlocking {
        val contexto = InstrumentationRegistry.getInstrumentation().targetContext
        val db = Room.inMemoryDatabaseBuilder(contexto, AquaFlowDatabase::class.java).build()
        try {
            val primera = PreChequeo("1", "Centro norte", "22/10/2026", "09:00", "Ana", "Luis")
            val segunda = primera.copy(id = "2", supervisorACargo = "Pedro")
            val dao = db.preChequeoDao()
            dao.guardar(AperturaEntity.desde(primera, 100L))
            dao.guardar(AperturaEntity.desde(segunda, 200L))
            val repo = PreChequeoRepository(dao)
            val supervisor = Usuario("Luis", "1234", Rol.SUPERVISOR)
            val admin = Usuario("jefe", "1234", Rol.JEFE)
            assertEquals(listOf(segunda, primera), repo.observar(admin).first())
            assertEquals(listOf(primera), repo.observar(supervisor).first())
            assertEquals(segunda, repo.observarDetalle("2", admin).first())
            assertNull(repo.observarDetalle("2", supervisor).first())
            assertNull(repo.observarDetalle("inexistente", admin).first())
            dao.guardar(AperturaEntity.desde(primera.copy(centroCultivo = "Centro actualizado"), 300L))
            assertEquals("Centro actualizado", repo.observar(admin).first().first().centroCultivo)
        } finally {
            db.close()
        }
    }

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
