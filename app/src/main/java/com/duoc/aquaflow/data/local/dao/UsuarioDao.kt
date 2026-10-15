package com.duoc.aquaflow.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.duoc.aquaflow.model.Usuario
import kotlinx.coroutines.flow.Flow

@Dao
interface UsuarioDao {
    @Insert
    suspend fun insert(usuario: Usuario): Long

    @Insert
    suspend fun insertAll(usuarios: List<Usuario>)

    @Update
    suspend fun update(usuario: Usuario)

    @Query("SELECT * FROM usuarios WHERE id = :id")
    suspend fun findById(id: Long): Usuario?

    @Query("SELECT * FROM usuarios WHERE rut = :rut LIMIT 1")
    suspend fun findByRut(rut: String): Usuario?

    @Query("SELECT * FROM usuarios ORDER BY nombre COLLATE NOCASE")
    fun observeAll(): Flow<List<Usuario>>

    @Query("SELECT COUNT(*) FROM usuarios")
    suspend fun count(): Int
}
