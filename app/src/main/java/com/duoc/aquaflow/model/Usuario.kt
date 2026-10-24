package com.duoc.aquaflow.model

// Conservamos JEFE como identificador hasta que el equipo acuerde el nombre interno.
enum class Rol(val texto: String) {
    SUPERVISOR("Supervisor"),
    JEFE("Administrador")
}

data class Usuario(
    val nombre: String,
    val clave: String,
    val rol: Rol
)
