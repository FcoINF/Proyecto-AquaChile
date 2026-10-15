package com.duoc.aquaflow.domain.rules

import com.duoc.aquaflow.domain.AppError
import com.duoc.aquaflow.model.EstadoFaena

/**
 * Transiciones válidas del ciclo de vida de una faena:
 *
 * PRECHEQUEO -> PENDIENTE_AUTORIZACION (supervisor firma y envía) | CANCELADA (supervisor cancela)
 * PENDIENTE_AUTORIZACION -> AUTORIZADA | RECHAZADA (jefatura)
 * AUTORIZADA -> CERRADA (registro posterior)
 * RECHAZADA, CANCELADA y CERRADA son terminales.
 */
object FaenaStateMachine {

    private val permitidas: Map<EstadoFaena, Set<EstadoFaena>> = mapOf(
        EstadoFaena.PRECHEQUEO to setOf(EstadoFaena.PENDIENTE_AUTORIZACION, EstadoFaena.CANCELADA),
        EstadoFaena.PENDIENTE_AUTORIZACION to setOf(EstadoFaena.AUTORIZADA, EstadoFaena.RECHAZADA),
        EstadoFaena.AUTORIZADA to setOf(EstadoFaena.CERRADA),
        EstadoFaena.RECHAZADA to emptySet(),
        EstadoFaena.CANCELADA to emptySet(),
        EstadoFaena.CERRADA to emptySet()
    )

    fun destinosDe(desde: EstadoFaena): Set<EstadoFaena> = permitidas.getValue(desde)

    fun puedePasar(desde: EstadoFaena, hacia: EstadoFaena): Boolean = hacia in destinosDe(desde)

    /** Devuelve un error si la transición no está permitida, o null si es válida. */
    fun validar(desde: EstadoFaena, hacia: EstadoFaena): AppError.InvalidState? =
        if (puedePasar(desde, hacia)) {
            null
        } else {
            AppError.InvalidState("No es posible pasar de \"${desde.etiqueta}\" a \"${hacia.etiqueta}\".")
        }
}
