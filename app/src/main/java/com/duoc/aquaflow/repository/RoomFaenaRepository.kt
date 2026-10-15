package com.duoc.aquaflow.repository

import androidx.room.withTransaction
import com.duoc.aquaflow.data.local.AquaFlowDatabase
import com.duoc.aquaflow.domain.AppError
import com.duoc.aquaflow.domain.AppResult
import com.duoc.aquaflow.domain.Fechas
import com.duoc.aquaflow.domain.catalog.Catalogos
import com.duoc.aquaflow.domain.config.ReglasConfig
import com.duoc.aquaflow.domain.fail
import com.duoc.aquaflow.domain.rules.AccessPolicy
import com.duoc.aquaflow.domain.rules.FaenaEvaluator
import com.duoc.aquaflow.domain.rules.FaenaStateMachine
import com.duoc.aquaflow.domain.rules.Validadores
import com.duoc.aquaflow.domain.runCatchingApp
import com.duoc.aquaflow.model.AntecedentesAst
import com.duoc.aquaflow.model.EncuestaSalud
import com.duoc.aquaflow.model.EstadoFaena
import com.duoc.aquaflow.model.EstadoSync
import com.duoc.aquaflow.model.EventoFaena
import com.duoc.aquaflow.model.Evidencia
import com.duoc.aquaflow.model.Faena
import com.duoc.aquaflow.model.FaenaDetalle
import com.duoc.aquaflow.model.RegistroPosterior
import com.duoc.aquaflow.model.RespuestaChecklist
import com.duoc.aquaflow.model.RespuestaFormulario
import com.duoc.aquaflow.model.Rol
import com.duoc.aquaflow.model.TipoEvento
import com.duoc.aquaflow.model.TipoEvidencia
import com.duoc.aquaflow.model.TipoFormulario
import com.duoc.aquaflow.model.Usuario
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.util.Locale

