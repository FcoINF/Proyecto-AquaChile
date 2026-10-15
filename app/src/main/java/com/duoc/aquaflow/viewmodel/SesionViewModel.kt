package com.duoc.aquaflow.viewmodel

import androidx.lifecycle.ViewModel
import com.duoc.aquaflow.model.Rol
import com.duoc.aquaflow.model.Usuario
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SesionViewModel : ViewModel() {

    private val usuarios = listOf(
        Usuario("supervisor", "1234", Rol.SUPERVISOR),
        Usuario("jefe", "1234", Rol.JEFE)
    )

    private val _usuario = MutableStateFlow<Usuario?>(null)
    val usuario: StateFlow<Usuario?> = _usuario.asStateFlow()


    fun ingresar(nombre: String, clave: String): Boolean {
        _usuario.value = usuarios.find { it.nombre == nombre.trim() && it.clave == clave }
        return _usuario.value != null
    }


    fun salir() {
        _usuario.value = null
    }
}