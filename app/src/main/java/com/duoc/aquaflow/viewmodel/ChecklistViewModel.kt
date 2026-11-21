package com.duoc.aquaflow.viewmodel

import androidx.lifecycle.SavedStateHandle
import com.duoc.aquaflow.model.EstadoSeccion
import androidx.lifecycle.ViewModel
import com.duoc.aquaflow.data.local.FaenaCodec
import com.duoc.aquaflow.model.ChecklistFaena
import com.duoc.aquaflow.model.EstadoEquipo
import com.duoc.aquaflow.model.ItemEquipamiento
import com.duoc.aquaflow.model.MetadatosFoto
import com.duoc.aquaflow.model.fotosAsociadasPorEquipo
import com.duoc.aquaflow.domain.ReglasSecciones
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ChecklistUiState(
    val items: List<ItemEquipamiento> = emptyList(),
    val fotos: List<String> = emptyList(),
    val fotosPorEquipo: Map<String, String> = emptyMap(),
    val pendiente: String? = null,
    val pendienteEquipoId: String? = null,
    val error: String? = null,
    val metadatosPorFoto: Map<String, MetadatosFoto> = emptyMap()
)

class ChecklistViewModel(private val saved: SavedStateHandle) : ViewModel() {
    private val restaurado = saved.get<String>("checklist")?.let(FaenaCodec::checklistDesde)
    private val _ui = MutableStateFlow(ChecklistUiState(
        items = restaurado?.items.orEmpty(), fotos = restaurado?.fotosEvidencia.orEmpty(),
        fotosPorEquipo = restaurado?.fotosAsociadasPorEquipo().orEmpty(),
        pendiente = saved["pendiente"], pendienteEquipoId = saved["pendienteEquipoId"],
        metadatosPorFoto = restaurado?.metadatosPorFoto.orEmpty()
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
            fotos = if (restaurar) restaurado?.fotosEvidencia.orEmpty() else inicial?.fotosEvidencia.orEmpty(),
            fotosPorEquipo = if (restaurar) restaurado?.fotosAsociadasPorEquipo().orEmpty()
                else inicial?.fotosAsociadasPorEquipo().orEmpty(),
            pendiente = if (restaurar) saved["pendiente"] else null,
            pendienteEquipoId = if (restaurar) saved["pendienteEquipoId"] else null,
            metadatosPorFoto = (if (restaurar) restaurado else inicial)?.metadatosPorFoto.orEmpty()
        )
        conservar()
    }

    fun cambiarEstado(itemId: String, estado: EstadoEquipo) {
        _ui.value = _ui.value.copy(items = _ui.value.items.map {
            if (it.id == itemId) it.copy(estado = estado) else it
        }, error = null)
        conservar()
    }

    fun prepararFoto(itemId: String, uri: String) {
        require(_ui.value.items.any { it.id == itemId }) { "El equipo no pertenece al checklist." }
        _ui.value = _ui.value.copy(pendiente = uri, pendienteEquipoId = itemId, error = null)
        conservar()
    }

    fun prepararFoto(uri: String) {
        val itemId = _ui.value.items.firstOrNull { it.id !in _ui.value.fotosPorEquipo }?.id
            ?: _ui.value.items.firstOrNull()?.id
            ?: return
        prepararFoto(itemId, uri)
    }

    fun terminarFoto(exito: Boolean, responsable: String = "") {
        val actual = _ui.value
        val itemId = actual.pendienteEquipoId
        val nuevasAsignaciones = if (exito && actual.pendiente != null && itemId != null) {
            actual.fotosPorEquipo + (itemId to actual.pendiente)
        } else actual.fotosPorEquipo
        val anteriores = actual.metadatosPorFoto.filterKeys { it in nuevasAsignaciones.values }
        val metadatos = if (exito && actual.pendiente != null && itemId != null) {
            val anterior = actual.fotosPorEquipo[itemId]?.let(actual.metadatosPorFoto::get)
            anteriores + (actual.pendiente to MetadatosFoto(
                System.currentTimeMillis(), responsable, referencia = anterior?.referencia
            ))
        } else anteriores
        _ui.value = actual.copy(
            pendiente = null,
            pendienteEquipoId = null,
            fotosPorEquipo = nuevasAsignaciones,
            fotos = nuevasAsignaciones.values.distinct(),
            metadatosPorFoto = metadatos,
            error = if (exito) null else "No se guardó una fotografía. Puedes intentarlo nuevamente."
        )
        conservar()
    }

