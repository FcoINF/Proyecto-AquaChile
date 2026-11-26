package com.duoc.aquaflow.model

enum class EstadoEquipo {
    CUMPLE,
    NO_CUMPLE,
    OBSERVADO,
    PENDIENTE
}

data class ItemEquipamiento(
    val id: String,
    val titulo: String,
    val descripcion: String,
    val estado: EstadoEquipo = EstadoEquipo.PENDIENTE,
    val notas: String = "",
    val categoria: String = "",
    val badge: String = ""
)

data class ChecklistFaena(
    val idFaena: String,
    val items: List<ItemEquipamiento>,
    val fotosEvidencia: List<String> = emptyList(),
    val estado: EstadoSeccion = EstadoSeccion.COMPLETA,
    val fotosPorEquipo: Map<String, String> = emptyMap(),
    val metadatosPorFoto: Map<String, MetadatosFoto> = emptyMap()
)

data class MetadatosFoto(
    val capturadaEn: Long? = null,
    val capturadaPor: String? = null,
    val referencia: String? = null,
    val coincide: Boolean? = null
)

/**
 * Los registros anteriores solo guardaban una lista de fotos. Se asocian por orden
 * para conservarlos, mientras que las capturas nuevas persisten el ID del equipo.
 */
fun ChecklistFaena.fotosAsociadasPorEquipo(): Map<String, String> =
    fotosPorEquipo.ifEmpty {
        items.map { it.id }.zip(fotosEvidencia).toMap()
    }
