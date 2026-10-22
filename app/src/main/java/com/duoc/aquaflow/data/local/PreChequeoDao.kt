package com.duoc.aquaflow.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface PreChequeoDao {
    @Upsert
    suspend fun guardar(apertura: AperturaEntity)

    @Query("SELECT * FROM aperturas WHERE supervisorACargo = :supervisor ORDER BY actualizadoEn DESC, id DESC LIMIT 1")
    suspend fun ultimaApertura(supervisor: String): AperturaEntity?
}
