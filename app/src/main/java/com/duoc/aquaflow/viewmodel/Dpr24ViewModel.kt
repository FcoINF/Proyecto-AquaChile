package com.duoc.aquaflow.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.duoc.aquaflow.domain.AppResult
import com.duoc.aquaflow.domain.FieldErrors
import com.duoc.aquaflow.domain.config.ReglasConfig
import com.duoc.aquaflow.domain.rules.AccessPolicy
import com.duoc.aquaflow.domain.rules.EvaluacionSeccion
import com.duoc.aquaflow.domain.rules.FaenaEvaluator
import com.duoc.aquaflow.model.EstadoEquipo
import com.duoc.aquaflow.model.Evidencia
import com.duoc.aquaflow.model.RespuestaChecklist
import com.duoc.aquaflow.model.TipoEvidencia
import com.duoc.aquaflow.model.Usuario
import com.duoc.aquaflow.repository.EvidenciaArchivos
import com.duoc.aquaflow.repository.FaenaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ItemChecklistForm(val estado: EstadoEquipo? = null, val nota: String = "")

data class Dpr24UiState(
    val cargando: Boolean = true,
    val disponible: Boolean = true,
    val codigo: String = "",
    val editable: Boolean = false,
    val items: Map<String, ItemChecklistForm> = emptyMap(),
    val evidencias: List<Evidencia> = emptyList(),
    val evaluacion: EvaluacionSeccion = EvaluacionSeccion(),
    val sinGuardar: Boolean = false,
    val guardando: Boolean = false,
    val errores: FieldErrors = emptyMap(),
    val mensaje: String? = null
)

class Dpr24ViewModel(
    private val actor: Usuario,
    private val faenaId: Long,
    private val faenas: FaenaRepository,
    private val archivos: EvidenciaArchivos,
    private val reglas: ReglasConfig
) : ViewModel() {

    private val _state = MutableStateFlow(Dpr24UiState())
    val state: StateFlow<Dpr24UiState> = _state.asStateFlow()

    private var inicializado = false
    private var guardado: Map<String, ItemChecklistForm> = emptyMap()

    init {
        viewModelScope.launch {
            faenas.observarDetalle(actor, faenaId).collect { detalle ->
                if (detalle == null) {
                    _state.update { it.copy(cargando = false, disponible = false) }
                } else {
                    if (!inicializado) {
                        inicializado = true
                        guardado = detalle.checklist.associate { it.itemId to ItemChecklistForm(it.estado, it.nota) }
                        _state.update { it.copy(items = guardado) }
                    }
                    _state.update {
                        it.copy(
                            cargando = false,
                            disponible = true,
                            codigo = detalle.faena.codigo,
                            editable = AccessPolicy.puedeEditarPrechequeo(actor, detalle.faena),
                            evidencias = detalle.evidencias,
                            evaluacion = FaenaEvaluator.evaluarChecklist(detalle.checklist, detalle.evidencias, reglas)
                        )
                    }
                }
            }
        }
    }

    private fun modificar(transformar: (Map<String, ItemChecklistForm>) -> Map<String, ItemChecklistForm>) =
        _state.update { s ->
            if (!s.editable) {
                s
            } else {
                val nuevo = transformar(s.items)
                s.copy(items = nuevo, sinGuardar = nuevo != guardado, errores = emptyMap())
            }
        }

    fun onEstado(itemId: String, estado: EstadoEquipo) = modificar { items ->
        items + (itemId to (items[itemId] ?: ItemChecklistForm()).copy(estado = estado))
    }

    fun onNota(itemId: String, nota: String) = modificar { items ->
        items + (itemId to (items[itemId] ?: ItemChecklistForm()).copy(nota = nota.take(300)))
    }

    fun guardar() {
        val s = _state.value
        if (!s.editable || s.guardando) return
        val respuestas = s.items.mapNotNull { (id, form) ->
            form.estado?.let { RespuestaChecklist(faenaId = faenaId, itemId = id, estado = it, nota = form.nota) }
        }
        _state.update { it.copy(guardando = true) }
        viewModelScope.launch {
            when (val r = faenas.guardarChecklist(actor, faenaId, respuestas)) {
                is AppResult.Ok -> {
                    guardado = s.items
                    _state.update {
                        it.copy(guardando = false, sinGuardar = it.items != guardado, mensaje = "Checklist guardado.")
                    }
                }
                is AppResult.Fail -> _state.update {
                    it.copy(guardando = false, errores = r.error.camposConError(), mensaje = r.error.mensaje)
                }
            }
        }
    }

    /** Registra una foto ya almacenada en disco. Si no se puede registrar, se borra el archivo. */
    fun agregarEvidencia(tipo: TipoEvidencia, itemId: String?, rutaRelativa: String) {
        viewModelScope.launch {
            when (val r = faenas.agregarEvidencia(actor, faenaId, tipo, itemId, rutaRelativa)) {
                is AppResult.Ok -> _state.update { it.copy(mensaje = "Evidencia guardada.") }
                is AppResult.Fail -> {
                    archivos.eliminar(rutaRelativa)
                    _state.update { it.copy(mensaje = r.error.mensaje) }
                }
            }
        }
    }

    fun eliminarEvidencia(evidenciaId: Long) {
        viewModelScope.launch {
            when (val r = faenas.eliminarEvidencia(actor, evidenciaId)) {
                is AppResult.Ok -> _state.update { it.copy(mensaje = "Evidencia eliminada.") }
                is AppResult.Fail -> _state.update { it.copy(mensaje = r.error.mensaje) }
            }
        }
    }

    fun avisar(mensaje: String) = _state.update { it.copy(mensaje = mensaje) }

    fun consumirMensaje() = _state.update { it.copy(mensaje = null) }
}
