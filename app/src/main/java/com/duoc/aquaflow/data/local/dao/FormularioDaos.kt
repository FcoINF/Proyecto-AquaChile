package com.duoc.aquaflow.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.duoc.aquaflow.model.AntecedentesAst
import com.duoc.aquaflow.model.EncuestaSalud
import com.duoc.aquaflow.model.EventoFaena
import com.duoc.aquaflow.model.Evidencia
import com.duoc.aquaflow.model.RegistroPosterior
import com.duoc.aquaflow.model.RespuestaChecklist
import com.duoc.aquaflow.model.RespuestaFormulario
import kotlinx.coroutines.flow.Flow

@Dao
interface ChecklistDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(respuestas: List<RespuestaChecklist>)

    @Query("SELECT * FROM respuestas_checklist WHERE faenaId = :faenaId")
    fun observe(faenaId: Long): Flow<List<RespuestaChecklist>>

    @Query("SELECT * FROM respuestas_checklist WHERE faenaId = :faenaId")
    suspend fun get(faenaId: Long): List<RespuestaChecklist>
}

@Dao
interface AntecedentesAstDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(antecedentes: AntecedentesAst)

    @Query("SELECT * FROM antecedentes_ast WHERE faenaId = :faenaId")
    fun observe(faenaId: Long): Flow<AntecedentesAst?>

    @Query("SELECT * FROM antecedentes_ast WHERE faenaId = :faenaId")
    suspend fun get(faenaId: Long): AntecedentesAst?
}

@Dao
interface RespuestaFormularioDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(respuestas: List<RespuestaFormulario>)

    @Query("SELECT * FROM respuestas_formulario WHERE faenaId = :faenaId")
    fun observe(faenaId: Long): Flow<List<RespuestaFormulario>>

    @Query("SELECT * FROM respuestas_formulario WHERE faenaId = :faenaId")
    suspend fun get(faenaId: Long): List<RespuestaFormulario>
}

@Dao
interface EncuestaSaludDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(encuesta: EncuestaSalud)

    @Query("SELECT * FROM encuestas_salud WHERE faenaId = :faenaId")
    fun observe(faenaId: Long): Flow<EncuestaSalud?>

    @Query("SELECT * FROM encuestas_salud WHERE faenaId = :faenaId")
    suspend fun get(faenaId: Long): EncuestaSalud?
}

@Dao
interface EvidenciaDao {
    @Insert
    suspend fun insert(evidencia: Evidencia): Long

    @Query("DELETE FROM evidencias WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM evidencias WHERE id = :id")
    suspend fun findById(id: Long): Evidencia?

    @Query("SELECT * FROM evidencias WHERE faenaId = :faenaId ORDER BY creadoAt, id")
    fun observe(faenaId: Long): Flow<List<Evidencia>>

    @Query("SELECT * FROM evidencias WHERE faenaId = :faenaId ORDER BY creadoAt, id")
    suspend fun get(faenaId: Long): List<Evidencia>
}

@Dao
interface RegistroPosteriorDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(registro: RegistroPosterior)

    @Query("SELECT * FROM registros_posteriores WHERE faenaId = :faenaId")
    fun observe(faenaId: Long): Flow<RegistroPosterior?>

    @Query("SELECT * FROM registros_posteriores WHERE faenaId = :faenaId")
    suspend fun get(faenaId: Long): RegistroPosterior?

    @Query("SELECT * FROM registros_posteriores")
    fun observeAll(): Flow<List<RegistroPosterior>>
}

@Dao
interface EventoFaenaDao {
    @Insert
    suspend fun insert(evento: EventoFaena): Long

    @Query("SELECT * FROM eventos_faena WHERE faenaId = :faenaId ORDER BY `at`, id")
    fun observe(faenaId: Long): Flow<List<EventoFaena>>

    @Query("SELECT * FROM eventos_faena WHERE faenaId = :faenaId ORDER BY `at`, id")
    suspend fun get(faenaId: Long): List<EventoFaena>
}
