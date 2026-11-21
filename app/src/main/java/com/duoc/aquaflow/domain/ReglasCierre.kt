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

    fun errorMediciones(mediciones: MedicionesInmersion?, duracion: Int): String? = when {
        mediciones == null -> "Completa las mediciones de la inmersión y selecciona su origen."
        mediciones.tiempoFondoMinutos !in 1..duracion ->
            "El tiempo de fondo debe ser mayor que cero y no superar la duración de la inmersión."
        listOf(mediciones.profundidadMaximaMetros, mediciones.descensoMaximo, mediciones.ascensoMaximo)
            .any { !it.isFinite() || it <= 0 } -> "Ingresa valores positivos para la profundidad y las velocidades."
        else -> null
    }

    fun puedeIniciar(faena: FaenaGuardada, usuario: Usuario) =
        faena.decision?.decision == Decision.AUTORIZADA && faena.inicio == null && faena.cierre == null && propietario(faena, usuario)

    fun puedeCerrar(faena: FaenaGuardada, usuario: Usuario) =
        faena.decision?.decision == Decision.AUTORIZADA && faena.inicio != null && faena.cierre == null && propietario(faena, usuario)

    private fun propietario(faena: FaenaGuardada, usuario: Usuario) =
        usuario.rol == Rol.SUPERVISOR && faena.resumen.apertura.supervisorACargo == usuario.nombre
}
