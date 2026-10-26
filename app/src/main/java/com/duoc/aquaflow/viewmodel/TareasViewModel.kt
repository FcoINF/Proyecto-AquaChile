package com.duoc.aquaflow.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.duoc.aquaflow.data.local.FaenaCodec
import com.duoc.aquaflow.domain.ReglasSecciones
import com.duoc.aquaflow.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class TareasViewModel(private val saved: SavedStateHandle) : ViewModel() {
    private val _items = MutableStateFlow(saved.get<String>("tareas")?.let(FaenaCodec::tareasDesde)?.items ?: emptyList())
    val items = _items.asStateFlow()
    fun cargar(inicial: ChecklistTareas?) {
        if (saved.get<Boolean>("cargado") == true) return
        saved["cargado"] = true
        actualizar(inicial?.items.orEmpty())
    }
    private fun actualizar(items: List<TareaFaena>) {
        _items.value = items
        saved["tareas"] = FaenaCodec.tareasATexto(ChecklistTareas(items, EstadoSeccion.BORRADOR))
    }
    fun agregar() { if (_items.value.size < 50) actualizar(_items.value + TareaFaena("")) }
    fun cambiar(indice: Int, descripcion: String) = actualizar(_items.value.mapIndexed { i, t ->
        if (i == indice) t.copy(descripcion = descripcion.take(300), verificada = false) else t
    })
    fun verificar(indice: Int, valor: Boolean) = actualizar(_items.value.mapIndexed { i, t -> if (i == indice) t.copy(verificada = valor) else t })
    fun quitar(indice: Int) = actualizar(_items.value.filterIndexed { i, _ -> i != indice })
    fun borrador() = ChecklistTareas(_items.value, EstadoSeccion.BORRADOR)
    fun validar(): ChecklistTareas? = ChecklistTareas(_items.value.map { it.copy(descripcion = it.descripcion.trim()) })
        .takeIf { ReglasSecciones.tareas(it) == EstadoSeccion.COMPLETA }
}
