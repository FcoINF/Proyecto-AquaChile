package com.duoc.aquaflow.model

/** Fila del historial: resume una faena y, si existe, su encuesta de salud. */
data class HistorialItem(
    val id: String,
    val codigo: String,
    val centroCultivo: String,
    val buzoAsignado: String,
    val supervisorACargo: String,
    val fecha: String,
    val hora: String,
    val estado: EstadoFaena,
    val encuesta: EncuestaSalud? = null
)

// Hasta persistir decisiones, una apertura guardada sigue en pre-chequeo.
fun PreChequeo.aHistorialItem() = HistorialItem(
    id = id,
    codigo = id,
    centroCultivo = centroCultivo,
    buzoAsignado = buzoAsignado,
    supervisorACargo = supervisorACargo,
    fecha = fecha,
    hora = hora,
    estado = EstadoFaena.PRECHEQUEO
)
