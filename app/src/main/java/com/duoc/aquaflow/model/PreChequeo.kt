package com.duoc.aquaflow.model

data class PreChequeo(
    val id: String = "",
    val centroCultivo: String = "",
    val fecha: String = "",
    val hora: String = "",
    val buzoAsignado: String = "",
    val supervisorACargo: String = "",
    val condicionesListas: Boolean = false,
    // null = sin responder; false = el usuario respondió "No".
    val embarcacionLista: Boolean? = null,
    val comunicacionesOperativas: Boolean? = null
)
