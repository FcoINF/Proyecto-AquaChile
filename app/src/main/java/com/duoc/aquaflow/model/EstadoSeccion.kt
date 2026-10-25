package com.duoc.aquaflow.model

// Completitud del registro, independiente de su resultado de seguridad.
enum class EstadoSeccion(val texto: String) {
    SIN_REGISTRAR("Sin registrar"), BORRADOR("Borrador"), COMPLETA("Completa")
}
