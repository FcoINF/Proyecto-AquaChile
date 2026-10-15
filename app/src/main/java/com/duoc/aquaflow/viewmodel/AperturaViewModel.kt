package com.duoc.aquaflow.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.duoc.aquaflow.domain.AppError
import com.duoc.aquaflow.domain.AppResult
import com.duoc.aquaflow.domain.FieldErrors
import com.duoc.aquaflow.model.Buzo
import com.duoc.aquaflow.model.CentroCultivo
import com.duoc.aquaflow.model.Usuario
import com.duoc.aquaflow.repository.CatalogoRepository
import com.duoc.aquaflow.repository.FaenaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

data class AperturaUiState(
    val centros: List<CentroCultivo> = emptyList(),
    val buzos: List<Buzo> = emptyList(),
    val centroId: Long? = null,
    val buzoId: Long? = null,
    val fecha: LocalDate = LocalDate.now(),
    val hora: LocalTime = LocalTime.now().withSecond(0).withNano(0),
    val errores: FieldErrors = emptyMap(),
    val errorGeneral: String? = null,
    val guardando: Boolean = false,
    /** Id de la faena recién creada; la pantalla navega y lo consume. */
    val faenaCreadaId: Long? = null
)

class AperturaViewModel(
    private val actor: Usuario,
    private val faenas: FaenaRepository,
    catalogo: CatalogoRepository
) : ViewModel() {

    private val _state = MutableStateFlow(AperturaUiState())
    val state: StateFlow<AperturaUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            catalogo.observarCentros().collect { centros -> _state.update { it.copy(centros = centros) } }
        }
        viewModelScope.launch {
            catalogo.observarBuzos().collect { buzos -> _state.update { it.copy(buzos = buzos) } }
        }
    }

    fun onCentro(id: Long) = _state.update { it.copy(centroId = id, errores = it.errores - "centro", errorGeneral = null) }

    fun onBuzo(id: Long) = _state.update { it.copy(buzoId = id, errores = it.errores - "buzo", errorGeneral = null) }

    fun onFecha(fecha: LocalDate) = _state.update { it.copy(fecha = fecha, errores = it.errores - "fecha", errorGeneral = null) }

    fun onHora(hora: LocalTime) = _state.update { it.copy(hora = hora) }

    fun crear() {
        val actual = _state.value
        if (actual.guardando) return
        _state.update { it.copy(guardando = true, errorGeneral = null, errores = emptyMap()) }
        viewModelScope.launch {
            val inicio = LocalDateTime.of(actual.fecha, actual.hora)
                .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            when (val resultado = faenas.crearFaena(actor, actual.centroId, actual.buzoId, inicio)) {
                is AppResult.Ok -> _state.update { it.copy(guardando = false, faenaCreadaId = resultado.value) }
                is AppResult.Fail -> _state.update { estado ->
                    val error = resultado.error
                    if (error is AppError.Validation) {
                        estado.copy(guardando = false, errores = error.errores)
                    } else {
                        estado.copy(guardando = false, errorGeneral = error.mensaje)
                    }
                }
            }
        }
    }

    fun consumirCreada() = _state.update { it.copy(faenaCreadaId = null) }
}
