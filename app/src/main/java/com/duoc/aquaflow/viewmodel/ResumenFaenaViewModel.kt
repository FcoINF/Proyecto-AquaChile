package com.duoc.aquaflow.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import com.duoc.aquaflow.domain.ReglasDecision
import com.duoc.aquaflow.model.Decision
import com.duoc.aquaflow.model.RegistroDecision
import com.duoc.aquaflow.model.ResumenFaena
import com.duoc.aquaflow.model.Rol
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class ResumenUiState(
    val motivo: String = "",
    val error: String? = null,
    val enviando: Boolean = false
)

// Guarda el motivo que escribe la jefatura y valida la decisión.
// La decisión ya tomada vive en FaenaViewModel, junto al resto de la faena.
class ResumenFaenaViewModel : ViewModel() {
    private val _ui = MutableStateFlow(ResumenUiState())
    val ui: StateFlow<ResumenUiState> = _ui.asStateFlow()

    fun cambiarMotivo(v: String) = _ui.update { it.copy(motivo = v.take(200), error = null) }

    fun enviar(operacion: suspend () -> Unit, onEnviado: () -> Unit) {
        if (_ui.value.enviando) return
        _ui.update { it.copy(enviando = true, error = null) }
        viewModelScope.launch {
            try {
                operacion()
                onEnviado()
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                _ui.update { it.copy(error = error.message ?: "No se pudo enviar el expediente.") }
            } finally {
                _ui.update { it.copy(enviando = false) }
            }
        }
    }

    // Devuelve el registro si la decisión es válida; si no, deja el mensaje en `ui.error` y devuelve null.
    fun decidir(decision: Decision, resumen: ResumenFaena, rol: Rol, nombre: String): RegistroDecision? {
        val motivo = _ui.value.motivo.trim()
        val resultado = ReglasDecision.resultado(resumen)
        // La primera validación que falle define el mensaje que ve el usuario
        val error = when {
            !ReglasDecision.puedeDecidir(rol) -> "Solo la jefatura puede autorizar o rechazar"
            resultado == null -> "Faltan datos para decidir: ${ReglasDecision.faltantes(resumen).joinToString()}"
            else -> ReglasDecision.errorDecision(decision, resultado, motivo)
        }
        if (error != null) {
            _ui.update { it.copy(error = error) }
            return null
        }
        _ui.update { it.copy(error = null) }
        return RegistroDecision(decision, motivo, nombre)
    }
}
