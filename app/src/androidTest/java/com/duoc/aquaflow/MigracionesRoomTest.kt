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
        for (version in 1..6) {
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
                        AquaFlowDatabase.MIGRACION_4_5,
                        AquaFlowDatabase.MIGRACION_5_6,
                        AquaFlowDatabase.MIGRACION_6_7,
                        AquaFlowDatabase.MIGRACION_7_8
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
                    assertNull(registro.inicioJson)
                    assertNull(registro.tareasJson)
                    assertNull(registro.cierreJson)
                    assertNull(registro.decisionJson)
                    assertEquals("", registro.centroId)
                    assertEquals("", registro.buzoId)
                    assertEquals(0, registro.edadBuzo)
                    assertNull(registro.examenPreocupacionalVigente)
                    assertEquals("[]", registro.intentosBloqueadosJson)
                } finally { db.close() }
            } finally { context.deleteDatabase(nombre) }
        }
    }

    @Test fun abreAmbosEsquemasDeVersionSieteSinPerderDatos() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val inicio = """{"iniciadoPor":"supervisor","iniciadoEn":1234}"""
        val intentos = """[{"accion":"AUTORIZADA"}]"""
        for (esquemaAnterior in listOf(true, false)) {
            val nombre = "migracion-v7-${UUID.randomUUID()}.db"
            val archivo = context.getDatabasePath(nombre)
            archivo.parentFile!!.mkdirs()
            try {
                val origen = if (esquemaAnterior) 6 else 7
                val schema = instrumentation.context.assets.open(
                    "com.duoc.aquaflow.data.local.AquaFlowDatabase/$origen.json"
                ).bufferedReader().use { JSONObject(it.readText()).getJSONObject("database") }
                val entidad = schema.getJSONArray("entities").getJSONObject(0)
                val sql = entidad.getString("createSql").replace("\${TABLE_NAME}", "aperturas")
                val firma = if (esquemaAnterior) "7a078c14ea431a7592b9df0304f54cf6" else schema.getString("identityHash")
                val columnasFicha = if (esquemaAnterior) "" else
                    ", centroId, buzoId, edadBuzo, imcBuzo, vigenciaExamenPreocupacional, vigenciaTestEsfuerzo, intentosBloqueadosJson, tareasJson"
                val valoresFicha = if (esquemaAnterior) "" else ", 'centro-1', 'buzo-1', 42, 24.5, '24/10/2027', '', ?, '[]'"
                SQLiteDatabase.openOrCreateDatabase(archivo, null).use { db ->
                    db.execSQL(if (esquemaAnterior) sql.replace(", `tareasJson` TEXT", "") else sql)
                    db.execSQL("CREATE TABLE room_master_table (id INTEGER PRIMARY KEY, identity_hash TEXT)")
                    db.execSQL("INSERT INTO room_master_table (id, identity_hash) VALUES (42, ?)", arrayOf(firma))
                    db.execSQL(
                        """
                        INSERT INTO aperturas (id, centroCultivo, fecha, hora, buzoAsignado,
                            supervisorACargo, actualizadoEn, condicionesListas, inicioJson$columnasFicha)
                        VALUES ('f1', 'Centro', '24/10/2026', '09:00', 'Ana',
                            'supervisor', 1, 1, ?$valoresFicha)
                        """.trimIndent(),
                        if (esquemaAnterior) arrayOf(inicio) else arrayOf(inicio, intentos)
                    )
                    db.version = 7
                }
                val db = Room.databaseBuilder(context, AquaFlowDatabase::class.java, nombre)
                    .addMigrations(AquaFlowDatabase.MIGRACION_7_8).build()
                try {
                        val registro = db.preChequeoDao().buscar("f1")!!
                        assertEquals("Ana", registro.buzoAsignado)
                        assertEquals(inicio, registro.inicioJson)
                        assertEquals(if (esquemaAnterior) "" else "centro-1", registro.centroId)
                        assertEquals(if (esquemaAnterior) 0 else 42, registro.edadBuzo)
                        assertEquals(if (esquemaAnterior) "[]" else intentos, registro.intentosBloqueadosJson)
                        assertEquals(if (esquemaAnterior) null else "[]", registro.tareasJson)
                } finally { db.close() }
            } finally { context.deleteDatabase(nombre) }
        }
    }
}
