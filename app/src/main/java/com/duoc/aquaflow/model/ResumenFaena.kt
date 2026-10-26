package com.duoc.aquaflow.model

// Resultado global de la faena. El orden importa: de menor a mayor gravedad.
enum class ResultadoFaena(val texto: String) {
    CUMPLE("Cumple"),
    OBSERVADO("Observado"),
    REQUIERE_REVISION("Requiere revisión")
}

// Lo que decide la jefatura sobre la faena.
enum class Decision(val texto: String) {
    AUTORIZADA("Autorizada"),
    RECHAZADA("Rechazada")
}

// Todo lo recolectado de una faena. Una parte en null significa que aún no se registra.
data class ResumenFaena(
    val apertura: PreChequeo,
    val checklist: ChecklistFaena?,
    val ast: FormularioAst?,
    val encuesta: EncuestaSalud?,
    val tareas: ChecklistTareas? = null
)

// La decisión tomada, con su motivo y quién la tomó.
data class RegistroDecision(
    val decision: Decision,
    val motivo: String,
    val decididoPor: String
)

// Una faena tal como queda guardada en el dispositivo: sus partes y, si ya existe, la decisión.
// El estado se obtiene de los registros persistidos de decisión, inicio y cierre.
data class FaenaGuardada(
    val resumen: ResumenFaena,
    val decision: RegistroDecision? = null,
    val cierre: RegistroCierre? = null,
    val inicio: RegistroInicio? = null
) {
    val estado: EstadoFaena
        get() = if (cierre != null) EstadoFaena.CERRADA else if (inicio != null) EstadoFaena.EN_EJECUCION else when (decision?.decision) {
            null -> EstadoFaena.PRECHEQUEO
            Decision.AUTORIZADA -> EstadoFaena.AUTORIZADA
            Decision.RECHAZADA -> EstadoFaena.RECHAZADA
        }
}
