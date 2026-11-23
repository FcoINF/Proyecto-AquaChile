package com.duoc.aquaflow.data.remote

import com.duoc.aquaflow.domain.Reglas
import com.duoc.aquaflow.domain.ReglasAst
import com.duoc.aquaflow.model.*

internal enum class EquipoRemoto(val local: String, val titulo: String) {
    MALETA_AIRE("1", "Maleta de suministro de aire"),
    FILTROS_COMPRESOR("2", "Filtros del compresor"),
    ALETAS("3", "Aletas y equipamiento personal"),
    CENSUS("4", "Dispositivo Census")
}

internal enum class TipoFoto { CAPTURA, REFERENCIA }
internal data class FotoRemota(val equipo: EquipoRemoto, val tipo: TipoFoto, val tomadaEn: String, val responsable: String)
internal data class InspeccionRemota(
    val equipo: EquipoRemoto, val estado: EstadoEquipo, val notas: String?, val coincideConReferencia: Boolean?
)
internal data class SaludRemota(val sistolica: Int, val diastolica: Int, val saturacion: Int, val respuestas: List<Boolean>) {
    constructor(salud: EncuestaSalud) : this(salud.sistolica, salud.diastolica, salud.saturacion,
        PREGUNTAS_SALUD.map { requireNotNull(salud.respuestas[it]) { "Completa todas las respuestas de salud." } })

    fun local(): EncuestaSalud {
        val respuestasPorPregunta = PREGUNTAS_SALUD.zip(respuestas).toMap()
        val alertas = respuestasPorPregunta.filterValues { !it }.keys.toList()
        return EncuestaSalud(sistolica, diastolica, saturacion, alertas,
            Reglas.nivelSalud(sistolica, diastolica, saturacion, alertas.size), respuestas = respuestasPorPregunta)
    }
}

internal data class AstRemoto(
    val tipologia: String, val requiereNuevoAst: Boolean, val respuestas: List<Boolean>, val pasos: List<PasoAst>
) {
    constructor(ast: FormularioAst) : this(ast.tipologia, ast.requiereNuevoAst,
        ast.respuestas.map { requireNotNull(it) { "Completa todas las respuestas del AST." } }, ast.pasos)

    fun local() = FormularioAst(tipologia, requiereNuevoAst, respuestas, pasos,
        ReglasAst.resultado(respuestas, pasos, requiereNuevoAst))
}

internal data class PrechequeoRemoto(
    val checklist: List<InspeccionRemota>, val saludInicial: SaludRemota?, val ast: AstRemoto?, val tareas: List<TareaFaena>
)
internal data class SolicitudDecision(val resultado: Decision, val motivo: String)
internal data class DecisionRemota(val resultado: Decision, val motivo: String?, val responsable: String)
internal data class InicioRemoto(val registradaEn: String, val responsable: String)
internal data class CierreRemoto(
    val duracionMinutos: Int, val observaciones: String, val revisionSalida: Boolean,
    val saludSalida: SaludRemota, val mediciones: CierreHistoricoRemoto
)
internal data class BitacoraRemota(
    val duracionMinutos: Int, val observaciones: String, val responsable: String,
    val saludSalida: SaludRemota, val mediciones: CierreHistoricoRemoto, val alertas: List<AlertaRemota>
) {
    fun local() = RegistroCierre(duracionMinutos, observaciones, true, responsable, mediciones.cerradoEn,
        saludSalida.local(), mediciones.mediciones())
}
data class AlertaRemota(val tipo: String, val velocidad: Double, val limite: Double, val revisadaPor: String?)
internal data class BloqueoRemoto(val operacion: String, val motivo: String, val registradaEn: String, val responsable: String)
internal data class ExpedienteRemoto(
    val apertura: AperturaRemota, val version: Long, val prechequeo: PrechequeoRemoto, val fotos: List<FotoRemota>,
    val decision: DecisionRemota?, val inicio: InicioRemoto?, val bitacora: BitacoraRemota?, val semaforo: SemaforoRemoto,
    val publicado: Boolean, val operacion: String?
)
data class SemaforoRemoto(val nivel: String, val bloqueos: List<String>, val observaciones: List<String>)
data class DetalleRemoto(
    val faena: FaenaGuardada, val version: Long?, val semaforo: SemaforoRemoto?, val alertas: List<AlertaRemota>,
    val publicado: Boolean = false
)

internal fun AperturaRemota.local() = datos.run {
    PreChequeo(id, centroCultivo, fecha, hora, buzoAsignado, supervisorACargo, condicionesListas,
        embarcacionLista, comunicacionesOperativas, centroId, buzoId, edadBuzo, imcBuzo,
        examenPreocupacionalVigente, vigenciaExamenPreocupacional, puntajeCalbuco, testEsfuerzoVigente,
        vigenciaTestEsfuerzo.orEmpty(), buzoEmergenciaDisponible, meteorologia)
}
