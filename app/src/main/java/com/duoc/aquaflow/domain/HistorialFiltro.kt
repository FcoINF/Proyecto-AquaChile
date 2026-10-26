package com.duoc.aquaflow.domain

import com.duoc.aquaflow.model.EstadoFaena
import com.duoc.aquaflow.model.HistorialItem

/** Filtros del historial: texto libre y estado. */
data class HistorialFiltro(
    val texto: String = "",
    val estado: EstadoFaena? = null,
    val fecha: String = ""
) {
    fun aplicar(items: List<HistorialItem>): List<HistorialItem> {
        val consulta = texto.trim().lowercase()
        return items.filter { item ->
            (estado == null || item.estado == estado) &&
                (fecha.isBlank() || (fechaValida(fecha) && item.fecha == fecha)) &&
                (consulta.isEmpty() || coincide(item, consulta))
        }
    }

    val errorFecha: String? get() = if (fecha.isNotBlank() && !fechaValida(fecha)) "Usa una fecha válida: dd/MM/aaaa" else null

    private fun fechaValida(texto: String): Boolean {
        if (!Regex("[0-9]{2}/[0-9]{2}/[0-9]{4}").matches(texto)) return false
        val posicion = java.text.ParsePosition(0)
        return java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.ROOT).apply { isLenient = false }
            .parse(texto, posicion) != null && posicion.index == texto.length
    }

    private fun coincide(item: HistorialItem, consulta: String): Boolean =
        item.codigo.lowercase().contains(consulta) ||
            item.centroCultivo.lowercase().contains(consulta) ||
            item.buzoAsignado.lowercase().contains(consulta) ||
            item.supervisorACargo.lowercase().contains(consulta)
}
