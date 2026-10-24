package com.duoc.aquaflow.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [AperturaEntity::class], version = 3, exportSchema = true)
abstract class AquaFlowDatabase : RoomDatabase() {
    abstract fun preChequeoDao(): PreChequeoDao

    companion object {
        // v1 -> v2: agrega las secciones de la faena.
        val MIGRACION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `aperturas` (
                        `id` TEXT NOT NULL,
                        `centroCultivo` TEXT NOT NULL,
                        `fecha` TEXT NOT NULL,
                        `hora` TEXT NOT NULL,
                        `buzoAsignado` TEXT NOT NULL,
                        `supervisorACargo` TEXT NOT NULL,
                        `actualizadoEn` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                """.trimIndent())
                listOf("checklistJson", "astJson", "encuestaJson", "decisionJson").forEach { columna ->
                    try {
                        db.execSQL("ALTER TABLE `aperturas` ADD COLUMN `$columna` TEXT")
                    } catch (_: Exception) {
                        // Columna ya existe
                    }
                }
            }
        }

        // v2 -> v3: agrega las condiciones iniciales de la apertura.
        val MIGRACION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `aperturas` (
                        `id` TEXT NOT NULL,
                        `centroCultivo` TEXT NOT NULL,
                        `fecha` TEXT NOT NULL,
                        `hora` TEXT NOT NULL,
                        `buzoAsignado` TEXT NOT NULL,
                        `supervisorACargo` TEXT NOT NULL,
                        `actualizadoEn` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                """.trimIndent())
                listOf(
                    "condicionesListas INTEGER NOT NULL DEFAULT 0",
                    "embarcacionLista INTEGER NOT NULL DEFAULT 0",
                    "comunicacionesOperativas INTEGER NOT NULL DEFAULT 0"
                ).forEach { def ->
                    try {
                        db.execSQL("ALTER TABLE `aperturas` ADD COLUMN $def")
                    } catch (_: Exception) {
                        // Columna ya existe
                    }
                }
            }
        }

        @Volatile private var instancia: AquaFlowDatabase? = null

        fun obtener(context: Context): AquaFlowDatabase = instancia ?: synchronized(this) {
            instancia ?: Room.databaseBuilder(
                context.applicationContext, AquaFlowDatabase::class.java, "aquaflow.db"
            )
                .addMigrations(MIGRACION_1_2, MIGRACION_2_3)
                .fallbackToDestructiveMigration(true)
                .fallbackToDestructiveMigrationOnDowngrade(true)
                .build()
                .also { instancia = it }
        }
    }
}
