package com.duoc.aquaflow.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [AperturaEntity::class], version = 6, exportSchema = true)
abstract class AquaFlowDatabase : RoomDatabase() {
    abstract fun preChequeoDao(): PreChequeoDao

    companion object {
        // v1 -> v2: agrega las secciones de la faena.
        val MIGRACION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                listOf("checklistJson", "astJson", "encuestaJson", "decisionJson").forEach {
                    db.execSQL("ALTER TABLE aperturas ADD COLUMN $it TEXT")
                }
            }
        }

        val MIGRACION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                listOf("condicionesListas", "embarcacionLista", "comunicacionesOperativas").forEach {
                    db.execSQL("ALTER TABLE aperturas ADD COLUMN $it INTEGER NOT NULL DEFAULT 0")
                }
            }
        }

        val MIGRACION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE aperturas ADD COLUMN cierreJson TEXT")
            }
        }

        // v4 -> v5: distingue "sin responder" (NULL) de "No" (0) en las condiciones iniciales.
        // Los valores antiguos en 0 no permiten saber si hubo una respuesta, así que se migran a NULL.
        val MIGRACION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `aperturas` RENAME TO `aperturas_v4`")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `aperturas` (
                        `id` TEXT NOT NULL,
                        `centroCultivo` TEXT NOT NULL,
                        `fecha` TEXT NOT NULL,
                        `hora` TEXT NOT NULL,
                        `buzoAsignado` TEXT NOT NULL,
                        `supervisorACargo` TEXT NOT NULL,
                        `condicionesListas` INTEGER NOT NULL,
                        `embarcacionLista` INTEGER,
                        `comunicacionesOperativas` INTEGER,
                        `actualizadoEn` INTEGER NOT NULL,
                        `checklistJson` TEXT,
                        `astJson` TEXT,
                        `encuestaJson` TEXT,
                        `decisionJson` TEXT,
                        `cierreJson` TEXT,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO `aperturas` (
                        `id`, `centroCultivo`, `fecha`, `hora`, `buzoAsignado`, `supervisorACargo`,
                        `condicionesListas`, `embarcacionLista`, `comunicacionesOperativas`, `actualizadoEn`,
                        `checklistJson`, `astJson`, `encuestaJson`, `decisionJson`, `cierreJson`
                    )
                    SELECT
                        `id`, `centroCultivo`, `fecha`, `hora`, `buzoAsignado`, `supervisorACargo`,
                        `condicionesListas`,
                        CASE WHEN `embarcacionLista` = 1 THEN 1 ELSE NULL END,
                        CASE WHEN `comunicacionesOperativas` = 1 THEN 1 ELSE NULL END,
                        `actualizadoEn`, `checklistJson`, `astJson`, `encuestaJson`, `decisionJson`, `cierreJson`
                    FROM `aperturas_v4`
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE `aperturas_v4`")
            }
        }

        val MIGRACION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE aperturas ADD COLUMN tareasJson TEXT")
                db.execSQL("ALTER TABLE aperturas ADD COLUMN inicioJson TEXT")
            }
        }

        @Volatile private var instancia: AquaFlowDatabase? = null

        fun obtener(context: Context): AquaFlowDatabase = instancia ?: synchronized(this) {
            instancia ?: Room.databaseBuilder(
                context.applicationContext, AquaFlowDatabase::class.java, "aquaflow.db"
            )
                .addMigrations(MIGRACION_1_2, MIGRACION_2_3, MIGRACION_3_4, MIGRACION_4_5, MIGRACION_5_6)
                .build()
                .also { instancia = it }
        }
    }
}
