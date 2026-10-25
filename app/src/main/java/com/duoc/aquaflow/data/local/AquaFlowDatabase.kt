package com.duoc.aquaflow.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [AperturaEntity::class], version = 4, exportSchema = true)
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

        @Volatile private var instancia: AquaFlowDatabase? = null

        fun obtener(context: Context): AquaFlowDatabase = instancia ?: synchronized(this) {
            instancia ?: Room.databaseBuilder(
                context.applicationContext, AquaFlowDatabase::class.java, "aquaflow.db"
            )
                .addMigrations(MIGRACION_1_2, MIGRACION_2_3, MIGRACION_3_4)
                .build()
                .also { instancia = it }
        }
    }
}
