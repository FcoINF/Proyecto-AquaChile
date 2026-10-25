package com.duoc.aquaflow.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface PreChequeoDao {
    @Upsert
    suspend fun guardar(apertura: AperturaEntity)

    @Transaction
    suspend fun guardarBorrador(nueva: AperturaEntity) {
        val previa = buscar(nueva.id)
        check(previa?.decisionJson == null) { "La faena ya fue decidida; consulta su historial." }
        guardar(if (previa != null && previa.mismosDatosQue(nueva)) nueva.copy(
            checklistJson = previa.checklistJson, astJson = previa.astJson,
            encuestaJson = previa.encuestaJson
        ) else nueva)
    }

    @Query("SELECT * FROM aperturas WHERE id = :id")
    suspend fun buscar(id: String): AperturaEntity?

    @Query("SELECT * FROM aperturas WHERE supervisorACargo = :supervisor ORDER BY actualizadoEn DESC, id DESC LIMIT 1")
    suspend fun ultimaApertura(supervisor: String): AperturaEntity?

    // null permite consultar todas; un supervisor solo consulta sus aperturas.
    @Query("SELECT * FROM aperturas WHERE (:supervisor IS NULL OR supervisorACargo = :supervisor) ORDER BY actualizadoEn DESC, id DESC")
    fun observarAperturas(supervisor: String?): Flow<List<AperturaEntity>>

    @Query("SELECT * FROM aperturas WHERE id = :id AND (:supervisor IS NULL OR supervisorACargo = :supervisor)")
    fun observarApertura(id: String, supervisor: String?): Flow<AperturaEntity?>

    // Cada sección se actualiza por separado: guardar el checklist no pisa el AST ni la salud.
    @Query("UPDATE aperturas SET checklistJson = :json, actualizadoEn = :ahora WHERE id = :id AND decisionJson IS NULL")
    suspend fun guardarChecklist(id: String, json: String, ahora: Long): Int

    @Query("UPDATE aperturas SET astJson = :json, actualizadoEn = :ahora WHERE id = :id AND decisionJson IS NULL")
    suspend fun guardarAst(id: String, json: String, ahora: Long): Int

    @Query("UPDATE aperturas SET encuestaJson = :json, actualizadoEn = :ahora WHERE id = :id AND decisionJson IS NULL")
    suspend fun guardarEncuesta(id: String, json: String, ahora: Long): Int

    // Una decisión ya tomada no se sobrescribe (queda como respaldo de auditoría).
    // Devuelve las filas cambiadas: 0 si la faena no existe o ya estaba decidida.
    @Query("UPDATE aperturas SET decisionJson = :json, actualizadoEn = :ahora WHERE id = :id AND decisionJson IS NULL")
    suspend fun guardarDecision(id: String, json: String, ahora: Long): Int
}
