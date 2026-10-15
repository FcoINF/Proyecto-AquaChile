package com.duoc.aquaflow.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.duoc.aquaflow.model.Buzo
import com.duoc.aquaflow.model.CentroCultivo
import kotlinx.coroutines.flow.Flow

@Dao
interface CentroDao {
    @Insert
    suspend fun insertAll(centros: List<CentroCultivo>)

    @Query("SELECT * FROM centros WHERE activo = 1 ORDER BY nombre COLLATE NOCASE")
    fun observeActivos(): Flow<List<CentroCultivo>>

    @Query("SELECT * FROM centros WHERE id = :id")
    suspend fun findById(id: Long): CentroCultivo?

    @Query("SELECT COUNT(*) FROM centros")
    suspend fun count(): Int
}

@Dao
interface BuzoDao {
    @Insert
    suspend fun insertAll(buzos: List<Buzo>)

    @Query("SELECT * FROM buzos WHERE activo = 1 ORDER BY nombre COLLATE NOCASE")
    fun observeActivos(): Flow<List<Buzo>>

    @Query("SELECT * FROM buzos WHERE id = :id")
    suspend fun findById(id: Long): Buzo?

    @Query("SELECT COUNT(*) FROM buzos")
    suspend fun count(): Int
}
