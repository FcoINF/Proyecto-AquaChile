package com.duoc.aquaflow.model

data class RegistroCierre(
    val duracionMinutos: Int,
    val observaciones: String,
    val revisionSalida: Boolean,
    val cerradoPor: String = "",
    val cerradoEn: Long = 0L,
    val saludSalida: EncuestaSalud? = null
)
