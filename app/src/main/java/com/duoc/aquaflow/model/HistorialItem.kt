package com.duoc.aquaflow.model

/** Fila del historial: resume una faena y, si existe, su encuesta de salud. */
data class HistorialItem(
    val id: Int,
    val codigo: String,
    val centroCultivo: String,
    val buzoAsignado: String,
    val supervisorACargo: String,
    val fecha: String,
    val hora: String,
    val estado: EstadoFaena,
    val encuesta: EncuestaSalud? = null
)
