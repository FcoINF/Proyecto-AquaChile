package com.duoc.aquaflow

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.duoc.aquaflow.data.local.AquaFlowDatabase
import com.duoc.aquaflow.data.local.EvidenciasFotos
import com.duoc.aquaflow.data.repository.PreChequeoRepository
import com.duoc.aquaflow.model.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

class EvidenciasRoomTest {
    @Test fun conservaArchivoYReferenciaAlReabrirRoom() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val uri = EvidenciasFotos.crear(context)
        val nombre = "foto-${UUID.randomUUID()}.db"
        var db = Room.databaseBuilder(context, AquaFlowDatabase::class.java, nombre).build()
        try {
            assertFalse(EvidenciasFotos.existe(context, uri))
            context.contentResolver.openOutputStream(uri)!!.use {
                val bitmap = Bitmap.createBitmap(32, 32, Bitmap.Config.ARGB_8888)
                bitmap.eraseColor(android.graphics.Color.BLUE)
                assertTrue(bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it))
                bitmap.recycle()
            }
            val repo = PreChequeoRepository(db.preChequeoDao())
            repo.guardar(PreChequeo("f1", "Centro", "24/10/2026", "10:00", "Ana", "supervisor"))
            repo.guardarChecklist("f1", ChecklistFaena("f1", emptyList(), listOf(uri.toString())))
            db.close()
            db = Room.databaseBuilder(context, AquaFlowDatabase::class.java, nombre).build()
            val foto = PreChequeoRepository(db.preChequeoDao()).recuperarFaena("supervisor")!!.resumen.checklist!!.fotosEvidencia.single()
            assertEquals(uri, Uri.parse(foto))
            assertTrue(EvidenciasFotos.existe(context, Uri.parse(foto)))
            context.contentResolver.openInputStream(uri)!!.use {
                assertEquals(32, BitmapFactory.decodeStream(it)!!.width)
            }
        } finally {
            db.close()
            context.deleteDatabase(nombre)
            EvidenciasFotos.descartar(context, uri)
        }
    }
}
