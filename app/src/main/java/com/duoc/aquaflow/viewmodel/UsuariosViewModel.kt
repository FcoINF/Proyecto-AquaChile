package com.duoc.aquaflow.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.duoc.aquaflow.domain.AppResult
import com.duoc.aquaflow.domain.FieldErrors
import com.duoc.aquaflow.model.Rol
import com.duoc.aquaflow.model.Usuario
import com.duoc.aquaflow.repository.UsuarioRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NuevoUsuarioForm(
    val nombre: String = "",
    val rut: String = "",
    val rol: Rol = Rol.SUPERVISOR,
    val pin: String = "",
    val pinConfirmacion: String = ""
)

data class UsuariosUiState(
    val usuarios: List<Usuario> = emptyList(),
    val dialogoAbierto: Boolean = false,
    val form: NuevoUsuarioForm = NuevoUsuarioForm(),
    val errores: FieldErrors = emptyMap(),
    val guardando: Boolean = false,
    val mensaje: String? = null
)

class UsuariosViewModel(
    private val actor: Usuario,
    private val usuarios: UsuarioRepository
) : ViewModel() {

    private val _state = MutableStateFlow(UsuariosUiState())
    val state: StateFlow<UsuariosUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            usuarios.observarUsuarios().collect { lista -> _state.update { it.copy(usuarios = lista) } }
        }
    }

    fun abrirDialogo() = _state.update { it.copy(dialogoAbierto = true, form = NuevoUsuarioForm(), errores = emptyMap()) }

    fun cerrarDialogo() = _state.update { it.copy(dialogoAbierto = false, form = NuevoUsuarioForm(), errores = emptyMap()) }

    fun onNombre(v: String) = _state.update { it.copy(form = it.form.copy(nombre = v.take(60)), errores = it.errores - "nombre") }
    fun onRut(v: String) = _state.update {
        it.copy(form = it.form.copy(rut = v.filter { c -> c.isLetterOrDigit() || c == '.' || c == '-' }.take(12)), errores = it.errores - "rut")
    }
    fun onRol(v: Rol) = _state.update { it.copy(form = it.form.copy(rol = v)) }
    fun onPin(v: String) = _state.update {
        it.copy(form = it.form.copy(pin = v.filter { c -> c.isDigit() }.take(4)), errores = it.errores - "pin")
    }
    fun onPinConfirmacion(v: String) = _state.update {
        it.copy(form = it.form.copy(pinConfirmacion = v.filter { c -> c.isDigit() }.take(4)), errores = it.errores - "pinConfirmacion")
    }

    fun crear() {
        val s = _state.value
        if (s.guardando) return
        _state.update { it.copy(guardando = true) }
        viewModelScope.launch {
            val f = s.form
            when (val r = usuarios.crear(actor, f.nombre, f.rut, f.rol, f.pin, f.pinConfirmacion)) {
                is AppResult.Ok -> _state.update {
                    it.copy(guardando = false, dialogoAbierto = false, form = NuevoUsuarioForm(), mensaje = "Usuario creado.")
                }
                is AppResult.Fail -> _state.update {
                    val campos = r.error.camposConError()
                    it.copy(guardando = false, errores = campos, mensaje = if (campos.isEmpty()) r.error.mensaje else null)
                }
            }
        }
    }

    fun cambiarActivo(usuario: Usuario, activo: Boolean) {
        viewModelScope.launch {
            when (val r = usuarios.cambiarActivo(actor, usuario.id, activo)) {
                is AppResult.Ok -> _state.update {
                    it.copy(mensaje = if (activo) "${usuario.nombre} fue activado." else "${usuario.nombre} fue desactivado.")
                }
                is AppResult.Fail -> _state.update { it.copy(mensaje = r.error.mensaje) }
            }
        }
    }

    fun consumirMensaje() = _state.update { it.copy(mensaje = null) }
}
