package com.duoc.aquaflow.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import com.duoc.aquaflow.domain.ReglasDecision
import com.duoc.aquaflow.model.RegistroDecision

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
            encuestaJson = previa.encuestaJson, tareasJson = previa.tareasJson
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

    @Query("UPDATE aperturas SET tareasJson = :json, actualizadoEn = :ahora WHERE id = :id AND decisionJson IS NULL")
    suspend fun guardarTareas(id: String, json: String, ahora: Long): Int

    @Transaction
    suspend fun iniciarValidada(id: String, usuario: com.duoc.aquaflow.model.Usuario) {
        val fila = checkNotNull(buscar(id)) { "La faena no existe." }
        val faena = fila.aFaena()
        check(com.duoc.aquaflow.domain.ReglasCierre.puedeIniciar(faena, usuario)) {
            "Solo el supervisor propietario puede iniciar una faena autorizada, una vez."
        }
        check(ReglasDecision.faltantes(faena.resumen).isEmpty()) { "El expediente está incompleto." }
        val ahora = System.currentTimeMillis()
        guardar(fila.copy(inicioJson = FaenaCodec.inicioATexto(com.duoc.aquaflow.model.RegistroInicio(usuario.nombre, ahora)), actualizadoEn = ahora))
    }

    @Transaction
    suspend fun cerrarValidada(id: String, cierre: com.duoc.aquaflow.model.RegistroCierre, usuario: com.duoc.aquaflow.model.Usuario) {
        val fila = checkNotNull(buscar(id)) { "La faena no existe." }
        check(com.duoc.aquaflow.domain.ReglasCierre.puedeCerrar(fila.aFaena(), usuario)) {
            "Solo el supervisor propietario puede cerrar una faena en ejecución, una vez."
        }
        val error = com.duoc.aquaflow.domain.ReglasCierre.errorCampos(cierre.duracionMinutos, cierre.observaciones, cierre.revisionSalida)
        require(error == null) { error.orEmpty() }
        require(com.duoc.aquaflow.domain.ReglasSecciones.encuesta(cierre.saludSalida) == com.duoc.aquaflow.model.EstadoSeccion.COMPLETA) {
            "Completa la encuesta de salud de salida."
        }
        val ahora = System.currentTimeMillis()
        val registro = cierre.copy(observaciones = cierre.observaciones.trim(), cerradoPor = usuario.nombre, cerradoEn = ahora)
        guardar(fila.copy(cierreJson = FaenaCodec.cierreATexto(registro), actualizadoEn = ahora))
    }

    @Transaction
    suspend fun guardarDecisionValidada(id: String, decision: RegistroDecision) {
        val fila = checkNotNull(buscar(id)) { "La faena no existe." }
        check(fila.decisionJson == null) { "La faena ya tiene una decisión." }
        val resultado = checkNotNull(ReglasDecision.resultado(fila.aFaena().resumen)) {
            "Completa las secciones antes de decidir."
        }
        val error = ReglasDecision.errorDecision(decision.decision, resultado, decision.motivo)
        require(error == null) { error.orEmpty() }
        check(guardarDecision(id, FaenaCodec.decisionATexto(decision.copy(motivo = decision.motivo.trim())), System.currentTimeMillis()) == 1) {
            "La faena no existe o ya tiene una decisión."
        }
    }

    // Una decisión ya tomada no se sobrescribe (queda como respaldo de auditoría).
    // Devuelve las filas cambiadas: 0 si la faena no existe o ya estaba decidida.
    @Query("UPDATE aperturas SET decisionJson = :json, actualizadoEn = :ahora WHERE id = :id AND decisionJson IS NULL")
    suspend fun guardarDecision(id: String, json: String, ahora: Long): Int
    @Query("UPDATE aperturas SET cierreJson = :json, actualizadoEn = :ahora WHERE id = :id AND cierreJson IS NULL AND decisionJson = :decisionEsperada")
    suspend fun guardarCierre(id: String, json: String, ahora: Long, decisionEsperada: String): Int
}
