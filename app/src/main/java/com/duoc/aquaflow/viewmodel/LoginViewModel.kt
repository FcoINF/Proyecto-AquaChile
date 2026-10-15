package com.duoc.aquaflow.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.duoc.aquaflow.domain.AppError
import com.duoc.aquaflow.domain.AppResult
import com.duoc.aquaflow.model.Rol
import com.duoc.aquaflow.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LoginUiState(
    val rol: Rol = Rol.SUPERVISOR,
    val rut: String = "",
    val pin: String = "",
    val cargando: Boolean = false,
    val errorRut: String? = null,
    val errorPin: String? = null,
    val error: String? = null
)

class LoginViewModel(private val auth: AuthRepository) : ViewModel() {

    private val _state = MutableStateFlow(LoginUiState())
    val state: StateFlow<LoginUiState> = _state.asStateFlow()

    fun onRolChange(rol: Rol) = _state.update { it.copy(rol = rol, error = null) }

    fun onRutChange(valor: String) =
        _state.update { it.copy(rut = valor.filter { c -> c.isLetterOrDigit() || c == '.' || c == '-' }.take(12), errorRut = null, error = null) }

    fun onPinChange(valor: String) =
        _state.update { it.copy(pin = valor.filter { c -> c.isDigit() }.take(4), errorPin = null, error = null) }

    fun ingresar() {
        val actual = _state.value
        if (actual.cargando) return
        _state.update { it.copy(cargando = true, error = null, errorRut = null, errorPin = null) }
        viewModelScope.launch {
            when (val resultado = auth.login(actual.rut, actual.pin, actual.rol)) {
                is AppResult.Ok -> _state.value = LoginUiState(rol = actual.rol)
                is AppResult.Fail -> _state.update { estado ->
                    val error = resultado.error
                    if (error is AppError.Validation) {
                        estado.copy(
                            cargando = false,
                            pin = "",
                            errorRut = error.errores["rut"],
                            errorPin = error.errores["pin"]
                        )
                    } else {
                        estado.copy(cargando = false, pin = "", error = error.mensaje)
                    }
                }
            }
        }
    }
}
