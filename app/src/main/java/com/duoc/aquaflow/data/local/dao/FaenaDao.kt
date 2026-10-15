package com.duoc.aquaflow.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.duoc.aquaflow.model.Faena
import kotlinx.coroutines.flow.Flow

@Dao
interface FaenaDao {
    @Insert
    suspend fun insert(faena: Faena): Long

    /** Nunca se usa REPLACE sobre faenas: borraría en cascada todos sus formularios. */
    @Update
    suspend fun update(faena: Faena)

    @Query("SELECT * FROM faenas WHERE id = :id")
    suspend fun findById(id: Long): Faena?

    @Query("SELECT * FROM faenas WHERE id = :id")
    fun observeById(id: Long): Flow<Faena?>

    @Query("SELECT * FROM faenas ORDER BY creadoAt DESC, id DESC")
    fun observeAll(): Flow<List<Faena>>

    @Query("SELECT * FROM faenas WHERE supervisorId = :supervisorId ORDER BY creadoAt DESC, id DESC")
    fun observeBySupervisor(supervisorId: Long): Flow<List<Faena>>

    /** [estados] son nombres de [com.duoc.aquaflow.model.EstadoFaena]. */
    @Query("SELECT COUNT(*) FROM faenas WHERE buzoId = :buzoId AND estado IN (:estados)")
    suspend fun countByBuzoAndEstados(buzoId: Long, estados: List<String>): Int

    @Query("SELECT COUNT(*) FROM faenas WHERE estadoSync = 'PENDIENTE'")
    fun observePendientesSync(): Flow<Int>

    @Query("SELECT COUNT(*) FROM faenas")
    suspend fun count(): Int
}
