package com.duoc.aquaflow.domain.rules

import com.duoc.aquaflow.domain.catalog.Catalogos
import com.duoc.aquaflow.domain.config.ReglasConfig
import com.duoc.aquaflow.model.AntecedentesAst
import com.duoc.aquaflow.model.EncuestaSalud
import com.duoc.aquaflow.model.EstadoEquipo
import com.duoc.aquaflow.model.Evidencia
import com.duoc.aquaflow.model.FaenaDetalle
import com.duoc.aquaflow.model.RespuestaChecklist
import com.duoc.aquaflow.model.RespuestaFormulario
import com.duoc.aquaflow.model.TipoEvidencia
import com.duoc.aquaflow.model.TipoFormulario

/**
 * Resultado de evaluar una sección de la faena.
 *  - [faltantes]: información que falta para considerarla completa.
 *  - [bloqueos]: condiciones de seguridad no cumplidas que impiden AUTORIZAR (la faena sí puede rechazarse).
 *  - [advertencias]: avisos que no impiden avanzar pero la jefatura debe ver.
 */
data class EvaluacionSeccion(
    val faltantes: List<String> = emptyList(),
    val bloqueos: List<String> = emptyList(),
    val advertencias: List<String> = emptyList()
) {
    val completa: Boolean get() = faltantes.isEmpty()
}

data class EvaluacionFaena(
    val checklist: EvaluacionSeccion,
    val ast: EvaluacionSeccion,
    val salud: EvaluacionSeccion
) {
    val todasCompletas: Boolean get() = checklist.completa && ast.completa && salud.completa
    val faltantes: List<String> get() = checklist.faltantes + ast.faltantes + salud.faltantes
    val bloqueos: List<String> get() = checklist.bloqueos + ast.bloqueos + salud.bloqueos
    val advertencias: List<String> get() = checklist.advertencias + ast.advertencias + salud.advertencias

    /** El supervisor puede firmar y enviar a revisión cuando toda la información está completa. */
    val puedeEnviarARevision: Boolean get() = todasCompletas

    /** Se puede autorizar solo con información completa y sin condiciones de seguridad incumplidas. */
    val puedeAutorizar: Boolean get() = todasCompletas && bloqueos.isEmpty()
}

object FaenaEvaluator {

    fun evaluar(detalle: FaenaDetalle, config: ReglasConfig = ReglasConfig()): EvaluacionFaena =
        EvaluacionFaena(
            checklist = evaluarChecklist(detalle.checklist, detalle.evidencias, config),
            ast = evaluarAst(detalle.antecedentesAst, detalle.respuestas, config),
            salud = evaluarSalud(detalle.encuestaSalud, detalle.respuestas, config)
        )

    fun evaluarChecklist(
        respuestas: List<RespuestaChecklist>,
        evidencias: List<Evidencia>,
        config: ReglasConfig = ReglasConfig()
    ): EvaluacionSeccion {
        val porId = respuestas.associateBy { it.itemId }
        val faltantes = mutableListOf<String>()
        val bloqueos = mutableListOf<String>()
        val advertencias = mutableListOf<String>()

        for (equipo in Catalogos.equipos) {
            val respuesta = porId[equipo.id]
            if (respuesta == null) {
                faltantes += "Falta revisar: ${equipo.nombre}"
                continue
            }
            if (respuesta.estado == EstadoEquipo.CUMPLE) continue

            if (respuesta.estado == EstadoEquipo.NO_CUMPLE) {
                bloqueos += "El equipo no cumple: ${equipo.nombre}"
            } else {
                advertencias += "Equipo observado: ${equipo.nombre}"
            }
            if (respuesta.nota.isBlank()) {
                faltantes += "Falta la nota obligatoria en: ${equipo.nombre}"
            }
            val tieneFoto = evidencias.any { it.tipo == TipoEvidencia.EQUIPO && it.itemId == equipo.id }
            if (config.exigirFotoEnFallas && !tieneFoto) {
                faltantes += "Falta la foto de evidencia en: ${equipo.nombre}"
            }
        }
        return EvaluacionSeccion(faltantes, bloqueos, advertencias)
    }

