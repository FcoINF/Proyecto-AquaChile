package com.duoc.aquaflow.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.duoc.aquaflow.domain.AppResult
import com.duoc.aquaflow.domain.config.ReglasConfig
import com.duoc.aquaflow.domain.rules.AccessPolicy
import com.duoc.aquaflow.domain.rules.EvaluacionFaena
import com.duoc.aquaflow.domain.rules.FaenaEvaluator
import com.duoc.aquaflow.model.FaenaDetalle
import com.duoc.aquaflow.model.Usuario
import com.duoc.aquaflow.repository.AuthRepository
import com.duoc.aquaflow.repository.FaenaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DecisionUiState(
    val cargando: Boolean = true,
    val disponible: Boolean = true,
    val detalle: FaenaDetalle? = null,
    val evaluacion: EvaluacionFaena? = null,
    /** El supervisor responsable puede firmar y enviar (información completa, faena en pre-chequeo). */
    val puedeEnviar: Boolean = false,
    val puedeDecidir: Boolean = false,
    val puedeAutorizar: Boolean = false,
    val operando: Boolean = false,
    /** Error mostrado dentro del diálogo de PIN o de motivo. */
    val errorDialogo: String? = null,
    val mensaje: String? = null
)

class DecisionViewModel(
    private val actor: Usuario,
    private val faenaId: Long,
    private val faenas: FaenaRepository,
    private val auth: AuthRepository,
    private val reglas: ReglasConfig
) : ViewModel() {

    private val _state = MutableStateFlow(DecisionUiState())
    val state: StateFlow<DecisionUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            faenas.observarDetalle(actor, faenaId).collect { detalle ->
                _state.update {
                    if (detalle == null) {
                        it.copy(cargando = false, disponible = false, detalle = null, evaluacion = null)
                    } else {
                        val evaluacion = FaenaEvaluator.evaluar(detalle, reglas)
                        val puedeDecidir = AccessPolicy.puedeDecidir(actor, detalle.faena)
                        it.copy(
                            cargando = false,
                            disponible = true,
                            detalle = detalle,
                            evaluacion = evaluacion,
                            puedeEnviar = AccessPolicy.puedeEditarPrechequeo(actor, detalle.faena) && evaluacion.puedeEnviarARevision,
                            puedeDecidir = puedeDecidir,
                            puedeAutorizar = puedeDecidir && evaluacion.puedeAutorizar
                        )
                    }
                }
            }
        }
    }

    /** Firma del supervisor con su PIN y envío a revisión. [alTerminar] se invoca solo si todo salió bien. */
    fun firmarYEnviar(pin: String, alTerminar: () -> Unit) = ejecutarConPin(pin, "Faena firmada y enviada a revisión.", alTerminar) {
        faenas.enviarARevision(actor, faenaId)
    }

    fun autorizar(pin: String, alTerminar: () -> Unit) = ejecutarConPin(pin, "Faena autorizada.", alTerminar) {
        faenas.autorizar(actor, faenaId)
    }

    fun rechazar(motivo: String, alTerminar: () -> Unit) {
        if (_state.value.operando) return
        _state.update { it.copy(operando = true, errorDialogo = null) }
        viewModelScope.launch {
            when (val r = faenas.rechazar(actor, faenaId, motivo)) {
                is AppResult.Ok -> {
                    _state.update { it.copy(operando = false, mensaje = "Faena rechazada.") }
                    alTerminar()
                }
                is AppResult.Fail -> _state.update {
                    it.copy(operando = false, errorDialogo = r.error.camposConError()["motivo"] ?: r.error.mensaje)
                }
            }
        }
    }

    fun limpiarErrorDialogo() = _state.update { it.copy(errorDialogo = null) }

    fun consumirMensaje() = _state.update { it.copy(mensaje = null) }

    private fun ejecutarConPin(
        pin: String,
        mensajeOk: String,
        alTerminar: () -> Unit,
        operacion: suspend () -> AppResult<Unit>
    ) {
        if (_state.value.operando) return
        _state.update { it.copy(operando = true, errorDialogo = null) }
        viewModelScope.launch {
            if (!auth.verifyPin(actor.id, pin)) {
                _state.update { it.copy(operando = false, errorDialogo = "PIN incorrecto o bloqueado temporalmente.") }
                return@launch
            }
            when (val r = operacion()) {
                is AppResult.Ok -> {
                    _state.update { it.copy(operando = false, mensaje = mensajeOk) }
                    alTerminar()
                }
                is AppResult.Fail -> _state.update { it.copy(operando = false, errorDialogo = r.error.mensaje) }
            }
        }
    }
}
