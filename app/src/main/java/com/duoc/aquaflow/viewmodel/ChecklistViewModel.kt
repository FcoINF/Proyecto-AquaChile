package com.duoc.aquaflow.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.duoc.aquaflow.model.ChecklistFaena
import com.duoc.aquaflow.model.EstadoEquipo
import com.duoc.aquaflow.model.ItemEquipamiento
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ChecklistUiState(
    val items: List<ItemEquipamiento> = emptyList(),
    val fotos: List<String> = emptyList(),
    val pendiente: String? = null,
    val error: String? = null
)

class ChecklistViewModel(private val saved: SavedStateHandle) : ViewModel() {
    private val _ui = MutableStateFlow(ChecklistUiState())
    val ui = _ui.asStateFlow()
    private var id = ""

    fun cargar(faenaId: String, inicial: ChecklistFaena?) {
        if (id == faenaId) return
        id = faenaId
        val restaurar = saved.get<String>("faena") == id
        val items = inicial?.items?.takeIf { it.isNotEmpty() } ?: listOf(
            ItemEquipamiento("1", "Compresor y filtros", ""),
            ItemEquipamiento("2", "Traje, arnés y casco", ""),
            ItemEquipamiento("3", "Botellón de rescate", ""),
            ItemEquipamiento("4", "Manguera umbilical y conexiones", "")
        )
        val estados = if (restaurar) saved.get<ArrayList<String>>("estados") else null
        _ui.value = ChecklistUiState(
            items = items.mapIndexed { i, item ->
                item.copy(estado = estados?.getOrNull(i)?.let { valor ->
                    EstadoEquipo.entries.find { it.name == valor }
                } ?: item.estado)
            },
            fotos = if (restaurar) saved.get<ArrayList<String>>("fotos").orEmpty() else inicial?.fotosEvidencia.orEmpty(),
            pendiente = if (restaurar) saved["pendiente"] else null
        )
        conservar()
    }

    fun cambiarEstado(itemId: String, estado: EstadoEquipo) {
        _ui.value = _ui.value.copy(items = _ui.value.items.map {
            if (it.id == itemId) it.copy(estado = estado) else it
        }, error = null)
        conservar()
    }

    fun prepararFoto(uri: String) {
        _ui.value = _ui.value.copy(pendiente = uri, error = null)
        conservar()
    }

    fun terminarFoto(exito: Boolean) {
        val actual = _ui.value
        _ui.value = actual.copy(
            pendiente = null,
            fotos = if (exito && actual.pendiente != null) (actual.fotos + actual.pendiente).distinct() else actual.fotos,
            error = if (exito) null else "No se guardó una fotografía. Puedes intentarlo nuevamente."
        )
        conservar()
    }

    fun errorCamara() {
        terminarFoto(false)
        _ui.value = _ui.value.copy(error = "No se pudo abrir la cámara. Comprueba que haya una aplicación de cámara disponible.")
    }

    fun quitarReferencia(uri: String) {
        _ui.value = _ui.value.copy(fotos = _ui.value.fotos - uri, error = null)
        conservar()
    }

    fun guardar(): ChecklistFaena? {
        if (_ui.value.items.isEmpty() || _ui.value.items.any { it.estado == EstadoEquipo.PENDIENTE }) {
            _ui.value = _ui.value.copy(error = "Revisa todos los equipos antes de continuar.")
            return null
        }
        return ChecklistFaena(id, _ui.value.items, _ui.value.fotos)
    }

    private fun conservar() {
        saved["faena"] = id
        saved["estados"] = ArrayList(_ui.value.items.map { it.estado.name })
        saved["fotos"] = ArrayList(_ui.value.fotos)
        saved["pendiente"] = _ui.value.pendiente
    }
}
