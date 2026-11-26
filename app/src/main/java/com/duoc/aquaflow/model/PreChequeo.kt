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
    val comunicacionesOperativas: Boolean? = null,
    val centroId: String = if (centroCultivo.isBlank()) "" else "LEGACY-CENTRO",
    val buzoId: String = if (buzoAsignado.isBlank()) "" else "LEGACY-BUZO",
    val edadBuzo: Int = 35,
    val imcBuzo: Double = 24.0,
    val examenPreocupacionalVigente: Boolean? = true,
    val vigenciaExamenPreocupacional: String = "31/12/2099",
    val puntajeCalbuco: Int? = 100,
    val testEsfuerzoVigente: Boolean? = true,
    val vigenciaTestEsfuerzo: String = "31/12/2099",
    val buzoEmergenciaDisponible: Boolean? = true,
    val meteorologia: CondicionesMaritimas? = null
)
