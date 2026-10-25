package com.duoc.aquaflow

import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.duoc.aquaflow.data.local.AquaFlowDatabase
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

class MigracionesRoomTest {
    @Test fun conservaRegistrosDesdeCadaVersionPublicada() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        for (version in 1..2) {
            val nombre = "migracion-${UUID.randomUUID()}.db"
            val archivo = context.getDatabasePath(nombre)
            archivo.parentFile!!.mkdirs()
            try {
                val schema = instrumentation.context.assets.open(
                    "com.duoc.aquaflow.data.local.AquaFlowDatabase/$version.json"
                ).bufferedReader().use { JSONObject(it.readText()).getJSONObject("database") }
                SQLiteDatabase.openOrCreateDatabase(archivo, null).use { db ->
                    val entidades = schema.getJSONArray("entities")
                    for (i in 0 until entidades.length()) {
                        val entidad = entidades.getJSONObject(i)
                        db.execSQL(entidad.getString("createSql").replace("\${TABLE_NAME}", entidad.getString("tableName")))
                    }
                    db.execSQL("INSERT INTO aperturas (id, centroCultivo, fecha, hora, buzoAsignado, supervisorACargo, actualizadoEn) VALUES ('f1', 'Centro', '24/10/2026', '09:00', 'Ana', 'supervisor', 1)")
                    db.version = version
                }
                val db = Room.databaseBuilder(context, AquaFlowDatabase::class.java, nombre)
                    .addMigrations(AquaFlowDatabase.MIGRACION_1_2, AquaFlowDatabase.MIGRACION_2_3).build()
                try {
                    val registro = db.preChequeoDao().buscar("f1")!!
                    assertEquals("Ana", registro.buzoAsignado)
                    assertFalse(registro.condicionesListas)
                    assertNull(registro.decisionJson)
                } finally { db.close() }
            } finally { context.deleteDatabase(nombre) }
        }
    }
}
