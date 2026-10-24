package com.duoc.aquaflow.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [AperturaEntity::class], version = 2, exportSchema = true)
abstract class AquaFlowDatabase : RoomDatabase() {
    abstract fun preChequeoDao(): PreChequeoDao

    companion object {
        // v1 -> v2: agrega las secciones de la faena. Las aperturas ya guardadas se conservan
        // y quedan con las secciones en null ("sin registrar").
        val MIGRACION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                listOf("checklistJson", "astJson", "encuestaJson", "decisionJson").forEach { columna ->
                    db.execSQL("ALTER TABLE aperturas ADD COLUMN $columna TEXT")
                }
            }
        }

        @Volatile private var instancia: AquaFlowDatabase? = null

        fun obtener(context: Context): AquaFlowDatabase = instancia ?: synchronized(this) {
            instancia ?: Room.databaseBuilder(
                context.applicationContext, AquaFlowDatabase::class.java, "aquaflow.db"
            ).addMigrations(MIGRACION_1_2).build().also { instancia = it }
        }
    }
}
