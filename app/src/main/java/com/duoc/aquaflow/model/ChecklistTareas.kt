package com.duoc.aquaflow.model

data class TareaFaena(val descripcion: String, val verificada: Boolean = false)
data class ChecklistTareas(val items: List<TareaFaena>, val estado: EstadoSeccion = EstadoSeccion.COMPLETA)
data class RegistroInicio(val iniciadoPor: String, val iniciadoEn: Long)
