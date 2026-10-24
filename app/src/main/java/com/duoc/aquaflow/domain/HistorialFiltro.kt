package com.duoc.aquaflow.domain

import com.duoc.aquaflow.model.EstadoFaena
import com.duoc.aquaflow.model.HistorialItem

/** Filtros del historial: texto libre y estado. */
data class HistorialFiltro(
    val texto: String = "",
    val estado: EstadoFaena? = null
) {
    fun aplicar(items: List<HistorialItem>): List<HistorialItem> {
        val consulta = texto.trim().lowercase()
        return items.filter { item ->
            (estado == null || item.estado == estado) &&
                (consulta.isEmpty() || coincide(item, consulta))
        }
    }

    private fun coincide(item: HistorialItem, consulta: String): Boolean =
        item.codigo.lowercase().contains(consulta) ||
            item.centroCultivo.lowercase().contains(consulta) ||
            item.buzoAsignado.lowercase().contains(consulta) ||
            item.supervisorACargo.lowercase().contains(consulta)
}
