package com.duoc.aquaflow.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.duoc.aquaflow.data.repository.BorradoresApertura
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import com.duoc.aquaflow.domain.Reglas
import com.duoc.aquaflow.model.EncuestaSalud
import com.duoc.aquaflow.model.FormularioAst
import com.duoc.aquaflow.model.PreChequeo
import com.duoc.aquaflow.model.RegistroDecision
import com.duoc.aquaflow.model.Rol
import com.duoc.aquaflow.model.Usuario
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class FaenaUiState(
    val apertura: PreChequeo = PreChequeo(),
    val aperturaConfirmada: Boolean = false,
    val ast: FormularioAst? = null,
    val encuesta: EncuestaSalud? = null,
    val decision: RegistroDecision? = null,
    val error: String? = null,
    val ocupado: Boolean = false,
    val mensaje: String? = null,
)

// Borrador compartido por las pantallas del grafo "faena".
class FaenaViewModel(private val borradores: BorradoresApertura) : ViewModel() {
    private val _ui = MutableStateFlow(FaenaUiState())
    val ui: StateFlow<FaenaUiState> = _ui.asStateFlow()

    fun iniciar(usuario: Usuario) {
        if (usuario.rol != Rol.SUPERVISOR || _ui.value.apertura.id.isNotEmpty()) return
        val ahora = Date()
        _ui.value = FaenaUiState(
            apertura = PreChequeo(
                id = UUID.randomUUID().toString(),
                fecha = SimpleDateFormat("dd/MM/yyyy", Locale.ROOT).format(ahora),
                hora = SimpleDateFormat("HH:mm", Locale.ROOT).format(ahora),
                supervisorACargo = usuario.nombre
            )
        )
    }

    fun actualizar(apertura: PreChequeo) = _ui.update { actual ->
        if (actual.ocupado) return@update actual
        // La identidad y el supervisor provienen de la sesión, no del formulario.
        val nueva = apertura.copy(
            id = actual.apertura.id,
            supervisorACargo = actual.apertura.supervisorACargo
        )
        if (nueva == actual.apertura) actual else actual.copy(
            apertura = nueva,
            aperturaConfirmada = false,
            encuesta = null,
            ast = null,
            decision = null,
            error = null,
            mensaje = null
        )
    }

    fun confirmarApertura(): Boolean {
        val actual = _ui.value.apertura
        val limpia = actual.copy(
            centroCultivo = actual.centroCultivo.trim(),
            fecha = actual.fecha.trim(),
            hora = actual.hora.trim(),
            buzoAsignado = actual.buzoAsignado.trim(),
            supervisorACargo = actual.supervisorACargo.trim()
        )
        val error = Reglas.errorApertura(limpia)
        _ui.update {
            it.copy(apertura = limpia, aperturaConfirmada = error == null, error = error)
        }
        return error == null
    }

    // Un borrador puede estar incompleto. Para avanzar sí se valida todo.
    fun guardarApertura(continuar: Boolean = false, alGuardar: () -> Unit = {}) {
        if (_ui.value.ocupado || _ui.value.apertura.id.isBlank()) return
        if (continuar && !confirmarApertura()) return
        val apertura = _ui.value.apertura
        _ui.update { it.copy(ocupado = true, error = null, mensaje = null) }
        viewModelScope.launch {
            try {
                borradores.guardar(apertura)
                _ui.update { it.copy(mensaje = "Apertura guardada en el dispositivo.") }
                alGuardar()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _ui.update { it.copy(error = "No se pudo guardar la apertura. Intenta nuevamente.") }
            } finally {
                _ui.update { it.copy(ocupado = false) }
            }
        }
    }

    fun recuperarApertura() {
        val actual = _ui.value
        if (actual.ocupado || actual.apertura.id.isBlank()) return
        _ui.update { it.copy(ocupado = true, error = null, mensaje = null) }
        viewModelScope.launch {
            try {
                val apertura = borradores.recuperar(actual.apertura.supervisorACargo)
                _ui.value = if (apertura == null) {
                    actual.copy(mensaje = "No tienes aperturas guardadas.")
                } else {
                    FaenaUiState(apertura = apertura, mensaje = "Apertura recuperada. Revisa los datos antes de continuar.")
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _ui.update { it.copy(error = "No se pudo recuperar la apertura. Intenta nuevamente.") }
            } finally {
                _ui.update { it.copy(ocupado = false) }
            }
        }
    }

    fun registrarEncuesta(encuesta: EncuestaSalud): Boolean {
        if (!_ui.value.aperturaConfirmada) return false
        _ui.update { it.copy(encuesta = encuesta, decision = null) }
        return true
    }

    fun registrarAst(ast: FormularioAst): Boolean {
        if (!_ui.value.aperturaConfirmada) return false
        _ui.update { it.copy(ast = ast, decision = null) }
        return true
    }

    fun registrarDecision(decision: RegistroDecision): Boolean {
        if (!_ui.value.aperturaConfirmada) return false
        _ui.update { it.copy(decision = decision) }
        return true
    }
}