class RoomFaenaRepository(
    private val db: AquaFlowDatabase,
    private val archivos: EvidenciaArchivos,
    private val config: ReglasConfig = ReglasConfig(),
    private val ahora: () -> Long = { System.currentTimeMillis() }
) : FaenaRepository {

    // ---------------------------------------------------------------- Lectura

    override fun observarFaenas(actor: Usuario): Flow<List<Faena>> =
        if (actor.rol == Rol.ADMINISTRADOR) {
            db.faenaDao().observeAll()
        } else {
            db.faenaDao().observeBySupervisor(actor.id)
        }

    override fun observarPendientesSync(): Flow<Int> = db.faenaDao().observePendientesSync()

    override fun observarRegistros(): Flow<Map<Long, RegistroPosterior>> =
        db.registroPosteriorDao().observeAll().map { lista -> lista.associateBy { it.faenaId } }

    private class ParteFormularios(
        val faena: Faena?,
        val checklist: List<RespuestaChecklist>,
        val ast: AntecedentesAst?,
        val respuestas: List<RespuestaFormulario>,
        val salud: EncuestaSalud?
    )

    private class ParteRegistros(
        val evidencias: List<Evidencia>,
        val registro: RegistroPosterior?,
        val eventos: List<EventoFaena>
    )

    override fun observarDetalle(actor: Usuario, faenaId: Long): Flow<FaenaDetalle?> {
        val formularios = combine(
            db.faenaDao().observeById(faenaId),
            db.checklistDao().observe(faenaId),
            db.antecedentesAstDao().observe(faenaId),
            db.respuestaFormularioDao().observe(faenaId),
            db.encuestaSaludDao().observe(faenaId)
        ) { faena, checklist, ast, respuestas, salud ->
            ParteFormularios(faena, checklist, ast, respuestas, salud)
        }
        val registros = combine(
            db.evidenciaDao().observe(faenaId),
            db.registroPosteriorDao().observe(faenaId),
            db.eventoFaenaDao().observe(faenaId)
        ) { evidencias, registro, eventos ->
            ParteRegistros(evidencias, registro, eventos)
        }
        return combine(formularios, registros) { f, r ->
            val faena = f.faena
            if (faena == null || !AccessPolicy.puedeVer(actor, faena)) {
                null
            } else {
                FaenaDetalle(
                    faena = faena,
                    checklist = f.checklist,
                    antecedentesAst = f.ast,
                    respuestas = f.respuestas,
                    encuestaSalud = f.salud,
                    evidencias = r.evidencias,
                    registro = r.registro,
                    eventos = r.eventos
                )
            }
        }
    }

    private suspend fun cargarDetalle(faena: Faena): FaenaDetalle = FaenaDetalle(
        faena = faena,
        checklist = db.checklistDao().get(faena.id),
        antecedentesAst = db.antecedentesAstDao().get(faena.id),
        respuestas = db.respuestaFormularioDao().get(faena.id),
        encuestaSalud = db.encuestaSaludDao().get(faena.id),
        evidencias = db.evidenciaDao().get(faena.id),
        registro = db.registroPosteriorDao().get(faena.id),
        eventos = db.eventoFaenaDao().get(faena.id)
    )

    // ---------------------------------------------------------------- Infraestructura de escritura

    private suspend fun actorActivo(actor: Usuario): Usuario? =
        db.usuarioDao().findById(actor.id)?.takeIf { it.activo }

    /**
     * Ejecuta [bloque] en una transacción tras comprobar que el usuario existe, está activo,
     * puede ver la faena y cumple [permiso]. Cualquier violación devuelve un error sin tocar datos.
     */
    private suspend fun <T> sobreFaena(
        actor: Usuario,
        faenaId: Long,
        permiso: (Usuario, Faena) -> Boolean,
        bloque: suspend (Usuario, Faena) -> AppResult<T>
    ): AppResult<T> = runCatchingApp {
        val usuario = actorActivo(actor) ?: return@runCatchingApp fail(AppError.Forbidden)
        db.withTransaction<AppResult<T>> {
            val faena = db.faenaDao().findById(faenaId)
            if (faena == null || !AccessPolicy.puedeVer(usuario, faena)) {
                fail(AppError.NotFound)
            } else if (!permiso(usuario, faena)) {
                fail(AppError.Forbidden)
            } else {
                bloque(usuario, faena)
            }
        }
    }

    /** Actualiza la faena (marcándola pendiente de sincronizar) y deja constancia del evento. */
    private suspend fun registrar(
        usuario: Usuario,
        faena: Faena,
        tipo: TipoEvento,
        detalle: String = "",
        cambios: (Faena) -> Faena = { it }
    ) {
        val ts = ahora()
        db.faenaDao().update(cambios(faena).copy(actualizadoAt = ts, estadoSync = EstadoSync.PENDIENTE))
        db.eventoFaenaDao().insert(
            EventoFaena(
                faenaId = faena.id,
                tipo = tipo,
                detalle = detalle,
                actorId = usuario.id,
                actorNombre = usuario.nombre,
                at = ts
            )
        )
    }

    // ---------------------------------------------------------------- Apertura

    override suspend fun crearFaena(
        actor: Usuario,
        centroId: Long?,
        buzoId: Long?,
        fechaInicio: Long
    ): AppResult<Long> = runCatchingApp {
        val usuario = actorActivo(actor)
        if (usuario == null || !AccessPolicy.puedeCrearFaena(usuario)) return@runCatchingApp fail(AppError.Forbidden)

        db.withTransaction<AppResult<Long>> {
            val centro = centroId?.let { db.centroDao().findById(it) }?.takeIf { it.activo }
            val buzo = buzoId?.let { db.buzoDao().findById(it) }?.takeIf { it.activo }
            val ts = ahora()

            val errores = Validadores.apertura(centro?.id, buzo, fechaInicio, ts).toMutableMap()
            if (buzo != null && !errores.containsKey("buzo")) {
                val activas = db.faenaDao().countByBuzoAndEstados(buzo.id, ESTADOS_ACTIVOS)
                if (activas > 0) errores["buzo"] = "${buzo.nombre} ya tiene una faena activa."
            }
            if (errores.isNotEmpty() || centro == null || buzo == null) {
                fail(AppError.Validation(errores))
            } else {
                val id = db.faenaDao().insert(
                    Faena(
                        codigo = "",
                        centroId = centro.id,
                        centroNombre = centro.nombre,
                        buzoId = buzo.id,
                        buzoNombre = buzo.nombre,
                        supervisorId = usuario.id,
                        supervisorNombre = usuario.nombre,
                        fechaInicio = fechaInicio,
                        estado = EstadoFaena.PRECHEQUEO,
                        creadoAt = ts,
                        actualizadoAt = ts
                    )
                )
                val anio = Fechas.aLocalDate(ts).year
                val codigo = String.format(Locale.ROOT, "F-%d-%04d", anio, id)
                val creada = db.faenaDao().findById(id)!!.copy(codigo = codigo)
                registrar(usuario, creada, TipoEvento.APERTURA, "${centro.nombre} · ${buzo.nombre}")
                AppResult.Ok(id)
            }
        }
    }

    // ---------------------------------------------------------------- Formularios

    override suspend fun guardarChecklist(
        actor: Usuario,
        faenaId: Long,
        respuestas: List<RespuestaChecklist>
    ): AppResult<Unit> = sobreFaena(actor, faenaId, AccessPolicy::puedeEditarPrechequeo) { usuario, faena ->
        val errores = mutableMapOf<String, String>()
        respuestas.forEach { r ->
            if (Catalogos.equipo(r.itemId) == null) errores[r.itemId] = "Equipo desconocido."
            if (r.nota.length > MAX_NOTA) errores[r.itemId] = "La nota admite máximo $MAX_NOTA caracteres."
        }
        if (errores.isNotEmpty()) {
            fail(AppError.Validation(errores))
        } else {
            db.checklistDao().upsertAll(respuestas.map { it.copy(faenaId = faena.id, nota = it.nota.trim()) })
            registrar(usuario, faena, TipoEvento.CHECKLIST_GUARDADO)
            AppResult.Ok(Unit)
        }
    }

    override suspend fun guardarAst(
        actor: Usuario,
        faenaId: Long,
        antecedentes: AntecedentesAst,
        gating: Map<String, Boolean>,
        riesgos: Map<String, Boolean>
    ): AppResult<Unit> = sobreFaena(actor, faenaId, AccessPolicy::puedeEditarPrechequeo) { usuario, faena ->
        val errores = Validadores.ast(antecedentes, config).toMutableMap()
        if (gating.keys.any { Catalogos.gating(it) == null } || riesgos.keys.any { Catalogos.riesgo(it) == null }) {
            errores["formulario"] = "El formulario contiene preguntas desconocidas."
        }
        if (errores.isNotEmpty()) {
            fail(AppError.Validation(errores))
        } else {
            val ts = ahora()
            db.antecedentesAstDao().upsert(
                antecedentes.copy(
                    faenaId = faena.id,
                    tipologia = antecedentes.tipologia.trim(),
                    tarea = antecedentes.tarea.trim(),
                    pasos = antecedentes.pasos.trim(),
                    actualizadoAt = ts
                )
            )
            val filas = gating.map { (clave, valor) ->
                RespuestaFormulario(faena.id, TipoFormulario.AST_GATING, clave, valor)
            } + riesgos.map { (clave, valor) ->
                RespuestaFormulario(faena.id, TipoFormulario.AST_RIESGO, clave, valor)
            }
            db.respuestaFormularioDao().upsertAll(filas)
            registrar(usuario, faena, TipoEvento.AST_GUARDADO)
            AppResult.Ok(Unit)
        }
    }

    override suspend fun guardarSalud(
        actor: Usuario,
        faenaId: Long,
        encuesta: EncuestaSalud,
        declaraciones: Map<String, Boolean>
    ): AppResult<Unit> = sobreFaena(actor, faenaId, AccessPolicy::puedeEditarPrechequeo) { usuario, faena ->
        val errores = Validadores.encuestaSalud(encuesta, config).toMutableMap()
        if (declaraciones.keys.any { Catalogos.preguntaSalud(it) == null }) {
            errores["formulario"] = "El formulario contiene preguntas desconocidas."
        }
        if (errores.isNotEmpty()) {
            fail(AppError.Validation(errores))
        } else {
            db.encuestaSaludDao().upsert(
                encuesta.copy(faenaId = faena.id, observaciones = encuesta.observaciones.trim(), actualizadoAt = ahora())
            )
            db.respuestaFormularioDao().upsertAll(
                declaraciones.map { (clave, valor) ->
                    RespuestaFormulario(faena.id, TipoFormulario.SALUD_DECLARACION, clave, valor)
                }
            )
            registrar(usuario, faena, TipoEvento.SALUD_GUARDADA)
            AppResult.Ok(Unit)
        }
    }

    // ---------------------------------------------------------------- Evidencias

    override suspend fun agregarEvidencia(
        actor: Usuario,
        faenaId: Long,
        tipo: TipoEvidencia,
        itemId: String?,
        rutaRelativa: String
    ): AppResult<Evidencia> = sobreFaena(actor, faenaId, AccessPolicy::puedeEditarPrechequeo) { usuario, faena ->
        val itemValido = if (tipo == TipoEvidencia.EQUIPO) itemId != null && Catalogos.equipo(itemId) != null else itemId == null
        if (!itemValido || rutaRelativa.isBlank()) {
            fail(AppError.Validation(mapOf("evidencia" to "La evidencia no es válida.")))
        } else {
            val ts = ahora()
            val evidencia = Evidencia(faenaId = faena.id, tipo = tipo, itemId = itemId, ruta = rutaRelativa, creadoAt = ts)
            val id = db.evidenciaDao().insert(evidencia)
            registrar(usuario, faena, TipoEvento.EVIDENCIA_AGREGADA, tipo.etiqueta)
            AppResult.Ok(evidencia.copy(id = id))
        }
    }

    override suspend fun eliminarEvidencia(actor: Usuario, evidenciaId: Long): AppResult<Unit> {
        val evidencia = db.evidenciaDao().findById(evidenciaId) ?: return fail(AppError.NotFound)
        val resultado = sobreFaena(actor, evidencia.faenaId, AccessPolicy::puedeEditarPrechequeo) { usuario, faena ->
            db.evidenciaDao().deleteById(evidenciaId)
            registrar(usuario, faena, TipoEvento.EVIDENCIA_ELIMINADA, evidencia.tipo.etiqueta)
            AppResult.Ok(Unit)
        }
        if (resultado is AppResult.Ok) archivos.eliminar(evidencia.ruta)
        return resultado
    }

    // ---------------------------------------------------------------- Transiciones de estado

    override suspend fun enviarARevision(actor: Usuario, faenaId: Long): AppResult<Unit> =
        sobreFaena(actor, faenaId, AccessPolicy::puedeEditarPrechequeo) { usuario, faena ->
            val errorEstado = FaenaStateMachine.validar(faena.estado, EstadoFaena.PENDIENTE_AUTORIZACION)
            val evaluacion = FaenaEvaluator.evaluar(cargarDetalle(faena), config)
            if (errorEstado != null) {
                fail(errorEstado)
            } else if (!evaluacion.puedeEnviarARevision) {
                fail(AppError.Blocked(listOf("Completa la información antes de firmar y enviar:") + evaluacion.faltantes))
            } else {
                val ts = ahora()
                registrar(usuario, faena, TipoEvento.ENVIADA_A_REVISION) {
                    it.copy(estado = EstadoFaena.PENDIENTE_AUTORIZACION, supervisorFirmaAt = ts)
                }
                AppResult.Ok(Unit)
            }
        }

    override suspend fun autorizar(actor: Usuario, faenaId: Long): AppResult<Unit> =
        sobreFaena(actor, faenaId, AccessPolicy::puedeDecidir) { usuario, faena ->
            val errorEstado = FaenaStateMachine.validar(faena.estado, EstadoFaena.AUTORIZADA)
            val evaluacion = FaenaEvaluator.evaluar(cargarDetalle(faena), config)
            if (errorEstado != null) {
                fail(errorEstado)
            } else if (!evaluacion.puedeAutorizar) {
                val motivos = evaluacion.bloqueos + evaluacion.faltantes
                fail(AppError.Blocked(listOf("No se puede autorizar la faena:") + motivos))
            } else {
                val ts = ahora()
                registrar(usuario, faena, TipoEvento.AUTORIZADA) {
                    it.copy(
                        estado = EstadoFaena.AUTORIZADA,
                        decisionPorId = usuario.id,
                        decisionPorNombre = usuario.nombre,
                        decisionAt = ts,
                        decisionMotivo = null
                    )
                }
                AppResult.Ok(Unit)
            }
        }

    override suspend fun rechazar(actor: Usuario, faenaId: Long, motivo: String): AppResult<Unit> =
        sobreFaena(actor, faenaId, AccessPolicy::puedeDecidir) { usuario, faena ->
            cerrarSinInmersion(usuario, faena, EstadoFaena.RECHAZADA, TipoEvento.RECHAZADA, motivo)
        }

    override suspend fun cancelar(actor: Usuario, faenaId: Long, motivo: String): AppResult<Unit> =
        sobreFaena(actor, faenaId, AccessPolicy::puedeEditarPrechequeo) { usuario, faena ->
            cerrarSinInmersion(usuario, faena, EstadoFaena.CANCELADA, TipoEvento.CANCELADA, motivo)
        }

    private suspend fun cerrarSinInmersion(
        usuario: Usuario,
        faena: Faena,
        destino: EstadoFaena,
        evento: TipoEvento,
        motivo: String
    ): AppResult<Unit> {
        val errorEstado = FaenaStateMachine.validar(faena.estado, destino)
        val errores = Validadores.motivo(motivo, config)
        if (errorEstado != null) return fail(errorEstado)
        if (errores.isNotEmpty()) return fail(AppError.Validation(errores))
        val ts = ahora()
        val texto = motivo.trim()
        registrar(usuario, faena, evento, texto) {
            it.copy(
                estado = destino,
                decisionPorId = usuario.id,
                decisionPorNombre = usuario.nombre,
                decisionAt = ts,
                decisionMotivo = texto
            )
        }
        return AppResult.Ok(Unit)
    }

    override suspend fun registrarPosterior(
        actor: Usuario,
        faenaId: Long,
        tiempoInmersionMin: Int?,
        profundidadMaxM: Double?,
        presionBancoBar: Int?,
        sinDolorArticular: Boolean?,
        respiracionNormal: Boolean?,
        observaciones: String
    ): AppResult<Unit> = sobreFaena(actor, faenaId, AccessPolicy::puedeRegistrarPosterior) { usuario, faena ->
        val errorEstado = FaenaStateMachine.validar(faena.estado, EstadoFaena.CERRADA)
        val errores = Validadores.registroPosterior(tiempoInmersionMin, profundidadMaxM, presionBancoBar, config).toMutableMap()
        if (sinDolorArticular == null) errores["sinDolor"] = "Responde el chequeo médico de salida."
        if (respiracionNormal == null) errores["respiracion"] = "Responde el chequeo médico de salida."
        if (observaciones.length > 500) errores["observaciones"] = "Máximo 500 caracteres."
        if (errorEstado != null) {
            fail(errorEstado)
        } else if (errores.isNotEmpty() || tiempoInmersionMin == null || profundidadMaxM == null ||
            presionBancoBar == null || sinDolorArticular == null || respiracionNormal == null
        ) {
            fail(AppError.Validation(errores))
        } else {
            val ts = ahora()
            db.registroPosteriorDao().upsert(
                RegistroPosterior(
                    faenaId = faena.id,
                    tiempoInmersionMin = tiempoInmersionMin,
                    profundidadMaxM = profundidadMaxM,
                    presionBancoBar = presionBancoBar,
                    sinDolorArticular = sinDolorArticular,
                    respiracionNormal = respiracionNormal,
                    observaciones = observaciones.trim(),
                    registradoPorId = usuario.id,
                    registradoAt = ts
                )
            )
            registrar(usuario, faena, TipoEvento.CERRADA) { it.copy(estado = EstadoFaena.CERRADA) }
            AppResult.Ok(Unit)
        }
    }

    private companion object {
        const val MAX_NOTA = 300
        val ESTADOS_ACTIVOS: List<String> = EstadoFaena.values().filter { it.estaActiva }.map { it.name }
    }
}
