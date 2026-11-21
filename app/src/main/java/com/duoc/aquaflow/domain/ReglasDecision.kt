package com.duoc.aquaflow.domain

import com.duoc.aquaflow.model.Decision
import com.duoc.aquaflow.model.EstadoEquipo
import com.duoc.aquaflow.model.NivelSalud
import com.duoc.aquaflow.model.ResultadoAst
import com.duoc.aquaflow.model.ResultadoFaena
import com.duoc.aquaflow.model.ResumenFaena
import com.duoc.aquaflow.model.EstadoSeccion
import com.duoc.aquaflow.model.Rol
import com.duoc.aquaflow.model.fotosAsociadasPorEquipo

data class EvaluacionFaena(val resultado: ResultadoFaena, val motivos: List<String>)

// Reglas de la toma de decisión (Kotlin puro, sin Android).
object ReglasDecision {

    // Partes de la faena que todavía no están completas
    fun faltantes(resumen: ResumenFaena): List<String> =
        ReglasSecciones.estados(resumen).filterValues { it != EstadoSeccion.COMPLETA }.keys.toList()

    // Una decisión necesita el expediente completo, incluso cuando ya se conoce su semáforo.
    fun resultado(resumen: ResumenFaena): ResultadoFaena? {
        if (faltantes(resumen).isNotEmpty()) return null
        return evaluar(resumen).resultado
    }

    fun evaluar(resumen: ResumenFaena): EvaluacionFaena {
        val bloqueos = mutableListOf<String>()
        val observaciones = mutableListOf<String>()
        Reglas.bloqueoTestEsfuerzo(resumen.apertura)?.let(bloqueos::add)
        val faltantes = faltantes(resumen)
        if (faltantes.isNotEmpty()) observaciones += "Revisión pendiente: ${faltantes.joinToString()}."

        resumen.checklist?.let { checklist ->
            val fotos = checklist.fotosAsociadasPorEquipo()
            checklist.items.forEach { equipo ->
                when (equipo.estado) {
                    EstadoEquipo.NO_CUMPLE -> bloqueos += "Equipo crítico sin conformidad: ${equipo.titulo}."
                    EstadoEquipo.OBSERVADO -> observaciones += "Equipo observado: ${equipo.titulo}."
                    else -> Unit
                }
                fotos[equipo.id]?.let(checklist.metadatosPorFoto::get)?.let { foto ->
                    when (foto.coincide) {
                        false -> observaciones += "La foto de ${equipo.titulo} difiere de su referencia."
                        null -> observaciones += "Comparación fotográfica pendiente: ${equipo.titulo}."
                        true -> Unit
                    }
                }
            }
        }
        resumen.ast?.let { ast ->
            val criticas = ReglasAst.condicionesCriticasIncumplidas(ast.respuestas)
            when {
                criticas.isNotEmpty() -> criticas.forEach { pregunta -> bloqueos += "AST: $pregunta" }
                ast.resultado == ResultadoAst.REQUIERE_REVISION -> bloqueos += "El AST registra un bloqueo crítico."
                ast.resultado == ResultadoAst.OBSERVADO ||
                    ReglasAst.resultado(ast.respuestas, ast.pasos, ast.requiereNuevoAst) == ResultadoAst.OBSERVADO ->
                    observaciones += "El AST tiene observaciones para revisar."
            }
        }
        resumen.encuesta?.let { e ->
            when (Reglas.nivelSalud(e.sistolica, e.diastolica, e.saturacion, e.alertasEncuesta.size)) {
                NivelSalud.CRITICO -> bloqueos += "La encuesta de salud contiene valores críticos."
                NivelSalud.ATENCION -> observaciones += "La encuesta de salud requiere revisión."
                NivelSalud.NORMAL -> Unit
            }
        }
        val resultado = when {
            bloqueos.isNotEmpty() -> ResultadoFaena.REQUIERE_REVISION
            observaciones.isNotEmpty() -> ResultadoFaena.OBSERVADO
            else -> ResultadoFaena.CUMPLE
        }
        return EvaluacionFaena(resultado, bloqueos + observaciones)
    }

    // Según el README, la jefatura es quien autoriza o rechaza.
    // Es el único lugar donde se define quién puede decidir.
    fun puedeDecidir(rol: Rol) = rol == Rol.JEFE

    // Mensaje de error si la decisión no es válida; null si lo es.
    fun errorDecision(decision: Decision, resultado: ResultadoFaena, motivo: String): String? = when {
        decision == Decision.AUTORIZADA && resultado == ResultadoFaena.REQUIERE_REVISION ->
            "Una faena en estado rojo no puede ser autorizada. Corrige los bloqueos críticos y vuelve a evaluarla"
        decision == Decision.RECHAZADA && motivo.isBlank() ->
            "Indica el motivo del rechazo"
        else -> null
    }
}
