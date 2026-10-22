package com.duoc.aquaflow.model

// Pregunta del chequeo inicial del AST (Sí/No). Si es crítica y se responde "No",
// el resultado pasa a "Requiere revisión".
class PreguntaAst(val texto: String, val critica: Boolean)

// Contenido ficticio de ejemplo: si el cliente cambia el AST, solo se edita esta lista.
val PREGUNTAS_AST = listOf(
    PreguntaAst("¿El equipo fue informado de los riesgos de la tarea?", true),
    PreguntaAst("¿El plan de emergencia y el buzo de emergencia están definidos?", true),
    PreguntaAst("¿Las condiciones ambientales (viento, oleaje, visibilidad) son aptas?", true),
    PreguntaAst("¿El sistema de comunicación está operativo?", false),
    PreguntaAst("¿Se delimitó y señalizó el área de trabajo?", false)
)

// Tipos de tarea entre los que elige el supervisor (ejemplos ficticios).
val TIPOLOGIAS_AST = listOf(
    "Inspección de redes",
    "Limpieza de fondos",
    "Mantención de estructuras",
    "Retiro de mortalidad"
)

// Resultado preliminar del AST (semáforo). Propio del AST para no depender de otros módulos.
enum class ResultadoAst(val texto: String) {
    CUMPLE("Cumple"),
    OBSERVADO("Observado"),
    REQUIERE_REVISION("Requiere revisión")
}

// Un paso de la tarea con su peligro y la medida de control que lo mitiga.
data class PasoAst(
    val tarea: String = "",
    val peligro: String = "",
    val control: String = "",
    val critico: Boolean = false
)

// Formulario AST ya validado. El centro y la fecha no se repiten aquí: viven en la apertura de la faena.
// `respuestas` va en el mismo orden que PREGUNTAS_AST.
data class FormularioAst(
    val tipologia: String,
    val requiereNuevoAst: Boolean,
    val respuestas: List<Boolean>,
    val pasos: List<PasoAst>,
    val resultado: ResultadoAst
)