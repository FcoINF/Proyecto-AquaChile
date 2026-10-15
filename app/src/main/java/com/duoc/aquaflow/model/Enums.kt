package com.duoc.aquaflow.model

/** Perfil operativo del usuario. */
enum class Rol(val etiqueta: String, val descripcion: String) {
    SUPERVISOR("Supervisor de Buceo", "Responsable de AST, cuadrilla, compresor y tabla de inmersión."),
    ADMINISTRADOR("Jefe de Centro / Prevención", "Auditoría, trazabilidad y autorización de faenas.")
}

/**
 * Ciclo de vida de una faena. Las transiciones válidas están en
 * [com.duoc.aquaflow.domain.rules.FaenaStateMachine].
 */
enum class EstadoFaena(val etiqueta: String) {
    PRECHEQUEO("En pre-chequeo"),
    PENDIENTE_AUTORIZACION("Pendiente de autorización"),
    AUTORIZADA("Autorizada"),
    RECHAZADA("Rechazada"),
    CANCELADA("Cancelada"),
    CERRADA("Cerrada");

    /** Faena que todavía ocupa al buzo (no ha terminado su ciclo). */
    val estaActiva: Boolean
        get() = this == PRECHEQUEO || this == PENDIENTE_AUTORIZACION || this == AUTORIZADA

    val esTerminal: Boolean
        get() = !estaActiva
}

enum class EstadoEquipo(val etiqueta: String) {
    CUMPLE("Cumple"),
    NO_CUMPLE("No cumple"),
    OBSERVADO("Observado")
}

enum class TipoEvidencia(val etiqueta: String) {
    EQUIPO("Equipo"),
    BUZO_EQUIPADO("Buzo equipado"),
    BUZO_EMERGENCIA("Buzo de emergencia"),
    FILTROS("Filtros")
}

/** Familias de respuestas Sí/No guardadas en una misma tabla. */
enum class TipoFormulario {
    AST_GATING,
    AST_RIESGO,
    SALUD_DECLARACION
}

enum class EstadoSync { PENDIENTE, SINCRONIZADO }

enum class TipoEvento(val etiqueta: String) {
    APERTURA("Apertura de faena"),
    CHECKLIST_GUARDADO("Checklist DPR-24 guardado"),
    AST_GUARDADO("Formulario AST guardado"),
    SALUD_GUARDADA("Encuesta de salud guardada"),
    EVIDENCIA_AGREGADA("Evidencia agregada"),
    EVIDENCIA_ELIMINADA("Evidencia eliminada"),
    ENVIADA_A_REVISION("Firmada y enviada a revisión"),
    AUTORIZADA("Faena autorizada"),
    RECHAZADA("Faena rechazada"),
    CANCELADA("Faena cancelada"),
    CERRADA("Registro posterior y cierre")
}