    fun evaluarAst(
        antecedentes: AntecedentesAst?,
        respuestas: List<RespuestaFormulario>,
        config: ReglasConfig = ReglasConfig()
    ): EvaluacionSeccion {
        val faltantes = mutableListOf<String>()
        val bloqueos = mutableListOf<String>()
        val advertencias = mutableListOf<String>()

        if (antecedentes == null || antecedentes.tipologia.isBlank()) faltantes += "Falta la tipología de la tarea"
        if (antecedentes == null || antecedentes.tarea.isBlank()) faltantes += "Falta la descripción de la tarea"
        if (antecedentes == null || antecedentes.pasos.isBlank()) faltantes += "Faltan los pasos de la tarea"

        val gating = respuestas.filter { it.formulario == TipoFormulario.AST_GATING }
            .associate { it.clave to it.respuesta }
        for (pregunta in Catalogos.gatingAst) {
            when (gating[pregunta.id]) {
                null -> faltantes += "Falta responder: ${pregunta.categoria}"
                false -> bloqueos += "Condición previa no cumplida (${pregunta.categoria}): la inmersión debe suspenderse"
                true -> Unit
            }
        }

        val riesgos = respuestas.filter { it.formulario == TipoFormulario.AST_RIESGO }
            .associate { it.clave to it.respuesta }
        for (riesgo in Catalogos.riesgosAst) {
            if (riesgos[riesgo.id] != true) faltantes += "Falta verificar la medida de control: ${riesgo.titulo}"
        }

        if (antecedentes?.requiereNuevoAst == true) {
            advertencias += "Se indicó que se requiere un nuevo AST por cambio de condiciones"
        }
        val prof = antecedentes?.profundidadMaxPlanificadaM
        if (prof != null && prof > config.profundidadMaxPlanificadaM) {
            bloqueos += "La profundidad planificada supera el límite configurado"
        }
        return EvaluacionSeccion(faltantes, bloqueos, advertencias)
    }

    fun evaluarSalud(
        encuesta: EncuestaSalud?,
        respuestas: List<RespuestaFormulario>,
        config: ReglasConfig = ReglasConfig()
    ): EvaluacionSeccion {
        val faltantes = mutableListOf<String>()
        val bloqueos = mutableListOf<String>()
        val advertencias = mutableListOf<String>()

        if (encuesta?.sistolica == null || encuesta.diastolica == null) faltantes += "Falta la presión arterial"
        if (encuesta?.saturacionO2 == null) faltantes += "Falta la saturación de oxígeno"
        if (encuesta?.temperaturaC == null) faltantes += "Falta la temperatura"

        val declaraciones = respuestas.filter { it.formulario == TipoFormulario.SALUD_DECLARACION }
            .associate { it.clave to it.respuesta }
        for (pregunta in Catalogos.declaracionSalud) {
            when (declaraciones[pregunta.id]) {
                null -> faltantes += "Falta responder: ${pregunta.categoria}"
                false -> bloqueos += "Declaración desfavorable (${pregunta.categoria}): el buzo queda NO APTO"
                true -> Unit
            }
        }

        if (encuesta != null) {
            val fuera = signosFueraDeReferencia(encuesta, config)
            if (config.bloquearAutorizacionPorSignos) bloqueos += fuera else advertencias += fuera
        }
        return EvaluacionSeccion(faltantes, bloqueos, advertencias)
    }

    /**
     * Mensajes de los signos vitales fuera del rango de referencia configurado (PROVISIONAL, ver
     * [com.duoc.aquaflow.domain.config.ReferenciasSignosVitales]). No es un diagnóstico.
     */
    fun signosFueraDeReferencia(encuesta: EncuestaSalud, config: ReglasConfig = ReglasConfig()): List<String> {
        val ref = config.referencias
        val mensajes = mutableListOf<String>()
        val sis = encuesta.sistolica
        val dia = encuesta.diastolica
        if (sis != null && dia != null && (sis !in ref.sistolica || dia !in ref.diastolica)) {
            mensajes += "Presión arterial $sis/$dia fuera del rango de referencia configurado (provisional)"
        }
        val spo2 = encuesta.saturacionO2
        if (spo2 != null && spo2 < ref.saturacionMinima) {
            mensajes += "Saturación $spo2% bajo el mínimo de referencia configurado (provisional)"
        }
        val temp = encuesta.temperaturaC
        if (temp != null && (temp < ref.temperaturaMin || temp > ref.temperaturaMax)) {
            mensajes += "Temperatura $temp °C fuera del rango de referencia configurado (provisional)"
        }
        return mensajes
    }
}
