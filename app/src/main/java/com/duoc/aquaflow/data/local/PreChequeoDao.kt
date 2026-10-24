package com.duoc.aquaflow.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface PreChequeoDao {
    @Upsert
    suspend fun guardar(apertura: AperturaEntity)

    @Query("SELECT * FROM aperturas WHERE supervisorACargo = :supervisor ORDER BY actualizadoEn DESC, id DESC LIMIT 1")
    suspend fun ultimaApertura(supervisor: String): AperturaEntity?

    // null permite consultar todas; un supervisor solo consulta sus aperturas.
    @Query("SELECT * FROM aperturas WHERE (:supervisor IS NULL OR supervisorACargo = :supervisor) ORDER BY actualizadoEn DESC, id DESC")
    fun observarAperturas(supervisor: String?): Flow<List<AperturaEntity>>

    @Query("SELECT * FROM aperturas WHERE id = :id AND (:supervisor IS NULL OR supervisorACargo = :supervisor)")
    fun observarApertura(id: String, supervisor: String?): Flow<AperturaEntity?>
}
