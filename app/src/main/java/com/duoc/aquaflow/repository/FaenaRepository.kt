package com.duoc.aquaflow.repository

import com.duoc.aquaflow.domain.AppResult
import com.duoc.aquaflow.model.AntecedentesAst
import com.duoc.aquaflow.model.EncuestaSalud
import com.duoc.aquaflow.model.Evidencia
import com.duoc.aquaflow.model.Faena
import com.duoc.aquaflow.model.FaenaDetalle
import com.duoc.aquaflow.model.RegistroPosterior
import com.duoc.aquaflow.model.RespuestaChecklist
import com.duoc.aquaflow.model.TipoEvidencia
import com.duoc.aquaflow.model.Usuario
import kotlinx.coroutines.flow.Flow

/**
 * Operaciones sobre faenas. Todas reciben al usuario que actúa y la implementación vuelve a
 * comprobar permisos, estado y reglas de negocio (no se confía en lo que muestre la interfaz).
 */
interface FaenaRepository {

    /** Administrador: todas las faenas. Supervisor: solo las suyas. */
    fun observarFaenas(actor: Usuario): Flow<List<Faena>>

    /** Emite null si la faena no existe o el usuario no puede verla. */
    fun observarDetalle(actor: Usuario, faenaId: Long): Flow<FaenaDetalle?>

    fun observarPendientesSync(): Flow<Int>

    /** Todos los registros posteriores indexados por faenaId (para el historial consolidado). */
    fun observarRegistros(): Flow<Map<Long, RegistroPosterior>>

    suspend fun crearFaena(actor: Usuario, centroId: Long?, buzoId: Long?, fechaInicio: Long): AppResult<Long>

    suspend fun guardarChecklist(actor: Usuario, faenaId: Long, respuestas: List<RespuestaChecklist>): AppResult<Unit>

    suspend fun guardarAst(
        actor: Usuario,
        faenaId: Long,
        antecedentes: AntecedentesAst,
        gating: Map<String, Boolean>,
        riesgos: Map<String, Boolean>
    ): AppResult<Unit>

    suspend fun guardarSalud(
        actor: Usuario,
        faenaId: Long,
        encuesta: EncuestaSalud,
        declaraciones: Map<String, Boolean>
    ): AppResult<Unit>

    suspend fun agregarEvidencia(
        actor: Usuario,
        faenaId: Long,
        tipo: TipoEvidencia,
        itemId: String?,
        rutaRelativa: String
    ): AppResult<Evidencia>

    suspend fun eliminarEvidencia(actor: Usuario, evidenciaId: Long): AppResult<Unit>

    /** Firma del supervisor: PRECHEQUEO -> PENDIENTE_AUTORIZACION. */
    suspend fun enviarARevision(actor: Usuario, faenaId: Long): AppResult<Unit>

    /** PENDIENTE_AUTORIZACION -> AUTORIZADA (solo administrador y sin bloqueos de seguridad). */
    suspend fun autorizar(actor: Usuario, faenaId: Long): AppResult<Unit>

    /** PENDIENTE_AUTORIZACION -> RECHAZADA con motivo obligatorio. */
    suspend fun rechazar(actor: Usuario, faenaId: Long, motivo: String): AppResult<Unit>

    /** PRECHEQUEO -> CANCELADA con motivo obligatorio. */
    suspend fun cancelar(actor: Usuario, faenaId: Long, motivo: String): AppResult<Unit>

    /** AUTORIZADA -> CERRADA: guarda el registro posterior a la inmersión. */
    suspend fun registrarPosterior(
        actor: Usuario,
        faenaId: Long,
        tiempoInmersionMin: Int?,
        profundidadMaxM: Double?,
        presionBancoBar: Int?,
        sinDolorArticular: Boolean?,
        respiracionNormal: Boolean?,
        observaciones: String
    ): AppResult<Unit>
}
