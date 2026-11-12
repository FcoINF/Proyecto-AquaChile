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
                titulo = "Maleta de Suministro de Aire",
                descripcion = "Verificación de manifold, válvulas de alivio, manómetro análogo y presurización de línea de alimentación principal.",
                categoria = "EQUIPO 01 • SUMINISTRO PRIMARIO",
                badge = "FOTO OBLIGATORIA"
            ),
            ItemEquipamiento(
                id = "2",
                titulo = "Filtros del Compresor",
                descripcion = "Test de CO/CO2 con tubo colorimétrico OK. Purgas automáticas drenadas y banco presurizado.",
                categoria = "EQUIPO 02 • COMPRESOR",
                badge = "FOTO OBLIGATORIA"
            ),
            ItemEquipamiento(
                id = "3",
                titulo = "Aletas y Equipamiento Personal",
                descripcion = "Prueba de tirantes de aletas, ensamble de hebillas y estanqueidad de traje seco/casco KMB.",
                categoria = "EQUIPO 03 • INDIVIDUAL BUZO",
                badge = "FOTO OBLIGATORIA"
            ),
            ItemEquipamiento(
                id = "4",
                titulo = "Dispositivo Census",
                descripcion = "Consola de monitoreo de profundidad, velocidad de ascenso/descenso y telemetría de inmersión conectada.",
                categoria = "EQUIPO 04 • MONITOREO Y CONTROL",
                badge = "FOTO OBLIGATORIA"
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
        if (_ui.value.fotos.size < 4) {
            val faltantes = 4 - _ui.value.fotos.size
            _ui.value = _ui.value.copy(
                error = "Debes capturar las 4 fotos obligatorias (Maleta de Aire, Filtros Compresor, Aletas y Dispositivo Census) antes de continuar. Faltan $faltantes captura(s)."
            )
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
