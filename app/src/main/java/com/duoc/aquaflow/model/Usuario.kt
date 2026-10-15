package com.duoc.aquaflow.model
enum class Rol { SUPERVISOR, JEFE }

data class Usuario(
    val nombre: String,
    val clave: String,
    val rol: Rol
)