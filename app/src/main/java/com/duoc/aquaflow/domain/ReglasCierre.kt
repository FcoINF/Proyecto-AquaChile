package com.duoc.aquaflow.domain

import com.duoc.aquaflow.model.*

object ReglasCierre {
    fun errorCampos(duracion: Int?, observaciones: String, revisionSalida: Boolean): String? = when {
        duracion == null || duracion <= 0 -> "Ingresa la duración de la inmersión en minutos enteros mayores que cero."
        observaciones.isBlank() -> "Registra las observaciones de salida; indica si no hubo novedades."
        observaciones.length > 1000 -> "Las observaciones admiten hasta 1000 caracteres."
        !revisionSalida -> "Confirma que se realizó la revisión de salida."
        else -> null
    }

    fun puedeIniciar(faena: FaenaGuardada, usuario: Usuario) =
        faena.decision?.decision == Decision.AUTORIZADA && faena.inicio == null && faena.cierre == null && propietario(faena, usuario)

    fun puedeCerrar(faena: FaenaGuardada, usuario: Usuario) =
        faena.decision?.decision == Decision.AUTORIZADA && faena.inicio != null && faena.cierre == null && propietario(faena, usuario)

    private fun propietario(faena: FaenaGuardada, usuario: Usuario) =
        usuario.rol == Rol.SUPERVISOR && faena.resumen.apertura.supervisorACargo == usuario.nombre
}
