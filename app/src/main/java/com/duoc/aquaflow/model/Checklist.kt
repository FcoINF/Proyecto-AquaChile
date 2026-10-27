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
    val estado: EstadoSeccion = EstadoSeccion.COMPLETA
)
