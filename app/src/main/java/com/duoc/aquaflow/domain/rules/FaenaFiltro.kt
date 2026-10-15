package com.duoc.aquaflow.domain.rules

import com.duoc.aquaflow.model.EstadoFaena
import com.duoc.aquaflow.model.Faena

enum class RangoFecha(val etiqueta: String, val dias: Int?) {
    TODOS("Todas", null),
    ULTIMOS_7("7 días", 7),
    ULTIMOS_30("30 días", 30)
}

/** Filtros del historial de faenas: texto libre, estado y rango de fechas. */
data class FaenaFiltro(
    val texto: String = "",
    val estado: EstadoFaena? = null,
    val rango: RangoFecha = RangoFecha.TODOS
) {
    fun aplicar(faenas: List<Faena>, ahora: Long): List<Faena> {
        val consulta = texto.trim().lowercase()
        val desde = rango.dias?.let { ahora - it * MILLIS_POR_DIA }
        return faenas.filter { faena ->
            (estado == null || faena.estado == estado) &&
                (desde == null || faena.fechaInicio >= desde) &&
                (consulta.isEmpty() || coincide(faena, consulta))
        }
    }

    private fun coincide(faena: Faena, consulta: String): Boolean =
        faena.codigo.lowercase().contains(consulta) ||
            faena.buzoNombre.lowercase().contains(consulta) ||
            faena.centroNombre.lowercase().contains(consulta) ||
            faena.supervisorNombre.lowercase().contains(consulta)

    private companion object {
        const val MILLIS_POR_DIA = 24L * 60 * 60 * 1000
    }
}
