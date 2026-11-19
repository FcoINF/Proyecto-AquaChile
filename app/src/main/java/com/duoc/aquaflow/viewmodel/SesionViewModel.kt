package com.duoc.aquaflow.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.duoc.aquaflow.data.remote.FaenasRemotas
import com.duoc.aquaflow.model.Rol
import com.duoc.aquaflow.model.Usuario
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class IngresoUiState(
    val nombre: String = "",
    val clave: String = "",
    val rol: Rol = Rol.SUPERVISOR,
    val error: String? = null,
    val ocupado: Boolean = false
)

class SesionViewModel(val remoto: FaenasRemotas? = null) : ViewModel() {
    private val usuarios = listOf(
        Usuario("supervisor", "1234", Rol.SUPERVISOR),
        Usuario("jefe", "1234", Rol.JEFE)
    )
    private val _usuario = MutableStateFlow<Usuario?>(null)
    val usuario: StateFlow<Usuario?> = _usuario.asStateFlow()
    private val _ingreso = MutableStateFlow(IngresoUiState())
    val ingreso = _ingreso.asStateFlow()
    private var autenticacion: Job? = null

    fun cambiarNombre(valor: String) = _ingreso.update { if (it.ocupado) it else it.copy(nombre = valor, error = null) }
    fun cambiarClave(valor: String) = _ingreso.update { if (it.ocupado) it else it.copy(clave = valor, error = null) }
    fun seleccionarRol(rol: Rol) = _ingreso.update { if (it.ocupado) it else it.copy(rol = rol, error = null) }

    private fun validar(datos: IngresoUiState): Boolean {
        if (datos.nombre.isNotBlank() && datos.clave.isNotBlank()) return true
        _ingreso.update { it.copy(error = "Completa el usuario y la contraseña.") }
        return false
    }

    fun ingresar(alIngresar: () -> Unit) {
        val servidor = remoto
        if (servidor == null) {
            if (ingresar()) alIngresar()
            return
        }
        val datos = _ingreso.value
        if (datos.ocupado || !validar(datos)) return
        _ingreso.update { it.copy(ocupado = true, error = null) }
        autenticacion = viewModelScope.launch {
            val cuenta = try {
                servidor.ingresar(datos.nombre.trim(), datos.clave, datos.rol)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _ingreso.update {
                    it.copy(ocupado = false, error = e.message ?: "No se pudo iniciar sesión.")
                }
                return@launch
            }
            _usuario.value = cuenta
            _ingreso.value = IngresoUiState()
            alIngresar()
        }
    }

    fun ingresar(): Boolean {
        val datos = _ingreso.value
        if (remoto != null || !validar(datos)) {
            return false
        }
        val valido = ingresar(datos.nombre, datos.clave, datos.rol)
        if (valido) _ingreso.value = IngresoUiState()
        else _ingreso.update { it.copy(error = "Revisa el usuario, la contraseña y el perfil seleccionado.") }
        return valido
    }

    // El perfil seleccionado nunca cambia los permisos de la cuenta.
    fun ingresar(nombre: String, clave: String, rol: Rol? = null): Boolean {
        if (remoto != null) return false
        _usuario.value = usuarios.find {
            it.nombre.equals(nombre.trim(), ignoreCase = true) &&
                it.clave == clave &&
                (rol == null || it.rol == rol)
        }
        return _usuario.value != null
    }

    fun salir() {
        autenticacion?.cancel()
        remoto?.salir()
        _usuario.value = null
        _ingreso.value = IngresoUiState()
    }
}
