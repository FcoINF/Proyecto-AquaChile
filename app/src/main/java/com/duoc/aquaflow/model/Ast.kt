package com.duoc.aquaflow.model

// Pregunta del chequeo inicial del AST (Sí/No). Si es crítica y se responde "No",
// el resultado pasa a "Requiere revisión".
class PreguntaAst(val texto: String, val critica: Boolean)

// Contenido ficticio de ejemplo: si el cliente cambia el AST, solo se edita esta lista.
val PREGUNTAS_AST_ANTERIORES = listOf(
    PreguntaAst("¿El equipo fue informado de los riesgos de la tarea?", true),
    PreguntaAst("¿El plan de emergencia y el buzo de emergencia están definidos?", true),
    PreguntaAst("¿Las condiciones ambientales (viento, oleaje, visibilidad) son aptas?", true),
    PreguntaAst("¿El sistema de comunicación está operativo?", false),
    PreguntaAst("¿Se delimitó y señalizó el área de trabajo?", false)
)

val PREGUNTAS_AST = listOf(
    PreguntaAst("¿Entendemos qué vamos a hacer y cuáles son los riesgos?", true),
    PreguntaAst("¿Tenemos permisos, controles y elementos de protección personal adecuados?", true),
    PreguntaAst("¿Estamos capacitados y los equipos en buen estado?", true),
    PreguntaAst("¿Estamos trabajando en un área segura (donde no sobrevenga un riesgo grave e inminente para la vida o salud de los trabajadores)?", true),
    PreguntaAst("¿Sabemos qué hacer en caso de emergencia o si algo cambia en nuestra planificación?", true)
)

val TIPOLOGIAS_AST = listOf(
    "Trabajo en Altura Física",
    "Trabajo eléctrico o intervención en Sistemas Eléctricos",
    "Trabajo con Sustancias Químicas Peligrosas",
    "Trabajo en espacio confinado",
    "Trabajo en caliente (con riesgo de incendio)",
    "Exposición a zonas peligrosas en maquinaria y equipos",
    "Trabajo con Cargas Suspendidas (incluye maniobras en líneas en tensión)",
    "Otro"
)

data class AntecedentesAst(
    val fechaInicio: String = "",
    val turnoInicio: String = "",
    val fechaCierre: String = "",
    val turnoCierre: String = "",
    val descripcion: String = "",
    val ubicacion: String = "",
    val ejecucion: String = "",
    val empresa: String = "",
    val rutEmpresa: String = "",
    val otraTipologia: String = ""
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
    val critico: Boolean = false,
    val fechasRevision: List<String> = List(4) { "" }
)

// Formulario AST ya validado. El centro y la fecha no se repiten aquí: viven en la apertura de la faena.
// `respuestas` va en el mismo orden que PREGUNTAS_AST.
data class FormularioAst(
    val tipologia: String,
    val requiereNuevoAst: Boolean,
    val respuestas: List<Boolean>,
    val pasos: List<PasoAst>,
    val resultado: ResultadoAst,
    val estado: EstadoSeccion = EstadoSeccion.COMPLETA,
    val antecedentes: AntecedentesAst = AntecedentesAst(),
    val versionPreguntas: Int = 2
)