    fun errorCamara() {
        terminarFoto(false)
        _ui.value = _ui.value.copy(error = "No se pudo abrir la cámara. Comprueba que haya una aplicación de cámara disponible.")
    }

    fun quitarReferencia(uri: String) {
        val asignaciones = _ui.value.fotosPorEquipo.filterValues { it != uri }
        _ui.value = _ui.value.copy(
            fotos = asignaciones.values.distinct(), fotosPorEquipo = asignaciones, error = null,
            metadatosPorFoto = _ui.value.metadatosPorFoto - uri
        )
        conservar()
    }

    fun guardar(): ChecklistFaena? {
        if (_ui.value.items.isEmpty() || _ui.value.items.any { it.estado == EstadoEquipo.PENDIENTE }) {
            _ui.value = _ui.value.copy(error = "Revisa todos los equipos antes de continuar.")
            return null
        }
        val noCumplen = _ui.value.items.filter { it.estado == EstadoEquipo.NO_CUMPLE }
        if (noCumplen.isNotEmpty()) {
            _ui.value = _ui.value.copy(
                error = "Bloqueo de seguridad: ${noCumplen.joinToString { it.titulo }} no cumple. Una fotografía no corrige un equipo crítico en rojo."
            )
            return null
        }
        val equiposSinFoto = _ui.value.items.filter { _ui.value.fotosPorEquipo[it.id].isNullOrBlank() }
        if (equiposSinFoto.isNotEmpty()) {
            _ui.value = _ui.value.copy(
                error = "Falta una foto asociada a: ${equiposSinFoto.joinToString { it.titulo }}."
            )
            return null
        }
        val checklist = checklist(EstadoSeccion.COMPLETA)
        check(ReglasSecciones.checklistHabilitaContinuidad(checklist)) { "El checklist crítico no puede completarse." }
        return checklist
    }

    fun borrador(): ChecklistFaena = checklist(EstadoSeccion.BORRADOR)

    fun referencia(uri: String, referencia: String) {
        val estado = _ui.value
        if (uri !in estado.fotos) return
        val foto = estado.metadatosPorFoto[uri] ?: MetadatosFoto()
        actualizarFoto(uri, foto.copy(referencia = referencia, coincide = null))
    }

    fun comparar(uri: String, coincide: Boolean) {
        val foto = _ui.value.metadatosPorFoto[uri]?.takeIf { it.referencia != null } ?: return
        actualizarFoto(uri, foto.copy(coincide = coincide))
    }

    fun errorReferencia() {
        _ui.value = _ui.value.copy(error = "No se pudo importar la imagen de referencia.")
    }

    private fun actualizarFoto(uri: String, foto: MetadatosFoto) {
        _ui.value = _ui.value.copy(metadatosPorFoto = _ui.value.metadatosPorFoto + (uri to foto), error = null)
        conservar()
    }

    private fun checklist(estado: EstadoSeccion) = ChecklistFaena(
        id, _ui.value.items, _ui.value.fotos, estado, _ui.value.fotosPorEquipo, _ui.value.metadatosPorFoto
    )

    private fun conservar() {
        saved["checklist"] = FaenaCodec.checklistATexto(checklist(EstadoSeccion.COMPLETA))
        saved["faena"] = id
        saved["estados"] = ArrayList(_ui.value.items.map { it.estado.name })
        saved["fotos"] = ArrayList(_ui.value.fotos)
        saved["pendiente"] = _ui.value.pendiente
        saved["pendienteEquipoId"] = _ui.value.pendienteEquipoId
    }
}
