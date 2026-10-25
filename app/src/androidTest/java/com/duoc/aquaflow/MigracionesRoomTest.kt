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
        for (version in 1..4) {
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
                    val extras = if (version >= 3) ", condicionesListas, embarcacionLista, comunicacionesOperativas" else ""
                    val valores = if (version >= 3) ", 1, 1, 1" else ""
                    db.execSQL("INSERT INTO aperturas (id, centroCultivo, fecha, hora, buzoAsignado, supervisorACargo, actualizadoEn$extras) VALUES ('f1', 'Centro', '24/10/2026', '09:00', 'Ana', 'supervisor', 1$valores)")
                    db.version = version
                }
                val db = Room.databaseBuilder(context, AquaFlowDatabase::class.java, nombre)
                    .addMigrations(
                        AquaFlowDatabase.MIGRACION_1_2,
                        AquaFlowDatabase.MIGRACION_2_3,
                        AquaFlowDatabase.MIGRACION_3_4,
                        AquaFlowDatabase.MIGRACION_4_5
                    ).build()
                try {
                    val registro = db.preChequeoDao().buscar("f1")!!
                    assertEquals("Ana", registro.buzoAsignado)
                    assertEquals(version >= 3, registro.condicionesListas)
                    if (version >= 3) {
                        assertEquals(true, registro.embarcacionLista)
                        assertEquals(true, registro.comunicacionesOperativas)
                    } else {
                        assertNull(registro.embarcacionLista)
                        assertNull(registro.comunicacionesOperativas)
                    }
                    assertNull(registro.cierreJson)
                    assertNull(registro.decisionJson)
                } finally { db.close() }
            } finally { context.deleteDatabase(nombre) }
        }
    }
}
