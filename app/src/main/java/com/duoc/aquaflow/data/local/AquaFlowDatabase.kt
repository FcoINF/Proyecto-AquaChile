package com.duoc.aquaflow.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [AperturaEntity::class], version = 1, exportSchema = true)
abstract class AquaFlowDatabase : RoomDatabase() {
    abstract fun preChequeoDao(): PreChequeoDao

    companion object {
        @Volatile private var instancia: AquaFlowDatabase? = null

        fun obtener(context: Context): AquaFlowDatabase = instancia ?: synchronized(this) {
            instancia ?: Room.databaseBuilder(
                context.applicationContext, AquaFlowDatabase::class.java, "aquaflow.db"
            ).build().also { instancia = it }
        }
    }
}
