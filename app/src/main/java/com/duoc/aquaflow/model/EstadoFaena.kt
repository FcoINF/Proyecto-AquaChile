package com.duoc.aquaflow.model

/** Ciclo de vida simplificado de una faena para el historial. */
enum class EstadoFaena(val etiqueta: String) {
    PRECHEQUEO("En pre-chequeo"),
    AUTORIZADA("Autorizada"),
    EN_EJECUCION("En ejecución"),
    RECHAZADA("Rechazada"),
    CERRADA("Cerrada")
}
