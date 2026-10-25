package com.duoc.aquaflow.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.duoc.aquaflow.domain.ReglasCierre
import com.duoc.aquaflow.model.RegistroCierre
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class CierreUiState(
    val minutos: String = "",
    val observaciones: String = "",
    val revisionSalida: Boolean = false,
    val error: String? = null
)

class CierreFaenaViewModel(private val saved: SavedStateHandle) : ViewModel() {
    private val _ui = MutableStateFlow(CierreUiState(
        saved["minutos"] ?: "", saved["observaciones"] ?: "", saved["revision"] ?: false
    ))
    val ui = _ui.asStateFlow()
    fun minutos(valor: String) = actualizar(_ui.value.copy(minutos = valor.take(10)))
    fun observaciones(valor: String) = actualizar(_ui.value.copy(observaciones = valor.take(1000)))
    fun revision(valor: Boolean) = actualizar(_ui.value.copy(revisionSalida = valor))
    private fun actualizar(estado: CierreUiState) {
        _ui.value = estado.copy(error = null)
        saved["minutos"] = estado.minutos
        saved["observaciones"] = estado.observaciones
        saved["revision"] = estado.revisionSalida
    }
    fun validar(): RegistroCierre? {
        val estado = _ui.value
        val minutos = estado.minutos.trim().toIntOrNull()
        val error = ReglasCierre.errorCampos(minutos, estado.observaciones, estado.revisionSalida)
        _ui.value = estado.copy(error = error)
        if (error != null) return null
        return RegistroCierre(requireNotNull(minutos), estado.observaciones.trim(), estado.revisionSalida)
    }
}
