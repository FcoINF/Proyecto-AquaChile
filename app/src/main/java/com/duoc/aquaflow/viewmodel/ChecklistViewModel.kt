package com.duoc.aquaflow.viewmodel

import androidx.lifecycle.SavedStateHandle
import com.duoc.aquaflow.model.EstadoSeccion
import androidx.lifecycle.ViewModel
import com.duoc.aquaflow.data.local.FaenaCodec
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
    private val restaurado = saved.get<String>("checklist")?.let(FaenaCodec::checklistDesde)
    private val _ui = MutableStateFlow(ChecklistUiState(
        items = restaurado?.items.orEmpty(), fotos = restaurado?.fotosEvidencia.orEmpty(),
        pendiente = saved["pendiente"]
    ))
    val ui = _ui.asStateFlow()
    private var id = restaurado?.idFaena.orEmpty()

    fun cargar(faenaId: String, inicial: ChecklistFaena?) {
        if (id == faenaId) return
        id = faenaId
        val restaurar = saved.get<String>("faena") == id
        val items = inicial?.items?.takeIf { it.isNotEmpty() } ?: listOf(
            ItemEquipamiento(
                id = "1",
                titulo = "Compresor, Purgas y Filtros Carbón",
                descripcion = "Test de CO/CO2 con tubo colorimétrico OK. Purgas automáticas drenadas y banco presurizado.",
                categoria = "EQUIPO 01 • SUMINISTRO PRIMARIO",
                badge = "220 BAR"
            ),
            ItemEquipamiento(
                id = "2",
                titulo = "Traje Seco, Arnés y Casco KMB",
                descripcion = "Prueba de estanqueidad de cremallera estanca, diafragma buconasal y fonía bidireccional limpia.",
                categoria = "EQUIPO 02 • PERSONAL",
                badge = "BUZO #1"
            ),
            ItemEquipamiento(
                id = "3",
                titulo = "Botellón de Rescate (Bail-out)",
                descripcion = "Presión 210 BAR verificada en manómetro análogo sumergible. Conmutación manual activa.",
                categoria = "EQUIPO 03 • RESPALDO RESCATE",
                badge = "210 BAR"
            ),
            ItemEquipamiento(
                id = "4",
                titulo = "Manguera Umbilical y Conexiones",
                descripcion = "Roco superficial en manga protectora externa 15m. Conexiones neumáticas y audio 100% funcionales.",
                categoria = "EQUIPO 04 • DISTRIBUCIÓN Y LÍNEA",
                badge = "OBSERVACIÓN",
            )
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

    fun borrador(): ChecklistFaena = ChecklistFaena(id, _ui.value.items, _ui.value.fotos, EstadoSeccion.BORRADOR)

    private fun conservar() {
        saved["checklist"] = FaenaCodec.checklistATexto(ChecklistFaena(id, _ui.value.items, _ui.value.fotos))
        saved["faena"] = id
        saved["estados"] = ArrayList(_ui.value.items.map { it.estado.name })
        saved["fotos"] = ArrayList(_ui.value.fotos)
        saved["pendiente"] = _ui.value.pendiente
    }
}
