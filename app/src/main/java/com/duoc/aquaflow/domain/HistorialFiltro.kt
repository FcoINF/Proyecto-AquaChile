package com.duoc.aquaflow.domain

import com.duoc.aquaflow.model.EstadoFaena
import com.duoc.aquaflow.model.HistorialItem
import com.duoc.aquaflow.model.ResultadoFaena

enum class CategoriaHistorial(val etiqueta: String) {
    TODAS("Todas"),
    APROBADAS("Aprobadas"),
    OBSERVADAS("Con observación"),
    EN_CURSO("En curso"),
    RECHAZADAS("Rechazadas")
}

/** Filtros del historial: texto libre y estado. */
data class HistorialFiltro(
    val texto: String = "",
    val estado: EstadoFaena? = null,
    val fecha: String = "",
    val categoria: CategoriaHistorial = CategoriaHistorial.TODAS
) {
    fun aplicar(items: List<HistorialItem>): List<HistorialItem> {
        val consulta = texto.trim().lowercase()
        return items.filter { item ->
            (estado == null || item.estado == estado) &&
                categoriaCoincide(item) &&
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

    private fun categoriaCoincide(item: HistorialItem): Boolean = when (categoria) {
        CategoriaHistorial.TODAS -> true
        CategoriaHistorial.APROBADAS -> item.estado == EstadoFaena.AUTORIZADA || item.estado == EstadoFaena.CERRADA
        CategoriaHistorial.OBSERVADAS -> item.estado == EstadoFaena.PRECHEQUEO && item.resultado == ResultadoFaena.OBSERVADO
        CategoriaHistorial.EN_CURSO -> item.estado == EstadoFaena.EN_EJECUCION
        CategoriaHistorial.RECHAZADAS -> item.estado == EstadoFaena.RECHAZADA
    }
}
