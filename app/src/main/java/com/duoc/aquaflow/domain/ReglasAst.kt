package com.duoc.aquaflow.domain

import com.duoc.aquaflow.model.PREGUNTAS_AST
import com.duoc.aquaflow.model.PasoAst
import com.duoc.aquaflow.model.ResultadoAst

// Reglas del formulario AST (Kotlin puro, sin Android).
object ReglasAst {

    const val MAX_PASOS = 10

    // null = pregunta sin responder
    fun respuestasCompletas(respuestas: List<Boolean?>) =
        respuestas.size == PREGUNTAS_AST.size && respuestas.none { it == null }

    // Mensaje de error del formulario; null si está completo. La primera falla define el mensaje.
    fun error(tipologia: String, respuestas: List<Boolean?>, pasos: List<PasoAst>): String? = when {
        tipologia.isBlank() -> "Elige la tipología de la tarea"
        !respuestasCompletas(respuestas) -> "Responde todas las preguntas del chequeo inicial"
        pasos.isEmpty() || pasos.any { it.tarea.isBlank() || it.peligro.isBlank() } -> "Cada paso necesita tarea y peligro"
        pasos.any { it.control.isBlank() } -> "Cada peligro necesita su medida de control"
        else -> null
    }

    // Misma lógica de semáforo que el resto de la app: rojo > amarillo > verde
    fun resultado(respuestas: List<Boolean?>, pasos: List<PasoAst>, requiereNuevoAst: Boolean): ResultadoAst {
        // ¿Alguna pregunta crítica respondida "No"? (zip junta cada pregunta con su respuesta)
        val criticaFalla = PREGUNTAS_AST.zip(respuestas).any { (p, r) -> p.critica && r == false }
        val hayNo = respuestas.any { it == false }
        val hayRiesgoCritico = pasos.any { it.critico }
        return when {
            criticaFalla -> ResultadoAst.REQUIERE_REVISION
            hayNo || hayRiesgoCritico || requiereNuevoAst -> ResultadoAst.OBSERVADO
            else -> ResultadoAst.CUMPLE
        }
    }
}