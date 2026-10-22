package com.duoc.aquaflow.viewmodel

import androidx.lifecycle.ViewModel
import com.duoc.aquaflow.domain.Reglas
import com.duoc.aquaflow.model.EncuestaSalud
import com.duoc.aquaflow.model.PreChequeo
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
import com.duoc.aquaflow.model.FormularioAst

data class FaenaUiState(
    val apertura: PreChequeo = PreChequeo(),
    val aperturaConfirmada: Boolean = false,
    val ast: FormularioAst? = null,
    val encuesta: EncuestaSalud? = null,
    val error: String? = null,
)

// Borrador compartido por las pantallas del grafo "faena".
class FaenaViewModel : ViewModel() {
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
            error = null
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

    fun registrarEncuesta(encuesta: EncuestaSalud): Boolean {
        if (!_ui.value.aperturaConfirmada) return false
        _ui.update { it.copy(encuesta = encuesta) }
        return true
    }
    fun registrarAst(ast: FormularioAst): Boolean {
        if (!_ui.value.aperturaConfirmada) return false
        _ui.update { it.copy(ast = ast) }
        return true
    }
}
