package com.duoc.aquaflow.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.duoc.aquaflow.domain.AppError
import com.duoc.aquaflow.domain.AppResult
import com.duoc.aquaflow.domain.FieldErrors
import com.duoc.aquaflow.domain.config.ReglasConfig
import com.duoc.aquaflow.domain.rules.AccessPolicy
import com.duoc.aquaflow.domain.rules.EvaluacionFaena
import com.duoc.aquaflow.domain.rules.FaenaEvaluator
import com.duoc.aquaflow.model.FaenaDetalle
import com.duoc.aquaflow.model.Usuario
import com.duoc.aquaflow.repository.FaenaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FaenaHubUiState(
    val cargando: Boolean = true,
    val disponible: Boolean = true,
    val detalle: FaenaDetalle? = null,
    val evaluacion: EvaluacionFaena? = null,
    val puedeCancelar: Boolean = false,
    val puedeRegistrarPosterior: Boolean = false,
    val errorCancelar: String? = null,
    val mensaje: String? = null
)

class FaenaHubViewModel(
    private val actor: Usuario,
    private val faenaId: Long,
    private val faenas: FaenaRepository,
    private val reglas: ReglasConfig
) : ViewModel() {

    private val _state = MutableStateFlow(FaenaHubUiState())
    val state: StateFlow<FaenaHubUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            faenas.observarDetalle(actor, faenaId).collect { detalle ->
                _state.update {
                    if (detalle == null) {
                        it.copy(cargando = false, disponible = false, detalle = null, evaluacion = null)
                    } else {
                        it.copy(
                            cargando = false,
                            disponible = true,
                            detalle = detalle,
                            evaluacion = FaenaEvaluator.evaluar(detalle, reglas),
                            puedeCancelar = AccessPolicy.puedeEditarPrechequeo(actor, detalle.faena),
                            puedeRegistrarPosterior = AccessPolicy.puedeRegistrarPosterior(actor, detalle.faena)
                        )
                    }
                }
            }
        }
    }

    fun cancelar(motivo: String, alTerminar: () -> Unit) {
        viewModelScope.launch {
            when (val resultado = faenas.cancelar(actor, faenaId, motivo)) {
                is AppResult.Ok -> {
                    _state.update { it.copy(errorCancelar = null, mensaje = "Faena cancelada.") }
                    alTerminar()
                }
                is AppResult.Fail -> _state.update { it.copy(errorCancelar = mensajeDe(resultado.error)) }
            }
        }
    }

    fun limpiarErrorCancelar() = _state.update { it.copy(errorCancelar = null) }

    fun consumirMensaje() = _state.update { it.copy(mensaje = null) }

    private fun mensajeDe(error: AppError): String {
        val campos: FieldErrors = error.camposConError()
        return campos["motivo"] ?: error.mensaje
    }
}
