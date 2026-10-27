package com.duoc.aquaflow.viewmodel

import androidx.lifecycle.ViewModel
import com.duoc.aquaflow.model.Rol
import com.duoc.aquaflow.model.Usuario
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class IngresoUiState(
    val nombre: String = "",
    val clave: String = "",
    val rol: Rol = Rol.SUPERVISOR,
    val error: String? = null
)

class SesionViewModel : ViewModel() {
    private val usuarios = listOf(
        Usuario("supervisor", "1234", Rol.SUPERVISOR),
        Usuario("jefe", "1234", Rol.JEFE)
    )
    private val _usuario = MutableStateFlow<Usuario?>(null)
    val usuario: StateFlow<Usuario?> = _usuario.asStateFlow()
    private val _ingreso = MutableStateFlow(IngresoUiState())
    val ingreso = _ingreso.asStateFlow()

    fun cambiarNombre(valor: String) = _ingreso.update { it.copy(nombre = valor, error = null) }
    fun cambiarClave(valor: String) = _ingreso.update { it.copy(clave = valor, error = null) }
    fun seleccionarRol(rol: Rol) = _ingreso.update { it.copy(rol = rol, error = null) }

    fun ingresar(): Boolean {
        val datos = _ingreso.value
        if (datos.nombre.isBlank() || datos.clave.isBlank()) {
            _ingreso.update { it.copy(error = "Completa el usuario y la contraseña.") }
            return false
        }
        val valido = ingresar(datos.nombre, datos.clave, datos.rol)
        if (valido) _ingreso.value = IngresoUiState()
        else _ingreso.update { it.copy(error = "Revisa el usuario, la contraseña y el perfil seleccionado.") }
        return valido
    }

    // El perfil seleccionado nunca cambia los permisos de la cuenta.
    fun ingresar(nombre: String, clave: String, rol: Rol? = null): Boolean {
        _usuario.value = usuarios.find {
            it.nombre.equals(nombre.trim(), ignoreCase = true) &&
                it.clave == clave &&
                (rol == null || it.rol == rol)
        }
        return _usuario.value != null
    }

    fun salir() {
        _usuario.value = null
        _ingreso.value = IngresoUiState()
    }
}
