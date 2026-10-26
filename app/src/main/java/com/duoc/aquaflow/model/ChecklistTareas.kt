package com.duoc.aquaflow.model

data class TareaFaena(val descripcion: String, val verificada: Boolean = false,
    val peligro: String = "", val control: String = "",
    val fechasRevision: List<String> = List(4) { "" })
data class ChecklistTareas(val items: List<TareaFaena>, val estado: EstadoSeccion = EstadoSeccion.COMPLETA)
data class RegistroInicio(val iniciadoPor: String, val iniciadoEn: Long)
