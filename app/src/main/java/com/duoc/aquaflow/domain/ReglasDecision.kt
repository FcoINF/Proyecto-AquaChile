package com.duoc.aquaflow.domain

import com.duoc.aquaflow.model.Decision
import com.duoc.aquaflow.model.EstadoEquipo
import com.duoc.aquaflow.model.NivelSalud
import com.duoc.aquaflow.model.ResultadoAst
import com.duoc.aquaflow.model.ResultadoFaena
import com.duoc.aquaflow.model.ResumenFaena
import com.duoc.aquaflow.model.Rol

// Reglas de la toma de decisión (Kotlin puro, sin Android).
object ReglasDecision {

    // Partes de la faena que todavía no están completas
    fun faltantes(resumen: ResumenFaena): List<String> = buildList {
        val items = resumen.checklist?.items.orEmpty()
        if (items.isEmpty() || items.any { it.estado == EstadoEquipo.PENDIENTE }) add("Checklist")
        if (resumen.ast == null) add("AST")
        if (resumen.encuesta == null) add("Encuesta de salud")
    }

    // Semáforo global: gana el peor de checklist, AST y salud.
    // Es null mientras falte alguna parte, para no mostrar un verde engañoso.
    fun resultado(resumen: ResumenFaena): ResultadoFaena? {
        if (faltantes(resumen).isNotEmpty()) return null

        val checklist = resumen.checklist?.items.orEmpty().map {
            when (it.estado) {
                EstadoEquipo.NO_CUMPLE -> ResultadoFaena.REQUIERE_REVISION
                EstadoEquipo.OBSERVADO -> ResultadoFaena.OBSERVADO
                else -> ResultadoFaena.CUMPLE
            }
        }
        val ast = when (resumen.ast?.resultado) {
            ResultadoAst.REQUIERE_REVISION -> ResultadoFaena.REQUIERE_REVISION
            ResultadoAst.OBSERVADO -> ResultadoFaena.OBSERVADO
            else -> ResultadoFaena.CUMPLE
        }
        // El nivel de salud se recalcula con la misma regla usada al guardar la encuesta
        val salud = resumen.encuesta?.let { e ->
            when (Reglas.nivelSalud(e.sistolica, e.diastolica, e.saturacion, e.alertasEncuesta.size)) {
                NivelSalud.CRITICO -> ResultadoFaena.REQUIERE_REVISION
                NivelSalud.ATENCION -> ResultadoFaena.OBSERVADO
                NivelSalud.NORMAL -> ResultadoFaena.CUMPLE
            }
        } ?: ResultadoFaena.CUMPLE

        return (checklist + ast + salud).maxBy { it.ordinal }
    }

    // Según el README, la jefatura es quien autoriza o rechaza.
    // Es el único lugar donde se define quién puede decidir.
    fun puedeDecidir(rol: Rol) = rol == Rol.JEFE

    // Mensaje de error si la decisión no es válida; null si lo es.
    fun errorDecision(decision: Decision, resultado: ResultadoFaena, motivo: String): String? = when {
        decision == Decision.RECHAZADA && motivo.isBlank() ->
            "Indica el motivo del rechazo"
        decision == Decision.AUTORIZADA && resultado == ResultadoFaena.REQUIERE_REVISION && motivo.isBlank() ->
            "Para autorizar un resultado que requiere revisión, indica el motivo"
        else -> null
    }
}